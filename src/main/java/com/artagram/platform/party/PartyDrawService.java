package com.artagram.platform.party;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.common.lock.ConcurrencyMetrics;
import com.artagram.platform.common.lock.DistributedLockService;
import com.artagram.platform.party.dto.BloqueoBorradorResponse;
import com.artagram.platform.party.dto.PartyEventoDto;
import com.artagram.platform.party.dto.PartyTrazoDto;
import com.artagram.platform.party.dto.TrazarEnRondaRequest;
import com.artagram.platform.realtime.RealtimeBroadcaster;
import java.time.Duration;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dibujar durante la ronda (HU-19: todos con el mismo pincel, append-only) y
 * pelear por el borrador (HU-20: exclusion mutua con compare-and-set).
 */
@Service
public class PartyDrawService {

    private static final Duration TTL_BORRADOR = Duration.ofSeconds(10);
    private static final String SEPARADOR = "::";

    private final SalaFiestaRepository salaRepository;
    private final ParticipanteSalaRepository participanteRepository;
    private final PartyRondaEstadoRepository rondaEstadoRepository;
    private final PartyTrazoRepository partyTrazoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DistributedLockService lockService;
    private final ConcurrencyMetrics metrics;
    private final RealtimeBroadcaster broadcaster;
    private final PartyProperties properties;

    public PartyDrawService(SalaFiestaRepository salaRepository, ParticipanteSalaRepository participanteRepository,
                             PartyRondaEstadoRepository rondaEstadoRepository, PartyTrazoRepository partyTrazoRepository,
                             UsuarioRepository usuarioRepository, DistributedLockService lockService,
                             ConcurrencyMetrics metrics, RealtimeBroadcaster broadcaster, PartyProperties properties) {
        this.salaRepository = salaRepository;
        this.participanteRepository = participanteRepository;
        this.rondaEstadoRepository = rondaEstadoRepository;
        this.partyTrazoRepository = partyTrazoRepository;
        this.usuarioRepository = usuarioRepository;
        this.lockService = lockService;
        this.metrics = metrics;
        this.broadcaster = broadcaster;
        this.properties = properties;
    }

    @Transactional
    public void trazar(UUID salaId, UUID usuarioId, String nombreUsuario, TrazarEnRondaRequest request) {
        SalaFiesta sala = exigirRondaActiva(salaId, usuarioId);
        long secuencia = siguienteSecuencia(salaId, sala.getNumeroRonda());
        Usuario autor = usuarioRepository.getReferenceById(usuarioId);
        com.artagram.platform.canvas.TipoPincel pincel = request.tipoPincel() != null ? request.tipoPincel() : sala.getPincelActual();
        String color = request.colorHex() != null ? request.colorHex() : sala.getColorActual();
        double grosor = request.grosor() != null ? request.grosor() : sala.getGrosorActual();
        double opacidad = request.opacidad() != null ? request.opacidad() : sala.getOpacidadActual();
        PartyTrazo trazo = new PartyTrazo(salaId, sala.getNumeroRonda(), secuencia, autor, false,
                pincel, color, grosor, opacidad, request.puntos());
        partyTrazoRepository.save(trazo);
        publicar(salaId, trazo);
    }

    public BloqueoBorradorResponse tomarBorrador(UUID salaId, UUID usuarioId, String nombreUsuario) {
        if (!participanteRepository.existsBySala_IdAndUsuario_IdAndActivoTrue(salaId, usuarioId)) {
            throw ApiException.forbidden("NO_ERES_PARTICIPANTE", "No perteneces a esta sala");
        }
        String clave = claveBorrador(salaId);
        String propietario = propietario(usuarioId, nombreUsuario);
        boolean adquirido = lockService.adquirir(clave, propietario, TTL_BORRADOR);
        if (adquirido) {
            broadcaster.enviar("/topic/fiesta/" + salaId, PartyEventoDto.borradorTomado(nombreUsuario));
            return new BloqueoBorradorResponse(true, nombreUsuario);
        }
        metrics.registrarBloqueoNegado();
        metrics.registrarDisputaBorrador();
        String actual = lockService.propietarioActual(clave).map(this::extraerNombre).orElse("alguien");
        return new BloqueoBorradorResponse(false, actual);
    }

    public void parcial(UUID salaId, UUID usuarioId, String nombreUsuario, TrazarEnRondaRequest request) {
        if (!participanteRepository.existsBySala_IdAndUsuario_IdAndActivoTrue(salaId, usuarioId)) {
            return;
        }
        PartyTrazoDto dto = new PartyTrazoDto(0, usuarioId, nombreUsuario, false,
                request.tipoPincel(), request.colorHex(), request.grosor(), request.opacidad(), request.puntos());
        broadcaster.enviar("/topic/fiesta/" + salaId, PartyEventoDto.trazoParcial(dto));
    }

    @Transactional
    public void borrar(UUID salaId, UUID usuarioId, String nombreUsuario, TrazarEnRondaRequest request) {
        SalaFiesta sala = exigirRondaActiva(salaId, usuarioId);
        String actualHerramienta = lockService.propietarioActual(claveHerramienta(salaId, "BORRADOR")).orElse(null);
        String actualBorrador = lockService.propietarioActual(claveBorrador(salaId)).orElse(null);
        String miPropietario = propietario(usuarioId, nombreUsuario);
        if (!miPropietario.equals(actualHerramienta) && !miPropietario.equals(actualBorrador)) {
            throw ApiException.forbidden("NO_TIENES_EL_BORRADOR", "No tienes el borrador en este momento");
        }
        long secuencia = siguienteSecuencia(salaId, sala.getNumeroRonda());
        Usuario autor = usuarioRepository.getReferenceById(usuarioId);
        PartyTrazo trazo = new PartyTrazo(salaId, sala.getNumeroRonda(), secuencia, autor, true,
                null, null, 40.0, 1.0, request.puntos());
        partyTrazoRepository.save(trazo);
        publicar(salaId, trazo);
    }

    public void liberarBorrador(UUID salaId, UUID usuarioId, String nombreUsuario) {
        String propietario = propietario(usuarioId, nombreUsuario);
        lockService.liberar(claveHerramienta(salaId, "BORRADOR"), propietario);
        if (lockService.liberar(claveBorrador(salaId), propietario)) {
            broadcaster.enviar("/topic/fiesta/" + salaId, PartyEventoDto.borradorLiberado());
        }
    }

    private SalaFiesta exigirRondaActiva(UUID salaId, UUID usuarioId) {
        if (!participanteRepository.existsBySala_IdAndUsuario_IdAndActivoTrue(salaId, usuarioId)) {
            throw ApiException.forbidden("NO_ERES_PARTICIPANTE", "No perteneces a esta sala");
        }
        SalaFiesta sala = salaRepository.findById(salaId)
                .orElseThrow(() -> ApiException.notFound("SALA_NO_ENCONTRADA", "Esa sala no existe"));
        if (sala.getNumeroRonda() == 0) {
            sala.iniciarNuevaRonda(com.artagram.platform.canvas.TipoPincel.LAPIZ, "#FF6B57", 20.0, 1.0);
            rondaEstadoRepository.save(new PartyRondaEstado(sala.getId(), sala.getNumeroRonda()));
        } else if (sala.getEstado() != EstadoSala.EN_RONDA) {
            sala.setEstado(EstadoSala.EN_RONDA);
        }
        return sala;
    }

    private long siguienteSecuencia(UUID salaId, int numeroRonda) {
        PartyRondaEstado estado = rondaEstadoRepository.buscarParaActualizar(salaId, numeroRonda)
                .orElseThrow(() -> ApiException.notFound("RONDA_NO_ENCONTRADA", "Esa ronda no existe"));
        return estado.siguienteSecuencia();
    }

    private void publicar(UUID salaId, PartyTrazo trazo) {
        PartyTrazoDto dto = new PartyTrazoDto(trazo.getSecuencia(), trazo.getAutor().getId(),
                trazo.getAutor().getNombreArtista(), trazo.isBorrador(), trazo.getTipoPincel(), trazo.getColorHex(),
                trazo.getGrosor(), trazo.getOpacidad(), trazo.getPuntos());
        broadcaster.enviar("/topic/fiesta/" + salaId, PartyEventoDto.trazoNuevo(dto));
    }

    public com.artagram.platform.party.dto.BloqueoHerramientaResponse tomarHerramienta(UUID salaId, UUID usuarioId, String nombreUsuario, String herramienta) {
        if (!participanteRepository.existsBySala_IdAndUsuario_IdAndActivoTrue(salaId, usuarioId)) {
            throw ApiException.forbidden("NO_ERES_PARTICIPANTE", "No perteneces a esta sala");
        }
        String clave = claveHerramienta(salaId, herramienta);
        String propietario = propietario(usuarioId, nombreUsuario);
        boolean adquirido = lockService.adquirir(clave, propietario, Duration.ofSeconds(60));
        if (adquirido) {
            if ("BORRADOR".equalsIgnoreCase(herramienta)) {
                lockService.adquirir(claveBorrador(salaId), propietario, Duration.ofSeconds(60));
            }
            broadcaster.enviar("/topic/fiesta/" + salaId, PartyEventoDto.herramientaTomada(herramienta.toUpperCase(), nombreUsuario));
            return new com.artagram.platform.party.dto.BloqueoHerramientaResponse(true, herramienta.toUpperCase(), nombreUsuario);
        }
        metrics.registrarBloqueoNegado();
        String actual = lockService.propietarioActual(clave).map(this::extraerNombre).orElse("alguien");
        return new com.artagram.platform.party.dto.BloqueoHerramientaResponse(false, herramienta.toUpperCase(), actual);
    }

    public void liberarHerramienta(UUID salaId, UUID usuarioId, String nombreUsuario, String herramienta) {
        String clave = claveHerramienta(salaId, herramienta);
        String propietario = propietario(usuarioId, nombreUsuario);
        if ("BORRADOR".equalsIgnoreCase(herramienta)) {
            lockService.liberar(claveBorrador(salaId), propietario);
        }
        if (lockService.liberar(clave, propietario)) {
            broadcaster.enviar("/topic/fiesta/" + salaId, PartyEventoDto.herramientaLiberada(herramienta.toUpperCase()));
        }
    }

    public java.util.Map<String, String> consultarHerramientas(UUID salaId) {
        java.util.Map<String, String> mapa = new java.util.LinkedHashMap<>();
        for (String h : java.util.List.of("LAPIZ", "MARCADOR", "ACUARELA", "PLUMA", "AEROGRAFO", "BORRADOR")) {
            lockService.propietarioActual(claveHerramienta(salaId, h))
                    .ifPresent(p -> mapa.put(h, extraerNombre(p)));
        }
        return mapa;
    }

    private String claveHerramienta(UUID salaId, String herramienta) {
        return "fiesta-herramienta:" + salaId + ":" + herramienta.toUpperCase();
    }

    private String claveBorrador(UUID salaId) {
        return "fiesta-borrador:" + salaId;
    }

    private String propietario(UUID usuarioId, String nombreUsuario) {
        return usuarioId + SEPARADOR + nombreUsuario;
    }

    private String extraerNombre(String propietario) {
        int idx = propietario.indexOf(SEPARADOR);
        return idx >= 0 ? propietario.substring(idx + SEPARADOR.length()) : propietario;
    }
}

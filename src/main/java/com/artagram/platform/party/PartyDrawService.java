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
        PartyTrazo trazo = new PartyTrazo(salaId, sala.getNumeroRonda(), secuencia, autor, false,
                sala.getPincelActual(), sala.getColorActual(), sala.getGrosorActual(), sala.getOpacidadActual(), request.puntos());
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

    @Transactional
    public void borrar(UUID salaId, UUID usuarioId, String nombreUsuario, TrazarEnRondaRequest request) {
        SalaFiesta sala = exigirRondaActiva(salaId, usuarioId);
        String actual = lockService.propietarioActual(claveBorrador(salaId)).orElse(null);
        if (actual == null || !actual.equals(propietario(usuarioId, nombreUsuario))) {
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
        if (lockService.liberar(claveBorrador(salaId), propietario(usuarioId, nombreUsuario))) {
            broadcaster.enviar("/topic/fiesta/" + salaId, PartyEventoDto.borradorLiberado());
        }
    }

    private SalaFiesta exigirRondaActiva(UUID salaId, UUID usuarioId) {
        if (!participanteRepository.existsBySala_IdAndUsuario_IdAndActivoTrue(salaId, usuarioId)) {
            throw ApiException.forbidden("NO_ERES_PARTICIPANTE", "No perteneces a esta sala");
        }
        SalaFiesta sala = salaRepository.findById(salaId)
                .orElseThrow(() -> ApiException.notFound("SALA_NO_ENCONTRADA", "Esa sala no existe"));
        if (sala.getEstado() != EstadoSala.EN_RONDA) {
            throw ApiException.conflict("RONDA_NO_ACTIVA", "La ronda no esta activa ahora mismo");
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

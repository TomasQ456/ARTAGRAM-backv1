package com.artagram.platform.party;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.billing.QuotaService;
import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.party.dto.CrearSalaRequest;
import com.artagram.platform.party.dto.EstadoRondaResponse;
import com.artagram.platform.party.dto.PartyEventoDto;
import com.artagram.platform.party.dto.PartyTrazoDto;
import com.artagram.platform.party.dto.SalaFiestaResponse;
import com.artagram.platform.realtime.RealtimeBroadcaster;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Salas de modo fiesta (HU-18) y control de sus rondas (HU-19). El estado
 * "quien esta en la sala" y su configuracion vive en Postgres (el respaldo);
 * el reloj vivo de la ronda vive en Redis via PartyRoundClock.
 */
@Service
public class SalaFiestaService {

    private static final Logger log = LoggerFactory.getLogger(SalaFiestaService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALFABETO_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final SalaFiestaRepository salaRepository;
    private final ParticipanteSalaRepository participanteRepository;
    private final PartyRondaEstadoRepository rondaEstadoRepository;
    private final PartyTrazoRepository partyTrazoRepository;
    private final UsuarioRepository usuarioRepository;
    private final QuotaService quotaService;
    private final PartyRoundClock roundClock;
    private final SeleccionadorPincelRonda seleccionadorPincel;
    private final RealtimeBroadcaster broadcaster;

    public SalaFiestaService(SalaFiestaRepository salaRepository, ParticipanteSalaRepository participanteRepository,
                              PartyRondaEstadoRepository rondaEstadoRepository, PartyTrazoRepository partyTrazoRepository,
                              UsuarioRepository usuarioRepository, QuotaService quotaService, PartyRoundClock roundClock,
                              SeleccionadorPincelRonda seleccionadorPincel,
                              RealtimeBroadcaster broadcaster) {
        this.salaRepository = salaRepository;
        this.participanteRepository = participanteRepository;
        this.rondaEstadoRepository = rondaEstadoRepository;
        this.partyTrazoRepository = partyTrazoRepository;
        this.usuarioRepository = usuarioRepository;
        this.quotaService = quotaService;
        this.roundClock = roundClock;
        this.seleccionadorPincel = seleccionadorPincel;
        this.broadcaster = broadcaster;
    }

    @Transactional
    public SalaFiestaResponse crear(UUID usuarioId, CrearSalaRequest request) {
        quotaService.reservarCupoSalaFiesta(usuarioId);
        Usuario anfitrion = usuarioRepository.getReferenceById(usuarioId);
        String codigo = generarCodigoUnico();
        SalaFiesta sala = salaRepository.save(new SalaFiesta(codigo, anfitrion, request.duracionRondaSegundos(), request.cupoMaximo()));
        participanteRepository.save(new ParticipanteSala(sala, anfitrion));
        log.info("Sala de fiesta creada: id={} codigo={} anfitrion={} cupo={}", sala.getId(), codigo, usuarioId, request.cupoMaximo());
        return aRespuesta(sala);
    }

    @Transactional
    public SalaFiestaResponse unirse(UUID usuarioId, String codigo) {
        SalaFiesta sala = salaRepository.findByCodigo(codigo.toUpperCase())
                .orElseThrow(() -> ApiException.notFound("SALA_NO_ENCONTRADA", "No existe una sala con ese codigo"));
        Optional<ParticipanteSala> existente = participanteRepository.findBySala_IdAndUsuario_Id(sala.getId(), usuarioId);
        if (existente.isPresent() && existente.get().isActivo()) {
            return aRespuesta(sala);
        }
        long activos = participanteRepository.countBySala_IdAndActivoTrue(sala.getId());
        if (activos >= sala.getCupoMaximo()) {
            throw ApiException.conflict("SALA_LLENA", "La sala esta llena");
        }
        if (existente.isPresent()) {
            existente.get().reactivar();
        } else {
            Usuario usuario = usuarioRepository.getReferenceById(usuarioId);
            participanteRepository.save(new ParticipanteSala(sala, usuario));
        }
        return aRespuesta(sala);
    }

    @Transactional
    public void iniciarRonda(UUID usuarioId, UUID salaId) {
        SalaFiesta sala = salaRepository.buscarParaActualizar(salaId)
                .orElseThrow(() -> ApiException.notFound("SALA_NO_ENCONTRADA", "Esa sala no existe"));
        if (!sala.getAnfitrion().getId().equals(usuarioId)) {
            throw ApiException.forbidden("SOLO_ANFITRION", "Solo el anfitrion puede iniciar la ronda");
        }
        SeleccionadorPincelRonda.EstiloRonda estilo = seleccionadorPincel.seleccionar();
        sala.iniciarNuevaRonda(estilo.pincel(), estilo.colorHex(), estilo.grosor(), estilo.opacidad());
        rondaEstadoRepository.save(new PartyRondaEstado(sala.getId(), sala.getNumeroRonda()));
        roundClock.iniciarRonda(sala.getId(), sala.getDuracionRondaSegundos() * 1000L);
        log.info("Ronda {} iniciada en sala {}: pincel={} color={}", sala.getNumeroRonda(), salaId, estilo.pincel(), estilo.colorHex());
    }

    @Transactional
    public void terminarRonda(UUID salaId) {
        salaRepository.buscarParaActualizar(salaId).ifPresent(SalaFiesta::congelar);
        roundClock.terminarRonda(salaId);
        log.info("Ronda terminada en sala {}", salaId);
    }

    @Transactional(readOnly = true)
    public EstadoRondaResponse obtenerEstado(UUID salaId, String propietarioBorrador) {
        SalaFiesta sala = salaRepository.findById(salaId)
                .orElseThrow(() -> ApiException.notFound("SALA_NO_ENCONTRADA", "Esa sala no existe"));
        List<PartyTrazoDto> trazos = partyTrazoRepository.findBySalaIdAndNumeroRondaOrderBySecuenciaAsc(salaId, sala.getNumeroRonda())
                .stream()
                .map(t -> new PartyTrazoDto(t.getSecuencia(), t.getAutor().getId(), t.getAutor().getNombreArtista(),
                        t.isBorrador(), t.getTipoPincel(), t.getColorHex(), t.getGrosor(), t.getOpacidad(), t.getPuntos()))
                .toList();
        long msRestantes = sala.getEstado() == EstadoSala.EN_RONDA ? roundClock.msRestantes(salaId) : 0;
        return new EstadoRondaResponse(sala.getEstado().name(), sala.getNumeroRonda(), msRestantes,
                sala.getPincelActual(), sala.getColorActual(), sala.getGrosorActual(), sala.getOpacidadActual(),
                propietarioBorrador, trazos);
    }

    @Transactional(readOnly = true)
    public boolean esParticipanteActivo(UUID usuarioId, UUID salaId) {
        return participanteRepository.existsBySala_IdAndUsuario_IdAndActivoTrue(salaId, usuarioId);
    }

    @Transactional(readOnly = true)
    public SalaFiestaResponse obtenerRespuesta(UUID salaId) {
        SalaFiesta sala = salaRepository.findById(salaId)
                .orElseThrow(() -> ApiException.notFound("SALA_NO_ENCONTRADA", "Esa sala no existe"));
        return aRespuesta(sala);
    }

    void notificarFinDeRonda(UUID salaId) {
        broadcaster.enviar("/topic/fiesta/" + salaId, PartyEventoDto.rondaTerminada());
    }

    void notificarTick(UUID salaId, long msRestantes) {
        broadcaster.enviar("/topic/fiesta/" + salaId, PartyEventoDto.tick(msRestantes));
    }

    void notificarBorradorDisponible(UUID salaId) {
        broadcaster.enviar("/topic/fiesta/" + salaId, PartyEventoDto.borradorDisponible());
    }

    private String generarCodigoUnico() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(ALFABETO_CODIGO.charAt(RANDOM.nextInt(ALFABETO_CODIGO.length())));
            }
            codigo = sb.toString();
        } while (salaRepository.existsByCodigo(codigo));
        return codigo;
    }

    private SalaFiestaResponse aRespuesta(SalaFiesta sala) {
        long total = participanteRepository.countBySala_IdAndActivoTrue(sala.getId());
        return new SalaFiestaResponse(sala.getId(), sala.getCodigo(), sala.getAnfitrion().getNombreArtista(),
                sala.getDuracionRondaSegundos(), sala.getCupoMaximo(), sala.getEstado().name(), total, sala.getNumeroRonda());
    }
}

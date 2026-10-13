package com.artagram.platform.chat;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.chat.dto.ConversacionResumenDto;
import com.artagram.platform.chat.dto.EnviarMensajePersonalRequest;
import com.artagram.platform.chat.dto.MensajePersonalDto;
import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.realtime.PresenceService;
import com.artagram.platform.realtime.RealtimeBroadcaster;
import com.artagram.platform.realtime.StompPrincipal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/**
 * Chat personal (HU-21): se guarda antes de publicarse, para que un fallo del
 * canal no lo pierda; si el destinatario esta conectado se entrega de una,
 * si no, le llega apenas se conecta (ver alConectar).
 */
@Service
public class ChatPersonalService {

    private static final Logger log = LoggerFactory.getLogger(ChatPersonalService.class);

    private final MensajePersonalRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final PresenceService presenceService;
    private final RealtimeBroadcaster broadcaster;

    public ChatPersonalService(MensajePersonalRepository mensajeRepository, UsuarioRepository usuarioRepository,
                                PresenceService presenceService, RealtimeBroadcaster broadcaster) {
        this.mensajeRepository = mensajeRepository;
        this.usuarioRepository = usuarioRepository;
        this.presenceService = presenceService;
        this.broadcaster = broadcaster;
    }

    @Transactional
    public MensajePersonalDto enviar(UUID remitenteId, String remitenteNombre, EnviarMensajePersonalRequest request) {
        if (remitenteId.equals(request.destinatarioId())) {
            throw ApiException.badRequest("DESTINATARIO_INVALIDO", "No puedes mandarte mensajes a ti mismo");
        }
        Usuario remitente = usuarioRepository.getReferenceById(remitenteId);
        Usuario destinatario = usuarioRepository.findById(request.destinatarioId())
                .orElseThrow(() -> ApiException.notFound("DESTINATARIO_NO_ENCONTRADO", "Ese artista no existe"));

        MensajePersonal mensaje = new MensajePersonal(remitente, destinatario, request.contenido());
        boolean conectado = presenceService.estaConectado(request.destinatarioId());
        if (conectado) {
            mensaje.marcarEntregado();
        }
        mensaje = mensajeRepository.save(mensaje);
        log.debug("Mensaje personal enviado: id={} de={} para={} entregadoDeInmediato={}",
                mensaje.getId(), remitenteId, request.destinatarioId(), conectado);

        MensajePersonalDto dto = aDto(mensaje);
        if (conectado) {
            broadcaster.enviarAUsuario(request.destinatarioId().toString(), "/queue/mensajes", dto);
        }
        broadcaster.enviarAUsuario(remitenteId.toString(), "/queue/mensajes", dto);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<MensajePersonalDto> conversacion(UUID usuarioA, UUID usuarioB) {
        return mensajeRepository.conversacion(usuarioA, usuarioB).stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ConversacionResumenDto> listarConversaciones(UUID usuarioId) {
        List<MensajePersonal> todos = mensajeRepository.mensajesDelUsuario(usuarioId);
        Map<UUID, ConversacionResumenDto> resumenes = new LinkedHashMap<>();
        for (MensajePersonal m : todos) {
            boolean soyRemitente = m.getRemitente().getId().equals(usuarioId);
            Usuario otro = soyRemitente ? m.getDestinatario() : m.getRemitente();
            if (!resumenes.containsKey(otro.getId())) {
                resumenes.put(otro.getId(), new ConversacionResumenDto(
                        otro.getId(),
                        otro.getNombreArtista(),
                        m.getContenido(),
                        m.getCreadoEn(),
                        m.isEntregado()
                ));
            }
        }
        return new ArrayList<>(resumenes.values());
    }

    /**
     * En cuanto alguien se suscribe a su cola personal, le llegan en orden los
     * mensajes que se le acumularon mientras estaba desconectado (HU-21,
     * escenario 2). Se dispara en el SUBSCRIBE, no en el CONNECT: si se hiciera
     * en el CONNECT, el push podria salir antes de que el cliente alcance a
     * suscribirse y se perderia (nadie estaria escuchando todavia).
     */
    @EventListener
    @Transactional
    public void alSuscribirse(SessionSubscribeEvent event) {
        if (!(event.getUser() instanceof StompPrincipal principal)) {
            return;
        }
        String destino = SimpMessageHeaderAccessor.wrap(event.getMessage()).getDestination();
        if (!"/user/queue/mensajes".equals(destino)) {
            return;
        }
        UUID usuarioId = principal.usuarioId();
        List<MensajePersonal> pendientes = mensajeRepository.findByDestinatario_IdAndEntregadoFalseOrderByOrdenAsc(usuarioId);
        for (MensajePersonal mensaje : pendientes) {
            mensaje.marcarEntregado();
            broadcaster.enviarAUsuario(usuarioId.toString(), "/queue/mensajes", aDto(mensaje));
        }
    }

    private MensajePersonalDto aDto(MensajePersonal mensaje) {
        return new MensajePersonalDto(mensaje.getId(), mensaje.getRemitente().getId(), mensaje.getRemitente().getNombreArtista(),
                mensaje.getDestinatario().getId(), mensaje.getContenido(), mensaje.isEntregado(), mensaje.getCreadoEn());
    }
}

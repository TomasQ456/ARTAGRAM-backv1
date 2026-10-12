package com.artagram.platform.chat;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.chat.dto.EnviarMensajeComunidadRequest;
import com.artagram.platform.chat.dto.MensajeComunidadDto;
import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.community.ComunidadService;
import com.artagram.platform.realtime.RealtimeBroadcaster;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chat de comunidad (HU-22): el permiso se revisa en CADA mensaje, no solo al
 * conectarse, para que a alguien recien expulsado no le siga llegando ni
 * dejandole escribir.
 */
@Service
public class ComunidadChatService {

    private static final Logger log = LoggerFactory.getLogger(ComunidadChatService.class);

    private final MensajeComunidadRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComunidadService comunidadService;
    private final RealtimeBroadcaster broadcaster;

    public ComunidadChatService(MensajeComunidadRepository mensajeRepository, UsuarioRepository usuarioRepository,
                                 ComunidadService comunidadService, RealtimeBroadcaster broadcaster) {
        this.mensajeRepository = mensajeRepository;
        this.usuarioRepository = usuarioRepository;
        this.comunidadService = comunidadService;
        this.broadcaster = broadcaster;
    }

    @Transactional
    public MensajeComunidadDto enviar(UUID comunidadId, UUID remitenteId, EnviarMensajeComunidadRequest request) {
        if (!comunidadService.esMiembroActivo(remitenteId, comunidadId)) {
            log.warn("Mensaje de comunidad rechazado (no es miembro activo): comunidad={} remitente={}", comunidadId, remitenteId);
            throw ApiException.forbidden("SIN_PERMISO", "No perteneces a esta comunidad (o te expulsaron)");
        }
        Usuario remitente = usuarioRepository.getReferenceById(remitenteId);
        MensajeComunidad mensaje = mensajeRepository.save(new MensajeComunidad(comunidadId, remitente, request.contenido()));
        log.debug("Mensaje de comunidad enviado: id={} comunidad={} remitente={}", mensaje.getId(), comunidadId, remitenteId);
        MensajeComunidadDto dto = aDto(mensaje);
        broadcaster.enviar("/topic/comunidad/" + comunidadId + "/chat", dto);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<MensajeComunidadDto> historial(UUID comunidadId, UUID usuarioId) {
        if (!comunidadService.esMiembroActivo(usuarioId, comunidadId)) {
            throw ApiException.forbidden("NO_ERES_MIEMBRO", "No perteneces a esta comunidad");
        }
        return mensajeRepository.findByComunidadIdOrderByOrdenAsc(comunidadId).stream().map(this::aDto).toList();
    }

    private MensajeComunidadDto aDto(MensajeComunidad mensaje) {
        return new MensajeComunidadDto(mensaje.getId(), mensaje.getRemitente().getId(),
                mensaje.getRemitente().getNombreArtista(), mensaje.getContenido(), mensaje.getCreadoEn());
    }
}

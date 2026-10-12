package com.artagram.platform.chat;

import com.artagram.platform.chat.dto.EnviarMensajeComunidadRequest;
import com.artagram.platform.realtime.RealtimeBroadcaster;
import com.artagram.platform.realtime.StompPrincipal;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class ComunidadChatStompController {

    private static final Logger log = LoggerFactory.getLogger(ComunidadChatStompController.class);

    private final ComunidadChatService comunidadChatService;
    private final RealtimeBroadcaster broadcaster;

    public ComunidadChatStompController(ComunidadChatService comunidadChatService, RealtimeBroadcaster broadcaster) {
        this.comunidadChatService = comunidadChatService;
        this.broadcaster = broadcaster;
    }

    @MessageMapping("/comunidad/{comunidadId}/chat/enviar")
    public void enviar(@DestinationVariable UUID comunidadId, @Valid @Payload EnviarMensajeComunidadRequest request, Principal principal) {
        StompPrincipal p = (StompPrincipal) principal;
        comunidadChatService.enviar(comunidadId, p.usuarioId(), request);
    }

    @MessageExceptionHandler
    public void manejarError(Exception ex, Principal principal) {
        log.warn("Error procesando mensaje de comunidad de {}: {}", principal != null ? principal.getName() : "?", ex.getMessage());
        if (principal != null) {
            broadcaster.enviarAUsuario(principal.getName(), "/queue/errores",
                    Map.of("mensaje", ex.getMessage() == null ? "Solicitud invalida" : ex.getMessage()));
        }
    }
}

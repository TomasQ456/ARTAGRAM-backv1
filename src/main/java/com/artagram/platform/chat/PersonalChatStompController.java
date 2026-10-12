package com.artagram.platform.chat;

import com.artagram.platform.chat.dto.EnviarMensajePersonalRequest;
import com.artagram.platform.realtime.RealtimeBroadcaster;
import com.artagram.platform.realtime.StompPrincipal;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class PersonalChatStompController {

    private static final Logger log = LoggerFactory.getLogger(PersonalChatStompController.class);

    private final ChatPersonalService chatPersonalService;
    private final RealtimeBroadcaster broadcaster;

    public PersonalChatStompController(ChatPersonalService chatPersonalService, RealtimeBroadcaster broadcaster) {
        this.chatPersonalService = chatPersonalService;
        this.broadcaster = broadcaster;
    }

    @MessageMapping("/mensajes/enviar")
    public void enviar(@Valid @Payload EnviarMensajePersonalRequest request, Principal principal) {
        StompPrincipal p = (StompPrincipal) principal;
        chatPersonalService.enviar(p.usuarioId(), p.nombreArtista(), request);
    }

    @MessageExceptionHandler
    public void manejarError(Exception ex, Principal principal) {
        log.warn("Error procesando mensaje personal de {}: {}", principal != null ? principal.getName() : "?", ex.getMessage());
        if (principal != null) {
            broadcaster.enviarAUsuario(principal.getName(), "/queue/errores",
                    Map.of("mensaje", ex.getMessage() == null ? "Solicitud invalida" : ex.getMessage()));
        }
    }
}

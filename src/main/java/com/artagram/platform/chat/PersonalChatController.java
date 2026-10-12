package com.artagram.platform.chat;

import com.artagram.platform.auth.JwtService;
import com.artagram.platform.chat.dto.EnviarMensajePersonalRequest;
import com.artagram.platform.chat.dto.MensajePersonalDto;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

/** HU-21: chat personal, tambien disponible por REST ademas del canal STOMP. */
@RestController
@Tag(name = "Chat", description = "HU-21/22: chat personal y de comunidad")
public class PersonalChatController {

    private final ChatPersonalService chatPersonalService;

    public PersonalChatController(ChatPersonalService chatPersonalService) {
        this.chatPersonalService = chatPersonalService;
    }

    @PostMapping("/api/mensajes")
    public ResponseEntity<MensajePersonalDto> enviar(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                                       @Valid @RequestBody EnviarMensajePersonalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatPersonalService.enviar(principal.usuarioId(), principal.nombreArtista(), request));
    }

    @GetMapping("/api/mensajes/con/{otroUsuarioId}")
    public List<MensajePersonalDto> conversacion(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                                  @PathVariable UUID otroUsuarioId) {
        return chatPersonalService.conversacion(principal.usuarioId(), otroUsuarioId);
    }
}

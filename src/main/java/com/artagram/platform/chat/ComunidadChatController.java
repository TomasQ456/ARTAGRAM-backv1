package com.artagram.platform.chat;

import com.artagram.platform.auth.JwtService;
import com.artagram.platform.chat.dto.EnviarMensajeComunidadRequest;
import com.artagram.platform.chat.dto.MensajeComunidadDto;
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

@RestController
@Tag(name = "Chat", description = "HU-21/22: chat personal y de comunidad")
public class ComunidadChatController {

    private final ComunidadChatService comunidadChatService;

    public ComunidadChatController(ComunidadChatService comunidadChatService) {
        this.comunidadChatService = comunidadChatService;
    }

    @PostMapping("/api/comunidades/{comunidadId}/chat")
    public ResponseEntity<MensajeComunidadDto> enviar(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                                        @PathVariable UUID comunidadId,
                                                        @Valid @RequestBody EnviarMensajeComunidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comunidadChatService.enviar(comunidadId, principal.usuarioId(), request));
    }

    @GetMapping("/api/comunidades/{comunidadId}/chat")
    public List<MensajeComunidadDto> historial(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                                @PathVariable UUID comunidadId) {
        return comunidadChatService.historial(comunidadId, principal.usuarioId());
    }
}

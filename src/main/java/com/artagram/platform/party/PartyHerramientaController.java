package com.artagram.platform.party;

import com.artagram.platform.auth.JwtService;
import com.artagram.platform.party.dto.BloqueoHerramientaResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Modo fiesta", description = "Herramientas exclusivas por participante durante la ronda")
public class PartyHerramientaController {

    private final PartyDrawService partyDrawService;

    public PartyHerramientaController(PartyDrawService partyDrawService) {
        this.partyDrawService = partyDrawService;
    }

    @PostMapping("/api/fiestas/{salaId}/herramientas/{herramienta}")
    public ResponseEntity<BloqueoHerramientaResponse> tomar(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                                             @PathVariable UUID salaId,
                                                             @PathVariable String herramienta) {
        BloqueoHerramientaResponse respuesta = partyDrawService.tomarHerramienta(salaId, principal.usuarioId(), principal.nombreArtista(), herramienta);
        return ResponseEntity.status(respuesta.tomado() ? HttpStatus.OK : HttpStatus.CONFLICT).body(respuesta);
    }

    @DeleteMapping("/api/fiestas/{salaId}/herramientas/{herramienta}")
    public ResponseEntity<Void> liberar(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                         @PathVariable UUID salaId,
                                         @PathVariable String herramienta) {
        partyDrawService.liberarHerramienta(salaId, principal.usuarioId(), principal.nombreArtista(), herramienta);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/fiestas/{salaId}/herramientas")
    public Map<String, String> consultar(@PathVariable UUID salaId) {
        return partyDrawService.consultarHerramientas(salaId);
    }
}

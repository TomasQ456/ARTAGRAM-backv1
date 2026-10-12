package com.artagram.platform.party;

import com.artagram.platform.auth.JwtService;
import com.artagram.platform.party.dto.BloqueoBorradorResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

/** HU-20: exclusion mutua sobre el borrador del modo fiesta. */
@RestController
@Tag(name = "Modo fiesta", description = "HU-18/19/20: salas, rondas cronometradas y borrador disputado")
public class PartyBorradorController {

    private final PartyDrawService partyDrawService;

    public PartyBorradorController(PartyDrawService partyDrawService) {
        this.partyDrawService = partyDrawService;
    }

    @PostMapping("/api/fiestas/{salaId}/borrador")
    public ResponseEntity<BloqueoBorradorResponse> tomar(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                                          @PathVariable UUID salaId) {
        BloqueoBorradorResponse respuesta = partyDrawService.tomarBorrador(salaId, principal.usuarioId(), principal.nombreArtista());
        return ResponseEntity.status(respuesta.tomado() ? HttpStatus.OK : HttpStatus.CONFLICT).body(respuesta);
    }

    @DeleteMapping("/api/fiestas/{salaId}/borrador")
    public ResponseEntity<Void> liberar(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                         @PathVariable UUID salaId) {
        partyDrawService.liberarBorrador(salaId, principal.usuarioId(), principal.nombreArtista());
        return ResponseEntity.noContent().build();
    }
}

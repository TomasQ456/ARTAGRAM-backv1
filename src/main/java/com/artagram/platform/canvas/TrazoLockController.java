package com.artagram.platform.canvas;

import com.artagram.platform.auth.JwtService;
import com.artagram.platform.canvas.dto.BloqueoTrazoResponse;
import com.artagram.platform.canvas.dto.EditarTrazoRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Exclusion mutua por trazo (HU-15).
 */
@RestController
@Tag(name = "Lienzo colaborativo", description = "HU-12/13/15: dibujo en tiempo real, snapshot y bloqueo de trazos")
public class TrazoLockController {

    private final TrazoLockService trazoLockService;

    public TrazoLockController(TrazoLockService trazoLockService) {
        this.trazoLockService = trazoLockService;
    }

    @PostMapping("/api/comunidades/{comunidadId}/lienzo/trazos/{trazoId}/bloqueo")
    public ResponseEntity<BloqueoTrazoResponse> tomar(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                                       @PathVariable UUID comunidadId, @PathVariable UUID trazoId) {
        BloqueoTrazoResponse respuesta = trazoLockService.tomar(comunidadId, trazoId, principal.usuarioId(), principal.nombreArtista());
        return ResponseEntity.status(respuesta.tomado() ? HttpStatus.OK : HttpStatus.CONFLICT).body(respuesta);
    }

    @PutMapping("/api/comunidades/{comunidadId}/lienzo/trazos/{trazoId}/bloqueo")
    public ResponseEntity<Void> renovar(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                         @PathVariable UUID comunidadId, @PathVariable UUID trazoId) {
        boolean renovado = trazoLockService.renovar(trazoId, principal.usuarioId(), principal.nombreArtista());
        return renovado ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.GONE).build();
    }

    @DeleteMapping("/api/comunidades/{comunidadId}/lienzo/trazos/{trazoId}/bloqueo")
    public ResponseEntity<Void> liberar(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                         @PathVariable UUID comunidadId, @PathVariable UUID trazoId) {
        trazoLockService.liberar(comunidadId, trazoId, principal.usuarioId(), principal.nombreArtista());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/comunidades/{comunidadId}/lienzo/trazos/{trazoId}")
    public ResponseEntity<Void> editar(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                        @PathVariable UUID comunidadId, @PathVariable UUID trazoId,
                                        @Valid @RequestBody EditarTrazoRequest request) {
        trazoLockService.editar(comunidadId, trazoId, principal.usuarioId(), principal.nombreArtista(), request);
        return ResponseEntity.ok().build();
    }
}

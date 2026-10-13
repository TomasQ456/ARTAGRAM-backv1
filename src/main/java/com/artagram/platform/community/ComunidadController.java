package com.artagram.platform.community;

import com.artagram.platform.auth.JwtService;
import com.artagram.platform.community.dto.CambiarRolRequest;
import com.artagram.platform.community.dto.ComunidadResponse;
import com.artagram.platform.community.dto.CrearComunidadRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Comunidades", description = "HU-10/11: crear/unirse, roles y moderación")
public class ComunidadController {

    private final ComunidadService comunidadService;

    public ComunidadController(ComunidadService comunidadService) {
        this.comunidadService = comunidadService;
    }

    @PostMapping("/api/comunidades")
    public ResponseEntity<ComunidadResponse> crear(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                                     @Valid @RequestBody CrearComunidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comunidadService.crear(principal.usuarioId(), request));
    }

    @PostMapping("/api/comunidades/{id}/miembros")
    public ComunidadResponse unirse(@AuthenticationPrincipal JwtService.TokenPrincipal principal, @PathVariable UUID id) {
        return comunidadService.unirse(principal.usuarioId(), id);
    }

    @GetMapping("/api/comunidades/{id}/miembros")
    public List<com.artagram.platform.community.dto.MiembroComunidadDto> listarMiembros(
            @AuthenticationPrincipal JwtService.TokenPrincipal principal, @PathVariable UUID id) {
        return comunidadService.listarMiembros(principal.usuarioId(), id);
    }

    @PostMapping("/api/comunidades/{id}/miembros/invitar")
    public com.artagram.platform.community.dto.MiembroComunidadDto invitar(
            @AuthenticationPrincipal JwtService.TokenPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody com.artagram.platform.community.dto.InvitarMiembroRequest request) {
        return comunidadService.invitarMiembro(principal.usuarioId(), id, request.nombreArtista());
    }

    @GetMapping("/api/comunidades/mias")
    public List<ComunidadResponse> misComunidades(@AuthenticationPrincipal JwtService.TokenPrincipal principal) {
        return comunidadService.misComunidades(principal.usuarioId());
    }

    @GetMapping("/api/comunidades/{id}")
    public ComunidadResponse ver(@AuthenticationPrincipal JwtService.TokenPrincipal principal, @PathVariable UUID id) {
        return comunidadService.ver(id, Optional.ofNullable(principal).map(JwtService.TokenPrincipal::usuarioId).orElse(null));
    }

    @PatchMapping("/api/comunidades/{id}/miembros/{usuarioObjetivoId}/rol")
    public ResponseEntity<Void> cambiarRol(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                            @PathVariable UUID id, @PathVariable UUID usuarioObjetivoId,
                                            @Valid @RequestBody CambiarRolRequest request) {
        comunidadService.cambiarRol(principal.usuarioId(), id, usuarioObjetivoId, request.rol());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/comunidades/{id}/miembros/{usuarioObjetivoId}")
    public ResponseEntity<Void> expulsar(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                          @PathVariable UUID id, @PathVariable UUID usuarioObjetivoId) {
        comunidadService.expulsar(principal.usuarioId(), id, usuarioObjetivoId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/comunidades/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal JwtService.TokenPrincipal principal, @PathVariable UUID id) {
        comunidadService.eliminar(principal.usuarioId(), id);
        return ResponseEntity.noContent().build();
    }
}

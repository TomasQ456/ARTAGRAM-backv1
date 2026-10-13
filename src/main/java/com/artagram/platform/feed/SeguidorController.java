package com.artagram.platform.feed;

import com.artagram.platform.auth.JwtService;
import com.artagram.platform.feed.dto.ArtistaResumenDto;
import com.artagram.platform.feed.dto.SeguimientoDto;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Feed", description = "HU-05/06/07: publicar obra+proceso, feed paginado y likes")
public class SeguidorController {

    private final SeguidorService seguidorService;

    public SeguidorController(SeguidorService seguidorService) {
        this.seguidorService = seguidorService;
    }

    @PostMapping("/api/artistas/{id}/seguir")
    public ResponseEntity<Void> seguir(@AuthenticationPrincipal JwtService.TokenPrincipal principal, @PathVariable UUID id) {
        seguidorService.seguir(principal.usuarioId(), id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/artistas/{id}/seguir")
    public ResponseEntity<Void> dejarDeSeguir(@AuthenticationPrincipal JwtService.TokenPrincipal principal, @PathVariable UUID id) {
        seguidorService.dejarDeSeguir(principal.usuarioId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/artistas")
    public List<ArtistaResumenDto> buscar(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                          @RequestParam(name = "q", required = false) String q) {
        return seguidorService.buscar(principal.usuarioId(), q);
    }

    @GetMapping("/api/artistas/{id}/seguimiento")
    public SeguimientoDto seguimiento(@AuthenticationPrincipal JwtService.TokenPrincipal principal, @PathVariable UUID id) {
        return seguidorService.seguimiento(principal.usuarioId(), id);
    }
}

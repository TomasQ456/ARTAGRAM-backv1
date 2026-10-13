package com.artagram.platform.feed;

import com.artagram.platform.auth.JwtService;
import com.artagram.platform.feed.dto.FeedResponse;
import com.artagram.platform.feed.dto.LikeResponse;
import com.artagram.platform.feed.dto.PublicacionDto;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import com.artagram.platform.feed.dto.EditarPublicacionRequest;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Feed", description = "HU-05/06/07: publicar obra+proceso, feed paginado y likes")
public class PublicacionController {

    private final PublicacionService publicacionService;

    public PublicacionController(PublicacionService publicacionService) {
        this.publicacionService = publicacionService;
    }

    @PostMapping(value = "/api/publicaciones", consumes = "multipart/form-data")
    public ResponseEntity<PublicacionDto> publicar(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                                     @RequestParam String titulo,
                                                     @RequestParam(required = false) String descripcion,
                                                     @RequestParam(required = false) Set<String> etiquetas,
                                                     @RequestParam MultipartFile obraFinal,
                                                     @RequestParam(required = false) List<MultipartFile> imagenesProceso) {
        PublicacionDto dto = publicacionService.publicar(principal.usuarioId(), titulo, descripcion, etiquetas,
                obraFinal, imagenesProceso);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/api/feed")
    public FeedResponse feed(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                              @RequestParam(required = false) Instant cursorFecha,
                              @RequestParam(required = false) UUID cursorId) {
        return publicacionService.feed(principal.usuarioId(), cursorFecha, cursorId);
    }

    @PostMapping("/api/publicaciones/{id}/like")
    public LikeResponse alternarLike(@AuthenticationPrincipal JwtService.TokenPrincipal principal, @PathVariable UUID id) {
        return publicacionService.alternarLike(principal.usuarioId(), id);
    }

    @RequestMapping(value = "/api/publicaciones/{id}", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public PublicacionDto editar(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                 @PathVariable UUID id,
                                 @Valid @RequestBody EditarPublicacionRequest request) {
        return publicacionService.editar(principal.usuarioId(), id, request);
    }

    @DeleteMapping("/api/publicaciones/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal JwtService.TokenPrincipal principal, @PathVariable UUID id) {
        publicacionService.eliminar(principal.usuarioId(), id);
        return ResponseEntity.noContent().build();
    }
}

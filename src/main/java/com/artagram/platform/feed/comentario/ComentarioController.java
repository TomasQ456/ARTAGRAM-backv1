package com.artagram.platform.feed.comentario;

import com.artagram.platform.auth.JwtService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Comentarios", description = "Comentarios planos en vivo sobre publicaciones")
public class ComentarioController {

    private final ComentarioService comentarioService;

    public ComentarioController(ComentarioService comentarioService) {
        this.comentarioService = comentarioService;
    }

    @PostMapping("/api/publicaciones/{id}/comentarios")
    public ResponseEntity<ComentarioDto> comentar(
            @AuthenticationPrincipal JwtService.TokenPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody CrearComentarioRequest request) {
        ComentarioDto dto = comentarioService.crearComentario(principal.usuarioId(), id, request.contenido());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/api/publicaciones/{id}/comentarios")
    public List<ComentarioDto> listar(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return comentarioService.listar(id, pagina, tamano);
    }

    @DeleteMapping("/api/comentarios/{id}")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal JwtService.TokenPrincipal principal,
            @PathVariable UUID id) {
        comentarioService.eliminar(principal.usuarioId(), id);
        return ResponseEntity.noContent().build();
    }
}

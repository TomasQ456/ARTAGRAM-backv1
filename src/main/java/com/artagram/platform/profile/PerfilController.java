package com.artagram.platform.profile;

import com.artagram.platform.auth.JwtService;
import com.artagram.platform.profile.dto.ActualizarPerfilRequest;
import com.artagram.platform.profile.dto.PerfilResponse;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Perfiles", description = "HU-03: perfil público del artista")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping("/api/perfil/me")
    public PerfilResponse verPropio(@AuthenticationPrincipal JwtService.TokenPrincipal principal) {
        return perfilService.verPropio(principal.usuarioId());
    }

    @PutMapping("/api/perfil/me")
    public PerfilResponse actualizar(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                      @Valid @RequestBody ActualizarPerfilRequest request) {
        return perfilService.actualizar(principal.usuarioId(), request);
    }

    @PostMapping("/api/perfil/me/foto")
    public Map<String, String> subirFoto(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                          @RequestParam("archivo") MultipartFile archivo) {
        return Map.of("fotoUrl", perfilService.subirFoto(principal.usuarioId(), archivo));
    }

    @PostMapping("/api/perfil/me/obras/imagen")
    public Map<String, String> subirImagenObra(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                                @RequestParam("archivo") MultipartFile archivo) {
        return Map.of("rutaImagen", perfilService.subirImagenObraDestacada(principal.usuarioId(), archivo));
    }

    @GetMapping("/api/perfiles/{nombreArtista}")
    public PerfilResponse verPublico(@PathVariable String nombreArtista,
                                      @AuthenticationPrincipal JwtService.TokenPrincipal principal) {
        return perfilService.verPublico(nombreArtista, Optional.ofNullable(principal).map(JwtService.TokenPrincipal::usuarioId).orElse(null));
    }
}

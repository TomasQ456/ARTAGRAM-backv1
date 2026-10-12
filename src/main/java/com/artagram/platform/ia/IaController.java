package com.artagram.platform.ia;

import com.artagram.platform.auth.JwtService;
import com.artagram.platform.ia.estrategia.SugerenciaEtiquetasResultado;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "Inteligencia Artificial", description = "Análisis técnico de obras con IA, historial y sugerencias de etiquetas")
public class IaController {

    private final IaService iaService;

    public IaController(IaService iaService) {
        this.iaService = iaService;
    }

    @PostMapping("/api/publicaciones/{id}/analisis")
    public ResponseEntity<AnalisisObraDto> analizarObra(
            @AuthenticationPrincipal JwtService.TokenPrincipal principal,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "TECNICO") TipoAnalisis tipo) {
        AnalisisObraDto dto = iaService.analizarPublicacion(principal.usuarioId(), id, tipo);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/api/publicaciones/{id}/analisis/ultimo")
    public ResponseEntity<AnalisisObraDto> obtenerUltimoAnalisis(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "TECNICO") TipoAnalisis tipo) {
        AnalisisObraDto dto = iaService.obtenerUltimoAnalisis(id, tipo);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/api/publicaciones/{id}/analisis")
    public List<AnalisisObraDto> historial(@PathVariable UUID id) {
        return iaService.historial(id);
    }

    @PostMapping(value = "/api/ia/sugerencias", consumes = "multipart/form-data")
    public ResponseEntity<SugerenciaEtiquetasResultado> sugerirMetadatos(
            @RequestParam("imagen") MultipartFile imagen) {
        SugerenciaEtiquetasResultado resultado = iaService.sugerirMetadatos(imagen);
        return ResponseEntity.ok(resultado);
    }
}

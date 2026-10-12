package com.artagram.platform.canvas;

import com.artagram.platform.auth.JwtService;
import com.artagram.platform.canvas.dto.TrazoDto;
import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.community.ComunidadService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Snapshot REST para quien llega tarde al lienzo (HU-12): trae todo lo ya
 * dibujado en una sola respuesta y de ahi en adelante solo le llega lo nuevo
 * por STOMP.
 */
@RestController
@Tag(name = "Lienzo colaborativo", description = "HU-12/13/15: dibujo en tiempo real, snapshot y bloqueo de trazos")
public class CanvasController {

    private final CanvasService canvasService;
    private final ComunidadService comunidadService;

    public CanvasController(CanvasService canvasService, ComunidadService comunidadService) {
        this.canvasService = canvasService;
        this.comunidadService = comunidadService;
    }

    @GetMapping("/api/comunidades/{comunidadId}/lienzo/trazos")
    public List<TrazoDto> obtenerTrazos(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                         @PathVariable UUID comunidadId,
                                         @RequestParam(defaultValue = "0") long desde) {
        if (!comunidadService.esMiembroActivo(principal.usuarioId(), comunidadId)) {
            throw ApiException.forbidden("NO_ERES_MIEMBRO", "No perteneces a esta comunidad");
        }
        return canvasService.obtenerTrazosDesde(comunidadId, desde).stream().map(TrazoDto::de).toList();
    }
}

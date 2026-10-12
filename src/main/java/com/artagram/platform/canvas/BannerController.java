package com.artagram.platform.canvas;

import com.artagram.platform.auth.JwtService;
import com.artagram.platform.canvas.dto.BannerTrazoEventoDto;
import com.artagram.platform.canvas.dto.PintarRegionRequest;
import com.artagram.platform.canvas.dto.RegionEstadoDto;
import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.community.ComunidadService;
import com.artagram.platform.realtime.RealtimeBroadcaster;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Ademas del canal STOMP (BannerStompController), se deja un endpoint REST
 * equivalente para pintar: util para pruebas de carga/concurrencia y para
 * clientes que no quieran mantener una sesion WebSocket solo para esto.
 */
@RestController
@Tag(name = "Banner mural", description = "HU-16: mural comunitario 1000×1000 particionado por regiones")
public class BannerController {

    private final BannerService bannerService;
    private final ComunidadService comunidadService;
    private final RealtimeBroadcaster broadcaster;

    public BannerController(BannerService bannerService, ComunidadService comunidadService,
                             RealtimeBroadcaster broadcaster) {
        this.bannerService = bannerService;
        this.comunidadService = comunidadService;
        this.broadcaster = broadcaster;
    }

    @GetMapping("/api/comunidades/{comunidadId}/banner")
    public List<RegionEstadoDto> obtenerEstado(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                                @PathVariable UUID comunidadId) {
        if (!comunidadService.esMiembroActivo(principal.usuarioId(), comunidadId)) {
            throw ApiException.forbidden("NO_ERES_MIEMBRO", "No perteneces a esta comunidad");
        }
        return bannerService.obtenerEstado(comunidadId);
    }

    @PostMapping("/api/comunidades/{comunidadId}/banner/pintar")
    public ResponseEntity<Void> pintar(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                        @PathVariable UUID comunidadId,
                                        @Valid @RequestBody PintarRegionRequest request) {
        BannerTrazoEventoDto evento = bannerService.pintar(comunidadId, principal.usuarioId(), principal.nombreArtista(), request);
        broadcaster.enviar("/topic/banner/" + comunidadId, evento);
        return ResponseEntity.ok().build();
    }
}

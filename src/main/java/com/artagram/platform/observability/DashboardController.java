package com.artagram.platform.observability;

import com.artagram.platform.auth.JwtService;
import com.artagram.platform.observability.dto.DashboardInfoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * HU-38: un solo lugar para las metricas tecnicas y los KPIs. Este endpoint
 * solo entrega las URLs (Grafana vive aparte, ver docker-compose); lo
 * importante para la historia es que solo un ADMIN llega hasta aqui
 * (SecurityConfig) y que el intento fallido queda registrado (AccessDeniedHandler).
 */
@RestController
@Tag(name = "Administración", description = "HU-37/38: KPIs de negocio y panel restringido a ADMIN")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @Value("${app.observability.grafana-tecnico-url:http://localhost:3001/d/tecnico}")
    private String panelTecnicoUrl;

    @Value("${app.observability.grafana-negocio-url:http://localhost:3001/d/negocio}")
    private String panelNegocioUrl;

    @Value("${app.observability.prometheus-url:http://localhost:9090}")
    private String prometheusUrl;

    @GetMapping("/api/admin/dashboard")
    public DashboardInfoDto obtener(@AuthenticationPrincipal JwtService.TokenPrincipal principal) {
        log.info("Acceso al dashboard concedido a usuario={}", principal.usuarioId());
        return new DashboardInfoDto(panelTecnicoUrl, panelNegocioUrl, prometheusUrl);
    }
}

package com.artagram.platform.observability;

import com.artagram.platform.observability.dto.KpiDetalleDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

/** HU-37: los 4 KPIs de negocio, calculados solos, con su formula y periodo. */
@RestController
@Tag(name = "Administración", description = "HU-37/38: KPIs de negocio y panel restringido a ADMIN")
public class KpiController {

    private final KpiService kpiService;

    public KpiController(KpiService kpiService) {
        this.kpiService = kpiService;
    }

    @GetMapping("/api/admin/kpis")
    public List<KpiDetalleDto> todos() {
        return kpiService.calcularTodos();
    }

    @GetMapping("/api/admin/kpis/{id}")
    public KpiDetalleDto detalle(@PathVariable String id) {
        return kpiService.calcular(id);
    }
}

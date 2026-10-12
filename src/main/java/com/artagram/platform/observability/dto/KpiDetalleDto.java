package com.artagram.platform.observability.dto;

import java.time.Instant;

public record KpiDetalleDto(
        String id,
        String nombre,
        double valorActual,
        double meta,
        String tendencia,
        String formula,
        Instant periodoDesde,
        Instant periodoHasta,
        long cantidadEventosContados
) {
}

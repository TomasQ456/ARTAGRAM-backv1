package com.artagram.platform.canvas.dto;

import java.util.List;

public record RegionEstadoDto(
        int regionIndex,
        String snapshotPngBase64,
        List<BannerTrazoEventoDto> trazosDesdeSnapshot
) {
}

package com.artagram.platform.canvas.dto;

import com.artagram.platform.canvas.Punto;
import com.artagram.platform.canvas.TipoPincel;
import java.util.List;
import java.util.UUID;

public record BannerTrazoEventoDto(
        int regionIndex,
        long secuencia,
        UUID autorId,
        String autorNombre,
        TipoPincel tipoPincel,
        String colorHex,
        double grosor,
        double opacidad,
        List<Punto> puntos
) {
}

package com.artagram.platform.community.dto;

import java.time.Instant;
import java.util.UUID;

public record MiembroComunidadDto(
        UUID usuarioId,
        String nombreArtista,
        String rol,
        Instant unidoEn
) {
}

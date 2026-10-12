package com.artagram.platform.feed.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PublicacionDto(
        UUID id,
        UUID autorId,
        String autorNombre,
        String titulo,
        String descripcion,
        String obraFinalUrl,
        List<String> imagenesProcesoUrls,
        Set<String> etiquetas,
        int contadorLikes,
        boolean meGusta,
        Instant creadoEn
) {
}

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
        Instant creadoEn,
        int contadorComentarios,
        Integer version,
        Instant editadoEn
) {
    public PublicacionDto(
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
        this(id, autorId, autorNombre, titulo, descripcion, obraFinalUrl, imagenesProcesoUrls, etiquetas, contadorLikes, meGusta, creadoEn, 0, 0, null);
    }
}

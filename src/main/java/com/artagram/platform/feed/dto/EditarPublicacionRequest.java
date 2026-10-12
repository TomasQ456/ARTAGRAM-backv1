package com.artagram.platform.feed.dto;

import jakarta.validation.constraints.Size;
import java.util.Set;

public record EditarPublicacionRequest(
        @Size(max = 120, message = "El título no puede superar 120 caracteres")
        String titulo,

        @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
        String descripcion,

        Set<String> etiquetas,

        Integer version
) {
}

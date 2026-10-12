package com.artagram.platform.feed.comentario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearComentarioRequest(
        @NotBlank(message = "El comentario no puede estar vacío")
        @Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
        String contenido
) {
}

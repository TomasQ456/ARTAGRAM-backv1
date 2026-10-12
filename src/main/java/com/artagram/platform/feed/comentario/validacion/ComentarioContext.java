package com.artagram.platform.feed.comentario.validacion;

import java.util.UUID;

public record ComentarioContext(
        UUID autorId,
        UUID publicacionId,
        String contenido
) {
}

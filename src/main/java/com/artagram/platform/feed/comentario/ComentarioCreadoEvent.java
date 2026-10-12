package com.artagram.platform.feed.comentario;

public record ComentarioCreadoEvent(
        ComentarioDto comentario,
        int contadorComentarios
) {
}

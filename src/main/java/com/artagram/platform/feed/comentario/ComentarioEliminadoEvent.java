package com.artagram.platform.feed.comentario;

public record ComentarioEliminadoEvent(
        ComentarioDto comentario,
        int contadorComentarios
) {
}

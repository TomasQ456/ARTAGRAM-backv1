package com.artagram.platform.feed.comentario;

public record ComentarioEvento(
        String tipo,
        ComentarioDto comentario,
        int contadorComentarios
) {
}

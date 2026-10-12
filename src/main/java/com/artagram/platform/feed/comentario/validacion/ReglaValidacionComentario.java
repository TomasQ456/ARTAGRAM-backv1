package com.artagram.platform.feed.comentario.validacion;

/**
 * Strategy para reglas de validación de comentarios antes de persistencia.
 */
public interface ReglaValidacionComentario {
    void validar(ComentarioContext contexto);
}

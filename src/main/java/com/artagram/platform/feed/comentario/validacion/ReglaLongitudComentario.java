package com.artagram.platform.feed.comentario.validacion;

import com.artagram.platform.common.error.ApiException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class ReglaLongitudComentario implements ReglaValidacionComentario {

    private static final int MAX_LONGITUD = 500;

    @Override
    public void validar(ComentarioContext contexto) {
        if (contexto == null || contexto.contenido() == null || contexto.contenido().trim().isEmpty()) {
            throw ApiException.badRequest("COMENTARIO_VACIO", "El comentario no puede estar vacío");
        }
        if (contexto.contenido().trim().length() > MAX_LONGITUD) {
            throw ApiException.badRequest("LONGITUD_EXCEDIDA", "El comentario no puede exceder " + MAX_LONGITUD + " caracteres");
        }
    }
}

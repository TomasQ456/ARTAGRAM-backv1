package com.artagram.platform.feed.comentario.validacion;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ValidadorComentarios {

    private final List<ReglaValidacionComentario> reglas;

    public ValidadorComentarios(List<ReglaValidacionComentario> reglas) {
        this.reglas = reglas;
    }

    public void validar(ComentarioContext contexto) {
        for (ReglaValidacionComentario regla : reglas) {
            regla.validar(contexto);
        }
    }
}

package com.artagram.platform.feed.comentario.validacion;

import com.artagram.platform.common.error.ApiException;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class ReglaPalabrasProhibidasComentario implements ReglaValidacionComentario {

    private static final Set<String> PALABRAS_PROHIBIDAS = Set.of(
            "idiota", "estupido", "estúpido", "estupida", "estúpida", "imbecil", "imbécil",
            "mierda", "basura", "spam", "scam"
    );

    @Override
    public void validar(ComentarioContext contexto) {
        String texto = contexto.contenido().toLowerCase();
        for (String prohibida : PALABRAS_PROHIBIDAS) {
            if (texto.matches(".*\\b" + Pattern.quote(prohibida) + "\\b.*")) {
                throw ApiException.badRequest("LENGUAJE_INAPROPIADO", "El comentario contiene términos no permitidos");
            }
        }
    }
}

package com.artagram.platform.feed.comentario;

import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.feed.comentario.validacion.ComentarioContext;
import com.artagram.platform.feed.comentario.validacion.ReglaLongitudComentario;
import com.artagram.platform.feed.comentario.validacion.ReglaPalabrasProhibidasComentario;
import com.artagram.platform.feed.comentario.validacion.ValidadorComentarios;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComentariosValidationTest {

    private final ReglaLongitudComentario reglaLongitud = new ReglaLongitudComentario();
    private final ReglaPalabrasProhibidasComentario reglaPalabras = new ReglaPalabrasProhibidasComentario();
    private final ValidadorComentarios validador = new ValidadorComentarios(List.of(reglaLongitud, reglaPalabras));

    @Test
    @DisplayName("Acepta comentarios válidos de longitud adecuada")
    void comentarioValido() {
        ComentarioContext ctx = new ComentarioContext(UUID.randomUUID(), UUID.randomUUID(), "Excelente uso de los colores y la perspectiva!");
        assertThatCode(() -> validador.validar(ctx)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rechaza comentarios vacíos o con solo espacios")
    void comentarioVacio() {
        ComentarioContext ctx = new ComentarioContext(UUID.randomUUID(), UUID.randomUUID(), "   ");
        assertThatThrownBy(() -> validador.validar(ctx))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    @DisplayName("Rechaza comentarios que superen 500 caracteres")
    void comentarioExcedido() {
        String largo = "a".repeat(501);
        ComentarioContext ctx = new ComentarioContext(UUID.randomUUID(), UUID.randomUUID(), largo);
        assertThatThrownBy(() -> validador.validar(ctx))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("500");
    }

    @Test
    @DisplayName("Rechaza comentarios con términos ofensivos o prohibidos")
    void comentarioPalabrasProhibidas() {
        ComentarioContext ctx = new ComentarioContext(UUID.randomUUID(), UUID.randomUUID(), "Este dibujo es una basura completa");
        assertThatThrownBy(() -> validador.validar(ctx))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("términos no permitidos");
    }
}

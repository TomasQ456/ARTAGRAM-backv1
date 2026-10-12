package com.artagram.platform.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.artagram.platform.support.IntegrationTestBase;
import com.artagram.platform.support.TestUsuarios;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Las excepciones de framework (ruta que no existe, verbo no soportado, JSON
 * mal formado) tienen que responder con su semantica HTTP propia — no un 500
 * generico via el catch-all de GlobalExceptionHandler — para que un cliente
 * pueda distinguir "tu URL esta mal" de "el servidor se rompio de verdad".
 */
class GlobalExceptionHandlerIT extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TestUsuarios testUsuarios;

    @Test
    void unaRutaQueNoExisteResponde404ConCodigoPropio() {
        String token = testUsuarios.registrarVerificarYLoguear(rest, "errores404@correo.com", "claveSegura123", "errores_404");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> respuesta = rest.exchange("/api/esta-ruta-no-existe-en-serio", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).contains("RECURSO_NO_ENCONTRADO");
    }

    @Test
    void unVerboNoSoportadoResponde405ConCodigoPropio() {
        ResponseEntity<String> respuesta = rest.exchange("/api/auth/login", HttpMethod.DELETE, null, String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(respuesta.getBody()).contains("METODO_NO_PERMITIDO");
    }

    @Test
    void unCuerpoJsonMalFormadoResponde400ConCodigoPropio() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        ResponseEntity<String> respuesta = rest.exchange("/api/auth/login", HttpMethod.POST,
                new HttpEntity<>("{ esto no es json valido", headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).contains("CUERPO_INVALIDO");
    }

    @Test
    void unaPeticionSinTokenResponde401NoUnos403() {
        ResponseEntity<String> respuesta = rest.exchange("/api/feed", HttpMethod.GET, null, String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody()).contains("NO_AUTENTICADO");
    }
}

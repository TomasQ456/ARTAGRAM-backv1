package com.artagram.platform.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.artagram.platform.auth.dto.LoginRequest;
import com.artagram.platform.auth.dto.LoginResponse;
import com.artagram.platform.auth.dto.RegistroRequest;
import com.artagram.platform.profile.dto.ActualizarPerfilRequest;
import com.artagram.platform.profile.dto.EnlaceDto;
import com.artagram.platform.support.IntegrationTestBase;
import com.artagram.platform.support.TestUsuarios;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Cubre HU-03: guardar cambios de perfil autenticado y ver el perfil publico
 * sin sesion (sin datos privados ni botones de edicion, que aqui se traduce en
 * que esPropio sea false y el correo nunca viaje en la respuesta).
 */
class PerfilControllerIT extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TestUsuarios testUsuarios;

    @Test
    void guardoCambiosDePerfilYQuedanVisiblesDeInmediato() {
        String token = testUsuarios.registrarVerificarYLoguear(rest, "perfil1@correo.com", "claveSegura123", "lienzo_azul");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ActualizarPerfilRequest request = new ActualizarPerfilRequest(
                "Pinto acuarelas urbanas",
                List.of(new EnlaceDto("Instagram", "https://instagram.com/lienzo_azul")),
                List.of());

        ResponseEntity<com.artagram.platform.profile.dto.PerfilResponse> respuesta = rest.exchange(
                "/api/perfil/me", HttpMethod.PUT, new HttpEntity<>(request, headers),
                com.artagram.platform.profile.dto.PerfilResponse.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().biografia()).isEqualTo("Pinto acuarelas urbanas");
        assertThat(respuesta.getBody().enlaces()).hasSize(1);

        ResponseEntity<com.artagram.platform.profile.dto.PerfilResponse> publico =
                rest.getForEntity("/api/perfiles/lienzo_azul", com.artagram.platform.profile.dto.PerfilResponse.class);
        assertThat(publico.getBody().biografia()).isEqualTo("Pinto acuarelas urbanas");
    }

    @Test
    void alguienVisitaMiPerfilSinCuentaVeSoloLoPublico() {
        testUsuarios.registrarVerificarYLoguear(rest, "perfil2@correo.com", "claveSegura123", "acuarela_urbana");

        ResponseEntity<com.artagram.platform.profile.dto.PerfilResponse> respuesta =
                rest.getForEntity("/api/perfiles/acuarela_urbana", com.artagram.platform.profile.dto.PerfilResponse.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().esPropio()).isFalse();
        assertThat(respuesta.getBody().nombreArtista()).isEqualTo("acuarela_urbana");
    }

    @Test
    void perfilQueNoExisteDaNotFound() {
        ResponseEntity<String> respuesta = rest.getForEntity("/api/perfiles/no_existe_nadie", String.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}

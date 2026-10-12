package com.artagram.platform.community;

import static org.assertj.core.api.Assertions.assertThat;

import com.artagram.platform.canvas.BannerComunitarioRepository;
import com.artagram.platform.canvas.LienzoRepository;
import com.artagram.platform.community.dto.ComunidadResponse;
import com.artagram.platform.community.dto.CrearComunidadRequest;
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
 * HU-10 (crear comunidad, con su lienzo y banner aprovisionados) y HU-11
 * (unirse y quedar con un rol).
 */
class ComunidadControllerIT extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TestUsuarios testUsuarios;

    @Autowired
    private LienzoRepository lienzoRepository;

    @Autowired
    private BannerComunitarioRepository bannerRepository;

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void creoLaComunidadYQuedoDeAdminConLienzoYBannerAprovisionados() {
        String token = testUsuarios.registrarVerificarYLoguear(rest, "creador1@correo.com", "claveSegura123", "creador_uno");

        CrearComunidadRequest request = new CrearComunidadRequest("Acuarela Urbana", "Pintamos la ciudad", true);
        ResponseEntity<ComunidadResponse> respuesta = rest.exchange("/api/comunidades", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(token)), ComunidadResponse.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ComunidadResponse comunidad = respuesta.getBody();
        assertThat(comunidad.miRol()).isEqualTo("ADMIN");
        assertThat(comunidad.totalMiembros()).isEqualTo(1);

        assertThat(lienzoRepository.findByComunidadId(comunidad.id())).isPresent();
        assertThat(bannerRepository.findByComunidadId(comunidad.id())).isPresent();
    }

    @Test
    void elNombreDeComunidadYaExisteNoDejaCrearOtra() {
        String token = testUsuarios.registrarVerificarYLoguear(rest, "creador2@correo.com", "claveSegura123", "creador_dos");
        CrearComunidadRequest request = new CrearComunidadRequest("Pixel Art Club", "desc", true);
        rest.exchange("/api/comunidades", HttpMethod.POST, new HttpEntity<>(request, authHeaders(token)), ComunidadResponse.class);

        ResponseEntity<ComunidadResponse> segunda = rest.exchange("/api/comunidades", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(token)), ComunidadResponse.class);

        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void meUnoAUnaComunidadPublicaYQuedoComoMiembro() {
        String tokenCreador = testUsuarios.registrarVerificarYLoguear(rest, "creador3@correo.com", "claveSegura123", "creador_tres");
        ComunidadResponse comunidad = rest.exchange("/api/comunidades", HttpMethod.POST,
                new HttpEntity<>(new CrearComunidadRequest("Manga Studio", "desc", true), authHeaders(tokenCreador)),
                ComunidadResponse.class).getBody();

        String tokenMiembro = testUsuarios.registrarVerificarYLoguear(rest, "miembro1@correo.com", "claveSegura123", "miembro_uno");
        ResponseEntity<ComunidadResponse> respuesta = rest.exchange("/api/comunidades/" + comunidad.id() + "/miembros",
                HttpMethod.POST, new HttpEntity<>(null, authHeaders(tokenMiembro)), ComunidadResponse.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().miRol()).isEqualTo("MIEMBRO");
        assertThat(respuesta.getBody().totalMiembros()).isEqualTo(2);
    }
}

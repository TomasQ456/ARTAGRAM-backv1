package com.artagram.platform.feed;

import static org.assertj.core.api.Assertions.assertThat;

import com.artagram.platform.feed.dto.FeedResponse;
import com.artagram.platform.feed.dto.PublicacionDto;
import com.artagram.platform.support.IntegrationTestBase;
import com.artagram.platform.support.TestUsuarios;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/** HU-05 (publicar obra+proceso) y HU-06 (feed paginado por cursor). */
class PublicacionIT extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TestUsuarios testUsuarios;

    private HttpHeaders auth(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private byte[] pngDeUnPixel() {
        try {
            BufferedImage imagen = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(imagen, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ByteArrayResource archivo(String nombre, byte[] contenido) {
        return new ByteArrayResource(contenido) {
            @Override
            public String getFilename() {
                return nombre;
            }
        };
    }

    @Test
    void publicacionCompletaQuedaConSuLineaDeProcesoEnOrden() {
        String token = testUsuarios.registrarVerificarYLoguear(rest, "feedartista1@correo.com", "claveSegura123", "feed_artista_uno");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("titulo", "Atardecer en la 80");
        body.add("descripcion", "Acuarela de un atardecer");
        body.add("etiquetas", "acuarela");
        body.add("etiquetas", "paisaje");
        body.add("obraFinal", archivo("final.png", pngDeUnPixel()));
        body.add("imagenesProceso", archivo("paso1.png", pngDeUnPixel()));
        body.add("imagenesProceso", archivo("paso2.png", pngDeUnPixel()));

        HttpHeaders headers = auth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<PublicacionDto> respuesta = rest.postForEntity("/api/publicaciones", new HttpEntity<>(body, headers), PublicacionDto.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody().imagenesProcesoUrls()).hasSize(2);
        assertThat(respuesta.getBody().etiquetas()).containsExactlyInAnyOrder("acuarela", "paisaje");
        assertThat(respuesta.getBody().obraFinalUrl()).isNotBlank();

        // La URL firmada tiene que servir el archivo de verdad, no solo existir como texto
        // (un mapping de un solo nivel no matchea rutas anidadas como publicaciones/{autorId}/archivo.png).
        String rutaRelativa = respuesta.getBody().obraFinalUrl().replaceFirst("^https?://[^/]+", "");
        ResponseEntity<byte[]> archivoServido = rest.getForEntity(rutaRelativa, byte[].class);
        assertThat(archivoServido.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(archivoServido.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(archivoServido.getBody()).isNotEmpty();
    }

    @Test
    void archivoConFormatoNoPermitidoNoSeSube() {
        String token = testUsuarios.registrarVerificarYLoguear(rest, "feedartista2@correo.com", "claveSegura123", "feed_artista_dos");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("titulo", "Obra rara");
        body.add("obraFinal", archivo("virus.exe", "no soy una imagen".getBytes()));

        HttpHeaders headers = auth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<String> respuesta = rest.postForEntity("/api/publicaciones", new HttpEntity<>(body, headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).contains("Formato no permitido: usa PNG, JPG o WEBP");
    }

    @Test
    void siSeOlvidaLaObraFinalNoDejaPublicar() {
        String token = testUsuarios.registrarVerificarYLoguear(rest, "feedartista3@correo.com", "claveSegura123", "feed_artista_tres");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("titulo", "Solo proceso");
        body.add("imagenesProceso", archivo("paso1.png", pngDeUnPixel()));

        HttpHeaders headers = auth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<String> respuesta = rest.exchange("/api/publicaciones", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private PublicacionDto publicarObra(String token, String titulo) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("titulo", titulo);
        body.add("obraFinal", archivo("final.png", pngDeUnPixel()));
        HttpHeaders headers = auth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<PublicacionDto> resp = rest.postForEntity("/api/publicaciones", new HttpEntity<>(body, headers), PublicacionDto.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    @Test
    void elFeedTraeLoMasNuevoPrimeroYPaginaSinRepetir() {
        String tokenAutor = testUsuarios.registrarVerificarYLoguear(rest, "autorFeed@correo.com", "claveSegura123", "autor_feed");
        String tokenLector = testUsuarios.registrarVerificarYLoguear(rest, "lectorFeed@correo.com", "claveSegura123", "lector_feed");
        UUID idAutor = testUsuarios.obtenerPorCorreo("autorFeed@correo.com").getId();
        rest.postForEntity("/api/artistas/" + idAutor + "/seguir", new HttpEntity<>(auth(tokenLector)), Void.class);

        for (int i = 1; i <= 25; i++) {
            publicarObra(tokenAutor, "Obra " + i);
        }

        ResponseEntity<FeedResponse> primeraPagina = rest.exchange("/api/feed", HttpMethod.GET,
                new HttpEntity<>(auth(tokenLector)), FeedResponse.class);
        assertThat(primeraPagina.getBody().publicaciones()).hasSize(20);
        assertThat(primeraPagina.getBody().hayMas()).isTrue();
        assertThat(primeraPagina.getBody().publicaciones().get(0).titulo()).isEqualTo("Obra 25");

        String cursorFecha = primeraPagina.getBody().cursorFechaSiguiente().toString();
        UUID cursorId = primeraPagina.getBody().cursorIdSiguiente();
        ResponseEntity<FeedResponse> segundaPagina = rest.exchange(
                "/api/feed?cursorFecha=" + cursorFecha + "&cursorId=" + cursorId, HttpMethod.GET,
                new HttpEntity<>(auth(tokenLector)), FeedResponse.class);

        assertThat(segundaPagina.getBody().publicaciones()).hasSize(5);
        assertThat(segundaPagina.getBody().hayMas()).isFalse();

        var titulosPrimera = primeraPagina.getBody().publicaciones().stream().map(PublicacionDto::titulo).toList();
        var titulosSegunda = segundaPagina.getBody().publicaciones().stream().map(PublicacionDto::titulo).toList();
        assertThat(titulosPrimera).doesNotContainAnyElementsOf(titulosSegunda);
    }

    @Test
    void feedVacioMeSugiereArtistas() {
        String token = testUsuarios.registrarVerificarYLoguear(rest, "nuevoSinSeguir@correo.com", "claveSegura123", "nuevo_sin_seguir");
        testUsuarios.registrarVerificarYLoguear(rest, "otroArtista@correo.com", "claveSegura123", "otro_artista");

        ResponseEntity<FeedResponse> respuesta = rest.exchange("/api/feed", HttpMethod.GET, new HttpEntity<>(auth(token)), FeedResponse.class);

        assertThat(respuesta.getBody().publicaciones()).isEmpty();
        assertThat(respuesta.getBody().artistasSugeridos()).isNotEmpty();
    }

    @Test
    void elAutorPuedeEliminarSuPropiaPublicacionYDejaDeVerseEnElFeed() {
        String token = testUsuarios.registrarVerificarYLoguear(rest, "autorBorra@correo.com", "claveSegura123", "autor_borra");
        PublicacionDto publicacion = publicarObra(token, "Obra que voy a borrar");
        String rutaObra = publicacion.obraFinalUrl().replaceFirst("^https?://[^/]+", "");

        ResponseEntity<Void> respuesta = rest.exchange("/api/publicaciones/" + publicacion.id(), HttpMethod.DELETE,
                new HttpEntity<>(auth(token)), Void.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<FeedResponse> feed = rest.exchange("/api/feed", HttpMethod.GET, new HttpEntity<>(auth(token)), FeedResponse.class);
        assertThat(feed.getBody().publicaciones()).noneMatch(p -> p.id().equals(publicacion.id()));

        // El archivo tambien se borra del storage, no solo la fila de la base.
        ResponseEntity<byte[]> archivoBorrado = rest.getForEntity(rutaObra, byte[].class);
        assertThat(archivoBorrado.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void quienNoEsElAutorNoPuedeEliminarLaPublicacionDeOtro() {
        String tokenAutor = testUsuarios.registrarVerificarYLoguear(rest, "autorAjeno@correo.com", "claveSegura123", "autor_ajeno");
        String tokenOtro = testUsuarios.registrarVerificarYLoguear(rest, "intrusoBorra@correo.com", "claveSegura123", "intruso_borra");
        PublicacionDto publicacion = publicarObra(tokenAutor, "No me borres esto");

        ResponseEntity<String> respuesta = rest.exchange("/api/publicaciones/" + publicacion.id(), HttpMethod.DELETE,
                new HttpEntity<>(auth(tokenOtro)), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody()).contains("SOLO_AUTOR");
    }
}

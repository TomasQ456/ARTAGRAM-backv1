package com.artagram.platform.feed;

import com.artagram.platform.feed.comentario.ComentarioDto;
import com.artagram.platform.feed.comentario.CrearComentarioRequest;
import com.artagram.platform.feed.dto.EditarPublicacionRequest;
import com.artagram.platform.feed.dto.PublicacionDto;
import com.artagram.platform.ia.AnalisisObraDto;
import com.artagram.platform.ia.TipoAnalisis;
import com.artagram.platform.ia.estrategia.SugerenciaEtiquetasResultado;
import com.artagram.platform.support.IntegrationTestBase;
import com.artagram.platform.support.TestUsuarios;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

class PublicacionNuevasFuncionalidadesIT extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TestUsuarios testUsuarios;

    private HttpHeaders auth(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private byte[] pngDePrueba() {
        try {
            BufferedImage imagen = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
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

    private PublicacionDto publicar(String token, String titulo) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("titulo", titulo);
        body.add("obraFinal", archivo("obra.png", pngDePrueba()));

        HttpHeaders headers = auth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<PublicacionDto> resp = rest.postForEntity("/api/publicaciones", new HttpEntity<>(body, headers), PublicacionDto.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    @Test
    @DisplayName("Editar publicación actualiza título/descripción y valida permisos y versión optimista")
    void editarPublicacionConVersion() {
        String tokenAutor = testUsuarios.registrarVerificarYLoguear(rest, "autor_edit@artagram.test", "Pass1234!", "AutorEdit");
        String tokenOtro = testUsuarios.registrarVerificarYLoguear(rest, "otro_edit@artagram.test", "Pass1234!", "OtroEdit");
        PublicacionDto pub = publicar(tokenAutor, "Título Original");

        // 1. Otro usuario intenta editar -> 403 Forbidden
        EditarPublicacionRequest reqOtro = new EditarPublicacionRequest("Hackeado", "Desc", Set.of(), pub.version());
        ResponseEntity<String> respOtro = rest.exchange(
                "/api/publicaciones/" + pub.id(),
                HttpMethod.PATCH,
                new HttpEntity<>(reqOtro, auth(tokenOtro)),
                String.class
        );
        assertThat(respOtro.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 2. Autor edita con versión correcta -> 200 OK y versión incrementada
        EditarPublicacionRequest reqAutor = new EditarPublicacionRequest("Título Editado", "Nueva descripción", Set.of("acuarela"), pub.version());
        ResponseEntity<PublicacionDto> respAutor = rest.exchange(
                "/api/publicaciones/" + pub.id(),
                HttpMethod.PATCH,
                new HttpEntity<>(reqAutor, auth(tokenAutor)),
                PublicacionDto.class
        );
        assertThat(respAutor.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respAutor.getBody().titulo()).isEqualTo("Título Editado");
        assertThat(respAutor.getBody().descripcion()).isEqualTo("Nueva descripción");
        assertThat(respAutor.getBody().version()).isGreaterThan(pub.version());

        // 3. Intento de editar con versión desactualizada -> 409 Conflict
        EditarPublicacionRequest reqVieja = new EditarPublicacionRequest("Título Conflito", "Desc", Set.of(), pub.version());
        ResponseEntity<String> respConflicto = rest.exchange(
                "/api/publicaciones/" + pub.id(),
                HttpMethod.PATCH,
                new HttpEntity<>(reqVieja, auth(tokenAutor)),
                String.class
        );
        assertThat(respConflicto.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Crear, listar y eliminar comentarios con contador en vivo")
    void cicloComentarios() {
        String tokenAutor = testUsuarios.registrarVerificarYLoguear(rest, "autor_com@artagram.test", "Pass1234!", "AutorCom");
        String tokenComentador = testUsuarios.registrarVerificarYLoguear(rest, "comentador@artagram.test", "Pass1234!", "Comentador");
        PublicacionDto pub = publicar(tokenAutor, "Obra para Comentar");

        // 1. Crear comentario
        CrearComentarioRequest req = new CrearComentarioRequest("¡Excelente obra! Me encantan las luces.");
        ResponseEntity<ComentarioDto> respCom = rest.postForEntity(
                "/api/publicaciones/" + pub.id() + "/comentarios",
                new HttpEntity<>(req, auth(tokenComentador)),
                ComentarioDto.class
        );
        assertThat(respCom.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ComentarioDto comentario = respCom.getBody();
        assertThat(comentario).isNotNull();
        assertThat(comentario.contenido()).isEqualTo("¡Excelente obra! Me encantan las luces.");

        // 2. Listar comentarios
        ResponseEntity<List<ComentarioDto>> respLista = rest.exchange(
                "/api/publicaciones/" + pub.id() + "/comentarios",
                HttpMethod.GET,
                new HttpEntity<>(auth(tokenAutor)),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(respLista.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respLista.getBody()).hasSize(1);
        assertThat(respLista.getBody().get(0).id()).isEqualTo(comentario.id());

        // 3. Eliminar comentario
        ResponseEntity<Void> respDel = rest.exchange(
                "/api/comentarios/" + comentario.id(),
                HttpMethod.DELETE,
                new HttpEntity<>(auth(tokenComentador)),
                Void.class
        );
        assertThat(respDel.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 4. Verificar lista vacía
        ResponseEntity<List<ComentarioDto>> respLista2 = rest.exchange(
                "/api/publicaciones/" + pub.id() + "/comentarios",
                HttpMethod.GET,
                new HttpEntity<>(auth(tokenAutor)),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(respLista2.getBody()).isEmpty();
    }

    @Test
    @DisplayName("Módulo de IA: análisis técnico persistido, historial y sugerencias de etiquetas")
    void cicloIa() {
        String tokenAutor = testUsuarios.registrarVerificarYLoguear(rest, "autor_ia@artagram.test", "Pass1234!", "AutorIa");
        PublicacionDto pub = publicar(tokenAutor, "Obra para IA");

        // 1. Ejecutar análisis técnico
        ResponseEntity<AnalisisObraDto> respAnalisis = rest.postForEntity(
                "/api/publicaciones/" + pub.id() + "/analisis?tipo=TECNICO",
                new HttpEntity<>(auth(tokenAutor)),
                AnalisisObraDto.class
        );
        assertThat(respAnalisis.getStatusCode()).isEqualTo(HttpStatus.OK);
        AnalisisObraDto analisis = respAnalisis.getBody();
        assertThat(analisis).isNotNull();
        assertThat(analisis.tipo()).isEqualTo(TipoAnalisis.TECNICO);
        assertThat(analisis.resultadoJson()).contains("puntosFuertes");

        // 2. Consultar último análisis (historial en caché sin reejecución)
        ResponseEntity<AnalisisObraDto> respUltimo = rest.exchange(
                "/api/publicaciones/" + pub.id() + "/analisis/ultimo?tipo=TECNICO",
                HttpMethod.GET,
                new HttpEntity<>(auth(tokenAutor)),
                AnalisisObraDto.class
        );
        assertThat(respUltimo.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respUltimo.getBody().id()).isEqualTo(analisis.id());

        // 3. Consultar historial completo
        ResponseEntity<List<AnalisisObraDto>> respHistorial = rest.exchange(
                "/api/publicaciones/" + pub.id() + "/analisis",
                HttpMethod.GET,
                new HttpEntity<>(auth(tokenAutor)),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(respHistorial.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respHistorial.getBody()).hasSize(1);

        // 4. Pre-publish sugerencia de etiquetas
        MultiValueMap<String, Object> bodySugerencias = new LinkedMultiValueMap<>();
        bodySugerencias.add("imagen", archivo("preview.png", pngDePrueba()));
        HttpHeaders headers = auth(tokenAutor);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<SugerenciaEtiquetasResultado> respSugerencias = rest.postForEntity(
                "/api/ia/sugerencias",
                new HttpEntity<>(bodySugerencias, headers),
                SugerenciaEtiquetasResultado.class
        );
        assertThat(respSugerencias.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respSugerencias.getBody().etiquetas()).isNotEmpty();
    }
}

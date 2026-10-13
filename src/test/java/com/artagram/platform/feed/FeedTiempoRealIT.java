package com.artagram.platform.feed;

import static org.assertj.core.api.Assertions.assertThat;

import com.artagram.platform.feed.comentario.ComentarioDto;
import com.artagram.platform.feed.comentario.ComentarioEvento;
import com.artagram.platform.feed.dto.LikeEventoDto;
import com.artagram.platform.feed.dto.PublicacionDto;
import com.artagram.platform.support.IntegrationTestBase;
import com.artagram.platform.support.StompTestClients;
import com.artagram.platform.support.TestUsuarios;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * HU-07 y comentarios en vivo con un cliente STOMP real: quien esta viendo una
 * obra recibe el contador de likes y los comentarios (nuevos y borrados) sin
 * recargar la pagina.
 */
class FeedTiempoRealIT extends IntegrationTestBase {

    @LocalServerPort
    private int puerto;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TestUsuarios testUsuarios;

    @Autowired
    private StompTestClients stompClients;

    @Test
    void quienVeLaObraRecibeLikesYComentariosEnVivo() throws Exception {
        String tokenAutora = testUsuarios.registrarVerificarYLoguear(rest, "vivoAutora@correo.com", "claveSegura123", "vivo_autora");
        String tokenFan = testUsuarios.registrarVerificarYLoguear(rest, "vivoFan@correo.com", "claveSegura123", "vivo_fan");
        PublicacionDto obra = publicarObra(tokenAutora);

        StompSession sesionAutora = stompClients.conectar(puerto, tokenAutora);
        String base = "/topic/publicaciones/" + obra.id();
        BlockingQueue<LikeEventoDto> likes = stompClients.suscribir(sesionAutora, base + "/likes", LikeEventoDto.class);
        BlockingQueue<ComentarioEvento> comentarios = stompClients.suscribir(sesionAutora, base + "/comentarios", ComentarioEvento.class);
        Thread.sleep(300);

        rest.exchange("/api/publicaciones/" + obra.id() + "/like", HttpMethod.POST, new HttpEntity<>(auth(tokenFan)), String.class);
        LikeEventoDto like = likes.poll(5, TimeUnit.SECONDS);
        assertThat(like).isNotNull();
        assertThat(like.tipo()).isEqualTo("LIKES_ACTUALIZADOS");
        assertThat(like.contadorLikes()).isEqualTo(1);

        rest.exchange("/api/publicaciones/" + obra.id() + "/like", HttpMethod.POST, new HttpEntity<>(auth(tokenFan)), String.class);
        assertThat(likes.poll(5, TimeUnit.SECONDS).contadorLikes()).isZero();

        ComentarioDto creado = rest.exchange("/api/publicaciones/" + obra.id() + "/comentarios", HttpMethod.POST,
                new HttpEntity<>(Map.of("contenido", "Que buena textura"), auth(tokenFan)), ComentarioDto.class).getBody();
        ComentarioEvento nuevo = comentarios.poll(5, TimeUnit.SECONDS);
        assertThat(nuevo).isNotNull();
        assertThat(nuevo.tipo()).isEqualTo("COMENTARIO_CREADO");
        assertThat(nuevo.contadorComentarios()).isEqualTo(1);

        rest.exchange("/api/comentarios/" + creado.id(), HttpMethod.DELETE, new HttpEntity<>(auth(tokenFan)), Void.class);
        ComentarioEvento borrado = comentarios.poll(5, TimeUnit.SECONDS);
        assertThat(borrado).isNotNull();
        assertThat(borrado.tipo()).isEqualTo("COMENTARIO_ELIMINADO");
        assertThat(borrado.comentario().id()).isEqualTo(creado.id());
        assertThat(borrado.contadorComentarios()).isZero();
    }

    private HttpHeaders auth(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private PublicacionDto publicarObra(String token) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("titulo", "Obra en vivo");
        body.add("obraFinal", new ByteArrayResource(pngDeUnPixel()) {
            @Override
            public String getFilename() {
                return "final.png";
            }
        });
        HttpHeaders headers = auth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return rest.postForEntity("/api/publicaciones", new HttpEntity<>(body, headers), PublicacionDto.class).getBody();
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
}

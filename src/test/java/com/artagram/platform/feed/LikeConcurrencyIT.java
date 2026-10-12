package com.artagram.platform.feed;

import static org.assertj.core.api.Assertions.assertThat;

import com.artagram.platform.auth.JwtService;
import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.feed.dto.LikeResponse;
import com.artagram.platform.feed.dto.PublicacionDto;
import com.artagram.platform.support.IntegrationTestBase;
import com.artagram.platform.support.TestUsuarios;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * HU-07: el escenario que de verdad importa es "200 personas le dan like en
 * el mismo segundo y el contador queda exactamente en 200, sin perder ninguna
 * reaccion ni reventar por bloqueos de base de datos".
 */
class LikeConcurrencyIT extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TestUsuarios testUsuarios;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PublicacionRepository publicacionRepository;

    private HttpHeaders auth(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void doscientasPersonasDandoLikeALaVezDejanElContadorExactoEnDoscientos() throws Exception {
        String tokenAutor = testUsuarios.registrarVerificarYLoguear(rest, "autorLikes@correo.com", "claveSegura123", "autor_likes");
        PublicacionDto publicacion = publicarObraDePrueba(tokenAutor);

        int participantes = 200;
        String[] tokens = new String[participantes];
        for (int i = 0; i < participantes; i++) {
            Usuario usuario = new Usuario("liker" + i + "@correo.com", "liker_" + i, passwordEncoder.encode("x"));
            usuario.marcarVerificado();
            usuario = usuarioRepository.save(usuario);
            tokens[i] = jwtService.generarToken(usuario);
        }

        ExecutorService pool = Executors.newFixedThreadPool(50);
        CountDownLatch listos = new CountDownLatch(participantes);
        CountDownLatch salida = new CountDownLatch(1);

        List<Runnable> tareas = IntStream.range(0, participantes).<Runnable>mapToObj(i -> () -> {
            listos.countDown();
            try {
                salida.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ResponseEntity<LikeResponse> resp = rest.postForEntity("/api/publicaciones/" + publicacion.id() + "/like",
                    new HttpEntity<>(auth(tokens[i])), LikeResponse.class);
            if (resp.getStatusCode() != HttpStatus.OK) {
                throw new IllegalStateException("like fallo con " + resp.getStatusCode());
            }
        }).toList();

        tareas.forEach(pool::submit);
        listos.await(10, TimeUnit.SECONDS);
        salida.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        int contador = publicacionRepository.findById(publicacion.id()).orElseThrow().getContadorLikes();
        assertThat(contador).isEqualTo(200);
    }

    @Test
    void volverAApretarElCorazonLoQuitaYElContadorNuncaQuedaNegativo() {
        String tokenAutor = testUsuarios.registrarVerificarYLoguear(rest, "autorLikes2@correo.com", "claveSegura123", "autor_likes_dos");
        String tokenFan = testUsuarios.registrarVerificarYLoguear(rest, "fanLikes@correo.com", "claveSegura123", "fan_likes");
        PublicacionDto publicacion = publicarObraDePrueba(tokenAutor);

        ResponseEntity<LikeResponse> primero = rest.postForEntity("/api/publicaciones/" + publicacion.id() + "/like",
                new HttpEntity<>(auth(tokenFan)), LikeResponse.class);
        assertThat(primero.getBody().meGusta()).isTrue();
        assertThat(primero.getBody().contadorLikes()).isEqualTo(1);

        ResponseEntity<LikeResponse> segundo = rest.postForEntity("/api/publicaciones/" + publicacion.id() + "/like",
                new HttpEntity<>(auth(tokenFan)), LikeResponse.class);
        assertThat(segundo.getBody().meGusta()).isFalse();
        assertThat(segundo.getBody().contadorLikes()).isEqualTo(0);

        for (int i = 0; i < 3; i++) {
            rest.postForEntity("/api/publicaciones/" + publicacion.id() + "/like", new HttpEntity<>(auth(tokenFan)), LikeResponse.class);
        }
        int contadorFinal = publicacionRepository.findById(publicacion.id()).orElseThrow().getContadorLikes();
        assertThat(contadorFinal).isGreaterThanOrEqualTo(0);
    }

    private PublicacionDto publicarObraDePrueba(String token) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("titulo", "Obra para likes " + UUID.randomUUID());
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
            java.awt.image.BufferedImage imagen = new java.awt.image.BufferedImage(4, 4, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(imagen, "png", out);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}

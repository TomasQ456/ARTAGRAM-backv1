package com.artagram.platform.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import com.artagram.platform.canvas.dto.BloqueoTrazoResponse;
import com.artagram.platform.canvas.dto.EditarTrazoRequest;
import com.artagram.platform.canvas.dto.EnviarPuntosRequest;
import com.artagram.platform.canvas.dto.TrazoDto;
import com.artagram.platform.community.dto.ComunidadResponse;
import com.artagram.platform.community.dto.CrearComunidadRequest;
import com.artagram.platform.support.IntegrationTestBase;
import com.artagram.platform.support.StompTestClients;
import com.artagram.platform.support.TestUsuarios;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.stomp.StompSession;

/**
 * HU-15: exclusion mutua sobre un trazo compartido. El escenario que de
 * verdad importa es "6 la tocan casi al tiempo, solo una gana" -> se prueba
 * con 6 hilos reales pegandole al mismo endpoint de bloqueo.
 */
class TrazoLockIT extends IntegrationTestBase {

    @LocalServerPort
    private int puerto;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TestUsuarios testUsuarios;

    @Autowired
    private StompTestClients stompClients;

    @Test
    void seisIntentanTomarElMismoTrazoYSoloUnoGana() throws Exception {
        String tokenCreador = testUsuarios.registrarVerificarYLoguear(rest, "editorcreador@correo.com", "claveSegura123", "editor_creador");
        HttpHeaders headersCreador = new HttpHeaders();
        headersCreador.setBearerAuth(tokenCreador);
        ComunidadResponse comunidad = rest.exchange("/api/comunidades", HttpMethod.POST,
                new HttpEntity<>(new CrearComunidadRequest("Mural Editable", "desc", true), headersCreador),
                ComunidadResponse.class).getBody();

        UUID trazoId = publicarTrazoYObtenerId(comunidad.id(), tokenCreador);

        int participantes = 6;
        HttpHeaders[] headers = new HttpHeaders[participantes];
        for (int i = 0; i < participantes; i++) {
            String token = testUsuarios.registrarVerificarYLoguear(rest, "editorlock" + i + "@correo.com", "claveSegura123", "editor_lock_" + i);
            HttpHeaders h = new HttpHeaders();
            h.setBearerAuth(token);
            rest.exchange("/api/comunidades/" + comunidad.id() + "/miembros", HttpMethod.POST, new HttpEntity<>(null, h), ComunidadResponse.class);
            headers[i] = h;
        }

        ExecutorService pool = Executors.newFixedThreadPool(participantes);
        CountDownLatch listos = new CountDownLatch(participantes);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger ganadores = new AtomicInteger();
        AtomicInteger rechazados = new AtomicInteger();

        List<Runnable> tareas = IntStream.range(0, participantes).<Runnable>mapToObj(i -> () -> {
            listos.countDown();
            try {
                salida.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ResponseEntity<BloqueoTrazoResponse> resp = rest.exchange(
                    "/api/comunidades/" + comunidad.id() + "/lienzo/trazos/" + trazoId + "/bloqueo",
                    HttpMethod.POST, new HttpEntity<>(headers[i]), BloqueoTrazoResponse.class);
            if (resp.getStatusCode() == HttpStatus.OK) {
                ganadores.incrementAndGet();
            } else {
                rechazados.incrementAndGet();
                assertThat(resp.getBody().propietarioNombre()).isNotBlank();
            }
        }).toList();

        tareas.forEach(pool::submit);
        listos.await(5, TimeUnit.SECONDS);
        salida.countDown();
        pool.shutdown();
        pool.awaitTermination(20, TimeUnit.SECONDS);

        assertThat(ganadores.get()).isEqualTo(1);
        assertThat(rechazados.get()).isEqualTo(5);
    }

    @Test
    void soloElDuenoDelBloqueoPuedeEditarYLiberarLoDejaDisponible() throws Exception {
        String tokenA = testUsuarios.registrarVerificarYLoguear(rest, "editorA@correo.com", "claveSegura123", "editor_a");
        String tokenB = testUsuarios.registrarVerificarYLoguear(rest, "editorB@correo.com", "claveSegura123", "editor_b");
        HttpHeaders headersA = new HttpHeaders();
        headersA.setBearerAuth(tokenA);
        HttpHeaders headersB = new HttpHeaders();
        headersB.setBearerAuth(tokenB);

        ComunidadResponse comunidad = rest.exchange("/api/comunidades", HttpMethod.POST,
                new HttpEntity<>(new CrearComunidadRequest("Mural Compartido", "desc", true), headersA),
                ComunidadResponse.class).getBody();
        rest.exchange("/api/comunidades/" + comunidad.id() + "/miembros", HttpMethod.POST, new HttpEntity<>(null, headersB), ComunidadResponse.class);

        UUID trazoId = publicarTrazoYObtenerId(comunidad.id(), tokenA);

        ResponseEntity<BloqueoTrazoResponse> tomaA = rest.exchange(
                "/api/comunidades/" + comunidad.id() + "/lienzo/trazos/" + trazoId + "/bloqueo",
                HttpMethod.POST, new HttpEntity<>(headersA), BloqueoTrazoResponse.class);
        assertThat(tomaA.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Reentrante para el mismo dueno (recargo la pagina con el trazo tomado); B sigue fuera
        ResponseEntity<BloqueoTrazoResponse> retomaA = rest.exchange(
                "/api/comunidades/" + comunidad.id() + "/lienzo/trazos/" + trazoId + "/bloqueo",
                HttpMethod.POST, new HttpEntity<>(headersA), BloqueoTrazoResponse.class);
        assertThat(retomaA.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<BloqueoTrazoResponse> intentoB = rest.exchange(
                "/api/comunidades/" + comunidad.id() + "/lienzo/trazos/" + trazoId + "/bloqueo",
                HttpMethod.POST, new HttpEntity<>(headersB), BloqueoTrazoResponse.class);
        assertThat(intentoB.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(intentoB.getBody().propietarioNombre()).isEqualTo("editor_a");

        EditarTrazoRequest edicion = new EditarTrazoRequest(TipoPincel.MARCADOR, "#ABCDEF", 10.0, 0.8, 1,
                List.of(new Punto(0.5, 0.5)));

        ResponseEntity<Void> edicionB = rest.exchange("/api/comunidades/" + comunidad.id() + "/lienzo/trazos/" + trazoId,
                HttpMethod.PUT, new HttpEntity<>(edicion, headersB), Void.class);
        assertThat(edicionB.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Void> edicionA = rest.exchange("/api/comunidades/" + comunidad.id() + "/lienzo/trazos/" + trazoId,
                HttpMethod.PUT, new HttpEntity<>(edicion, headersA), Void.class);
        assertThat(edicionA.getStatusCode()).isEqualTo(HttpStatus.OK);

        rest.exchange("/api/comunidades/" + comunidad.id() + "/lienzo/trazos/" + trazoId + "/bloqueo",
                HttpMethod.DELETE, new HttpEntity<>(headersA), Void.class);

        ResponseEntity<BloqueoTrazoResponse> tomaB = rest.exchange(
                "/api/comunidades/" + comunidad.id() + "/lienzo/trazos/" + trazoId + "/bloqueo",
                HttpMethod.POST, new HttpEntity<>(headersB), BloqueoTrazoResponse.class);
        assertThat(tomaB.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private UUID publicarTrazoYObtenerId(UUID comunidadId, String token) throws Exception {
        StompSession sesion = stompClients.conectar(puerto, token);
        EnviarPuntosRequest request = new EnviarPuntosRequest(UUID.randomUUID(), TipoPincel.LAPIZ, "#333333",
                4.0, 1.0, 0, List.of(new Punto(0.2, 0.2), new Punto(0.3, 0.3)));
        sesion.send("/app/lienzo/" + comunidadId + "/final", request);
        Thread.sleep(500);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<TrazoDto[]> snapshot = rest.exchange("/api/comunidades/" + comunidadId + "/lienzo/trazos?desde=0",
                HttpMethod.GET, new HttpEntity<>(headers), TrazoDto[].class);
        return snapshot.getBody()[0].id();
    }
}

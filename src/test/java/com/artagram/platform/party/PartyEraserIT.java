package com.artagram.platform.party;

import static org.assertj.core.api.Assertions.assertThat;

import com.artagram.platform.party.dto.BloqueoBorradorResponse;
import com.artagram.platform.party.dto.CrearSalaRequest;
import com.artagram.platform.party.dto.SalaFiestaResponse;
import com.artagram.platform.support.IntegrationTestBase;
import com.artagram.platform.support.TestUsuarios;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * HU-20: el caso de exclusion mutua mas limpio del proyecto. 6 participantes
 * lo tocan casi al tiempo, solo uno gana; solo el dueno puede borrar; al
 * liberarlo, vuelve a quedar disponible para el siguiente.
 */
class PartyEraserIT extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TestUsuarios testUsuarios;

    private HttpHeaders auth(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private SalaFiestaResponse crearSalaYEmpezarRonda(String tokenAnfitrion) {
        SalaFiestaResponse sala = rest.exchange("/api/fiestas", HttpMethod.POST,
                new HttpEntity<>(new CrearSalaRequest(30, 10), auth(tokenAnfitrion)), SalaFiestaResponse.class).getBody();
        rest.exchange("/api/fiestas/" + sala.id() + "/rondas", HttpMethod.POST, new HttpEntity<>(auth(tokenAnfitrion)), Void.class);
        return sala;
    }

    @Test
    void seisParticipantesPeleanPorElBorradorYSoloUnoGana() throws Exception {
        String tokenAnfitrion = testUsuarios.registrarVerificarYLoguear(rest, "goma0@correo.com", "claveSegura123", "goma_cero");
        SalaFiestaResponse sala = crearSalaYEmpezarRonda(tokenAnfitrion);

        int participantes = 6;
        HttpHeaders[] headers = new HttpHeaders[participantes];
        for (int i = 0; i < participantes; i++) {
            String token = testUsuarios.registrarVerificarYLoguear(rest, "gomalock" + i + "@correo.com", "claveSegura123", "goma_lock_" + i);
            HttpHeaders h = auth(token);
            rest.postForEntity("/api/fiestas/unirse?codigo=" + sala.codigo(), new HttpEntity<>(h), SalaFiestaResponse.class);
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
            ResponseEntity<BloqueoBorradorResponse> resp = rest.exchange("/api/fiestas/" + sala.id() + "/borrador",
                    HttpMethod.POST, new HttpEntity<>(headers[i]), BloqueoBorradorResponse.class);
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
    void soloElDuenoDelBorradorPuedeUsarloYAlLiberarQuedaDisponible() {
        String tokenA = testUsuarios.registrarVerificarYLoguear(rest, "gomaA@correo.com", "claveSegura123", "goma_a");
        String tokenB = testUsuarios.registrarVerificarYLoguear(rest, "gomaB@correo.com", "claveSegura123", "goma_b");
        SalaFiestaResponse sala = crearSalaYEmpezarRonda(tokenA);
        rest.postForEntity("/api/fiestas/unirse?codigo=" + sala.codigo(), new HttpEntity<>(auth(tokenB)), SalaFiestaResponse.class);

        ResponseEntity<BloqueoBorradorResponse> tomaA = rest.exchange("/api/fiestas/" + sala.id() + "/borrador",
                HttpMethod.POST, new HttpEntity<>(auth(tokenA)), BloqueoBorradorResponse.class);
        assertThat(tomaA.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Un no-dueno no puede tomar el borrador mientras A lo tiene.
        ResponseEntity<BloqueoBorradorResponse> tomaB = rest.exchange("/api/fiestas/" + sala.id() + "/borrador",
                HttpMethod.POST, new HttpEntity<>(auth(tokenB)), BloqueoBorradorResponse.class);
        assertThat(tomaB.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(tomaB.getBody().propietarioNombre()).isEqualTo("goma_a");

        rest.exchange("/api/fiestas/" + sala.id() + "/borrador", HttpMethod.DELETE, new HttpEntity<>(auth(tokenA)), Void.class);

        ResponseEntity<BloqueoBorradorResponse> tomaBTrasLiberar = rest.exchange("/api/fiestas/" + sala.id() + "/borrador",
                HttpMethod.POST, new HttpEntity<>(auth(tokenB)), BloqueoBorradorResponse.class);
        assertThat(tomaBTrasLiberar.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}

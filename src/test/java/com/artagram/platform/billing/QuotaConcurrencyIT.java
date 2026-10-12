package com.artagram.platform.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.artagram.platform.community.dto.ComunidadResponse;
import com.artagram.platform.community.dto.CrearComunidadRequest;
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
 * HU-26, el escenario que de verdad importa: 5 solicitudes de creacion de
 * comunidad disparadas al mismo tiempo cuando el usuario ya tiene 2 de 3, y
 * solo UNA debe completarse. Prueba el bloqueo pesimista de UsoCuentaRepository.
 */
class QuotaConcurrencyIT extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TestUsuarios testUsuarios;

    @Autowired
    private UsoCuentaRepository usoCuentaRepository;

    @Test
    void cincoSolicitudesConcurrentesSoloUnaCompletaElTopeDeTres() throws InterruptedException {
        String token = testUsuarios.registrarVerificarYLoguear(rest, "cuotas1@correo.com", "claveSegura123", "cuotas_uno");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        crearComunidad(headers, "Cuota Comunidad Uno");
        crearComunidad(headers, "Cuota Comunidad Dos");

        int intentos = 5;
        ExecutorService pool = Executors.newFixedThreadPool(intentos);
        CountDownLatch salida = new CountDownLatch(1);
        CountDownLatch listos = new CountDownLatch(intentos);
        AtomicInteger exitosos = new AtomicInteger();
        AtomicInteger rechazados = new AtomicInteger();

        List<Runnable> tareas = IntStream.range(0, intentos).<Runnable>mapToObj(i -> () -> {
            listos.countDown();
            try {
                salida.await();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            ResponseEntity<ComunidadResponse> respuesta = rest.exchange("/api/comunidades", HttpMethod.POST,
                    new HttpEntity<>(new CrearComunidadRequest("Concurrente-" + i, "desc", true), headers),
                    ComunidadResponse.class);
            if (respuesta.getStatusCode() == HttpStatus.CREATED) {
                exitosos.incrementAndGet();
            } else {
                rechazados.incrementAndGet();
            }
        }).toList();

        tareas.forEach(pool::submit);
        listos.await(5, TimeUnit.SECONDS);
        salida.countDown();
        pool.shutdown();
        pool.awaitTermination(20, TimeUnit.SECONDS);

        assertThat(exitosos.get()).isEqualTo(1);
        assertThat(rechazados.get()).isEqualTo(4);

        UsoCuenta uso = usoCuentaRepository.findById(testUsuarios.obtenerPorCorreo("cuotas1@correo.com").getId()).orElseThrow();
        assertThat(uso.getComunidadesCreadas()).isEqualTo(3);
    }

    @Test
    void llegueAlTopeDeTresYLaCuartaSeRechaza() {
        String token = testUsuarios.registrarVerificarYLoguear(rest, "cuotas2@correo.com", "claveSegura123", "cuotas_dos");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        crearComunidad(headers, "Tope Uno");
        crearComunidad(headers, "Tope Dos");
        crearComunidad(headers, "Tope Tres");

        ResponseEntity<String> cuarta = rest.exchange("/api/comunidades", HttpMethod.POST,
                new HttpEntity<>(new CrearComunidadRequest("Tope Cuatro", "desc", true), headers), String.class);

        assertThat(cuarta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(cuarta.getBody()).containsIgnoringCase("premium");
    }

    @Test
    void liberoCupoAlEliminarYPuedoCrearOtra() {
        String token = testUsuarios.registrarVerificarYLoguear(rest, "cuotas3@correo.com", "claveSegura123", "cuotas_tres");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        crearComunidad(headers, "Libero Uno");
        crearComunidad(headers, "Libero Dos");
        ComunidadResponse tercera = crearComunidadYRetornar(headers, "Libero Tres");

        rest.exchange("/api/comunidades/" + tercera.id(), HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        ResponseEntity<ComunidadResponse> cuarta = rest.exchange("/api/comunidades", HttpMethod.POST,
                new HttpEntity<>(new CrearComunidadRequest("Libero Cuatro", "desc", true), headers), ComunidadResponse.class);
        assertThat(cuarta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private ComunidadResponse crearComunidadYRetornar(HttpHeaders headers, String nombre) {
        return rest.exchange("/api/comunidades", HttpMethod.POST,
                new HttpEntity<>(new CrearComunidadRequest(nombre, "desc", true), headers), ComunidadResponse.class).getBody();
    }

    private void crearComunidad(HttpHeaders headers, String nombre) {
        ResponseEntity<ComunidadResponse> respuesta = rest.exchange("/api/comunidades", HttpMethod.POST,
                new HttpEntity<>(new CrearComunidadRequest(nombre, "desc", true), headers), ComunidadResponse.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}

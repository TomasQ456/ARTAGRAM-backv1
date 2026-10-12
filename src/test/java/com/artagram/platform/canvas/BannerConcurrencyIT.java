package com.artagram.platform.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import com.artagram.platform.canvas.dto.PintarRegionRequest;
import com.artagram.platform.canvas.dto.RegionEstadoDto;
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
 * HU-16: 30 pintando en regiones distintas no pierden nada; dos escrituras
 * casi simultaneas en LA MISMA region tampoco se pisan; y un cliente que se
 * pasa de ritmo se topa con el limite.
 */
class BannerConcurrencyIT extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TestUsuarios testUsuarios;

    @Test
    void treintaPersonasPintandoRegionesDistintasNoPierdenNada() throws Exception {
        String tokenCreador = testUsuarios.registrarVerificarYLoguear(rest, "muralista0@correo.com", "claveSegura123", "muralista_cero");
        HttpHeaders headersCreador = new HttpHeaders();
        headersCreador.setBearerAuth(tokenCreador);
        ComunidadResponse comunidad = rest.exchange("/api/comunidades", HttpMethod.POST,
                new HttpEntity<>(new CrearComunidadRequest("Mural Masivo", "desc", true), headersCreador),
                ComunidadResponse.class).getBody();

        int participantes = 30;
        HttpHeaders[] headers = new HttpHeaders[participantes];
        for (int i = 0; i < participantes; i++) {
            String token = testUsuarios.registrarVerificarYLoguear(rest, "muralista" + i + "@correo.com", "claveSegura123", "muralista_" + i);
            HttpHeaders h = new HttpHeaders();
            h.setBearerAuth(token);
            rest.exchange("/api/comunidades/" + comunidad.id() + "/miembros", HttpMethod.POST, new HttpEntity<>(null, h), ComunidadResponse.class);
            headers[i] = h;
        }

        ExecutorService pool = Executors.newFixedThreadPool(participantes);
        CountDownLatch listos = new CountDownLatch(participantes);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitosos = new AtomicInteger();

        List<Runnable> tareas = IntStream.range(0, participantes).<Runnable>mapToObj(i -> () -> {
            listos.countDown();
            try {
                salida.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            PintarRegionRequest request = new PintarRegionRequest(i, TipoPincel.LAPIZ, "#00AA00", 3.0, 1.0,
                    List.of(new Punto(0.1, 0.1), new Punto(0.5, 0.5)));
            ResponseEntity<Void> resp = rest.exchange("/api/comunidades/" + comunidad.id() + "/banner/pintar",
                    HttpMethod.POST, new HttpEntity<>(request, headers[i]), Void.class);
            if (resp.getStatusCode() == HttpStatus.OK) {
                exitosos.incrementAndGet();
            }
        }).toList();

        tareas.forEach(pool::submit);
        listos.await(5, TimeUnit.SECONDS);
        salida.countDown();
        pool.shutdown();
        pool.awaitTermination(20, TimeUnit.SECONDS);

        assertThat(exitosos.get()).isEqualTo(participantes);

        List<RegionEstadoDto> estado = obtenerEstado(comunidad.id(), headersCreador);
        for (int i = 0; i < participantes; i++) {
            RegionEstadoDto region = estado.get(i);
            assertThat(region.trazosDesdeSnapshot()).hasSize(1);
        }
    }

    @Test
    void dosEscriturasCasiSimultaneasEnLaMismaRegionNoSePierden() throws Exception {
        String token = testUsuarios.registrarVerificarYLoguear(rest, "muralistaR@correo.com", "claveSegura123", "muralista_region");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ComunidadResponse comunidad = rest.exchange("/api/comunidades", HttpMethod.POST,
                new HttpEntity<>(new CrearComunidadRequest("Mural Region Compartida", "desc", true), headers),
                ComunidadResponse.class).getBody();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitosos = new AtomicInteger();

        Runnable tarea = () -> {
            listos.countDown();
            try {
                salida.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            PintarRegionRequest request = new PintarRegionRequest(55, TipoPincel.MARCADOR, "#FF0000", 5.0, 1.0,
                    List.of(new Punto(0.2, 0.2), new Punto(0.4, 0.4)));
            ResponseEntity<Void> resp = rest.exchange("/api/comunidades/" + comunidad.id() + "/banner/pintar",
                    HttpMethod.POST, new HttpEntity<>(request, headers), Void.class);
            if (resp.getStatusCode() == HttpStatus.OK) {
                exitosos.incrementAndGet();
            }
        };
        pool.submit(tarea);
        pool.submit(tarea);
        listos.await(5, TimeUnit.SECONDS);
        salida.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(exitosos.get()).isEqualTo(2);
        List<RegionEstadoDto> estado = obtenerEstado(comunidad.id(), headers);
        assertThat(estado.get(55).trazosDesdeSnapshot()).hasSize(2);
        List<Long> secuencias = estado.get(55).trazosDesdeSnapshot().stream().map(t -> t.secuencia()).sorted().toList();
        assertThat(secuencias).containsExactly(1L, 2L);
    }

    @Test
    void quienSePasaDeRitmoSeTopaConElLimite() throws Exception {
        String token = testUsuarios.registrarVerificarYLoguear(rest, "muralistaVeloz@correo.com", "claveSegura123", "muralista_veloz");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ComunidadResponse comunidad = rest.exchange("/api/comunidades", HttpMethod.POST,
                new HttpEntity<>(new CrearComunidadRequest("Mural Veloz", "desc", true), headers),
                ComunidadResponse.class).getBody();

        int intentos = 150;
        ExecutorService pool = Executors.newFixedThreadPool(20);
        AtomicInteger exitosos = new AtomicInteger();
        AtomicInteger limitados = new AtomicInteger();
        List<? extends java.util.concurrent.Future<?>> futuros = IntStream.range(0, intentos).mapToObj(i -> pool.submit(() -> {
            PintarRegionRequest request = new PintarRegionRequest(i % 100, TipoPincel.LAPIZ, "#0000FF", 2.0, 1.0,
                    List.of(new Punto(0.1, 0.1)));
            ResponseEntity<Void> resp = rest.exchange("/api/comunidades/" + comunidad.id() + "/banner/pintar",
                    HttpMethod.POST, new HttpEntity<>(request, headers), Void.class);
            if (resp.getStatusCode() == HttpStatus.OK) {
                exitosos.incrementAndGet();
            } else if (resp.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                limitados.incrementAndGet();
            }
        })).toList();
        for (var f : futuros) {
            f.get(20, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(limitados.get()).isGreaterThan(0);
        assertThat(exitosos.get()).isLessThanOrEqualTo(100);
        assertThat(exitosos.get() + limitados.get()).isEqualTo(intentos);
    }

    @SuppressWarnings("unchecked")
    private List<RegionEstadoDto> obtenerEstado(java.util.UUID comunidadId, HttpHeaders headers) {
        ResponseEntity<RegionEstadoDto[]> resp = rest.exchange("/api/comunidades/" + comunidadId + "/banner",
                HttpMethod.GET, new HttpEntity<>(headers), RegionEstadoDto[].class);
        return List.of(resp.getBody());
    }
}

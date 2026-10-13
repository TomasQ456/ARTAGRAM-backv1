package com.artagram.platform.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import com.artagram.platform.canvas.dto.EnviarPuntosRequest;
import com.artagram.platform.canvas.dto.TrazoDto;
import com.artagram.platform.canvas.dto.TrazoEventoDto;
import com.artagram.platform.community.dto.ComunidadResponse;
import com.artagram.platform.community.dto.CrearComunidadRequest;
import com.artagram.platform.support.IntegrationTestBase;
import com.artagram.platform.support.StompTestClients;
import com.artagram.platform.support.TestUsuarios;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.stomp.StompSession;

/**
 * HU-12 de punta a punta con clientes STOMP reales (no mocks): el trazo de uno
 * le llega al otro, queda con secuencia, y alguien que no es miembro no recibe
 * nada aunque conozca el topico (HU-11).
 */
class CanvasWebSocketIT extends IntegrationTestBase {

    @LocalServerPort
    private int puerto;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TestUsuarios testUsuarios;

    @Autowired
    private StompTestClients stompClients;

    @Test
    void miTrazoLeLlegaAOtroMiembroYQuedaConSecuencia() throws Exception {
        String tokenA = testUsuarios.registrarVerificarYLoguear(rest, "dibujanteA@correo.com", "claveSegura123", "dibujante_a");
        String tokenB = testUsuarios.registrarVerificarYLoguear(rest, "dibujanteB@correo.com", "claveSegura123", "dibujante_b");

        HttpHeaders headersA = new HttpHeaders();
        headersA.setBearerAuth(tokenA);
        ComunidadResponse comunidad = rest.exchange("/api/comunidades", HttpMethod.POST,
                new HttpEntity<>(new CrearComunidadRequest("Estudio Colaborativo", "desc", true), headersA),
                ComunidadResponse.class).getBody();

        HttpHeaders headersB = new HttpHeaders();
        headersB.setBearerAuth(tokenB);
        rest.exchange("/api/comunidades/" + comunidad.id() + "/miembros", HttpMethod.POST,
                new HttpEntity<>(null, headersB), ComunidadResponse.class);

        StompSession sesionA = stompClients.conectar(puerto, tokenA);
        StompSession sesionB = stompClients.conectar(puerto, tokenB);

        String destino = "/topic/lienzo/" + comunidad.id();
        BlockingQueue<TrazoEventoDto> colaA = stompClients.suscribir(sesionA, destino, TrazoEventoDto.class);
        BlockingQueue<TrazoEventoDto> colaB = stompClients.suscribir(sesionB, destino, TrazoEventoDto.class);
        Thread.sleep(300); // dar tiempo a que el broker confirme la suscripcion

        UUID trazoEnProgresoId = UUID.randomUUID();
        EnviarPuntosRequest request = new EnviarPuntosRequest(trazoEnProgresoId, TipoPincel.ACUARELA, "#2E5E8C",
                24.0, 0.4, 0, List.of(new Punto(0.1, 0.1), new Punto(0.2, 0.15), new Punto(0.3, 0.2)));

        sesionA.send("/app/lienzo/" + comunidad.id() + "/final", request);

        TrazoEventoDto recibidoPorB = colaB.poll(5, TimeUnit.SECONDS);
        assertThat(recibidoPorB).isNotNull();
        assertThat(recibidoPorB.tipo()).isEqualTo("FINAL");
        assertThat(recibidoPorB.secuencia()).isEqualTo(1L);
        assertThat(recibidoPorB.autorNombre()).isEqualTo("dibujante_a");
        assertThat(recibidoPorB.puntos()).hasSize(3);
        // HU-13: el pincel, color, grosor y opacidad viajan intactos en el mensaje
        assertThat(recibidoPorB.tipoPincel()).isEqualTo(TipoPincel.ACUARELA);
        assertThat(recibidoPorB.colorHex()).isEqualTo("#2E5E8C");
        assertThat(recibidoPorB.grosor()).isEqualTo(24.0);
        assertThat(recibidoPorB.opacidad()).isEqualTo(0.4);

        TrazoEventoDto recibidoPorA = colaA.poll(5, TimeUnit.SECONDS);
        assertThat(recibidoPorA).isNotNull();

        ResponseEntity<TrazoDto[]> snapshot = rest.exchange("/api/comunidades/" + comunidad.id() + "/lienzo/trazos?desde=0",
                HttpMethod.GET, new HttpEntity<>(headersB), TrazoDto[].class);
        assertThat(snapshot.getBody()).hasSize(1);
        assertThat(snapshot.getBody()[0].secuencia()).isEqualTo(1L);
    }

    @Test
    void unTrazoRecienDibujadoSePuedeEditarConElIdQueLlegaEnElEvento() throws Exception {
        String token = testUsuarios.registrarVerificarYLoguear(rest, "escalador@correo.com", "claveSegura123", "escalador_a");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ComunidadResponse comunidad = rest.exchange("/api/comunidades", HttpMethod.POST,
                new HttpEntity<>(new CrearComunidadRequest("Estudio Escalado", "desc", true), headers),
                ComunidadResponse.class).getBody();

        StompSession sesion = stompClients.conectar(puerto, token);
        String destino = "/topic/lienzo/" + comunidad.id();
        BlockingQueue<TrazoEventoDto> cola = stompClients.suscribir(sesion, destino, TrazoEventoDto.class);
        Thread.sleep(300);

        UUID trazoEnProgresoId = UUID.randomUUID();
        sesion.send("/app/lienzo/" + comunidad.id() + "/final", new EnviarPuntosRequest(trazoEnProgresoId,
                TipoPincel.LAPIZ, "#000000", 4.0, 1.0, 0, List.of(new Punto(0.2, 0.2), new Punto(0.4, 0.4))));
        TrazoEventoDto finalizado = cola.poll(5, TimeUnit.SECONDS);
        assertThat(finalizado).isNotNull();
        // El id real (persistido) llega en el evento y es distinto del temporal del cliente
        assertThat(finalizado.trazoId()).isNotNull().isNotEqualTo(trazoEnProgresoId);

        // Sin recargar el snapshot: bloquear y escalar el trazo usando ese id
        String base = "/api/comunidades/" + comunidad.id() + "/lienzo/trazos/" + finalizado.trazoId();
        assertThat(rest.exchange(base + "/bloqueo", HttpMethod.POST, new HttpEntity<>(null, headers), String.class)
                .getStatusCode().is2xxSuccessful()).isTrue();
        var escalado = new com.artagram.platform.canvas.dto.EditarTrazoRequest(TipoPincel.LAPIZ, "#000000", 4.0, 1.0, 0,
                List.of(new Punto(0.2, 0.2), new Punto(0.6, 0.6)));
        assertThat(rest.exchange(base, HttpMethod.PUT, new HttpEntity<>(escalado, headers), Void.class)
                .getStatusCode().is2xxSuccessful()).isTrue();

        TrazoEventoDto editado = cola.poll(5, TimeUnit.SECONDS);
        assertThat(editado).isNotNull();
        assertThat(editado.tipo()).isEqualTo("EDITADO");
        assertThat(editado.trazoId()).isEqualTo(finalizado.trazoId());
        assertThat(editado.puntos()).containsExactly(new Punto(0.2, 0.2), new Punto(0.6, 0.6));
    }

    @Test
    void quienNoEsMiembroNoRecibeNiUnSoloTrazo() throws Exception {
        String tokenA = testUsuarios.registrarVerificarYLoguear(rest, "dibujanteC@correo.com", "claveSegura123", "dibujante_c");
        String tokenExtrano = testUsuarios.registrarVerificarYLoguear(rest, "extrano@correo.com", "claveSegura123", "extrano_uno");

        HttpHeaders headersA = new HttpHeaders();
        headersA.setBearerAuth(tokenA);
        ComunidadResponse comunidad = rest.exchange("/api/comunidades", HttpMethod.POST,
                new HttpEntity<>(new CrearComunidadRequest("Comunidad Cerrada", "desc", true), headersA),
                ComunidadResponse.class).getBody();

        StompSession sesionA = stompClients.conectar(puerto, tokenA);
        StompSession sesionExtrano = stompClients.conectar(puerto, tokenExtrano);

        String destino = "/topic/lienzo/" + comunidad.id();
        BlockingQueue<TrazoEventoDto> colaExtrano = stompClients.suscribir(sesionExtrano, destino, TrazoEventoDto.class);
        Thread.sleep(300);

        EnviarPuntosRequest request = new EnviarPuntosRequest(UUID.randomUUID(), TipoPincel.LAPIZ, "#000000",
                4.0, 1.0, 0, List.of(new Punto(0.5, 0.5)));
        sesionA.send("/app/lienzo/" + comunidad.id() + "/final", request);

        TrazoEventoDto recibido = colaExtrano.poll(2, TimeUnit.SECONDS);
        assertThat(recibido).isNull();
    }

    @Test
    void alguienQueLlegaTardeRecibeTodoLoYaDibujadoEnOrden() throws Exception {
        String tokenA = testUsuarios.registrarVerificarYLoguear(rest, "dibujanteD@correo.com", "claveSegura123", "dibujante_d");
        HttpHeaders headersA = new HttpHeaders();
        headersA.setBearerAuth(tokenA);
        ComunidadResponse comunidad = rest.exchange("/api/comunidades", HttpMethod.POST,
                new HttpEntity<>(new CrearComunidadRequest("Estudio Historico", "desc", true), headersA),
                ComunidadResponse.class).getBody();

        StompSession sesionA = stompClients.conectar(puerto, tokenA);
        for (int i = 0; i < 5; i++) {
            EnviarPuntosRequest request = new EnviarPuntosRequest(UUID.randomUUID(), TipoPincel.LAPIZ, "#111111",
                    2.0, 1.0, 0, List.of(new Punto(0.1 * i, 0.1 * i)));
            sesionA.send("/app/lienzo/" + comunidad.id() + "/final", request);
        }
        Thread.sleep(500);

        String tokenTarde = testUsuarios.registrarVerificarYLoguear(rest, "llegaTarde@correo.com", "claveSegura123", "llega_tarde");
        HttpHeaders headersTarde = new HttpHeaders();
        headersTarde.setBearerAuth(tokenTarde);
        rest.exchange("/api/comunidades/" + comunidad.id() + "/miembros", HttpMethod.POST,
                new HttpEntity<>(null, headersTarde), ComunidadResponse.class);

        ResponseEntity<TrazoDto[]> snapshot = rest.exchange("/api/comunidades/" + comunidad.id() + "/lienzo/trazos?desde=0",
                HttpMethod.GET, new HttpEntity<>(headersTarde), TrazoDto[].class);

        assertThat(snapshot.getBody()).hasSize(5);
        for (int i = 0; i < 5; i++) {
            assertThat(snapshot.getBody()[i].secuencia()).isEqualTo(i + 1L);
        }
    }
}

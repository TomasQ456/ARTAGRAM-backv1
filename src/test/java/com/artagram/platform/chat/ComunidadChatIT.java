package com.artagram.platform.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.artagram.platform.chat.dto.EnviarMensajeComunidadRequest;
import com.artagram.platform.chat.dto.MensajeComunidadDto;
import com.artagram.platform.community.dto.ComunidadResponse;
import com.artagram.platform.community.dto.CrearComunidadRequest;
import com.artagram.platform.support.IntegrationTestBase;
import com.artagram.platform.support.StompTestClients;
import com.artagram.platform.support.TestUsuarios;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.stomp.StompSession;

/** HU-22: chat de comunidad, con el permiso revisado en cada mensaje. */
class ComunidadChatIT extends IntegrationTestBase {

    @LocalServerPort
    private int puerto;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TestUsuarios testUsuarios;

    @Autowired
    private StompTestClients stompClients;

    private HttpHeaders auth(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void unMensajeLeLlegaATodosLosConectadosYQuedaEnElHistorial() throws Exception {
        String tokenA = testUsuarios.registrarVerificarYLoguear(rest, "chatComA@correo.com", "claveSegura123", "chatcom_a");
        String tokenB = testUsuarios.registrarVerificarYLoguear(rest, "chatComB@correo.com", "claveSegura123", "chatcom_b");
        ComunidadResponse comunidad = rest.exchange("/api/comunidades", HttpMethod.POST,
                new HttpEntity<>(new CrearComunidadRequest("Charla Creativa", "desc", true), auth(tokenA)), ComunidadResponse.class).getBody();
        rest.exchange("/api/comunidades/" + comunidad.id() + "/miembros", HttpMethod.POST, new HttpEntity<>(auth(tokenB)), ComunidadResponse.class);

        StompSession sesionA = stompClients.conectar(puerto, tokenA);
        StompSession sesionB = stompClients.conectar(puerto, tokenB);
        String destino = "/topic/comunidad/" + comunidad.id() + "/chat";
        BlockingQueue<MensajeComunidadDto> colaA = stompClients.suscribir(sesionA, destino, MensajeComunidadDto.class);
        BlockingQueue<MensajeComunidadDto> colaB = stompClients.suscribir(sesionB, destino, MensajeComunidadDto.class);
        Thread.sleep(300);

        sesionA.send("/app/comunidad/" + comunidad.id() + "/chat/enviar", new EnviarMensajeComunidadRequest("bienvenidos al mural!"));

        assertThat(colaA.poll(5, TimeUnit.SECONDS)).isNotNull();
        MensajeComunidadDto recibidoB = colaB.poll(5, TimeUnit.SECONDS);
        assertThat(recibidoB).isNotNull();
        assertThat(recibidoB.contenido()).isEqualTo("bienvenidos al mural!");

        ResponseEntity<MensajeComunidadDto[]> historial = rest.exchange("/api/comunidades/" + comunidad.id() + "/chat",
                HttpMethod.GET, new HttpEntity<>(auth(tokenB)), MensajeComunidadDto[].class);
        assertThat(historial.getBody()).hasSize(1);
    }

    @Test
    void alguienExpulsadoNoPuedeEscribirYNadieLoRecibe() throws Exception {
        String tokenAdmin = testUsuarios.registrarVerificarYLoguear(rest, "chatComC@correo.com", "claveSegura123", "chatcom_c");
        String tokenExpulsado = testUsuarios.registrarVerificarYLoguear(rest, "chatComD@correo.com", "claveSegura123", "chatcom_d");
        ComunidadResponse comunidad = rest.exchange("/api/comunidades", HttpMethod.POST,
                new HttpEntity<>(new CrearComunidadRequest("Comunidad Moderada", "desc", true), auth(tokenAdmin)), ComunidadResponse.class).getBody();
        rest.exchange("/api/comunidades/" + comunidad.id() + "/miembros", HttpMethod.POST, new HttpEntity<>(auth(tokenExpulsado)), ComunidadResponse.class);

        UUID idExpulsado = testUsuarios.obtenerPorCorreo("chatComD@correo.com").getId();
        rest.exchange("/api/comunidades/" + comunidad.id() + "/miembros/" + idExpulsado, HttpMethod.DELETE,
                new HttpEntity<>(auth(tokenAdmin)), Void.class);

        ResponseEntity<String> respuesta = rest.postForEntity("/api/comunidades/" + comunidad.id() + "/chat",
                new HttpEntity<>(new EnviarMensajeComunidadRequest("dejenme volver"), auth(tokenExpulsado)), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<MensajeComunidadDto[]> historial = rest.exchange("/api/comunidades/" + comunidad.id() + "/chat",
                HttpMethod.GET, new HttpEntity<>(auth(tokenAdmin)), MensajeComunidadDto[].class);
        assertThat(historial.getBody()).isEmpty();
    }
}

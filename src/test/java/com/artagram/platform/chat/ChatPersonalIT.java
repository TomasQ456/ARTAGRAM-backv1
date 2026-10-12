package com.artagram.platform.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.artagram.platform.chat.dto.EnviarMensajePersonalRequest;
import com.artagram.platform.chat.dto.MensajePersonalDto;
import com.artagram.platform.support.IntegrationTestBase;
import com.artagram.platform.support.StompTestClients;
import com.artagram.platform.support.TestUsuarios;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.stomp.StompSession;

/** HU-21: chat personal con ack de entrega y orden garantizado. */
class ChatPersonalIT extends IntegrationTestBase {

    @LocalServerPort
    private int puerto;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TestUsuarios testUsuarios;

    @Autowired
    private StompTestClients stompClients;

    @Test
    void elMensajeLlegaDeUnaCuandoElDestinatarioEstaConectado() throws Exception {
        String tokenA = testUsuarios.registrarVerificarYLoguear(rest, "chatA@correo.com", "claveSegura123", "chat_a");
        String tokenB = testUsuarios.registrarVerificarYLoguear(rest, "chatB@correo.com", "claveSegura123", "chat_b");

        StompSession sesionA = stompClients.conectar(puerto, tokenA);
        StompSession sesionB = stompClients.conectar(puerto, tokenB);
        BlockingQueue<MensajePersonalDto> colaB = stompClients.suscribir(sesionB, "/user/queue/mensajes", MensajePersonalDto.class);
        BlockingQueue<MensajePersonalDto> colaA = stompClients.suscribir(sesionA, "/user/queue/mensajes", MensajePersonalDto.class);
        Thread.sleep(300);

        java.util.UUID idB = testUsuarios.obtenerPorCorreo("chatB@correo.com").getId();
        sesionA.send("/app/mensajes/enviar", new EnviarMensajePersonalRequest(idB, "hola, vamos a colaborar?"));

        MensajePersonalDto recibidoB = colaB.poll(5, TimeUnit.SECONDS);
        assertThat(recibidoB).isNotNull();
        assertThat(recibidoB.contenido()).isEqualTo("hola, vamos a colaborar?");
        assertThat(recibidoB.entregado()).isTrue();

        MensajePersonalDto confirmacionA = colaA.poll(5, TimeUnit.SECONDS);
        assertThat(confirmacionA).isNotNull();
        assertThat(confirmacionA.entregado()).isTrue();
    }

    @Test
    void siNoEstaConectadoElMensajeQuedaGuardadoYLeLlegaAlConectarseEnOrden() throws Exception {
        String tokenA = testUsuarios.registrarVerificarYLoguear(rest, "chatC@correo.com", "claveSegura123", "chat_c");
        String tokenB = testUsuarios.registrarVerificarYLoguear(rest, "chatD@correo.com", "claveSegura123", "chat_d");
        java.util.UUID idB = testUsuarios.obtenerPorCorreo("chatD@correo.com").getId();

        HttpHeaders headersA = new HttpHeaders();
        headersA.setBearerAuth(tokenA);
        for (int i = 1; i <= 5; i++) {
            ResponseEntity<MensajePersonalDto> resp = rest.postForEntity("/api/mensajes",
                    new HttpEntity<>(new EnviarMensajePersonalRequest(idB, "mensaje " + i), headersA), MensajePersonalDto.class);
            assertThat(resp.getBody().entregado()).isFalse();
        }

        StompSession sesionB = stompClients.conectar(puerto, tokenB);
        BlockingQueue<MensajePersonalDto> colaB = stompClients.suscribir(sesionB, "/user/queue/mensajes", MensajePersonalDto.class);

        List<String> recibidosEnOrden = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            MensajePersonalDto dto = colaB.poll(5, TimeUnit.SECONDS);
            assertThat(dto).isNotNull();
            recibidosEnOrden.add(dto.contenido());
        }
        assertThat(recibidosEnOrden).containsExactly("mensaje 1", "mensaje 2", "mensaje 3", "mensaje 4", "mensaje 5");

        HttpHeaders headersB = new HttpHeaders();
        headersB.setBearerAuth(tokenB);
        List<MensajePersonalDto> historial = List.of(rest.exchange("/api/mensajes/con/" + testUsuarios.obtenerPorCorreo("chatC@correo.com").getId(),
                org.springframework.http.HttpMethod.GET, new HttpEntity<>(headersB), MensajePersonalDto[].class).getBody());
        assertThat(historial).hasSize(5);
    }
}

package com.artagram.platform.party;

import static org.assertj.core.api.Assertions.assertThat;

import com.artagram.platform.party.dto.CrearSalaRequest;
import com.artagram.platform.party.dto.EstadoRondaResponse;
import com.artagram.platform.party.dto.PartyEventoDto;
import com.artagram.platform.party.dto.SalaFiestaResponse;
import com.artagram.platform.support.IntegrationTestBase;
import com.artagram.platform.support.StompTestClients;
import com.artagram.platform.support.TestUsuarios;
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

/**
 * HU-18 (crear/entrar a una sala) y HU-19 (ronda sincronizada: pincel unico,
 * reloj autoritativo, congelado al cierre, reconexion con el tiempo real).
 */
class PartyRoomIT extends IntegrationTestBase {

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
    void armoLaSalaYSaleUnCodigoQuedoComoAnfitrion() {
        String token = testUsuarios.registrarVerificarYLoguear(rest, "anfitrion1@correo.com", "claveSegura123", "anfitrion_uno");
        ResponseEntity<SalaFiestaResponse> respuesta = rest.exchange("/api/fiestas", HttpMethod.POST,
                new HttpEntity<>(new CrearSalaRequest(90, 8), auth(token)), SalaFiestaResponse.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody().codigo()).hasSize(6);
        assertThat(respuesta.getBody().totalParticipantes()).isEqualTo(1);
    }

    @Test
    void laSalaLlenaNoDejaEntrarAUnoMas() {
        String tokenAnfitrion = testUsuarios.registrarVerificarYLoguear(rest, "anfitrion2@correo.com", "claveSegura123", "anfitrion_dos");
        SalaFiestaResponse sala = rest.exchange("/api/fiestas", HttpMethod.POST,
                new HttpEntity<>(new CrearSalaRequest(90, 2), auth(tokenAnfitrion)), SalaFiestaResponse.class).getBody();

        String tokenSegundo = testUsuarios.registrarVerificarYLoguear(rest, "invitado1@correo.com", "claveSegura123", "invitado_uno");
        rest.postForEntity("/api/fiestas/unirse?codigo=" + sala.codigo(), new HttpEntity<>(auth(tokenSegundo)), SalaFiestaResponse.class);

        String tokenTercero = testUsuarios.registrarVerificarYLoguear(rest, "invitado2@correo.com", "claveSegura123", "invitado_dos");
        ResponseEntity<String> respuesta = rest.postForEntity("/api/fiestas/unirse?codigo=" + sala.codigo(),
                new HttpEntity<>(auth(tokenTercero)), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void laRondaReparteElMismoPincelYElRelojEsIgualParaTodos() throws Exception {
        String tokenAnfitrion = testUsuarios.registrarVerificarYLoguear(rest, "anfitrion3@correo.com", "claveSegura123", "anfitrion_tres");
        SalaFiestaResponse sala = rest.exchange("/api/fiestas", HttpMethod.POST,
                new HttpEntity<>(new CrearSalaRequest(6, 8), auth(tokenAnfitrion)), SalaFiestaResponse.class).getBody();

        String tokenParticipante = testUsuarios.registrarVerificarYLoguear(rest, "invitado3@correo.com", "claveSegura123", "invitado_tres");
        rest.postForEntity("/api/fiestas/unirse?codigo=" + sala.codigo(), new HttpEntity<>(auth(tokenParticipante)), SalaFiestaResponse.class);

        StompSession sesionAnfitrion = stompClients.conectar(puerto, tokenAnfitrion);
        StompSession sesionParticipante = stompClients.conectar(puerto, tokenParticipante);
        BlockingQueue<PartyEventoDto> colaAnfitrion = stompClients.suscribir(sesionAnfitrion, "/topic/fiesta/" + sala.id(), PartyEventoDto.class);
        BlockingQueue<PartyEventoDto> colaParticipante = stompClients.suscribir(sesionParticipante, "/topic/fiesta/" + sala.id(), PartyEventoDto.class);
        Thread.sleep(300);

        rest.exchange("/api/fiestas/" + sala.id() + "/rondas", HttpMethod.POST, new HttpEntity<>(auth(tokenAnfitrion)), Void.class);

        EstadoRondaResponse estadoAnfitrion = rest.exchange("/api/fiestas/" + sala.id() + "/estado", HttpMethod.GET,
                new HttpEntity<>(auth(tokenAnfitrion)), EstadoRondaResponse.class).getBody();
        EstadoRondaResponse estadoParticipante = rest.exchange("/api/fiestas/" + sala.id() + "/estado", HttpMethod.GET,
                new HttpEntity<>(auth(tokenParticipante)), EstadoRondaResponse.class).getBody();

        assertThat(estadoAnfitrion.pincel()).isEqualTo(estadoParticipante.pincel());
        assertThat(estadoAnfitrion.colorHex()).isEqualTo(estadoParticipante.colorHex());
        assertThat(estadoAnfitrion.estadoSala()).isEqualTo("EN_RONDA");

        PartyEventoDto tickAnfitrion = esperarTick(colaAnfitrion);
        PartyEventoDto tickParticipante = esperarTick(colaParticipante);
        assertThat(Math.abs(tickAnfitrion.msRestantes() - tickParticipante.msRestantes())).isLessThan(500);

        boolean vioFin = esperarEvento(colaParticipante, "RONDA_TERMINADA", 10);
        assertThat(vioFin).isTrue();

        EstadoRondaResponse estadoTrasCierre = rest.exchange("/api/fiestas/" + sala.id() + "/estado", HttpMethod.GET,
                new HttpEntity<>(auth(tokenParticipante)), EstadoRondaResponse.class).getBody();
        assertThat(estadoTrasCierre.estadoSala()).isEqualTo("ESPERANDO");
    }

    private PartyEventoDto esperarTick(BlockingQueue<PartyEventoDto> cola) throws InterruptedException {
        long limite = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < limite) {
            PartyEventoDto evento = cola.poll(3, TimeUnit.SECONDS);
            if (evento != null && "TICK".equals(evento.tipo())) {
                return evento;
            }
        }
        throw new AssertionError("No llego ningun TICK a tiempo");
    }

    private boolean esperarEvento(BlockingQueue<PartyEventoDto> cola, String tipo, int segundos) throws InterruptedException {
        long limite = System.currentTimeMillis() + segundos * 1000L;
        while (System.currentTimeMillis() < limite) {
            PartyEventoDto evento = cola.poll(1, TimeUnit.SECONDS);
            if (evento != null && tipo.equals(evento.tipo())) {
                return true;
            }
        }
        return false;
    }
}

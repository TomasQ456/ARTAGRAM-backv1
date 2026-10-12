package com.artagram.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.artagram.platform.auth.RolGlobal;
import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.community.dto.ComunidadResponse;
import com.artagram.platform.community.dto.CrearComunidadRequest;
import com.artagram.platform.observability.dto.KpiDetalleDto;
import com.artagram.platform.support.IntegrationTestBase;
import com.artagram.platform.support.StompTestClients;
import com.artagram.platform.support.TestUsuarios;
import java.util.List;
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
 * HU-37: los KPIs se calculan solos a partir de eventos reales (login, entrar
 * a un lienzo compartido), y cada uno explica de donde sale su numero.
 */
class KpiIT extends IntegrationTestBase {

    @LocalServerPort
    private int puerto;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TestUsuarios testUsuarios;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private StompTestClients stompClients;

    private HttpHeaders auth(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private String comoAdmin(String correo, String token) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo).orElseThrow();
        usuario.setRolGlobal(RolGlobal.ADMIN);
        usuarioRepository.save(usuario);
        return rest.postForEntity("/api/auth/login", new com.artagram.platform.auth.dto.LoginRequest(correo, "claveSegura123"),
                com.artagram.platform.auth.dto.LoginResponse.class).getBody().accessToken();
    }

    @Test
    void entrarAUnLienzoCompartidoSubeLaTasaDeColaboracion() throws Exception {
        String tokenAdmin = testUsuarios.registrarVerificarYLoguear(rest, "kpiadmin@correo.com", "claveSegura123", "kpi_admin");
        tokenAdmin = comoAdmin("kpiadmin@correo.com", tokenAdmin);

        KpiDetalleDto antes = rest.exchange("/api/admin/kpis/tasa-colaboracion", HttpMethod.GET,
                new HttpEntity<>(auth(tokenAdmin)), KpiDetalleDto.class).getBody();

        String tokenPintor = testUsuarios.registrarVerificarYLoguear(rest, "kpipintor@correo.com", "claveSegura123", "kpi_pintor");
        ComunidadResponse comunidad = rest.exchange("/api/comunidades", HttpMethod.POST,
                new HttpEntity<>(new CrearComunidadRequest("Comunidad KPI", "desc", true), auth(tokenPintor)), ComunidadResponse.class).getBody();

        StompSession sesion = stompClients.conectar(puerto, tokenPintor);
        sesion.subscribe("/topic/lienzo/" + comunidad.id(), new org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter() { });
        Thread.sleep(300);

        ResponseEntity<KpiDetalleDto> respuesta = rest.exchange("/api/admin/kpis/tasa-colaboracion", HttpMethod.GET,
                new HttpEntity<>(auth(tokenAdmin)), KpiDetalleDto.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        KpiDetalleDto despues = respuesta.getBody();
        assertThat(despues.cantidadEventosContados()).isGreaterThanOrEqualTo(antes.cantidadEventosContados());
        assertThat(despues.valorActual()).isGreaterThan(0);
        assertThat(despues.formula()).isNotBlank();
        assertThat(despues.meta()).isEqualTo(0.40);
    }

    @Test
    void losCuatroKpisResponden() {
        String token = testUsuarios.registrarVerificarYLoguear(rest, "kpiadmin2@correo.com", "claveSegura123", "kpi_admin_dos");
        token = comoAdmin("kpiadmin2@correo.com", token);

        ResponseEntity<KpiDetalleDto[]> respuesta = rest.exchange("/api/admin/kpis", HttpMethod.GET,
                new HttpEntity<>(auth(token)), KpiDetalleDto[].class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> ids = List.of(respuesta.getBody()).stream().map(KpiDetalleDto::id).toList();
        assertThat(ids).containsExactlyInAnyOrder("tasa-colaboracion", "duracion-sesion", "retencion-7-dias", "conversion-premium");
    }
}

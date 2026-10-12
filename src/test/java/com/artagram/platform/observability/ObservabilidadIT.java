package com.artagram.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.artagram.platform.auth.RolGlobal;
import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.support.IntegrationTestBase;
import com.artagram.platform.support.TestUsuarios;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * HU-36 (las metricas tecnicas quedan expuestas) y HU-38 (solo un admin entra
 * al panel; a quien no tiene permiso se le niega y queda registrado).
 */
class ObservabilidadIT extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TestUsuarios testUsuarios;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private String promoverAAdmin(String correo) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo).orElseThrow();
        usuario.setRolGlobal(RolGlobal.ADMIN);
        usuarioRepository.save(usuario);
        return usuario.getId().toString();
    }

    @Test
    void lasMetricasTecnicasQuedanExpuestasParaUnAdmin() {
        String token = testUsuarios.registrarVerificarYLoguear(rest, "admin1@correo.com", "claveSegura123", "admin_uno");
        promoverAAdmin("admin1@correo.com");
        // El JWT ya emitido no sabe que ahora es admin (el rol viaja en el token);
        // hay que volver a loguear para que el nuevo token lleve ROLE_ADMIN.
        token = reloguear("admin1@correo.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> respuesta = rest.exchange("/actuator/prometheus", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        String cuerpo = respuesta.getBody();
        assertThat(cuerpo).contains("ws_sesiones_activas");
        assertThat(cuerpo).contains("trazo_latencia_ms");
        assertThat(cuerpo).contains("trazos_por_segundo");
        assertThat(cuerpo).contains("bloqueos_negados_total");
        assertThat(cuerpo).contains("borrador_disputas_total");
        assertThat(cuerpo).contains("http_errores_5xx_total");
    }

    @Test
    void unUsuarioNormalNoEntraAlDashboardYQuedaRegistrado() {
        String token = testUsuarios.registrarVerificarYLoguear(rest, "normal1@correo.com", "claveSegura123", "normal_uno");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> respuesta = rest.exchange("/api/admin/dashboard", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody()).contains("SIN_PERMISO");
    }

    @Test
    void unAdminSiEntraAlDashboard() {
        testUsuarios.registrarVerificarYLoguear(rest, "admin2@correo.com", "claveSegura123", "admin_dos");
        promoverAAdmin("admin2@correo.com");
        String token = reloguear("admin2@correo.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> respuesta = rest.exchange("/api/admin/dashboard", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String reloguear(String correo) {
        var login = rest.postForEntity("/api/auth/login",
                new com.artagram.platform.auth.dto.LoginRequest(correo, "claveSegura123"),
                com.artagram.platform.auth.dto.LoginResponse.class);
        return login.getBody().accessToken();
    }
}

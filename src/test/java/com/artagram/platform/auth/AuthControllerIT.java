package com.artagram.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.artagram.platform.auth.dto.LoginRequest;
import com.artagram.platform.auth.dto.LoginResponse;
import com.artagram.platform.auth.dto.NombreDisponibleResponse;
import com.artagram.platform.auth.dto.RegistroRequest;
import com.artagram.platform.auth.dto.RegistroResponse;
import com.artagram.platform.common.error.ErrorResponse;
import com.artagram.platform.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Cubre de punta a punta (HTTP real + Postgres real via Testcontainers) los
 * escenarios Gherkin de HU-01: registro exitoso, correo duplicado y nombre ocupado.
 */
class AuthControllerIT extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TokenVerificacionRepository tokenVerificacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void registroQueSalebienQuedaPendienteDeVerificarYPermiteLoginTrasVerificar() {
        RegistroRequest registro = new RegistroRequest("nueva@correo.com", "claveSegura123", "pincel_dorado");

        ResponseEntity<RegistroResponse> respuestaRegistro =
                rest.postForEntity("/api/auth/registro", registro, RegistroResponse.class);

        assertThat(respuestaRegistro.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuestaRegistro.getBody().mensaje()).containsIgnoringCase("bandeja");

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase("nueva@correo.com").orElseThrow();
        assertThat(usuario.isVerificado()).isFalse();

        ResponseEntity<LoginResponse> loginAntesDeVerificar =
                rest.postForEntity("/api/auth/login", new LoginRequest("nueva@correo.com", "claveSegura123"), LoginResponse.class);
        assertThat(loginAntesDeVerificar.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        String token = tokenVerificacionRepository.findAll().stream()
                .filter(t -> t.getUsuario().getId().equals(usuario.getId()))
                .findFirst().orElseThrow().getToken();

        ResponseEntity<Void> verificacion = rest.getForEntity("/api/auth/verificar?token=" + token, Void.class);
        assertThat(verificacion.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<LoginResponse> loginDespues =
                rest.postForEntity("/api/auth/login", new LoginRequest("nueva@correo.com", "claveSegura123"), LoginResponse.class);
        assertThat(loginDespues.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginDespues.getBody().accessToken()).isNotBlank();
    }

    @Test
    void elCorreoYaEstaUsadoNoCreaLaCuenta() {
        RegistroRequest primero = new RegistroRequest("repetido@correo.com", "claveSegura123", "artista_uno");
        rest.postForEntity("/api/auth/registro", primero, RegistroResponse.class);

        RegistroRequest segundo = new RegistroRequest("repetido@correo.com", "otraClave123", "artista_dos");
        ResponseEntity<ErrorResponse> respuesta =
                rest.postForEntity("/api/auth/registro", segundo, ErrorResponse.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().message()).isEqualTo("Este correo ya está registrado");
    }

    @Test
    void elNombreDeArtistaOcupadoSeMarcaYSugiereAlternativas() {
        rest.postForEntity("/api/auth/registro",
                new RegistroRequest("primero@correo.com", "claveSegura123", "lienzo_azul_x"), RegistroResponse.class);

        ResponseEntity<NombreDisponibleResponse> respuesta =
                rest.getForEntity("/api/auth/nombre-disponible?nombre=lienzo_azul_x", NombreDisponibleResponse.class);

        assertThat(respuesta.getBody().disponible()).isFalse();
        assertThat(respuesta.getBody().sugerencias()).isNotEmpty();
    }
}

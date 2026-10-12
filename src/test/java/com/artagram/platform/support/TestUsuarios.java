package com.artagram.platform.support;

import com.artagram.platform.auth.TokenVerificacionRepository;
import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.auth.dto.LoginRequest;
import com.artagram.platform.auth.dto.LoginResponse;
import com.artagram.platform.auth.dto.RegistroRequest;
import com.artagram.platform.auth.dto.RegistroResponse;
import java.util.Optional;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Atajo compartido por las pruebas de integracion: registra, verifica (leyendo
 * el token directo de la base, ya que el correo real solo se logea en dev) y
 * loguea a un usuario, devolviendo su JWT listo para usar.
 */
@TestComponent
public class TestUsuarios {

    private final UsuarioRepository usuarioRepository;
    private final TokenVerificacionRepository tokenVerificacionRepository;

    public TestUsuarios(UsuarioRepository usuarioRepository, TokenVerificacionRepository tokenVerificacionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.tokenVerificacionRepository = tokenVerificacionRepository;
    }

    public String registrarVerificarYLoguear(TestRestTemplate rest, String correo, String password, String nombreArtista) {
        // Reintenta unas pocas veces: justo despues de una prueba de carga con
        // cientos de hilos, el pool de conexiones/hilos de Tomcat puede tardar
        // un instante en asentarse y una unica solicitud puede fallar sin que
        // sea un bug de negocio.
        RuntimeException ultimoError = null;
        for (int intento = 0; intento < 5; intento++) {
            try {
                ResponseEntity<RegistroResponse> registro = rest.postForEntity(
                        "/api/auth/registro", new RegistroRequest(correo, password, nombreArtista), RegistroResponse.class);
                boolean yaExistia = registro.getStatusCode() == HttpStatus.CONFLICT;
                if (registro.getStatusCode() != HttpStatus.CREATED && !yaExistia) {
                    throw new IllegalStateException("Registro fallo con " + registro.getStatusCode());
                }
                Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo).orElseThrow();
                String token = tokenVerificacionRepository.findAll().stream()
                        .filter(t -> t.getUsuario().getId().equals(usuario.getId()))
                        .findFirst().orElseThrow().getToken();
                rest.getForEntity("/api/auth/verificar?token=" + token, Void.class);
                LoginResponse login = rest.postForEntity(
                        "/api/auth/login", new LoginRequest(correo, password), LoginResponse.class).getBody();
                return login.accessToken();
            } catch (RuntimeException e) {
                ultimoError = e;
                try {
                    Thread.sleep(300L * (intento + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw ultimoError;
    }

    public Usuario obtenerPorCorreo(String correo) {
        return usuarioRepository.findByCorreoIgnoreCase(correo).orElseThrow();
    }
}

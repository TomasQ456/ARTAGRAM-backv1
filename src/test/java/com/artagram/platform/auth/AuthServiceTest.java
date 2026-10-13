package com.artagram.platform.auth;

import static org.mockito.Mockito.never;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artagram.platform.auth.dto.LoginRequest;
import com.artagram.platform.auth.dto.RegistroRequest;
import com.artagram.platform.common.error.ApiException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Pruebas unitarias de la logica de negocio de HU-01 (registro con correo).
 * Repositorios y servicios externos van con mocks para aislar la logica.
 */
class AuthServiceTest {

    private UsuarioRepository usuarioRepository;
    private TokenVerificacionRepository tokenRepository;
    private EmailService emailService;
    private AuthService authService;
    private EmailProperties emailProperties;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        tokenRepository = mock(TokenVerificacionRepository.class);
        emailService = mock(EmailService.class);
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("unit-test-secret-please-change-32bytes-minimum");
        jwtProperties.setAccessTokenMinutes(60);
        emailProperties = new EmailProperties();
        emailProperties.setVerificationLinkBase("http://localhost/verificar");
        emailProperties.setFrom("no-reply@test.local");

        authService = new AuthService(
                usuarioRepository,
                tokenRepository,
                new BCryptPasswordEncoder(12),
                new JwtService(jwtProperties),
                emailService,
                emailProperties,
                new NombreSugeridoGenerator(),
                mock(org.springframework.context.ApplicationEventPublisher.class),
                new com.artagram.platform.observability.EventoNegocioRecorder(mock(com.artagram.platform.observability.EventoNegocioRepository.class))
        );

        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registroExitosoQuedaPendienteDeVerificarYEnviaCorreo() {
        RegistroRequest request = new RegistroRequest("artista@correo.com", "unaClaveLarga123", "lienzo_azul");
        when(usuarioRepository.existsByCorreoIgnoreCase("artista@correo.com")).thenReturn(false);
        when(usuarioRepository.existsByNombreArtistaIgnoreCase("lienzo_azul")).thenReturn(false);

        var respuesta = authService.registrar(request);

        assertThat(respuesta.correo()).isEqualTo("artista@correo.com");
        assertThat(respuesta.mensaje()).containsIgnoringCase("bandeja");
        verify(emailService).enviarVerificacion(eq("artista@correo.com"), eq("lienzo_azul"), any());
        verify(tokenRepository).save(any(TokenVerificacion.class));
    }

    @Test
    void sinVerificacionPorCorreoLaCuentaQuedaActivaDeInmediatoYNoEnviaCorreo() {
        emailProperties.setVerificacionRequerida(false);
        RegistroRequest request = new RegistroRequest("artista@correo.com", "unaClaveLarga123", "lienzo_azul");
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);

        var respuesta = authService.registrar(request);

        assertThat(respuesta.verificacionPendiente()).isFalse();
        verify(usuarioRepository).save(guardado.capture());
        assertThat(guardado.getValue().isVerificado()).isTrue();
        verify(emailService, never()).enviarVerificacion(any(), any(), any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void correoYaUsadoNoCreaLaCuenta() {
        when(usuarioRepository.existsByCorreoIgnoreCase("artista@correo.com")).thenReturn(true);
        RegistroRequest request = new RegistroRequest("artista@correo.com", "unaClaveLarga123", "otroNombre");

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Este correo ya está registrado");
    }

    @Test
    void nombreDeArtistaOcupadoSugiereTresAlternativasLibres() {
        when(usuarioRepository.existsByNombreArtistaIgnoreCase("lienzo_azul")).thenReturn(true);
        when(usuarioRepository.existsByNombreArtistaIgnoreCase("lienzo_azul_arte")).thenReturn(false);
        when(usuarioRepository.existsByNombreArtistaIgnoreCase("lienzo_azul_studio")).thenReturn(false);
        when(usuarioRepository.existsByNombreArtistaIgnoreCase("lienzo_azul_creativo")).thenReturn(false);

        var respuesta = authService.verificarNombreDisponible("lienzo_azul");

        assertThat(respuesta.disponible()).isFalse();
        assertThat(respuesta.sugerencias()).hasSize(3);
    }

    @Test
    void loginRechazaCredencialesInvalidas() {
        when(usuarioRepository.findByCorreoIgnoreCase("artista@correo.com")).thenReturn(Optional.empty());
        LoginRequest request = new LoginRequest("artista@correo.com", "cualquiera1234");

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(ApiException.class);
    }

    @Test
    void loginRechazaCuentaNoVerificada() {
        Usuario usuario = new Usuario("artista@correo.com", "lienzo_azul",
                new BCryptPasswordEncoder(12).encode("unaClaveLarga123"));
        when(usuarioRepository.findByCorreoIgnoreCase("artista@correo.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(new LoginRequest("artista@correo.com", "unaClaveLarga123")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Verifica tu correo");
    }
}

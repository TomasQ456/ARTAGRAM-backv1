package com.artagram.platform.auth;

import com.artagram.platform.auth.dto.LoginRequest;
import com.artagram.platform.auth.dto.LoginResponse;
import com.artagram.platform.auth.dto.NombreDisponibleResponse;
import com.artagram.platform.auth.dto.RegistroRequest;
import com.artagram.platform.auth.dto.RegistroResponse;
import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.observability.EventoNegocioRecorder;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int VERIFICACION_HORAS = 24;

    private final UsuarioRepository usuarioRepository;
    private final TokenVerificacionRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final EmailProperties emailProperties;
    private final NombreSugeridoGenerator nombreSugeridoGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final EventoNegocioRecorder eventoNegocioRecorder;

    public AuthService(UsuarioRepository usuarioRepository,
                        TokenVerificacionRepository tokenRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        EmailService emailService,
                        EmailProperties emailProperties,
                        NombreSugeridoGenerator nombreSugeridoGenerator,
                        ApplicationEventPublisher eventPublisher,
                        EventoNegocioRecorder eventoNegocioRecorder) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.emailProperties = emailProperties;
        this.nombreSugeridoGenerator = nombreSugeridoGenerator;
        this.eventPublisher = eventPublisher;
        this.eventoNegocioRecorder = eventoNegocioRecorder;
    }

    @Transactional
    public RegistroResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByCorreoIgnoreCase(request.correo())) {
            throw ApiException.conflict("CORREO_YA_REGISTRADO", "Este correo ya está registrado");
        }
        if (usuarioRepository.existsByNombreArtistaIgnoreCase(request.nombreArtista())) {
            throw ApiException.conflict("NOMBRE_OCUPADO", "Ese nombre de artista ya lo tiene otra persona");
        }

        Usuario usuario = new Usuario(
                request.correo().toLowerCase(),
                request.nombreArtista(),
                passwordEncoder.encode(request.password())
        );
        try {
            usuario = usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("REGISTRO_DUPLICADO", "Ese correo o nombre de artista ya esta en uso");
        }

        eventPublisher.publishEvent(new UsuarioRegistradoEvent(usuario.getId()));
        eventoNegocioRecorder.registro(usuario.getId());

        if (!emailProperties.isVerificacionRequerida()) {
            usuario.marcarVerificado();
            log.info("Usuario registrado y verificado de inmediato (verificacion por correo desactivada): id={}", usuario.getId());
            return new RegistroResponse(usuario.getId(), usuario.getCorreo(), usuario.getNombreArtista(),
                    "Tu cuenta está lista, ya puedes iniciar sesión", false);
        }

        String token = generarTokenAleatorio();
        tokenRepository.save(new TokenVerificacion(usuario, token, Instant.now().plus(VERIFICACION_HORAS, ChronoUnit.HOURS)));

        String enlace = emailProperties.getVerificationLinkBase() + "?token=" + token;
        emailService.enviarVerificacion(usuario.getCorreo(), usuario.getNombreArtista(), enlace);

        log.info("Usuario registrado, pendiente de verificacion: id={}", usuario.getId());
        return new RegistroResponse(usuario.getId(), usuario.getCorreo(), usuario.getNombreArtista(),
                "Revisa tu bandeja de entrada para confirmar tu cuenta", true);
    }

    @Transactional
    public void verificar(String token) {
        TokenVerificacion tokenVerificacion = tokenRepository.findByToken(token)
                .orElseThrow(() -> ApiException.badRequest("TOKEN_INVALIDO", "El enlace de verificacion no es valido"));
        if (tokenVerificacion.estaUsado()) {
            throw ApiException.badRequest("TOKEN_YA_USADO", "Este enlace ya fue usado");
        }
        if (tokenVerificacion.estaExpirado()) {
            throw ApiException.badRequest("TOKEN_EXPIRADO", "El enlace de verificacion vencio, pide uno nuevo");
        }
        tokenVerificacion.marcarUsado();
        tokenVerificacion.getUsuario().marcarVerificado();
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(request.correo())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", "Correo o contrasena incorrectos"));
        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", "Correo o contrasena incorrectos");
        }
        if (!usuario.isVerificado()) {
            throw ApiException.forbidden("CUENTA_NO_VERIFICADA", "Verifica tu correo antes de iniciar sesión");
        }
        String token = jwtService.generarToken(usuario);
        eventoNegocioRecorder.login(usuario.getId());
        return new LoginResponse(token, usuario.getId(), usuario.getNombreArtista(),
                usuario.getRolGlobal().name(), usuario.isPremium());
    }

    @Transactional(readOnly = true)
    public NombreDisponibleResponse verificarNombreDisponible(String nombre) {
        boolean disponible = !usuarioRepository.existsByNombreArtistaIgnoreCase(nombre);
        List<String> sugerencias = disponible
                ? List.of()
                : nombreSugeridoGenerator.sugerir(nombre, n -> !usuarioRepository.existsByNombreArtistaIgnoreCase(n), 3);
        return new NombreDisponibleResponse(disponible, sugerencias);
    }

    private String generarTokenAleatorio() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

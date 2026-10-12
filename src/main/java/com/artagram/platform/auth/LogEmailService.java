package com.artagram.platform.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implementacion de desarrollo: no hay proveedor SMTP configurado en este entorno,
 * asi que el enlace de verificacion se deja en el log en vez de perderse (HU-01).
 * Cambiar app.email.mode=smtp activa {@link SmtpEmailService} con credenciales reales.
 */
@Service
@ConditionalOnProperty(prefix = "app.email", name = "mode", havingValue = "log", matchIfMissing = true)
public class LogEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LogEmailService.class);

    @Override
    public void enviarVerificacion(String correoDestino, String nombreArtista, String enlaceVerificacion) {
        log.info("[EMAIL-DEV] Verificacion para {} ({}) -> {}", nombreArtista, correoDestino, enlaceVerificacion);
    }
}

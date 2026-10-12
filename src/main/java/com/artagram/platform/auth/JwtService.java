package com.artagram.platform.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Emite y valida el JWT que se usa tanto para REST como para el handshake STOMP
 * (misma llave, mismo token: HU-02/HU-11 lo exigen explicitamente).
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final int accessTokenMinutes;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenMinutes = properties.getAccessTokenMinutes();
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("nombreArtista", usuario.getNombreArtista())
                .claim("rolGlobal", usuario.getRolGlobal().name())
                .claim("premium", usuario.isPremium())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(accessTokenMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public Optional<TokenPrincipal> validar(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            UUID usuarioId = UUID.fromString(claims.getSubject());
            String nombreArtista = claims.get("nombreArtista", String.class);
            String rolGlobal = claims.get("rolGlobal", String.class);
            boolean premium = Boolean.TRUE.equals(claims.get("premium", Boolean.class));
            return Optional.of(new TokenPrincipal(usuarioId, nombreArtista, rolGlobal, premium));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token invalido o vencido: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public record TokenPrincipal(UUID usuarioId, String nombreArtista, String rolGlobal, boolean premium) {
    }
}

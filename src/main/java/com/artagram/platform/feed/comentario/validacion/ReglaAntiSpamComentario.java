package com.artagram.platform.feed.comentario.validacion;

import com.artagram.platform.common.error.ApiException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(30)
public class ReglaAntiSpamComentario implements ReglaValidacionComentario {

    private static final Logger log = LoggerFactory.getLogger(ReglaAntiSpamComentario.class);
    private final RedissonClient redissonClient;

    public ReglaAntiSpamComentario(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void validar(ComentarioContext contexto) {
        if (contexto == null || contexto.autorId() == null) {
            return;
        }
        try {
            RRateLimiter limiter = redissonClient.getRateLimiter("ratelimit:comentarios:" + contexto.autorId());
            limiter.trySetRate(RateType.OVERALL, 5, 10, RateIntervalUnit.SECONDS);
            if (!limiter.tryAcquire()) {
                log.warn("Rate limit de comentarios alcanzado para usuario {}", contexto.autorId());
                throw ApiException.tooManyRequests("RATE_LIMIT_COMENTARIOS",
                        "Has publicado muchos comentarios recientemente. Por favor espera unos segundos.");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al evaluar rate limiter de comentarios: {}", e.getMessage());
        }
    }
}

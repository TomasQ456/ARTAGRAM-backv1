package com.artagram.platform.realtime;

import java.util.UUID;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Quien esta conectado ahora mismo, deducido de los eventos de sesion STOMP
 * (HU-21/HU-23). Cuenta sesiones por usuario (no solo un flag) para que
 * cerrar una de varias pestanas no lo marque como desconectado.
 */
@Component
public class PresenceService {

    private static final String CLAVE = "usuarios-conectados";

    private final RedissonClient redissonClient;

    public PresenceService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @EventListener
    public void alConectar(SessionConnectedEvent event) {
        if (event.getUser() != null) {
            mapa().merge(event.getUser().getName(), 1, Integer::sum);
        }
    }

    @EventListener
    public void alDesconectar(SessionDisconnectEvent event) {
        if (event.getUser() != null) {
            mapa().merge(event.getUser().getName(), -1, (actual, delta) -> Math.max(0, actual + delta));
        }
    }

    public boolean estaConectado(UUID usuarioId) {
        Integer sesiones = mapa().get(usuarioId.toString());
        return sesiones != null && sesiones > 0;
    }

    private RMap<String, Integer> mapa() {
        return redissonClient.getMap(CLAVE);
    }
}

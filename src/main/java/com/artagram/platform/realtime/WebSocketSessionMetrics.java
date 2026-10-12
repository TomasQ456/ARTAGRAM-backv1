package com.artagram.platform.realtime;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * ws_sesiones_activas (HU-36): cuantas sesiones STOMP hay abiertas ahora mismo.
 */
@Component
public class WebSocketSessionMetrics {

    private final AtomicInteger sesionesActivas = new AtomicInteger(0);

    public WebSocketSessionMetrics(MeterRegistry registry) {
        registry.gauge("ws_sesiones_activas", sesionesActivas);
    }

    @EventListener
    public void alConectar(SessionConnectedEvent event) {
        sesionesActivas.incrementAndGet();
    }

    @EventListener
    public void alDesconectar(SessionDisconnectEvent event) {
        sesionesActivas.updateAndGet(actual -> Math.max(0, actual - 1));
    }
}

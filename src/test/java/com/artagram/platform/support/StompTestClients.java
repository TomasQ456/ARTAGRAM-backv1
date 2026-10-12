package com.artagram.platform.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * Cliente STOMP de verdad (no mocks) para probar los canales en tiempo real de
 * punta a punta, como pide la DoD para los canales de tiempo real.
 */
@TestComponent
public class StompTestClients {

    public StompSession conectar(int puerto, String jwt) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(new ObjectMapper().registerModule(new JavaTimeModule()));
        client.setMessageConverter(converter);
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.afterPropertiesSet();
        client.setTaskScheduler(scheduler);

        CompletableFuture<StompSession> future = new CompletableFuture<>();
        client.connectAsync("ws://localhost:" + puerto + "/ws?token=" + jwt,
                new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders headers) {
                future.complete(session);
            }

            @Override
            public void handleException(StompSession session, org.springframework.messaging.simp.stomp.StompCommand command,
                                         StompHeaders headers, byte[] payload, Throwable exception) {
                future.completeExceptionally(exception);
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                future.completeExceptionally(exception);
            }
        });
        return future.get(10, TimeUnit.SECONDS);
    }

    public <T> java.util.concurrent.BlockingQueue<T> suscribir(StompSession session, String destino, Class<T> tipo) {
        java.util.concurrent.BlockingQueue<T> cola = new java.util.concurrent.LinkedBlockingQueue<>();
        session.subscribe(destino, new org.springframework.messaging.simp.stomp.StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return tipo;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                cola.add(tipo.cast(payload));
            }
        });
        return cola;
    }
}

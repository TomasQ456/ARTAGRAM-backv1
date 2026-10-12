package com.artagram.platform.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Reparte los mensajes de tiempo real (lienzo, banner, modo fiesta, chat)
 * entre TODAS las instancias del backend, para que escalar a mas de una
 * instancia detras de un load balancer no rompa nada: un {@code SimpleBroker}
 * (ver {@link WebSocketConfig}) solo entrega a las sesiones WebSocket
 * conectadas a ESA misma instancia, asi que sin esto un cliente conectado a
 * la instancia A nunca veria un trazo dibujado por alguien en la instancia B.
 * <p>
 * En vez de un broker relay externo (RabbitMQ/ActiveMQ, la forma "de libro"
 * via {@code enableStompBrokerRelay}), reusamos el Redis que el proyecto ya
 * tiene para locks/rate-limiting: publicamos cada envio en un {@link RTopic}
 * y cada instancia (incluida la que publico) lo recibe y hace la entrega
 * local de verdad con {@link SimpMessagingTemplate}. Spring ya filtra sola a
 * las sesiones locales suscritas a ese destino/usuario — una instancia sin
 * ninguna sesion de ese usuario/destino simplemente no hace nada al recibir
 * el mensaje, asi que transmitir a todas y dejar que cada una filtre local
 * es correcto sin necesitar saber de antemano quien esta conectado donde.
 */
@Component
public class RealtimeBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(RealtimeBroadcaster.class);
    private static final String CANAL = "artagram:stomp-relay";

    private enum TipoEnvio { TOPICO, USUARIO }

    /**
     * El payload viaja ya serializado a JSON, y se recibe como un {@code Object}
     * generico (Jackson lo materializa en un {@code LinkedHashMap}/{@code List}) en
     * vez de reconstruir el tipo Java original — no hace falta: el conversor de
     * salida de STOMP va a volver a serializar lo que sea que le pasemos a JSON de
     * todas formas antes de mandarlo al navegador, asi que el JSON final es
     * identico tanto si se reconstruye el DTO original como si se manda el mapa
     * generico. Reconstruir el tipo original exigiria guardar su nombre de clase
     * y usar {@code Class.forName}, lo cual se rompe para payloads cuyo tipo
     * concreto no es Jackson-construible (ej. {@code Map.of(...)} en los
     * manejadores de error, que devuelve una clase interna del JDK sin
     * constructor accesible).
     */
    private record RelayEnvelope(TipoEnvio tipo, String destino, String usuarioId, String payloadJson) {
    }

    private final RedissonClient redissonClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private RTopic topic;

    public RealtimeBroadcaster(RedissonClient redissonClient, SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void suscribirse() {
        topic = redissonClient.getTopic(CANAL);
        topic.addListener(RelayEnvelope.class, (canal, envelope) -> entregarLocal(envelope));
    }

    /** Reemplaza {@code SimpMessagingTemplate.convertAndSend(destino, payload)}. */
    public void enviar(String destino, Object payload) {
        publicar(new RelayEnvelope(TipoEnvio.TOPICO, destino, null, serializar(payload)));
    }

    /** Reemplaza {@code SimpMessagingTemplate.convertAndSendToUser(usuarioId, destino, payload)}. */
    public void enviarAUsuario(String usuarioId, String destino, Object payload) {
        publicar(new RelayEnvelope(TipoEnvio.USUARIO, destino, usuarioId, serializar(payload)));
    }

    private void publicar(RelayEnvelope envelope) {
        topic.publish(envelope);
    }

    private void entregarLocal(RelayEnvelope envelope) {
        try {
            Object payload = objectMapper.readValue(envelope.payloadJson(), Object.class);
            if (envelope.tipo() == TipoEnvio.USUARIO) {
                messagingTemplate.convertAndSendToUser(envelope.usuarioId(), envelope.destino(), payload);
            } else {
                messagingTemplate.convertAndSend(envelope.destino(), payload);
            }
        } catch (Exception e) {
            log.error("No se pudo entregar un mensaje de tiempo real relayado: destino={}", envelope.destino(), e);
        }
    }

    private String serializar(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo serializar el payload de tiempo real: " + payload.getClass(), e);
        }
    }
}

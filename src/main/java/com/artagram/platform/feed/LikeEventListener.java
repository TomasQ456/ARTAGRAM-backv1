package com.artagram.platform.feed;

import com.artagram.platform.feed.dto.LikeEventoDto;
import com.artagram.platform.realtime.RealtimeBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class LikeEventListener {

    private static final Logger log = LoggerFactory.getLogger(LikeEventListener.class);

    private final RealtimeBroadcaster realtimeBroadcaster;

    public LikeEventListener(RealtimeBroadcaster realtimeBroadcaster) {
        this.realtimeBroadcaster = realtimeBroadcaster;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alActualizarLikes(LikeActualizadoEvent evento) {
        String destino = "/topic/publicaciones/" + evento.publicacionId() + "/likes";
        log.debug("Emitiendo likes a {}: contador={}", destino, evento.contadorLikes());
        realtimeBroadcaster.enviar(destino,
                new LikeEventoDto("LIKES_ACTUALIZADOS", evento.publicacionId(), evento.contadorLikes()));
    }
}

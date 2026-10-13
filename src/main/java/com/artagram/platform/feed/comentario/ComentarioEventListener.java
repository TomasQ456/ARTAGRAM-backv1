package com.artagram.platform.feed.comentario;

import com.artagram.platform.realtime.RealtimeBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ComentarioEventListener {

    private static final Logger log = LoggerFactory.getLogger(ComentarioEventListener.class);

    private final RealtimeBroadcaster realtimeBroadcaster;

    public ComentarioEventListener(RealtimeBroadcaster realtimeBroadcaster) {
        this.realtimeBroadcaster = realtimeBroadcaster;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCrearComentario(ComentarioCreadoEvent evento) {
        ComentarioDto dto = evento.comentario();
        String destino = "/topic/publicaciones/" + dto.publicacionId() + "/comentarios";
        ComentarioEvento payload = new ComentarioEvento("COMENTARIO_CREADO", dto, evento.contadorComentarios());
        log.debug("Emitiendo comentario a {}: id={}", destino, dto.id());
        realtimeBroadcaster.enviar(destino, payload);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alEliminarComentario(ComentarioEliminadoEvent evento) {
        ComentarioDto dto = evento.comentario();
        String destino = "/topic/publicaciones/" + dto.publicacionId() + "/comentarios";
        log.debug("Emitiendo eliminacion de comentario a {}: id={}", destino, dto.id());
        realtimeBroadcaster.enviar(destino, new ComentarioEvento("COMENTARIO_ELIMINADO", dto, evento.contadorComentarios()));
    }
}

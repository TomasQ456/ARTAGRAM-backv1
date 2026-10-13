package com.artagram.platform.feed;

import java.util.UUID;

/** Se publica dentro de la transaccion del like; LikeEventListener lo difunde tras el commit. */
public record LikeActualizadoEvent(UUID publicacionId, int contadorLikes) {
}

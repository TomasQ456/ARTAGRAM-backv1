package com.artagram.platform.feed.dto;

import java.util.UUID;

/** Mensaje de /topic/publicaciones/{id}/likes: el contador actualizado para todos los que ven la obra. */
public record LikeEventoDto(String tipo, UUID publicacionId, int contadorLikes) {
}

package com.artagram.platform.feed.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FeedResponse(
        List<PublicacionDto> publicaciones,
        Instant cursorFechaSiguiente,
        UUID cursorIdSiguiente,
        boolean hayMas,
        List<String> artistasSugeridos
) {
}

package com.artagram.platform.auth.dto;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        UUID usuarioId,
        String nombreArtista,
        String rolGlobal,
        boolean premium
) {
}

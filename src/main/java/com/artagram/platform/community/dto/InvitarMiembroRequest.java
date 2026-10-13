package com.artagram.platform.community.dto;

import jakarta.validation.constraints.NotBlank;

public record InvitarMiembroRequest(
        @NotBlank(message = "El nombre de artista no puede estar vacío")
        String nombreArtista
) {
}

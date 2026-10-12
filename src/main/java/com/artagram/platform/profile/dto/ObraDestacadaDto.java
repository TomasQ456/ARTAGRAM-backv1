package com.artagram.platform.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ObraDestacadaDto(
        @NotBlank @Size(max = 120) String titulo,
        @NotBlank String rutaImagen
) {
}

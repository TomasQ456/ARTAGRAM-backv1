package com.artagram.platform.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnlaceDto(
        @NotBlank @Size(max = 50) String etiqueta,
        @NotBlank @Size(max = 300) String url
) {
}

package com.artagram.platform.community.dto;

import com.artagram.platform.community.RolComunidad;
import jakarta.validation.constraints.NotNull;

public record CambiarRolRequest(@NotNull RolComunidad rol) {
}

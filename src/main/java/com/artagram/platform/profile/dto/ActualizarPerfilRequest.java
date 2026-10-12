package com.artagram.platform.profile.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ActualizarPerfilRequest(
        @Size(max = 500) String biografia,
        @Valid @Size(max = 5) List<EnlaceDto> enlaces,
        @Valid @Size(max = 3) List<ObraDestacadaDto> obrasDestacadas
) {
}

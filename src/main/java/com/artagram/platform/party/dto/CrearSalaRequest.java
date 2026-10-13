package com.artagram.platform.party.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CrearSalaRequest(
        @Min(5) @Max(600) int duracionRondaSegundos,
        @Min(2) @Max(50) int cupoMaximo,
        @Min(1) @Max(10) Integer totalRondas
) {
    public CrearSalaRequest(int duracionRondaSegundos, int cupoMaximo) {
        this(duracionRondaSegundos, cupoMaximo, 3);
    }
}

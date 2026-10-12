package com.artagram.platform.party.dto;

import com.artagram.platform.canvas.Punto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Solo puntos: el pincel, color, grosor y opacidad los impone el servidor
 * segun lo que se repartio al inicio de la ronda (HU-19, "nadie puede
 * cambiarlo por su cuenta").
 */
public record TrazarEnRondaRequest(@NotEmpty @Size(max = 500) List<Punto> puntos) {
}

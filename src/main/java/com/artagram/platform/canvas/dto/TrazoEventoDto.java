package com.artagram.platform.canvas.dto;

import com.artagram.platform.canvas.Punto;
import com.artagram.platform.canvas.TipoPincel;
import java.util.List;
import java.util.UUID;

/**
 * Lo que viaja por /topic/lienzo/{comunidadId}. tipo=PARCIAL son lotes de
 * puntos en vivo mientras alguien dibuja (no se persisten); tipo=FINAL es el
 * trazo ya cerrado con su numero de secuencia asignado por el servidor.
 */
public record TrazoEventoDto(
        String tipo,
        UUID trazoEnProgresoId,
        Long secuencia,
        UUID autorId,
        String autorNombre,
        TipoPincel tipoPincel,
        String colorHex,
        double grosor,
        double opacidad,
        int capa,
        List<Punto> puntos
) {
    public static TrazoEventoDto parcial(UUID trazoEnProgresoId, UUID autorId, String autorNombre, EnviarPuntosRequest r) {
        return new TrazoEventoDto("PARCIAL", trazoEnProgresoId, null, autorId, autorNombre,
                r.tipoPincel(), r.colorHex(), r.grosor(), r.opacidad(), r.capa(), r.puntos());
    }

    public static TrazoEventoDto finalizado(UUID trazoEnProgresoId, long secuencia, UUID autorId, String autorNombre, EnviarPuntosRequest r) {
        return new TrazoEventoDto("FINAL", trazoEnProgresoId, secuencia, autorId, autorNombre,
                r.tipoPincel(), r.colorHex(), r.grosor(), r.opacidad(), r.capa(), r.puntos());
    }
}

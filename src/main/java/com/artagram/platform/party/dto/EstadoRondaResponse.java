package com.artagram.platform.party.dto;

import com.artagram.platform.canvas.TipoPincel;
import java.util.List;

public record EstadoRondaResponse(
        String estadoSala,
        int numeroRonda,
        long msRestantes,
        TipoPincel pincel,
        String colorHex,
        Double grosor,
        Double opacidad,
        String borradorPropietario,
        List<PartyTrazoDto> trazos
) {
}

package com.artagram.platform.ia;

import java.time.Instant;
import java.util.UUID;

public record AnalisisObraDto(
        UUID id,
        UUID publicacionId,
        UUID solicitanteId,
        TipoAnalisis tipo,
        String resultadoJson,
        String modelo,
        Instant creadoEn
) {
}

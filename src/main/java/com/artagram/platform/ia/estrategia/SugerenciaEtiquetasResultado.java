package com.artagram.platform.ia.estrategia;

import java.util.List;

public record SugerenciaEtiquetasResultado(
        String tituloSugerido,
        String descripcionSugerida,
        List<String> etiquetas
) {
}

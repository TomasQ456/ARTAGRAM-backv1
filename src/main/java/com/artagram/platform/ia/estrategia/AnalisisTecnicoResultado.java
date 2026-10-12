package com.artagram.platform.ia.estrategia;

import java.util.List;

public record AnalisisTecnicoResultado(
        List<String> puntosFuertes,
        List<String> areasMejora,
        List<String> consejos,
        int scoreEstimado
) {
}

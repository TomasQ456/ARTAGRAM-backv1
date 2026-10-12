package com.artagram.platform.party.dto;

import java.util.UUID;

public record SalaFiestaResponse(
        UUID id,
        String codigo,
        String anfitrionNombre,
        int duracionRondaSegundos,
        int cupoMaximo,
        String estado,
        long totalParticipantes,
        int numeroRonda
) {
}

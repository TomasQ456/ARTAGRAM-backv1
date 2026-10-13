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
        int numeroRonda,
        int totalRondas
) {
    public SalaFiestaResponse(UUID id, String codigo, String anfitrionNombre, int duracionRondaSegundos, int cupoMaximo, String estado, long totalParticipantes, int numeroRonda) {
        this(id, codigo, anfitrionNombre, duracionRondaSegundos, cupoMaximo, estado, totalParticipantes, numeroRonda, 3);
    }
}

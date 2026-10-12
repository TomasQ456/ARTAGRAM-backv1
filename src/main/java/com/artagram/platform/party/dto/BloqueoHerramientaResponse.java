package com.artagram.platform.party.dto;

public record BloqueoHerramientaResponse(
        boolean tomado,
        String herramienta,
        String propietarioNombre
) {
}

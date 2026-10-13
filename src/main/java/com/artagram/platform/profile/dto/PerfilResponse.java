package com.artagram.platform.profile.dto;

import java.util.List;
import java.util.UUID;

public record PerfilResponse(
        UUID usuarioId,
        String nombreArtista,
        String biografia,
        String fotoUrl,
        List<EnlaceDto> enlaces,
        List<ObraDestacadaResumen> obrasDestacadas,
        boolean esPropio,
        boolean premium
) {

    public record ObraDestacadaResumen(String titulo, String imagenUrl) {
    }
}

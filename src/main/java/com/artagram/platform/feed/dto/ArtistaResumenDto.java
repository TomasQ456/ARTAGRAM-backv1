package com.artagram.platform.feed.dto;

import java.util.UUID;

/** Resultado del buscador de personas: lo justo para mostrar el boton Seguir / Siguiendo. */
public record ArtistaResumenDto(UUID usuarioId, String nombreArtista, boolean loSigo, long seguidores) {
}

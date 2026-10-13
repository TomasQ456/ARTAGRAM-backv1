package com.artagram.platform.auth.dto;

import java.util.UUID;

public record RegistroResponse(UUID id, String correo, String nombreArtista, String mensaje, boolean verificacionPendiente) {
}

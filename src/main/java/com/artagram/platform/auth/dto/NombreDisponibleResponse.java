package com.artagram.platform.auth.dto;

import java.util.List;

public record NombreDisponibleResponse(boolean disponible, List<String> sugerencias) {
}

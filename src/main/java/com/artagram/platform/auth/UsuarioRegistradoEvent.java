package com.artagram.platform.auth;

import java.util.UUID;

/**
 * Se publica dentro de la misma transaccion de registro para que otros modulos
 * (perfil, facturacion) puedan reaccionar sin acoplar AuthService a ellos.
 */
public record UsuarioRegistradoEvent(UUID usuarioId) {
}

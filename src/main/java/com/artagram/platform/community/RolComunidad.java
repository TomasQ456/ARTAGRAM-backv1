package com.artagram.platform.community;

public enum RolComunidad {
    ADMIN,
    MODERADOR,
    MIEMBRO;

    public boolean puedeModerar() {
        return this == ADMIN || this == MODERADOR;
    }
}

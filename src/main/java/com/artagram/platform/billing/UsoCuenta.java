package com.artagram.platform.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Fila de uso por usuario para las cuotas del plan gratis (HU-26). La clave
 * primaria ES el usuario_id (relacion 1:1), asi el bloqueo pesimista sobre esta
 * fila sirve como el punto de serializacion contra creaciones concurrentes.
 */
@Entity
@Table(name = "uso_cuenta")
public class UsoCuenta {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "comunidades_creadas", nullable = false)
    private int comunidadesCreadas = 0;

    @Column(name = "salas_fiesta_activas", nullable = false)
    private int salasFiestaActivas = 0;

    protected UsoCuenta() {
    }

    public UsoCuenta(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public int getComunidadesCreadas() {
        return comunidadesCreadas;
    }

    public void incrementarComunidades() {
        this.comunidadesCreadas++;
    }

    public void decrementarComunidades() {
        this.comunidadesCreadas = Math.max(0, this.comunidadesCreadas - 1);
    }

    public int getSalasFiestaActivas() {
        return salasFiestaActivas;
    }

    public void incrementarSalasFiesta() {
        this.salasFiestaActivas++;
    }

    public void decrementarSalasFiesta() {
        this.salasFiestaActivas = Math.max(0, this.salasFiestaActivas - 1);
    }
}

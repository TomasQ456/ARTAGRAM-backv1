package com.artagram.platform.feed;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.common.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Llave unica (seguidor, seguido) para que seguir dos veces seguidas sea
 * idempotente (igual necesidad que pide HU-04, aqui como soporte minimo de
 * HU-06: el feed no existe sin saber a quien sigue cada quien).
 */
@Entity
@Table(name = "seguidores", uniqueConstraints = @UniqueConstraint(columnNames = {"seguidor_id", "seguido_id"}))
public class Seguidor extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "seguidor_id", nullable = false)
    private Usuario seguidor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "seguido_id", nullable = false)
    private Usuario seguido;

    protected Seguidor() {
    }

    public Seguidor(Usuario seguidor, Usuario seguido) {
        this.seguidor = seguidor;
        this.seguido = seguido;
    }

    public Usuario getSeguidor() {
        return seguidor;
    }

    public Usuario getSeguido() {
        return seguido;
    }
}

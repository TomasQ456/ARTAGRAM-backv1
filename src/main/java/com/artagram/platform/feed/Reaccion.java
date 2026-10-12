package com.artagram.platform.feed;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.common.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * (usuario, publicacion) unico: la reaccion misma es la fuente de verdad de
 * "ya le di like o no"; el contador en Publicacion es una proyeccion atomica
 * que nunca se calcula con leer-sumar-escribir (HU-07).
 */
@Entity
@Table(name = "reacciones", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "publicacion_id"}))
public class Reaccion extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    protected Reaccion() {
    }

    public Reaccion(Usuario usuario, Publicacion publicacion) {
        this.usuario = usuario;
        this.publicacion = publicacion;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }
}

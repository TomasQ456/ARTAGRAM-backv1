package com.artagram.platform.feed.comentario;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.common.domain.BaseEntity;
import com.artagram.platform.feed.Publicacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "comentarios")
public class Comentario extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(nullable = false, length = 500)
    private String contenido;

    protected Comentario() {
    }

    public Comentario(Publicacion publicacion, Usuario autor, String contenido) {
        this.publicacion = publicacion;
        this.autor = autor;
        this.contenido = contenido;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }

    public Usuario getAutor() {
        return autor;
    }

    public String getContenido() {
        return contenido;
    }
}

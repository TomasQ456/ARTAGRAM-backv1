package com.artagram.platform.community;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "comunidades")
public class Comunidad extends BaseEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false)
    private boolean publica = true;

    @ManyToOne(optional = false)
    @JoinColumn(name = "creador_id", nullable = false)
    private Usuario creador;

    protected Comunidad() {
    }

    public Comunidad(String nombre, String descripcion, boolean publica, Usuario creador) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.publica = publica;
        this.creador = creador;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isPublica() {
        return publica;
    }

    public Usuario getCreador() {
        return creador;
    }
}

package com.artagram.platform.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ObraDestacada {

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(name = "imagen_url", nullable = false, length = 500)
    private String imagenUrl;

    @Column(nullable = false)
    private int orden;

    protected ObraDestacada() {
    }

    public ObraDestacada(String titulo, String imagenUrl, int orden) {
        this.titulo = titulo;
        this.imagenUrl = imagenUrl;
        this.orden = orden;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public int getOrden() {
        return orden;
    }
}

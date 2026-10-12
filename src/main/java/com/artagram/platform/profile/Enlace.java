package com.artagram.platform.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Enlace {

    @Column(nullable = false, length = 50)
    private String etiqueta;

    @Column(nullable = false, length = 300)
    private String url;

    @Column(nullable = false)
    private int orden;

    protected Enlace() {
    }

    public Enlace(String etiqueta, String url, int orden) {
        this.etiqueta = etiqueta;
        this.url = url;
        this.orden = orden;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getUrl() {
        return url;
    }

    public int getOrden() {
        return orden;
    }
}

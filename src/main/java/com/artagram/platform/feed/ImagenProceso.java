package com.artagram.platform.feed;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "imagenes_proceso")
public class ImagenProceso {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @Column(name = "ruta_imagen", nullable = false, length = 500)
    private String rutaImagen;

    @Column(nullable = false)
    private int orden;

    protected ImagenProceso() {
    }

    public ImagenProceso(Publicacion publicacion, String rutaImagen, int orden) {
        this.publicacion = publicacion;
        this.rutaImagen = rutaImagen;
        this.orden = orden;
    }

    public UUID getId() {
        return id;
    }

    public String getRutaImagen() {
        return rutaImagen;
    }

    public int getOrden() {
        return orden;
    }
}

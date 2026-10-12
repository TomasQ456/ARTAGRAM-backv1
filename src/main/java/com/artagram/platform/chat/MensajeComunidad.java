package com.artagram.platform.chat;

import com.artagram.platform.auth.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mensajes_comunidad")
public class MensajeComunidad {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, insertable = false, updatable = false)
    private long orden;

    @Column(name = "comunidad_id", nullable = false)
    private UUID comunidadId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "remitente_id", nullable = false)
    private Usuario remitente;

    @Column(nullable = false, length = 2000)
    private String contenido;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    protected MensajeComunidad() {
    }

    public MensajeComunidad(UUID comunidadId, Usuario remitente, String contenido) {
        this.comunidadId = comunidadId;
        this.remitente = remitente;
        this.contenido = contenido;
    }

    public UUID getId() {
        return id;
    }

    public long getOrden() {
        return orden;
    }

    public UUID getComunidadId() {
        return comunidadId;
    }

    public Usuario getRemitente() {
        return remitente;
    }

    public String getContenido() {
        return contenido;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}

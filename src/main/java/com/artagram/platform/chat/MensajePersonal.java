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

/**
 * Se guarda ANTES de intentar entregarlo (HU-21, nota tecnica): si el canal
 * falla, el mensaje no desaparece. `orden` (BIGSERIAL) es el desempate de
 * orden real, mas confiable que comparar timestamps de alta frecuencia.
 */
@Entity
@Table(name = "mensajes_personales")
public class MensajePersonal {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, insertable = false, updatable = false)
    private long orden;

    @ManyToOne(optional = false)
    @JoinColumn(name = "remitente_id", nullable = false)
    private Usuario remitente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Usuario destinatario;

    @Column(nullable = false, length = 2000)
    private String contenido;

    @Column(nullable = false)
    private boolean entregado = false;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    protected MensajePersonal() {
    }

    public MensajePersonal(Usuario remitente, Usuario destinatario, String contenido) {
        this.remitente = remitente;
        this.destinatario = destinatario;
        this.contenido = contenido;
    }

    public UUID getId() {
        return id;
    }

    public long getOrden() {
        return orden;
    }

    public Usuario getRemitente() {
        return remitente;
    }

    public Usuario getDestinatario() {
        return destinatario;
    }

    public String getContenido() {
        return contenido;
    }

    public boolean isEntregado() {
        return entregado;
    }

    public void marcarEntregado() {
        this.entregado = true;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}

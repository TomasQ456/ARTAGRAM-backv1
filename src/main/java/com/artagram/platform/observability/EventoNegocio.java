package com.artagram.platform.observability;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "eventos_negocio")
public class EventoNegocio {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEventoNegocio tipo;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "duracion_ms")
    private Long duracionMs;

    @Column(name = "ocurrido_en", nullable = false)
    private Instant ocurridoEn = Instant.now();

    protected EventoNegocio() {
    }

    public EventoNegocio(TipoEventoNegocio tipo, UUID usuarioId, Long duracionMs) {
        this.tipo = tipo;
        this.usuarioId = usuarioId;
        this.duracionMs = duracionMs;
    }

    public UUID getId() {
        return id;
    }

    public TipoEventoNegocio getTipo() {
        return tipo;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public Long getDuracionMs() {
        return duracionMs;
    }

    public Instant getOcurridoEn() {
        return ocurridoEn;
    }
}

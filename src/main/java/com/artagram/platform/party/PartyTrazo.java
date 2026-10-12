package com.artagram.platform.party;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.canvas.Punto;
import com.artagram.platform.canvas.PuntosConverter;
import com.artagram.platform.canvas.TipoPincel;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Registro append-only puro del lienzo de una ronda (HU-19): nunca se edita ni
 * se borra, ni siquiera el trazo de quien tuvo el borrador -- ese tambien
 * queda grabado, solo que marcado como borrador=true para que los clientes lo
 * rendericen como una goma en vez de un trazo normal.
 */
@Entity
@Table(name = "party_trazos")
public class PartyTrazo {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "sala_id", nullable = false)
    private UUID salaId;

    @Column(name = "numero_ronda", nullable = false)
    private int numeroRonda;

    @Column(nullable = false)
    private long secuencia;

    @ManyToOne(optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(nullable = false)
    private boolean borrador = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pincel", length = 20)
    private TipoPincel tipoPincel;

    @Column(name = "color_hex", length = 9)
    private String colorHex;

    @Column(nullable = false)
    private double grosor;

    @Column(nullable = false)
    private double opacidad;

    @Convert(converter = PuntosConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<Punto> puntos;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    protected PartyTrazo() {
    }

    public PartyTrazo(UUID salaId, int numeroRonda, long secuencia, Usuario autor, boolean borrador,
                       TipoPincel tipoPincel, String colorHex, double grosor, double opacidad, List<Punto> puntos) {
        this.salaId = salaId;
        this.numeroRonda = numeroRonda;
        this.secuencia = secuencia;
        this.autor = autor;
        this.borrador = borrador;
        this.tipoPincel = tipoPincel;
        this.colorHex = colorHex;
        this.grosor = grosor;
        this.opacidad = opacidad;
        this.puntos = puntos;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSalaId() {
        return salaId;
    }

    public int getNumeroRonda() {
        return numeroRonda;
    }

    public long getSecuencia() {
        return secuencia;
    }

    public Usuario getAutor() {
        return autor;
    }

    public boolean isBorrador() {
        return borrador;
    }

    public TipoPincel getTipoPincel() {
        return tipoPincel;
    }

    public String getColorHex() {
        return colorHex;
    }

    public double getGrosor() {
        return grosor;
    }

    public double getOpacidad() {
        return opacidad;
    }

    public List<Punto> getPuntos() {
        return puntos;
    }
}

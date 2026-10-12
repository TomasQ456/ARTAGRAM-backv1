package com.artagram.platform.ia;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.common.domain.BaseEntity;
import com.artagram.platform.feed.Publicacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "analisis_obra")
public class AnalisisObra extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TipoAnalisis tipo;

    @Column(name = "resultado_json", nullable = false, columnDefinition = "TEXT")
    private String resultadoJson;

    @Column(nullable = false, length = 64)
    private String modelo;

    protected AnalisisObra() {
    }

    public AnalisisObra(Publicacion publicacion, Usuario solicitante, TipoAnalisis tipo, String resultadoJson, String modelo) {
        this.publicacion = publicacion;
        this.solicitante = solicitante;
        this.tipo = tipo;
        this.resultadoJson = resultadoJson;
        this.modelo = modelo;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }

    public Usuario getSolicitante() {
        return solicitante;
    }

    public TipoAnalisis getTipo() {
        return tipo;
    }

    public String getResultadoJson() {
        return resultadoJson;
    }

    public String getModelo() {
        return modelo;
    }
}

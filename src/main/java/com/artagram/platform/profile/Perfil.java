package com.artagram.platform.profile;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "perfiles")
public class Perfil extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false, length = 500)
    private String biografia = "";

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @ElementCollection
    @CollectionTable(name = "perfil_enlaces", joinColumns = @JoinColumn(name = "perfil_id"))
    @OrderBy("orden asc")
    private List<Enlace> enlaces = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "perfil_obras_destacadas", joinColumns = @JoinColumn(name = "perfil_id"))
    @OrderBy("orden asc")
    private List<ObraDestacada> obrasDestacadas = new ArrayList<>();

    protected Perfil() {
    }

    public Perfil(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getBiografia() {
        return biografia;
    }

    public void setBiografia(String biografia) {
        this.biografia = biografia;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public List<Enlace> getEnlaces() {
        return enlaces;
    }

    public void reemplazarEnlaces(List<Enlace> nuevos) {
        this.enlaces.clear();
        this.enlaces.addAll(nuevos);
    }

    public List<ObraDestacada> getObrasDestacadas() {
        return obrasDestacadas;
    }

    public void reemplazarObrasDestacadas(List<ObraDestacada> nuevas) {
        this.obrasDestacadas.clear();
        this.obrasDestacadas.addAll(nuevas);
    }
}

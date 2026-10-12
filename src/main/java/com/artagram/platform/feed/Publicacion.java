package com.artagram.platform.feed;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "publicaciones")
public class Publicacion extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 2000)
    private String descripcion = "";

    @Column(name = "obra_final_ruta", nullable = false, length = 500)
    private String obraFinalRuta;

    @Column(name = "contador_likes", nullable = false)
    private int contadorLikes = 0;

    @ElementCollection
    @CollectionTable(name = "publicacion_etiquetas", joinColumns = @JoinColumn(name = "publicacion_id"))
    @Column(name = "etiqueta")
    private Set<String> etiquetas = new HashSet<>();

    @OneToMany(mappedBy = "publicacion", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orden asc")
    private List<ImagenProceso> imagenesProceso = new ArrayList<>();

    protected Publicacion() {
    }

    public Publicacion(Usuario autor, String titulo, String descripcion, String obraFinalRuta, Set<String> etiquetas) {
        this.autor = autor;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.obraFinalRuta = obraFinalRuta;
        this.etiquetas = etiquetas;
    }

    public Usuario getAutor() {
        return autor;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getObraFinalRuta() {
        return obraFinalRuta;
    }

    public int getContadorLikes() {
        return contadorLikes;
    }

    public Set<String> getEtiquetas() {
        return etiquetas;
    }

    public List<ImagenProceso> getImagenesProceso() {
        return imagenesProceso;
    }

    public void agregarImagenProceso(String ruta, int orden) {
        imagenesProceso.add(new ImagenProceso(this, ruta, orden));
    }
}

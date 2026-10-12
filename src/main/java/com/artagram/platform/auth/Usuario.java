package com.artagram.platform.auth;

import com.artagram.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String correo;

    @Column(name = "nombre_artista", nullable = false, unique = true)
    private String nombreArtista;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean verificado = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_global", nullable = false)
    private RolGlobal rolGlobal = RolGlobal.USUARIO;

    @Column(nullable = false)
    private boolean premium = false;

    protected Usuario() {
    }

    public Usuario(String correo, String nombreArtista, String passwordHash) {
        this.correo = correo;
        this.nombreArtista = nombreArtista;
        this.passwordHash = passwordHash;
    }

    public String getCorreo() {
        return correo;
    }

    public String getNombreArtista() {
        return nombreArtista;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isVerificado() {
        return verificado;
    }

    public void marcarVerificado() {
        this.verificado = true;
    }

    public RolGlobal getRolGlobal() {
        return rolGlobal;
    }

    public void setRolGlobal(RolGlobal rolGlobal) {
        this.rolGlobal = rolGlobal;
    }

    public boolean isPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public void setNombreArtista(String nombreArtista) {
        this.nombreArtista = nombreArtista;
    }
}

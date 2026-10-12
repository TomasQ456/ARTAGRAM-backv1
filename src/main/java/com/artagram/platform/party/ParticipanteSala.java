package com.artagram.platform.party;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "participantes_sala", uniqueConstraints = @UniqueConstraint(columnNames = {"sala_id", "usuario_id"}))
public class ParticipanteSala extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    private SalaFiesta sala;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private boolean activo = true;

    protected ParticipanteSala() {
    }

    public ParticipanteSala(SalaFiesta sala, Usuario usuario) {
        this.sala = sala;
        this.usuario = usuario;
    }

    public SalaFiesta getSala() {
        return sala;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public boolean isActivo() {
        return activo;
    }

    public void reactivar() {
        this.activo = true;
    }

    public void salir() {
        this.activo = false;
    }
}

package com.artagram.platform.profile;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilRepository extends JpaRepository<Perfil, UUID> {

    Optional<Perfil> findByUsuario_Id(UUID usuarioId);

    Optional<Perfil> findByUsuario_NombreArtistaIgnoreCase(String nombreArtista);
}

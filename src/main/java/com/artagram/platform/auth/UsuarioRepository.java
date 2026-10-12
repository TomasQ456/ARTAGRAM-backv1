package com.artagram.platform.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    Optional<Usuario> findByNombreArtistaIgnoreCase(String nombreArtista);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByNombreArtistaIgnoreCase(String nombreArtista);
}

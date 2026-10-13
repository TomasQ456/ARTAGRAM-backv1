package com.artagram.platform.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    Optional<Usuario> findByNombreArtistaIgnoreCase(String nombreArtista);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByNombreArtistaIgnoreCase(String nombreArtista);

    /** Buscador de personas del feed: solo cuentas verificadas y sin incluir a quien busca. */
    List<Usuario> findTop10ByNombreArtistaContainingIgnoreCaseAndVerificadoTrueAndIdNotOrderByNombreArtistaAsc(
            String texto, UUID excluirId);
}

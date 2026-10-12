package com.artagram.platform.community;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComunidadRepository extends JpaRepository<Comunidad, UUID> {

    boolean existsByNombreIgnoreCase(String nombre);

    Optional<Comunidad> findByNombreIgnoreCase(String nombre);
}

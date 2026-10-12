package com.artagram.platform.feed;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReaccionRepository extends JpaRepository<Reaccion, UUID> {

    Optional<Reaccion> findByUsuario_IdAndPublicacion_Id(UUID usuarioId, UUID publicacionId);

    boolean existsByUsuario_IdAndPublicacion_Id(UUID usuarioId, UUID publicacionId);
}

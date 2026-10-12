package com.artagram.platform.community;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembresiaComunidadRepository extends JpaRepository<MembresiaComunidad, UUID> {

    Optional<MembresiaComunidad> findByComunidad_IdAndUsuario_Id(UUID comunidadId, UUID usuarioId);

    List<MembresiaComunidad> findByComunidad_IdAndActivoTrue(UUID comunidadId);

    List<MembresiaComunidad> findByUsuario_IdAndActivoTrue(UUID usuarioId);

    long countByComunidad_IdAndActivoTrue(UUID comunidadId);

    boolean existsByComunidad_IdAndUsuario_IdAndActivoTrue(UUID comunidadId, UUID usuarioId);
}

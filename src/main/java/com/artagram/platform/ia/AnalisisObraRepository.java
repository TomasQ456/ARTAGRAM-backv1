package com.artagram.platform.ia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalisisObraRepository extends JpaRepository<AnalisisObra, UUID> {

    Optional<AnalisisObra> findFirstByPublicacion_IdAndTipoOrderByCreadoEnDesc(UUID publicacionId, TipoAnalisis tipo);

    List<AnalisisObra> findByPublicacion_IdOrderByCreadoEnDesc(UUID publicacionId);
}

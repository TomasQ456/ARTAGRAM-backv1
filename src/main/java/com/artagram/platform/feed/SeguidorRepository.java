package com.artagram.platform.feed;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeguidorRepository extends JpaRepository<Seguidor, UUID> {

    boolean existsBySeguidor_IdAndSeguido_Id(UUID seguidorId, UUID seguidoId);

    void deleteBySeguidor_IdAndSeguido_Id(UUID seguidorId, UUID seguidoId);

    List<Seguidor> findBySeguidor_Id(UUID seguidorId);

    long countBySeguido_Id(UUID seguidoId);
}

package com.artagram.platform.party;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipanteSalaRepository extends JpaRepository<ParticipanteSala, UUID> {

    Optional<ParticipanteSala> findBySala_IdAndUsuario_Id(UUID salaId, UUID usuarioId);

    List<ParticipanteSala> findBySala_IdAndActivoTrue(UUID salaId);

    long countBySala_IdAndActivoTrue(UUID salaId);

    boolean existsBySala_IdAndUsuario_IdAndActivoTrue(UUID salaId, UUID usuarioId);
}

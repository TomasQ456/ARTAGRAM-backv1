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

    @org.springframework.data.jpa.repository.Query("select p.sala from ParticipanteSala p where p.usuario.id = :usuarioId and p.activo = true and p.sala.estado <> com.artagram.platform.party.EstadoSala.CERRADA order by p.creadoEn desc")
    List<SalaFiesta> findSalasActivasDelUsuario(@org.springframework.data.repository.query.Param("usuarioId") UUID usuarioId);
}

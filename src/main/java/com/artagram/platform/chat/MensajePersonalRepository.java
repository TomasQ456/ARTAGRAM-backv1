package com.artagram.platform.chat;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MensajePersonalRepository extends JpaRepository<MensajePersonal, UUID> {

    @Query("select m from MensajePersonal m where "
            + "(m.remitente.id = :a and m.destinatario.id = :b) or (m.remitente.id = :b and m.destinatario.id = :a) "
            + "order by m.orden asc")
    List<MensajePersonal> conversacion(@Param("a") UUID usuarioA, @Param("b") UUID usuarioB);

    List<MensajePersonal> findByDestinatario_IdAndEntregadoFalseOrderByOrdenAsc(UUID destinatarioId);
}

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

    @Query("select m from MensajePersonal m join fetch m.remitente join fetch m.destinatario "
            + "where m.remitente.id = :usuarioId or m.destinatario.id = :usuarioId "
            + "order by m.orden desc")
    List<MensajePersonal> mensajesDelUsuario(@Param("usuarioId") UUID usuarioId);

    List<MensajePersonal> findByDestinatario_IdAndEntregadoFalseOrderByOrdenAsc(UUID destinatarioId);
}

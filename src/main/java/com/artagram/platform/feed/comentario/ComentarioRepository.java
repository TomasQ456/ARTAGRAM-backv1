package com.artagram.platform.feed.comentario;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComentarioRepository extends JpaRepository<Comentario, UUID> {

    @Query("select c from Comentario c join fetch c.autor join fetch c.publicacion "
            + "where c.publicacion.id = :publicacionId order by c.creadoEn asc, c.id asc")
    List<Comentario> listarPorPublicacion(@Param("publicacionId") UUID publicacionId, Pageable pageable);

    long countByPublicacion_Id(UUID publicacionId);
}

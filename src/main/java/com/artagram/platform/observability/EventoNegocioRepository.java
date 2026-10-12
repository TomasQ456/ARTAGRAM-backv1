package com.artagram.platform.observability;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoNegocioRepository extends JpaRepository<EventoNegocio, UUID> {

    @Query("select count(distinct e.usuarioId) from EventoNegocio e "
            + "where e.tipo = :tipo and e.ocurridoEn between :desde and :hasta")
    long contarUsuariosDistintos(@Param("tipo") TipoEventoNegocio tipo, @Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("select count(e) from EventoNegocio e where e.tipo = :tipo and e.ocurridoEn between :desde and :hasta")
    long contarEventos(@Param("tipo") TipoEventoNegocio tipo, @Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("select avg(e.duracionMs) from EventoNegocio e "
            + "where e.tipo = :tipo and e.duracionMs is not null and e.ocurridoEn between :desde and :hasta")
    Double promedioDuracionMs(@Param("tipo") TipoEventoNegocio tipo, @Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("select distinct e.usuarioId from EventoNegocio e where e.tipo = :tipo and e.ocurridoEn between :desde and :hasta")
    List<UUID> usuariosDistintos(@Param("tipo") TipoEventoNegocio tipo, @Param("desde") Instant desde, @Param("hasta") Instant hasta);

    boolean existsByUsuarioIdAndTipoAndOcurridoEnBetween(UUID usuarioId, TipoEventoNegocio tipo, Instant desde, Instant hasta);
}

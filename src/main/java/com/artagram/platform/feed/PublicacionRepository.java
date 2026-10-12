package com.artagram.platform.feed;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicacionRepository extends JpaRepository<Publicacion, UUID> {

    /**
     * Paginacion por cursor sobre (fecha, id) en vez de offset (HU-06, nota
     * tecnica): asi no se repiten ni se saltan publicaciones cuando entran
     * nuevas mientras el usuario sigue bajando.
     */
    @Query("select p from Publicacion p where p.autor.id in :autores "
            + "and (p.creadoEn < :cursorFecha or (p.creadoEn = :cursorFecha and p.id < :cursorId)) "
            + "order by p.creadoEn desc, p.id desc")
    List<Publicacion> feedDesde(@Param("autores") List<UUID> autores, @Param("cursorFecha") Instant cursorFecha,
                                 @Param("cursorId") UUID cursorId, org.springframework.data.domain.Pageable pageable);

    @Query("select p from Publicacion p where p.autor.id in :autores order by p.creadoEn desc, p.id desc")
    List<Publicacion> feedPrimeraPagina(@Param("autores") List<UUID> autores, org.springframework.data.domain.Pageable pageable);

    // clearAutomatically: una UPDATE en bloque no pasa por el contexto de
    // persistencia, asi que sin esto un findById posterior en la MISMA
    // transaccion devolveria el valor viejo que ya tenia cacheado (HU-07).
    @Modifying(clearAutomatically = true)
    @Query("update Publicacion p set p.contadorLikes = p.contadorLikes + 1 where p.id = :id")
    int incrementarLikes(@Param("id") UUID id);

    @Modifying(clearAutomatically = true)
    @Query("update Publicacion p set p.contadorLikes = case when p.contadorLikes > 0 then p.contadorLikes - 1 else 0 end where p.id = :id")
    int decrementarLikes(@Param("id") UUID id);

    @Modifying(clearAutomatically = true)
    @Query("update Publicacion p set p.contadorComentarios = p.contadorComentarios + 1 where p.id = :id")
    int incrementarComentarios(@Param("id") UUID id);

    @Modifying(clearAutomatically = true)
    @Query("update Publicacion p set p.contadorComentarios = case when p.contadorComentarios > 0 then p.contadorComentarios - 1 else 0 end where p.id = :id")
    int decrementarComentarios(@Param("id") UUID id);
}

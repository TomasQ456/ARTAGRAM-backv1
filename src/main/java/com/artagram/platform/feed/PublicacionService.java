package com.artagram.platform.feed;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.feed.dto.FeedResponse;
import com.artagram.platform.feed.dto.LikeResponse;
import com.artagram.platform.feed.dto.PublicacionDto;
import com.artagram.platform.profile.StorageService;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * HU-05 (publicar obra+proceso), HU-06 (feed paginado por cursor) y HU-07
 * (likes con contador atomico, sin leer-sumar-escribir).
 */
@Service
public class PublicacionService {

    private static final Logger log = LoggerFactory.getLogger(PublicacionService.class);
    private static final long TAMANO_MAX_IMAGEN = 15L * 1024 * 1024;
    private static final int MAX_IMAGENES_PROCESO = 10;
    private static final int MAX_ETIQUETAS = 5;
    private static final int TAMANO_PAGINA = 20;

    private final PublicacionRepository publicacionRepository;
    private final ReaccionRepository reaccionRepository;
    private final SeguidorRepository seguidorRepository;
    private final UsuarioRepository usuarioRepository;
    private final StorageService storageService;

    private final ApplicationEventPublisher eventPublisher;

    public PublicacionService(PublicacionRepository publicacionRepository, ReaccionRepository reaccionRepository,
                               SeguidorRepository seguidorRepository, UsuarioRepository usuarioRepository,
                               StorageService storageService, ApplicationEventPublisher eventPublisher) {
        this.publicacionRepository = publicacionRepository;
        this.eventPublisher = eventPublisher;
        this.reaccionRepository = reaccionRepository;
        this.seguidorRepository = seguidorRepository;
        this.usuarioRepository = usuarioRepository;
        this.storageService = storageService;
    }

    @Transactional
    public PublicacionDto publicar(UUID autorId, String titulo, String descripcion, Set<String> etiquetas,
                                    MultipartFile obraFinal, List<MultipartFile> imagenesProceso) {
        if (obraFinal == null || obraFinal.isEmpty()) {
            throw ApiException.badRequest("FALTA_OBRA_FINAL", "Falta marcar cual es la obra final");
        }
        if (imagenesProceso != null && imagenesProceso.size() > MAX_IMAGENES_PROCESO) {
            throw ApiException.badRequest("DEMASIADAS_IMAGENES", "Maximo " + MAX_IMAGENES_PROCESO + " imagenes de proceso");
        }
        Set<String> etiquetasFinales = etiquetas == null ? Set.of() : etiquetas;
        if (etiquetasFinales.size() > MAX_ETIQUETAS) {
            throw ApiException.badRequest("DEMASIADAS_ETIQUETAS", "Maximo " + MAX_ETIQUETAS + " etiquetas");
        }

        Usuario autor = usuarioRepository.getReferenceById(autorId);
        String subcarpeta = "publicaciones/" + autorId;
        String rutaObraFinal = storageService.guardar(obraFinal, subcarpeta, TAMANO_MAX_IMAGEN);

        Publicacion publicacion = new Publicacion(autor, titulo, descripcion == null ? "" : descripcion,
                rutaObraFinal, new LinkedHashSet<>(etiquetasFinales));
        if (imagenesProceso != null) {
            int orden = 0;
            for (MultipartFile imagen : imagenesProceso) {
                String ruta = storageService.guardar(imagen, subcarpeta + "/proceso", TAMANO_MAX_IMAGEN);
                publicacion.agregarImagenProceso(ruta, orden++);
            }
        }
        publicacion = publicacionRepository.save(publicacion);
        log.info("Publicación creada: id={} autor={} titulo='{}' imagenesProceso={}",
                publicacion.getId(), autorId, titulo, imagenesProceso == null ? 0 : imagenesProceso.size());
        return aDto(publicacion, autorId);
    }

    @Transactional(readOnly = true)
    public FeedResponse feed(UUID usuarioId, Instant cursorFecha, UUID cursorId) {
        List<UUID> autores = new java.util.ArrayList<>(seguidorRepository.findBySeguidor_Id(usuarioId).stream()
                .map(s -> s.getSeguido().getId()).toList());
        autores.add(usuarioId);

        List<Publicacion> pagina;
        if (cursorFecha == null || cursorId == null) {
            pagina = publicacionRepository.feedPrimeraPagina(autores, PageRequest.of(0, TAMANO_PAGINA + 1));
        } else {
            pagina = publicacionRepository.feedDesde(autores, cursorFecha, cursorId, PageRequest.of(0, TAMANO_PAGINA + 1));
        }

        boolean hayMas = pagina.size() > TAMANO_PAGINA;
        List<Publicacion> visibles = hayMas ? pagina.subList(0, TAMANO_PAGINA) : pagina;
        List<PublicacionDto> dtos = visibles.stream().map(p -> aDto(p, usuarioId)).toList();

        List<String> sugeridos = List.of();
        if (visibles.isEmpty() && autores.size() <= 1) {
            sugeridos = usuarioRepository.findAll().stream()
                    .filter(u -> !u.getId().equals(usuarioId))
                    .map(Usuario::getNombreArtista)
                    .limit(5)
                    .toList();
        }

        Publicacion ultima = visibles.isEmpty() ? null : visibles.get(visibles.size() - 1);
        return new FeedResponse(dtos, ultima == null ? null : ultima.getCreadoEn(), ultima == null ? null : ultima.getId(),
                hayMas, sugeridos);
    }

    @Transactional
    public LikeResponse alternarLike(UUID usuarioId, UUID publicacionId) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> ApiException.notFound("PUBLICACION_NO_ENCONTRADA", "Esa publicacion no existe"));
        boolean yaLeDioLike = reaccionRepository.existsByUsuario_IdAndPublicacion_Id(usuarioId, publicacionId);

        if (yaLeDioLike) {
            reaccionRepository.findByUsuario_IdAndPublicacion_Id(usuarioId, publicacionId)
                    .ifPresent(reaccionRepository::delete);
            reaccionRepository.flush();
            publicacionRepository.decrementarLikes(publicacionId);
            int actual = publicacionRepository.findById(publicacionId).map(Publicacion::getContadorLikes).orElse(0);
            log.debug("Like retirado: publicacion={} usuario={} contador={}", publicacionId, usuarioId, actual);
            eventPublisher.publishEvent(new LikeActualizadoEvent(publicacionId, actual));
            return new LikeResponse(false, actual);
        }

        try {
            Usuario usuario = usuarioRepository.getReferenceById(usuarioId);
            // flush explicito: la UPDATE en bloque de mas abajo limpia el
            // contexto de persistencia (clearAutomatically), y si el INSERT de
            // la reaccion todavia no se habia mandado a la base, se perderia.
            reaccionRepository.saveAndFlush(new Reaccion(usuario, publicacion));
            publicacionRepository.incrementarLikes(publicacionId);
        } catch (DataIntegrityViolationException e) {
            log.debug("Like duplicado ignorado: publicacion={} usuario={}", publicacionId, usuarioId);
        }
        int actual = publicacionRepository.findById(publicacionId).map(Publicacion::getContadorLikes).orElse(0);
        log.debug("Like registrado: publicacion={} usuario={} contador={}", publicacionId, usuarioId, actual);
        eventPublisher.publishEvent(new LikeActualizadoEvent(publicacionId, actual));
        return new LikeResponse(true, actual);
    }

    /**
     * Solo el autor puede eliminar su publicacion (no un moderador ni un
     * admin — no hay ninguna HU que pida eso). Las filas de likes/etiquetas/
     * imagenes de proceso se van solas por el ON DELETE CASCADE de la
     * migracion; los archivos del storage no tienen esa red de seguridad, asi
     * que se borran a mano antes de la fila.
     */
    @Transactional
    public void eliminar(UUID usuarioId, UUID publicacionId) {
        eliminar(usuarioId, publicacionId, false);
    }

    @Transactional
    public void eliminar(UUID usuarioId, UUID publicacionId, boolean esAdmin) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> ApiException.notFound("PUBLICACION_NO_ENCONTRADA", "Esa publicacion no existe"));
        if (!esAdmin && !publicacion.getAutor().getId().equals(usuarioId)) {
            throw ApiException.forbidden("SOLO_AUTOR", "Solo quien publico esto o un administrador puede eliminarlo");
        }

        storageService.eliminar(publicacion.getObraFinalRuta());
        publicacion.getImagenesProceso().forEach(img -> storageService.eliminar(img.getRutaImagen()));

        publicacionRepository.delete(publicacion);
        log.info("Publicación eliminada: id={} solicitante={} (esAdmin={})", publicacionId, usuarioId, esAdmin);
    }

    @Transactional
    public PublicacionDto editar(UUID usuarioId, UUID publicacionId, com.artagram.platform.feed.dto.EditarPublicacionRequest request) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> ApiException.notFound("PUBLICACION_NO_ENCONTRADA", "Esa publicacion no existe"));
        if (!publicacion.getAutor().getId().equals(usuarioId)) {
            throw ApiException.forbidden("SOLO_AUTOR", "Solo quien publico esto puede editarlo");
        }
        if (request.version() != null && !request.version().equals(publicacion.getVersion())) {
            throw new org.springframework.dao.OptimisticLockingFailureException("La publicacion fue modificada desde otra sesion");
        }
        if (request.etiquetas() != null && request.etiquetas().size() > MAX_ETIQUETAS) {
            throw ApiException.badRequest("DEMASIADAS_ETIQUETAS", "Maximo " + MAX_ETIQUETAS + " etiquetas");
        }
        publicacion.editar(request.titulo(), request.descripcion(), request.etiquetas());
        publicacion = publicacionRepository.save(publicacion);
        log.info("Publicación editada: id={} autor={} nuevaVersion={}", publicacionId, usuarioId, publicacion.getVersion());
        return aDto(publicacion, usuarioId);
    }

    private PublicacionDto aDto(Publicacion publicacion, UUID usuarioSolicitante) {
        boolean meGusta = usuarioSolicitante != null
                && reaccionRepository.existsByUsuario_IdAndPublicacion_Id(usuarioSolicitante, publicacion.getId());
        List<String> imagenesUrls = publicacion.getImagenesProceso().stream()
                .map(img -> storageService.urlFirmada(img.getRutaImagen()))
                .toList();
        Set<String> etiquetas = Set.copyOf(publicacion.getEtiquetas());
        return new PublicacionDto(publicacion.getId(), publicacion.getAutor().getId(), publicacion.getAutor().getNombreArtista(),
                publicacion.getTitulo(), publicacion.getDescripcion(), storageService.urlFirmada(publicacion.getObraFinalRuta()),
                imagenesUrls, etiquetas, publicacion.getContadorLikes(), meGusta, publicacion.getCreadoEn(),
                publicacion.getContadorComentarios(), publicacion.getVersion(), publicacion.getEditadoEn());
    }
}

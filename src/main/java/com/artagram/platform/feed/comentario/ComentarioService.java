package com.artagram.platform.feed.comentario;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.feed.Publicacion;
import com.artagram.platform.feed.PublicacionRepository;
import com.artagram.platform.feed.comentario.validacion.ComentarioContext;
import com.artagram.platform.feed.comentario.validacion.ValidadorComentarios;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComentarioService {

    private static final Logger log = LoggerFactory.getLogger(ComentarioService.class);

    private final ComentarioRepository comentarioRepository;
    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ValidadorComentarios validadorComentarios;
    private final ApplicationEventPublisher eventPublisher;

    public ComentarioService(ComentarioRepository comentarioRepository,
                             PublicacionRepository publicacionRepository,
                             UsuarioRepository usuarioRepository,
                             ValidadorComentarios validadorComentarios,
                             ApplicationEventPublisher eventPublisher) {
        this.comentarioRepository = comentarioRepository;
        this.publicacionRepository = publicacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.validadorComentarios = validadorComentarios;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ComentarioDto crearComentario(UUID autorId, UUID publicacionId, String contenido) {
        validadorComentarios.validar(new ComentarioContext(autorId, publicacionId, contenido));

        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> ApiException.notFound("PUBLICACION_NO_ENCONTRADA", "Esa publicación no existe"));
        Usuario autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> ApiException.notFound("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));

        Comentario comentario = new Comentario(publicacion, autor, contenido.trim());
        comentario = comentarioRepository.save(comentario);

        publicacionRepository.incrementarComentarios(publicacionId);
        int totalComentarios = publicacionRepository.findById(publicacionId)
                .map(Publicacion::getContadorComentarios)
                .orElse((int) comentarioRepository.countByPublicacion_Id(publicacionId));

        ComentarioDto dto = aDto(comentario);
        eventPublisher.publishEvent(new ComentarioCreadoEvent(dto, totalComentarios));

        log.info("Comentario creado: id={} publicacion={} autor={}", comentario.getId(), publicacionId, autorId);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ComentarioDto> listar(UUID publicacionId, int pagina, int tamano) {
        int tamanoFinal = Math.min(Math.max(tamano, 1), 50);
        return comentarioRepository.listarPorPublicacion(publicacionId, PageRequest.of(pagina, tamanoFinal))
                .stream()
                .map(this::aDto)
                .toList();
    }

    @Transactional
    public void eliminar(UUID usuarioId, UUID comentarioId) {
        Comentario comentario = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> ApiException.notFound("COMENTARIO_NO_ENCONTRADO", "Ese comentario no existe"));

        UUID autorComentarioId = comentario.getAutor().getId();
        UUID autorPublicacionId = comentario.getPublicacion().getAutor().getId();

        if (!usuarioId.equals(autorComentarioId) && !usuarioId.equals(autorPublicacionId)) {
            throw ApiException.forbidden("SOLO_AUTOR", "No tienes permiso para eliminar este comentario");
        }

        UUID publicacionId = comentario.getPublicacion().getId();
        comentarioRepository.delete(comentario);
        publicacionRepository.decrementarComentarios(publicacionId);

        log.info("Comentario eliminado: id={} por usuario={}", comentarioId, usuarioId);
    }

    private ComentarioDto aDto(Comentario comentario) {
        return new ComentarioDto(
                comentario.getId(),
                comentario.getPublicacion().getId(),
                comentario.getAutor().getId(),
                comentario.getAutor().getNombreArtista(),
                comentario.getContenido(),
                comentario.getCreadoEn()
        );
    }
}

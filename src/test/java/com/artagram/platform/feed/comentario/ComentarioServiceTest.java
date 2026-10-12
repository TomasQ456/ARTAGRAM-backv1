package com.artagram.platform.feed.comentario;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.feed.Publicacion;
import com.artagram.platform.feed.PublicacionRepository;
import com.artagram.platform.feed.comentario.validacion.ValidadorComentarios;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComentarioServiceTest {

    private ComentarioRepository comentarioRepository;
    private PublicacionRepository publicacionRepository;
    private UsuarioRepository usuarioRepository;
    private ApplicationEventPublisher eventPublisher;
    private ComentarioService service;

    @BeforeEach
    void setUp() {
        comentarioRepository = mock(ComentarioRepository.class);
        publicacionRepository = mock(PublicacionRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        ValidadorComentarios validadorComentarios = new ValidadorComentarios(List.of());

        service = new ComentarioService(comentarioRepository, publicacionRepository,
                usuarioRepository, validadorComentarios, eventPublisher);
    }

    @Test
    @DisplayName("Crea comentario exitosamente, incrementa contador y emite evento")
    void crearComentarioExitoso() {
        UUID autorId = UUID.randomUUID();
        UUID publicacionId = UUID.randomUUID();

        Usuario autor = new Usuario("autor@test.com", "ArtistaPro", "hash123");
        ReflectionTestUtils.setField(autor, "id", autorId);

        Publicacion publicacion = new Publicacion(autor, "Obra", "Desc", "path.png", java.util.Set.of());
        ReflectionTestUtils.setField(publicacion, "id", publicacionId);

        when(publicacionRepository.findById(publicacionId)).thenReturn(Optional.of(publicacion));
        when(usuarioRepository.findById(autorId)).thenReturn(Optional.of(autor));
        when(comentarioRepository.save(any(Comentario.class))).thenAnswer(inv -> inv.getArgument(0));

        ComentarioDto dto = service.crearComentario(autorId, publicacionId, "Hermosa paleta de colores");

        assertThat(dto).isNotNull();
        assertThat(dto.contenido()).isEqualTo("Hermosa paleta de colores");
        assertThat(dto.autorNombre()).isEqualTo("ArtistaPro");

        verify(publicacionRepository).incrementarComentarios(publicacionId);
        verify(eventPublisher).publishEvent(any(ComentarioCreadoEvent.class));
    }

    @Test
    @DisplayName("Falla al crear comentario si la publicación no existe")
    void crearComentarioPublicacionNoExiste() {
        UUID autorId = UUID.randomUUID();
        UUID publicacionId = UUID.randomUUID();
        when(publicacionRepository.findById(publicacionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearComentario(autorId, publicacionId, "Hola"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("no existe");
    }

    @Test
    @DisplayName("Solo autor del comentario o de la obra puede eliminarlo")
    void eliminarComentarioPermisos() {
        Usuario autorObra = new Usuario("obra@test.com", "AutorObra", "hash123");
        ReflectionTestUtils.setField(autorObra, "id", UUID.randomUUID());

        Usuario autorComentario = new Usuario("com@test.com", "AutorComentario", "hash123");
        ReflectionTestUtils.setField(autorComentario, "id", UUID.randomUUID());

        Usuario tercero = new Usuario("tercero@test.com", "Tercero", "hash123");
        ReflectionTestUtils.setField(tercero, "id", UUID.randomUUID());

        Publicacion publicacion = new Publicacion(autorObra, "Obra", "Desc", "path.png", java.util.Set.of());
        ReflectionTestUtils.setField(publicacion, "id", UUID.randomUUID());

        Comentario comentario = new Comentario(publicacion, autorComentario, "Comentario de prueba");
        UUID comentarioId = UUID.randomUUID();
        ReflectionTestUtils.setField(comentario, "id", comentarioId);

        when(comentarioRepository.findById(comentarioId)).thenReturn(Optional.of(comentario));

        // Tercero intenta borrar -> Forbidden
        assertThatThrownBy(() -> service.eliminar(tercero.getId(), comentarioId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No tienes permiso");

        // Autor del comentario borra -> OK
        service.eliminar(autorComentario.getId(), comentarioId);
        verify(comentarioRepository).delete(comentario);
        verify(publicacionRepository).decrementarComentarios(publicacion.getId());
    }
}

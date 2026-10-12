package com.artagram.platform.feed;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.feed.dto.EditarPublicacionRequest;
import com.artagram.platform.feed.dto.PublicacionDto;
import com.artagram.platform.profile.StorageProperties;
import com.artagram.platform.profile.StorageProvider;
import com.artagram.platform.profile.StorageService;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicacionEdicionServiceTest {

    private PublicacionRepository publicacionRepository;
    private ReaccionRepository reaccionRepository;
    private SeguidorRepository seguidorRepository;
    private UsuarioRepository usuarioRepository;
    private PublicacionService service;

    @BeforeEach
    void setUp() {
        publicacionRepository = mock(PublicacionRepository.class);
        reaccionRepository = mock(ReaccionRepository.class);
        seguidorRepository = mock(SeguidorRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);

        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setPublicBaseUrl("http://localhost:8080/media");
        storageProperties.setSigningSecret("dev-signing-secret-key-32-bytes-minimum");
        storageProperties.setSignedUrlTtlMinutes(60);
        StorageProvider storageProvider = mock(StorageProvider.class);
        StorageService storageService = new StorageService(storageProperties, storageProvider);

        service = new PublicacionService(publicacionRepository, reaccionRepository,
                seguidorRepository, usuarioRepository, storageService);
    }

    @Test
    @DisplayName("Edición exitosa cuando el solicitante es el autor y la versión coincide")
    void edicionExitosa() {
        Usuario autor = new Usuario("autor@test.com", "Autor", "hash123");
        ReflectionTestUtils.setField(autor, "id", UUID.randomUUID());

        Publicacion publicacion = new Publicacion(autor, "Viejo Título", "Vieja Desc", "obra.png", Set.of("oleo"));
        UUID pubId = UUID.randomUUID();
        ReflectionTestUtils.setField(publicacion, "id", pubId);

        when(publicacionRepository.findById(pubId)).thenReturn(Optional.of(publicacion));
        when(publicacionRepository.save(any(Publicacion.class))).thenAnswer(inv -> inv.getArgument(0));

        EditarPublicacionRequest req = new EditarPublicacionRequest("Nuevo Título", "Nueva Desc", Set.of("acuarela"), 0);
        PublicacionDto dto = service.editar(autor.getId(), pubId, req);

        assertThat(dto.titulo()).isEqualTo("Nuevo Título");
        assertThat(dto.descripcion()).isEqualTo("Nueva Desc");
        assertThat(dto.etiquetas()).containsExactly("acuarela");
        assertThat(dto.obraFinalUrl()).contains("obra.png");
    }

    @Test
    @DisplayName("Falla con Forbidden si un usuario que no es el autor intenta editar")
    void edicionTerceroForbidden() {
        Usuario autor = new Usuario("autor@test.com", "Autor", "hash123");
        ReflectionTestUtils.setField(autor, "id", UUID.randomUUID());

        Usuario tercero = new Usuario("tercero@test.com", "Tercero", "hash123");
        ReflectionTestUtils.setField(tercero, "id", UUID.randomUUID());

        Publicacion publicacion = new Publicacion(autor, "Título", "Desc", "obra.png", Set.of());
        UUID pubId = UUID.randomUUID();
        ReflectionTestUtils.setField(publicacion, "id", pubId);

        when(publicacionRepository.findById(pubId)).thenReturn(Optional.of(publicacion));

        EditarPublicacionRequest req = new EditarPublicacionRequest("Nuevo Título", "Desc", Set.of(), 0);

        assertThatThrownBy(() -> service.editar(tercero.getId(), pubId, req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Solo quien publico esto puede editarlo");
    }

    @Test
    @DisplayName("Falla con OptimisticLockingFailureException si la versión del request no coincide con la versión actual")
    void edicionConflictoVersion() {
        Usuario autor = new Usuario("autor@test.com", "Autor", "hash123");
        ReflectionTestUtils.setField(autor, "id", UUID.randomUUID());

        Publicacion publicacion = new Publicacion(autor, "Título", "Desc", "obra.png", Set.of());
        UUID pubId = UUID.randomUUID();
        ReflectionTestUtils.setField(publicacion, "id", pubId);

        when(publicacionRepository.findById(pubId)).thenReturn(Optional.of(publicacion));

        EditarPublicacionRequest reqConVersionVieja = new EditarPublicacionRequest("Nuevo Título", "Desc", Set.of(), 5);

        assertThatThrownBy(() -> service.editar(autor.getId(), pubId, reqConVersionVieja))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessageContaining("otra sesion");
    }
}

package com.artagram.platform.ia;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.feed.Publicacion;
import com.artagram.platform.feed.PublicacionRepository;
import com.artagram.platform.ia.estrategia.AnalisisTecnicoEstrategia;
import com.artagram.platform.ia.estrategia.SugerenciaEtiquetasEstrategia;
import com.artagram.platform.ia.estrategia.SugerenciaEtiquetasResultado;
import com.artagram.platform.ia.proveedor.FakeProveedorIa;
import com.artagram.platform.profile.StorageProperties;
import com.artagram.platform.profile.StorageProvider;
import com.artagram.platform.profile.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IaServiceUnitTest {

    private AnalisisObraRepository analisisRepository;
    private PublicacionRepository publicacionRepository;
    private UsuarioRepository usuarioRepository;
    private StorageProvider storageProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FakeProveedorIa proveedorIa = new FakeProveedorIa();

    private IaService iaService;

    @BeforeEach
    void setUp() {
        analisisRepository = mock(AnalisisObraRepository.class);
        publicacionRepository = mock(PublicacionRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        storageProvider = mock(StorageProvider.class);

        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setPublicBaseUrl("http://localhost:8080/media");
        storageProperties.setSigningSecret("dev-signing-secret-key-32-bytes-minimum");
        StorageService storageService = new StorageService(storageProperties, storageProvider);

        AnalisisTecnicoEstrategia tecnica = new AnalisisTecnicoEstrategia(objectMapper);
        SugerenciaEtiquetasEstrategia etiquetas = new SugerenciaEtiquetasEstrategia(objectMapper);
        iaService = new IaService(analisisRepository, publicacionRepository, usuarioRepository,
                storageService, proveedorIa, List.of(tecnica, etiquetas));
    }

    @Test
    @DisplayName("Analizar publicación persiste resultado técnico en BD y devuelve DTO")
    void analizarPublicacionGuardaEnHistorial() throws Exception {
        Usuario autor = new Usuario("autor@test.com", "hash", "ArtistaPro");
        Publicacion publicacion = new Publicacion(autor, "Obra Maestra", "Desc", "obra.png", Set.of());
        UUID pubId = UUID.randomUUID();

        when(publicacionRepository.findById(pubId)).thenReturn(Optional.of(publicacion));
        when(usuarioRepository.findById(autor.getId())).thenReturn(Optional.of(autor));
        when(storageProvider.leer("obra.png")).thenReturn(new InputStreamResource(new ByteArrayInputStream(new byte[]{1, 2, 3})));
        when(analisisRepository.save(any(AnalisisObra.class))).thenAnswer(inv -> inv.getArgument(0));

        AnalisisObraDto dto = iaService.analizarPublicacion(autor.getId(), pubId, TipoAnalisis.TECNICO);

        assertThat(dto).isNotNull();
        assertThat(dto.tipo()).isEqualTo(TipoAnalisis.TECNICO);
        assertThat(dto.resultadoJson()).contains("puntosFuertes");
        assertThat(dto.modelo()).isEqualTo("mock-ia-v1");

        verify(analisisRepository).save(any(AnalisisObra.class));
    }

    @Test
    @DisplayName("Sugerir metadatos desde imagen en asistente antes de publicar")
    void sugerirMetadatosFunciona() {
        MockMultipartFile file = new MockMultipartFile("imagen", "canvas.png", "image/png", new byte[]{1, 2, 3});
        SugerenciaEtiquetasResultado resultado = iaService.sugerirMetadatos(file);

        assertThat(resultado).isNotNull();
        assertThat(resultado.tituloSugerido()).isNotBlank();
        assertThat(resultado.etiquetas()).isNotEmpty();
    }
}

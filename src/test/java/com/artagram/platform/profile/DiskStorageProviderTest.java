package com.artagram.platform.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

/**
 * DiskStorageProvider es la implementacion por defecto de StorageProvider —
 * blindada con una prueba directa porque quedo separada de StorageService al
 * introducir la abstraccion disco/Azure Blob.
 */
class DiskStorageProviderTest {

    private DiskStorageProvider provider;

    @BeforeEach
    void setUp(@TempDir java.nio.file.Path tempDir) {
        StorageProperties properties = new StorageProperties();
        properties.setRoot(tempDir.toString());
        provider = new DiskStorageProvider(properties);
    }

    @Test
    void archivoNoExisteAntesDeGuardarlo() {
        assertThat(provider.existe("perfiles/no-existe.png")).isFalse();
    }

    @Test
    void guardarLoDejaExistenteYLegibleConElMismoContenido() throws IOException {
        byte[] contenido = "contenido de prueba".getBytes(StandardCharsets.UTF_8);

        try (InputStream in = new ByteArrayInputStream(contenido)) {
            provider.guardar("perfiles/foto.png", in, contenido.length, "image/png");
        }

        assertThat(provider.existe("perfiles/foto.png")).isTrue();
        Resource leido = provider.leer("perfiles/foto.png");
        assertThat(leido.getInputStream().readAllBytes()).isEqualTo(contenido);
    }

    @Test
    void guardarCreaLasSubcarpetasQueHagaFalta() throws IOException {
        byte[] contenido = "otro contenido".getBytes(StandardCharsets.UTF_8);

        try (InputStream in = new ByteArrayInputStream(contenido)) {
            provider.guardar("publicaciones/abc/proceso/paso1.jpg", in, contenido.length, "image/jpeg");
        }

        assertThat(provider.existe("publicaciones/abc/proceso/paso1.jpg")).isTrue();
    }
}

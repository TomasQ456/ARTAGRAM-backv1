package com.artagram.platform.profile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Guarda en el disco local del contenedor/servidor — el default, para
 * desarrollo y para el stack de Docker Compose. En un entorno con mas de una
 * instancia o contenedores efimeros (ej. Azure App Service) los archivos no
 * sobreviven un reinicio/redeploy ni se ven entre instancias; para eso existe
 * {@link AzureBlobStorageProvider} (activar con {@code STORAGE_PROVIDER=blob}).
 */
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "disk", matchIfMissing = true)
public class DiskStorageProvider implements StorageProvider {

    private final StorageProperties properties;

    public DiskStorageProvider(StorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void guardar(String ruta, InputStream contenido, long tamanoBytes, String contentType) throws IOException {
        Path destino = resolver(ruta);
        Files.createDirectories(destino.getParent());
        Files.copy(contenido, destino, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public boolean existe(String ruta) {
        return Files.exists(resolver(ruta));
    }

    @Override
    public Resource leer(String ruta) {
        return new FileSystemResource(resolver(ruta));
    }

    @Override
    public void eliminar(String ruta) throws IOException {
        Files.deleteIfExists(resolver(ruta));
    }

    private Path resolver(String ruta) {
        return Path.of(properties.getRoot(), ruta).normalize();
    }
}

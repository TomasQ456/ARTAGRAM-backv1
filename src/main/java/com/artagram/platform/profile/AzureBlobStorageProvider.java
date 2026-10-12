package com.artagram.platform.profile;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Guarda en Azure Blob Storage — la opcion para produccion en Azure, donde el
 * disco local de una instancia de App Service no persiste entre reinicios ni
 * es visible desde otras instancias. Se activa con
 * {@code STORAGE_PROVIDER=blob} + {@code AZURE_STORAGE_CONNECTION_STRING} +
 * {@code AZURE_STORAGE_CONTAINER} — sin esas variables, el backend sigue
 * arrancando con {@link DiskStorageProvider} (el default).
 * <p>
 * No tiene prueba automatizada: no hay una cuenta de Azure real disponible en
 * este entorno de desarrollo (igual que {@code SmtpEmailService}, ver su
 * Javadoc). Revisado a mano contra la documentacion del SDK.
 */
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "blob")
public class AzureBlobStorageProvider implements StorageProvider {

    private final BlobContainerClient containerClient;

    public AzureBlobStorageProvider(StorageProperties properties) {
        this.containerClient = new BlobServiceClientBuilder()
                .connectionString(properties.getAzure().getConnectionString())
                .buildClient()
                .getBlobContainerClient(properties.getAzure().getContainer());
        if (!containerClient.exists()) {
            containerClient.create();
        }
    }

    @Override
    public void guardar(String ruta, InputStream contenido, long tamanoBytes, String contentType) throws IOException {
        BlobClient blobClient = containerClient.getBlobClient(ruta);
        blobClient.upload(contenido, tamanoBytes, true);
        blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
    }

    @Override
    public boolean existe(String ruta) {
        return containerClient.getBlobClient(ruta).exists();
    }

    @Override
    public Resource leer(String ruta) {
        BlobClient blobClient = containerClient.getBlobClient(ruta);
        return new InputStreamResource(blobClient.openInputStream());
    }

    @Override
    public void eliminar(String ruta) {
        containerClient.getBlobClient(ruta).deleteIfExists();
    }
}

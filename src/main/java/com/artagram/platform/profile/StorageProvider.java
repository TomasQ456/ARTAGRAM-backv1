package com.artagram.platform.profile;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.Resource;

/**
 * Backend real de donde viven los bytes de un archivo subido — abstrae disco
 * local (dev/Docker Compose) de blob storage en la nube (produccion), para
 * que {@link StorageService} (validacion + firma HMAC, agnosticas del
 * backend) no dependa de cual de los dos esta activo. Ver
 * {@link DiskStorageProvider} y {@link AzureBlobStorageProvider}.
 */
public interface StorageProvider {

    void guardar(String ruta, InputStream contenido, long tamanoBytes, String contentType) throws IOException;

    boolean existe(String ruta);

    Resource leer(String ruta) throws IOException;

    void eliminar(String ruta) throws IOException;
}

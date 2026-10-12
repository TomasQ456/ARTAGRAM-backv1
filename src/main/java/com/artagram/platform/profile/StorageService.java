package com.artagram.platform.profile;

import com.artagram.platform.common.error.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Valida y firma con HMAC el acceso a los archivos subidos, para que el
 * original privado nunca quede accesible sin el token vigente (HU-03, HU-29)
 * — agnostico de donde viven los bytes en si (ver {@link StorageProvider}:
 * disco local en desarrollo, Azure Blob Storage en produccion).
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/png", "image/jpeg", "image/webp");
    private static final String HMAC_ALGO = "HmacSHA256";

    private final StorageProperties properties;
    private final StorageProvider storageProvider;
    private final Tika tika = new Tika();

    public StorageService(StorageProperties properties, StorageProvider storageProvider) {
        this.properties = properties;
        this.storageProvider = storageProvider;
    }

    public String guardar(MultipartFile archivo, String subcarpeta, long tamanoMaximoBytes) {
        if (archivo.isEmpty()) {
            throw ApiException.badRequest("ARCHIVO_VACIO", "El archivo esta vacio");
        }
        if (archivo.getSize() > tamanoMaximoBytes) {
            throw ApiException.badRequest("ARCHIVO_MUY_GRANDE",
                    "El archivo supera el tamano maximo permitido de " + (tamanoMaximoBytes / (1024 * 1024)) + " MB");
        }
        String tipoDetectado = detectarTipo(archivo);
        if (!TIPOS_PERMITIDOS.contains(tipoDetectado)) {
            throw ApiException.badRequest("FORMATO_NO_PERMITIDO", "Formato no permitido: usa PNG, JPG o WEBP");
        }

        String extension = switch (tipoDetectado) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String nombreArchivo = UUID.randomUUID() + extension;
        String ruta = subcarpeta + "/" + nombreArchivo;
        try (InputStream in = archivo.getInputStream()) {
            storageProvider.guardar(ruta, in, archivo.getSize(), tipoDetectado);
        } catch (IOException e) {
            log.error("No se pudo guardar el archivo en subcarpeta={}", subcarpeta, e);
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "ERROR_ALMACENAMIENTO", "No se pudo guardar el archivo");
        }
        log.debug("Archivo guardado: subcarpeta={} nombre={} tipo={}", subcarpeta, nombreArchivo, tipoDetectado);
        return ruta;
    }

    private String detectarTipo(MultipartFile archivo) {
        try (InputStream in = archivo.getInputStream()) {
            return tika.detect(in);
        } catch (IOException e) {
            throw ApiException.badRequest("ARCHIVO_ILEGIBLE", "No se pudo leer el archivo");
        }
    }

    public String urlFirmada(String rutaRelativa) {
        if (rutaRelativa == null) {
            return null;
        }
        long expira = Instant.now().plusSeconds((long) properties.getSignedUrlTtlMinutes() * 60).getEpochSecond();
        String firma = firmar(rutaRelativa, expira);
        return properties.getPublicBaseUrl() + "/" + rutaRelativa + "?exp=" + expira + "&sig=" + firma;
    }

    public boolean firmaValida(String rutaRelativa, long expira, String firmaRecibida) {
        if (Instant.now().getEpochSecond() > expira) {
            return false;
        }
        String esperada = firmar(rutaRelativa, expira);
        return MessageDigest.isEqual(esperada.getBytes(StandardCharsets.UTF_8), firmaRecibida.getBytes(StandardCharsets.UTF_8));
    }

    public boolean existe(String rutaRelativa) {
        return storageProvider.existe(rutaRelativa);
    }

    public Resource leer(String rutaRelativa) throws IOException {
        return storageProvider.leer(rutaRelativa);
    }

    /**
     * Best-effort: un archivo huerfano en el storage no es tan grave como
     * dejar bloqueada la eliminacion de la publicacion por un problema
     * transitorio del backend de almacenamiento.
     */
    public void eliminar(String rutaRelativa) {
        if (rutaRelativa == null) {
            return;
        }
        try {
            storageProvider.eliminar(rutaRelativa);
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo {} (queda huerfano en el storage)", rutaRelativa, e);
        }
    }

    private String firmar(String rutaRelativa, long expira) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(properties.getSigningSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] resultado = mac.doFinal((rutaRelativa + ":" + expira).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(resultado);
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            throw new IllegalStateException("No se pudo firmar la URL", e);
        }
    }
}

package com.artagram.platform.profile;

import com.artagram.platform.common.error.ApiException;
import java.io.IOException;
import java.util.Locale;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Sirve los archivos subidos (foto de perfil, imagenes de obra/proceso) via una
 * URL firmada; el archivo original privado nunca queda expuesto sin firma vigente.
 */
@RestController
@Tag(name = "Archivos", description = "Sirve imágenes subidas vía URL firmada con HMAC")
public class MediaController {

    private final StorageService storageService;

    public MediaController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * La ruta relativa que guarda StorageService no es un solo segmento: por
     * ejemplo "publicaciones/{autorId}/proceso/{archivo}.jpg" tiene 4. Un
     * {subcarpeta}/{archivo} de un solo nivel nunca matchea eso (Spring nunca
     * hace que un solo @PathVariable capture "/"), así que esto necesita el
     * comodín "de cola" {*rutaRelativa} para capturar todos los segmentos.
     */
    @GetMapping("/media/{*rutaRelativa}")
    public ResponseEntity<Resource> servir(@PathVariable String rutaRelativa,
                                            @RequestParam long exp,
                                            @RequestParam String sig) throws IOException {
        String ruta = rutaRelativa.startsWith("/") ? rutaRelativa.substring(1) : rutaRelativa;
        if (!storageService.firmaValida(ruta, exp, sig)) {
            throw ApiException.forbidden("URL_INVALIDA", "El enlace vencio o no es valido");
        }
        if (!storageService.existe(ruta)) {
            throw ApiException.notFound("ARCHIVO_NO_ENCONTRADO", "El archivo no existe");
        }
        return ResponseEntity.ok()
                .contentType(tipoPorExtension(ruta))
                .body(storageService.leer(ruta));
    }

    /**
     * Files.probeContentType() depende de una base de datos de MIME types que la
     * imagen JRE-alpine del contenedor no trae, y devuelve null (o lanza) ahi
     * mismo, aunque en un JDK de escritorio funcione bien. Como el propio
     * StorageService solo permite subir PNG/JPEG/WEBP, alcanza con mirar la
     * extension que el mismo servicio le puso al guardar el archivo.
     */
    private MediaType tipoPorExtension(String archivo) {
        String nombre = archivo.toLowerCase(Locale.ROOT);
        if (nombre.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (nombre.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }
}

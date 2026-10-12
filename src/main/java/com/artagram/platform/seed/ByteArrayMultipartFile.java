package com.artagram.platform.seed;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementación mínima de MultipartFile para poder reusar los Service de
 * siempre (PublicacionService, PerfilService) desde el seeder de datos de
 * ejemplo, sin pasar por HTTP y sin depender de spring-test en producción
 * (MockMultipartFile vive ahí, no en el classpath del jar final).
 */
final class ByteArrayMultipartFile implements MultipartFile {

    private final String nombreArchivo;
    private final byte[] contenido;

    ByteArrayMultipartFile(String nombreArchivo, byte[] contenido) {
        this.nombreArchivo = nombreArchivo;
        this.contenido = contenido;
    }

    @Override
    @NonNull
    public String getName() {
        return nombreArchivo;
    }

    @Override
    public String getOriginalFilename() {
        return nombreArchivo;
    }

    @Override
    public String getContentType() {
        return "image/jpeg";
    }

    @Override
    public boolean isEmpty() {
        return contenido.length == 0;
    }

    @Override
    public long getSize() {
        return contenido.length;
    }

    @Override
    @NonNull
    public byte[] getBytes() {
        return contenido;
    }

    @Override
    @NonNull
    public InputStream getInputStream() {
        return new ByteArrayInputStream(contenido);
    }

    @Override
    public void transferTo(@NonNull java.io.File dest) throws IOException {
        try (OutputStream out = new java.io.FileOutputStream(dest)) {
            out.write(contenido);
        }
    }
}

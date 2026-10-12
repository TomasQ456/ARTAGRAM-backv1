package com.artagram.platform.auth;

public interface EmailService {

    void enviarVerificacion(String correoDestino, String nombreArtista, String enlaceVerificacion);
}

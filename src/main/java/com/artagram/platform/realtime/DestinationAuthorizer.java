package com.artagram.platform.realtime;

/**
 * Cada modulo con canales en tiempo real (lienzo, chat de comunidad, fiesta...)
 * aporta uno de estos para que StompAuthChannelInterceptor decida si un SUBSCRIBE
 * se deja pasar. La suscripcion es el punto donde HU-11 exige revalidar el rol,
 * no solo la interfaz.
 */
public interface DestinationAuthorizer {

    boolean soporta(String destino);

    boolean autorizado(StompPrincipal principal, String destino);
}

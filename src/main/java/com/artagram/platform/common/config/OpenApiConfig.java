package com.artagram.platform.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata de Swagger UI / OpenAPI (springdoc la sirve en /swagger-ui/index.html
 * y la spec cruda en /v3/api-docs). El esquema "bearerAuth" agrega el botón
 * "Authorize" en la UI: pegás el JWT que te devuelve /api/auth/login y de ahí
 * en más todos los "Try it out" de endpoints protegidos lo mandan solos.
 *
 * Los canales de tiempo real (STOMP sobre /ws) no quedan documentados acá —
 * OpenAPI 3 describe REST, no WebSocket — están explicados en el README y en
 * cada *StompController con javadoc de qué destino STOMP corresponde a qué
 * historia de usuario.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI artagramOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Artagram API")
                        .description("""
                                Red social colaborativa para artistas visuales: comunidades con lienzo
                                compartido en tiempo real, modo fiesta, feed con proceso de obra, chat y
                                panel de administración.

                                Los endpoints REST están documentados acá. Los canales de tiempo real van
                                por STOMP sobre WebSocket en `/ws` (JWT como `?token=` en la URL de
                                conexión, ya que el navegador no permite headers propios en el handshake) —
                                ver el README del repositorio para el detalle de cada topic/destino.
                                """)
                        .version("v1")
                        .contact(new Contact().name("Artagram")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                                .name(ESQUEMA_JWT)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Pegá acá el accessToken que devuelve POST /api/auth/login")));
    }
}

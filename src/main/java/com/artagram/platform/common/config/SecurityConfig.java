package com.artagram.platform.common.config;

import com.artagram.platform.auth.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private static final int BCRYPT_COST = 12;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_COST);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService, CorsProperties corsProperties)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource(corsProperties)))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/media/**", "/ws/**").permitAll()
                        // Documentación de la API (Swagger UI + spec OpenAPI): solo describe la
                        // forma de los endpoints, no expone datos — se deja pública a propósito,
                        // igual que /actuator/health.
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // /actuator/health y /actuator/prometheus quedan sin JWT a proposito: son
                        // los que scrapea Prometheus / consulta el orquestador, sin sesion de
                        // usuario. En produccion esto se protege con red (no exponerlos al
                        // publico), no con JWT; el resto de /actuator si pide ADMIN.
                        .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/perfiles/**").permitAll()
                        .requestMatchers("/actuator/**", "/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(accessDeniedHandler())
                        .authenticationEntryPoint(authenticationEntryPoint()))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * HU-38: "alguien sin permisos... el intento queda registrado". El
     * rechazo pasa por aqui (no llega al controlador), asi que el log de
     * auditoria tiene que vivir en el manejador de acceso denegado.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        ObjectMapper mapper = new ObjectMapper();
        return (request, response, ex) -> {
            String usuario = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonimo";
            log.warn("Acceso denegado: usuario={} intento entrar a {} {}", usuario, request.getMethod(), request.getRequestURI());
            response.setStatus(org.springframework.http.HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(mapper.writeValueAsString(
                    Map.of("code", "SIN_PERMISO", "message", "No tienes permiso para entrar aqui")));
        };
    }

    /**
     * Sin esto, Spring Security cae en su Http403ForbiddenEntryPoint por
     * defecto para cualquier request sin autenticar (token ausente, invalido o
     * vencido) — el frontend queda sin forma de distinguir "tu sesion expiro"
     * (401, deberia mandarte a /login) de "no tienes permiso para esto" (403,
     * accessDeniedHandler de arriba, con sesion valida pero rol insuficiente).
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        ObjectMapper mapper = new ObjectMapper();
        return (request, response, ex) -> {
            log.debug("Peticion sin autenticar: {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(org.springframework.http.HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(mapper.writeValueAsString(
                    Map.of("code", "NO_AUTENTICADO", "message", "Tu sesion expiro o no has iniciado sesion")));
        };
    }

    private CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("X-Trace-Id"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

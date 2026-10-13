package com.artagram.platform.ia.proveedor;

import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.ia.IaProperties;
import com.artagram.platform.ia.prompt.PromptIa;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@ConditionalOnProperty(name = "artagram.ia.proveedor", havingValue = "groq", matchIfMissing = true)
public class GroqProveedorIa implements ProveedorIa {

    private static final Logger log = LoggerFactory.getLogger(GroqProveedorIa.class);

    private final IaProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GroqProveedorIa(IaProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String getNombreModelo() {
        return properties.getModelo();
    }

    @Override
    public boolean soportaVision() {
        return true;
    }

    @Override
    public String completar(PromptIa prompt) {
        boolean esLocal = properties.getGroqUrl() != null &&
                (properties.getGroqUrl().contains("localhost") || properties.getGroqUrl().contains("127.0.0.1"));
        if (!esLocal && (properties.getGroqApiKey() == null || properties.getGroqApiKey().isBlank())) {
            throw ApiException.badRequest("IA_NO_CONFIGURADA",
                    "La clave API de Groq no está configurada. Define la variable GROQ_API_KEY o configura IA_PROVEEDOR=fake.");
        }

        try {
            Map<String, Object> peticion = new LinkedHashMap<>();
            peticion.put("model", properties.getModelo());
            peticion.put("max_tokens", prompt.getMaxTokens());
            peticion.put("temperature", prompt.getTemperatura());
            if (prompt.isModoJson()) {
                peticion.put("response_format", Map.of("type", "json_object"));
            }

            List<Map<String, Object>> messages = new ArrayList<>();
            if (prompt.getSystemPrompt() != null && !prompt.getSystemPrompt().isBlank()) {
                messages.add(Map.of("role", "system", "content", prompt.getSystemPrompt()));
            }

            if (prompt.getImagenDataUriOUrl() != null && !prompt.getImagenDataUriOUrl().isBlank()) {
                List<Map<String, Object>> contentParts = new ArrayList<>();
                contentParts.add(Map.of("type", "text", "text", prompt.getInstruccionUsuario()));
                contentParts.add(Map.of("type", "image_url", "image_url", Map.of("url", prompt.getImagenDataUriOUrl())));
                messages.add(Map.of("role", "user", "content", contentParts));
            } else {
                messages.add(Map.of("role", "user", "content", prompt.getInstruccionUsuario()));
            }

            peticion.put("messages", messages);
            String bodyJson = objectMapper.writeValueAsString(peticion);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getGroqUrl()))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Artagram/1.0")
                    .timeout(Duration.ofSeconds(properties.getTimeoutSegundos()))
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson));

            if (properties.getGroqApiKey() != null && !properties.getGroqApiKey().isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + properties.getGroqApiKey().trim());
            }

            HttpRequest request = requestBuilder.build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Error respuesta Groq: status={} body={}", response.statusCode(), response.body());
                if (response.statusCode() == 429) {
                    throw ApiException.tooManyRequests("LIMITE_IA_EXCEDIDO", "El proveedor de IA superó el límite de solicitudes por minuto. Intenta en un momento.");
                }
                throw ApiException.badRequest("ERROR_PROVEEDOR_IA", "Error en proveedor de IA: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (choices.isEmpty()) {
                throw ApiException.badRequest("RESPUESTA_IA_VACIA", "La IA no devolvió respuestas");
            }

            String content = choices.get(0).path("message").path("content").asText();
            return limpiarRespuestaJson(content);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Fallo al invocar Groq API", e);
            throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "FALLO_CONEXION_IA", "No se pudo comunicar con el servicio de IA: " + e.getMessage());
        }
    }

    private String limpiarRespuestaJson(String raw) {
        if (raw == null) {
            return "{}";
        }
        String clean = raw.trim();
        if (clean.startsWith("```json")) {
            clean = clean.substring(7);
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3);
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        return clean.trim();
    }
}

package com.artagram.platform.ia.estrategia;

import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.ia.TipoAnalisis;
import com.artagram.platform.ia.prompt.PromptIa;
import com.artagram.platform.ia.proveedor.ProveedorIa;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SugerenciaEtiquetasEstrategia implements EstrategiaAnalisis<SugerenciaEtiquetasResultado> {

    private static final Logger log = LoggerFactory.getLogger(SugerenciaEtiquetasEstrategia.class);
    private final ObjectMapper objectMapper;

    public SugerenciaEtiquetasEstrategia(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public TipoAnalisis getTipo() {
        return TipoAnalisis.SUGERENCIA_ETIQUETAS;
    }

    @Override
    public SugerenciaEtiquetasResultado analizar(String imagenDataUriOUrl, ProveedorIa proveedor) {
        String instruccion = """
                Observa esta ilustración y genera en español un título evocador, una descripción artística corta y 4 a 6 etiquetas relevantes.
                Responde EXCLUSIVAMENTE con un JSON con esta estructura:
                {
                  "tituloSugerido": "Título sugerido...",
                  "descripcionSugerida": "Descripción de la obra...",
                  "etiquetas": ["estilo", "tecnica", "tema"]
                }
                """;

        PromptIa prompt = PromptIa.builder()
                .conSystemPrompt("Eres un curador de arte digital y redactor de portafolios artísticos.")
                .conInstruccion(instruccion)
                .conImagenUrl(imagenDataUriOUrl)
                .conMaxTokens(350)
                .conTemperatura(0.4)
                .conModoJson(true)
                .build();

        String rawJson = proveedor.completar(prompt);
        try {
            return objectMapper.readValue(rawJson, SugerenciaEtiquetasResultado.class);
        } catch (Exception e) {
            log.error("Error deserializando sugerencia de etiquetas de IA: {}", rawJson, e);
            throw ApiException.badRequest("FORMATO_IA_INVALIDO", "La IA no devolvió el formato de sugerencias esperado");
        }
    }

    @Override
    public String aJson(SugerenciaEtiquetasResultado resultado) {
        try {
            return objectMapper.writeValueAsString(resultado);
        } catch (Exception e) {
            throw new IllegalStateException("Error al serializar resultado de etiquetas", e);
        }
    }
}

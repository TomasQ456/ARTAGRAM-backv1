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
public class AnalisisTecnicoEstrategia implements EstrategiaAnalisis<AnalisisTecnicoResultado> {

    private static final Logger log = LoggerFactory.getLogger(AnalisisTecnicoEstrategia.class);
    private final ObjectMapper objectMapper;

    public AnalisisTecnicoEstrategia(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public TipoAnalisis getTipo() {
        return TipoAnalisis.TECNICO;
    }

    @Override
    public AnalisisTecnicoResultado analizar(String imagenDataUriOUrl, ProveedorIa proveedor) {
        String instruccion = """
                Analiza críticamente esta obra visual en español. Evalúa composición, paleta de color, trazo, luz/sombra y perspectiva.
                Responde EXCLUSIVAMENTE con un objeto JSON válido con esta estructura:
                {
                  "puntosFuertes": ["puntos positivos..."],
                  "areasMejora": ["aspectos a mejorar..."],
                  "consejos": ["3 consejos pedagógicos constructivos..."],
                  "scoreEstimado": 85
                }
                """;

        PromptIa prompt = PromptIa.builder()
                .conSystemPrompt("Eres un maestro de bellas artes y crítico de pintura digital enfocado en feedback pedagógico y constructivo.")
                .conInstruccion(instruccion)
                .conImagenUrl(imagenDataUriOUrl)
                .conMaxTokens(450)
                .conTemperatura(0.2)
                .conModoJson(true)
                .build();

        String rawJson = proveedor.completar(prompt);
        try {
            return objectMapper.readValue(rawJson, AnalisisTecnicoResultado.class);
        } catch (Exception e) {
            log.error("Error deserializando análisis técnico de IA: {}", rawJson, e);
            throw ApiException.badRequest("FORMATO_IA_INVALIDO", "La IA no devolvió el formato técnico esperado");
        }
    }

    @Override
    public String aJson(AnalisisTecnicoResultado resultado) {
        try {
            return objectMapper.writeValueAsString(resultado);
        } catch (Exception e) {
            throw new IllegalStateException("Error al serializar resultado técnico", e);
        }
    }
}

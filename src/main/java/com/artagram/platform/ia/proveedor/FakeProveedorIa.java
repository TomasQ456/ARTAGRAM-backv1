package com.artagram.platform.ia.proveedor;

import com.artagram.platform.ia.prompt.PromptIa;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "artagram.ia.proveedor", havingValue = "fake")
public class FakeProveedorIa implements ProveedorIa {

    @Override
    public String getNombreModelo() {
        return "mock-ia-v1";
    }

    @Override
    public boolean soportaVision() {
        return true;
    }

    @Override
    public String completar(PromptIa prompt) {
        if (prompt.getInstruccionUsuario().toLowerCase().contains("etiquetas")
                || prompt.getInstruccionUsuario().toLowerCase().contains("descripcion")) {
            return """
                    {
                      "tituloSugerido": "Amanecer en acuarela",
                      "descripcionSugerida": "Estudio de luces matutinas y texturas orgánicas con pigmentos transparentes.",
                      "etiquetas": ["acuarela", "paisaje", "iluminacion", "arte-digital", "amanecer"]
                    }
                    """;
        }
        return """
                {
                  "puntosFuertes": [
                    "Gran balance cromático en la zona focal",
                    "Pinceladas dinámicas y expresivas",
                    "Composición armoniosa siguiendo la regla de tercios"
                  ],
                  "areasMejora": [
                    "Valores tonales en sombras podrían tener mayor contraste",
                    "Definición de contornos en los planos medios"
                  ],
                  "consejos": [
                    "Prueba intensificar los oscuros para dar mayor profundidad tridimensional.",
                    "Experimenta con pinceles secos para agregar textura en el fondo.",
                    "Cuida la temperatura de color entre las fuentes de luz directa y reflejada."
                  ],
                  "scoreEstimado": 88
                }
                """;
    }
}

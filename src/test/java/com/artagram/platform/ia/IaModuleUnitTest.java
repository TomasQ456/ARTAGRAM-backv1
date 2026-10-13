package com.artagram.platform.ia;

import com.artagram.platform.ia.estrategia.AnalisisTecnicoEstrategia;
import com.artagram.platform.ia.estrategia.AnalisisTecnicoResultado;
import com.artagram.platform.ia.estrategia.SugerenciaEtiquetasEstrategia;
import com.artagram.platform.ia.estrategia.SugerenciaEtiquetasResultado;
import com.artagram.platform.ia.prompt.PromptIa;
import com.artagram.platform.ia.prompt.PromptIaBuilder;
import com.artagram.platform.ia.proveedor.FakeProveedorIa;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IaModuleUnitTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FakeProveedorIa fakeProveedor = new FakeProveedorIa();
    private final AnalisisTecnicoEstrategia estrategiaTecnica = new AnalisisTecnicoEstrategia(objectMapper);
    private final SugerenciaEtiquetasEstrategia estrategiaEtiquetas = new SugerenciaEtiquetasEstrategia(objectMapper);

    @Test
    @DisplayName("PromptIaBuilder construye prompts con imagen y tokens correctos")
    void promptBuilderFunciona() {
        PromptIa prompt = PromptIa.builder()
                .conSystemPrompt("System test")
                .conInstruccion("Analiza este arte")
                .conImagenBytes(new byte[]{1, 2, 3}, "image/png")
                .conMaxTokens(300)
                .conTemperatura(0.5)
                .conModoJson(true)
                .build();

        assertThat(prompt.getSystemPrompt()).isEqualTo("System test");
        assertThat(prompt.getInstruccionUsuario()).isEqualTo("Analiza este arte");
        assertThat(prompt.getImagenDataUriOUrl()).startsWith("data:image/png;base64,");
        assertThat(prompt.getMaxTokens()).isEqualTo(300);
        assertThat(prompt.getTemperatura()).isEqualTo(0.5);
        assertThat(prompt.isModoJson()).isTrue();
    }

    @Test
    @DisplayName("PromptIaBuilder falla si no hay instrucción de usuario")
    void promptBuilderSinInstruccion() {
        assertThatThrownBy(() -> PromptIa.builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no puede estar vacía");
    }

    @Test
    @DisplayName("AnalisisTecnicoEstrategia procesa y deserializa respuesta estructurada")
    void analisisTecnicoEstrategiaFunciona() {
        AnalisisTecnicoResultado resultado = estrategiaTecnica.analizar("data:image/png;base64,AAAA", fakeProveedor);

        assertThat(resultado).isNotNull();
        assertThat(resultado.puntosFuertes()).isNotEmpty();
        assertThat(resultado.areasMejora()).isNotEmpty();
        assertThat(resultado.consejos()).hasSize(3);
        assertThat(resultado.scoreEstimado()).isGreaterThan(0);
    }

    @Test
    @DisplayName("SugerenciaEtiquetasEstrategia procesa y devuelve etiquetas y título")
    void sugerenciaEtiquetasEstrategiaFunciona() {
        SugerenciaEtiquetasResultado resultado = estrategiaEtiquetas.analizar("data:image/png;base64,AAAA", fakeProveedor);

        assertThat(resultado).isNotNull();
        assertThat(resultado.tituloSugerido()).isNotBlank();
        assertThat(resultado.descripcionSugerida()).isNotBlank();
        assertThat(resultado.etiquetas()).isNotEmpty();
    }

    @Test
    @DisplayName("IaProperties resuelve API key y modelo activo por defecto")
    void iaPropertiesResolucion() {
        IaProperties props = new IaProperties();
        props.setModelo("llama-3.2-11b-vision-preview");
        // Debe auto-corregir modelo deprecado a qwen/qwen3.8-27b
        assertThat(props.getModelo()).isEqualTo("qwen/qwen3.8-27b");

        // Con .env presente en el directorio del proyecto, debe resolver la API key automáticamente
        assertThat(props.getGroqApiKey()).isNotBlank();
    }
}

package com.artagram.platform.ia;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "artagram.ia")
public class IaProperties {

    private boolean habilitado = true;
    private String groqApiKey = "";
    private String groqUrl = "https://api.groq.com/openai/v1/chat/completions";
    private String modelo = "qwen/qwen3.8-27b";
    private int timeoutSegundos = 20;

    public boolean isHabilitado() {
        return habilitado;
    }

    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    public String getGroqApiKey() {
        return groqApiKey;
    }

    public void setGroqApiKey(String groqApiKey) {
        this.groqApiKey = groqApiKey;
    }

    public String getGroqUrl() {
        return groqUrl;
    }

    public void setGroqUrl(String groqUrl) {
        this.groqUrl = groqUrl;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public int getTimeoutSegundos() {
        return timeoutSegundos;
    }

    public void setTimeoutSegundos(int timeoutSegundos) {
        this.timeoutSegundos = timeoutSegundos;
    }
}

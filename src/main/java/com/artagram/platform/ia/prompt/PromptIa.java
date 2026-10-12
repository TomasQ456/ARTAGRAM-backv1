package com.artagram.platform.ia.prompt;

public class PromptIa {

    private final String systemPrompt;
    private final String instruccionUsuario;
    private final String imagenDataUriOUrl;
    private final int maxTokens;
    private final double temperatura;
    private final boolean modoJson;

    PromptIa(String systemPrompt, String instruccionUsuario, String imagenDataUriOUrl,
             int maxTokens, double temperatura, boolean modoJson) {
        this.systemPrompt = systemPrompt;
        this.instruccionUsuario = instruccionUsuario;
        this.imagenDataUriOUrl = imagenDataUriOUrl;
        this.maxTokens = maxTokens;
        this.temperatura = temperatura;
        this.modoJson = modoJson;
    }

    public static PromptIaBuilder builder() {
        return new PromptIaBuilder();
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getInstruccionUsuario() {
        return instruccionUsuario;
    }

    public String getImagenDataUriOUrl() {
        return imagenDataUriOUrl;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public double getTemperatura() {
        return temperatura;
    }

    public boolean isModoJson() {
        return modoJson;
    }
}

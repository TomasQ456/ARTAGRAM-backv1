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
        if (groqApiKey != null && !groqApiKey.isBlank()) {
            return groqApiKey;
        }
        String envKey = System.getenv("GROQ_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }
        String sysProp = System.getProperty("GROQ_API_KEY");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        return leerDeArchivoEnv("GROQ_API_KEY");
    }

    public void setGroqApiKey(String groqApiKey) {
        this.groqApiKey = groqApiKey;
    }

    private String leerDeArchivoEnv(String clave) {
        String[] posiblesRutas = {".env", ".env.local", "../.env"};
        for (String ruta : posiblesRutas) {
            try {
                java.nio.file.Path path = java.nio.file.Paths.get(ruta);
                if (java.nio.file.Files.exists(path)) {
                    java.util.List<String> lineas = java.nio.file.Files.readAllLines(path);
                    for (String linea : lineas) {
                        String lineaTrim = linea.trim();
                        if (lineaTrim.startsWith(clave + "=")) {
                            String valor = lineaTrim.substring((clave + "=").length()).trim();
                            if ((valor.startsWith("\"") && valor.endsWith("\""))
                                    || (valor.startsWith("'") && valor.endsWith("'"))) {
                                valor = valor.substring(1, valor.length() - 1);
                            }
                            if (!valor.isBlank()) {
                                return valor;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // Continúa a la siguiente ruta
            }
        }
        return "";
    }

    public String getGroqUrl() {
        return groqUrl;
    }

    public void setGroqUrl(String groqUrl) {
        this.groqUrl = groqUrl;
    }

    public String getModelo() {
        if (modelo != null && !modelo.isBlank() && !"llama-3.2-11b-vision-preview".equals(modelo)) {
            return modelo;
        }
        String envModelo = System.getenv("GROQ_MODELO");
        if (envModelo != null && !envModelo.isBlank()) {
            return envModelo.trim();
        }
        return "qwen/qwen3.8-27b";
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

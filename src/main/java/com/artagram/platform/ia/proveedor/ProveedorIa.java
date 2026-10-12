package com.artagram.platform.ia.proveedor;

import com.artagram.platform.ia.prompt.PromptIa;

/**
 * Strategy para abstracción de proveedores externos de IA (Groq, OpenAI, Fake/Mock).
 */
public interface ProveedorIa {
    String getNombreModelo();
    String completar(PromptIa prompt);
    boolean soportaVision();
}

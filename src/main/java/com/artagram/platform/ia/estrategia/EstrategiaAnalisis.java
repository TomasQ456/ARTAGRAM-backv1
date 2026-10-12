package com.artagram.platform.ia.estrategia;

import com.artagram.platform.ia.TipoAnalisis;
import com.artagram.platform.ia.proveedor.ProveedorIa;

/**
 * Strategy para diferentes modalidades de análisis de arte (técnico, etiquetas, estilo).
 */
public interface EstrategiaAnalisis<T> {

    TipoAnalisis getTipo();

    T analizar(String imagenDataUriOUrl, ProveedorIa proveedor);

    String aJson(T resultado);
}

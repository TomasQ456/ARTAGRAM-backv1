package com.artagram.platform.observability;

import java.util.UUID;
import org.springframework.stereotype.Component;

/** Punto unico para dejar los eventos minimos que alimentan los KPIs de HU-37. */
@Component
public class EventoNegocioRecorder {

    private final EventoNegocioRepository repository;

    public EventoNegocioRecorder(EventoNegocioRepository repository) {
        this.repository = repository;
    }

    public void registro(UUID usuarioId) {
        repository.save(new EventoNegocio(TipoEventoNegocio.REGISTRO, usuarioId, null));
    }

    public void login(UUID usuarioId) {
        repository.save(new EventoNegocio(TipoEventoNegocio.LOGIN, usuarioId, null));
    }

    public void entroColaborativo(UUID usuarioId) {
        repository.save(new EventoNegocio(TipoEventoNegocio.ENTRO_COLABORATIVO, usuarioId, null));
    }

    public void salioColaborativo(UUID usuarioId, long duracionMs) {
        repository.save(new EventoNegocio(TipoEventoNegocio.SALIO_COLABORATIVO, usuarioId, duracionMs));
    }
}

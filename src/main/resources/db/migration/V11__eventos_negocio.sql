CREATE TABLE eventos_negocio (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo            VARCHAR(30) NOT NULL,
    usuario_id      UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    duracion_ms     BIGINT,
    ocurrido_en     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_eventos_tipo_fecha ON eventos_negocio(tipo, ocurrido_en);
CREATE INDEX idx_eventos_usuario_tipo_fecha ON eventos_negocio(usuario_id, tipo, ocurrido_en);

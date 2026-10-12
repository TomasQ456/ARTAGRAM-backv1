CREATE TABLE analisis_obra (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    publicacion_id  UUID NOT NULL REFERENCES publicaciones(id) ON DELETE CASCADE,
    solicitante_id  UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    tipo            VARCHAR(32) NOT NULL,
    resultado_json  TEXT NOT NULL,
    modelo          VARCHAR(64) NOT NULL,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_analisis_obra_publicacion ON analisis_obra(publicacion_id, creado_en DESC);
CREATE INDEX idx_analisis_obra_solicitante ON analisis_obra(solicitante_id);

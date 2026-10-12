CREATE TABLE comentarios (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    publicacion_id  UUID NOT NULL REFERENCES publicaciones(id) ON DELETE CASCADE,
    autor_id        UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    contenido       VARCHAR(500) NOT NULL,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_comentarios_publicacion ON comentarios(publicacion_id, creado_en ASC, id ASC);
CREATE INDEX idx_comentarios_autor ON comentarios(autor_id);

ALTER TABLE publicaciones ADD COLUMN contador_comentarios INT NOT NULL DEFAULT 0;

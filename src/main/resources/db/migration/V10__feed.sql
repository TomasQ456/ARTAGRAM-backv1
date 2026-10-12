CREATE TABLE seguidores (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seguidor_id     UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    seguido_id      UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (seguidor_id, seguido_id)
);

CREATE INDEX idx_seguidores_seguidor ON seguidores(seguidor_id);
CREATE INDEX idx_seguidores_seguido ON seguidores(seguido_id);

CREATE TABLE publicaciones (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    autor_id        UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    titulo          VARCHAR(120) NOT NULL,
    descripcion     VARCHAR(2000) NOT NULL DEFAULT '',
    obra_final_ruta VARCHAR(500) NOT NULL,
    contador_likes  INT NOT NULL DEFAULT 0,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_publicaciones_autor_fecha ON publicaciones(autor_id, creado_en DESC, id DESC);
CREATE INDEX idx_publicaciones_fecha ON publicaciones(creado_en DESC, id DESC);

CREATE TABLE publicacion_etiquetas (
    publicacion_id  UUID NOT NULL REFERENCES publicaciones(id) ON DELETE CASCADE,
    etiqueta        VARCHAR(40) NOT NULL
);

CREATE INDEX idx_publicacion_etiquetas ON publicacion_etiquetas(publicacion_id);

CREATE TABLE imagenes_proceso (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    publicacion_id  UUID NOT NULL REFERENCES publicaciones(id) ON DELETE CASCADE,
    ruta_imagen     VARCHAR(500) NOT NULL,
    orden           INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_imagenes_proceso_publicacion ON imagenes_proceso(publicacion_id, orden);

CREATE TABLE reacciones (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id      UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    publicacion_id  UUID NOT NULL REFERENCES publicaciones(id) ON DELETE CASCADE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (usuario_id, publicacion_id)
);

CREATE INDEX idx_reacciones_publicacion ON reacciones(publicacion_id);

CREATE TABLE perfiles (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id         UUID NOT NULL UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,
    biografia          VARCHAR(500) NOT NULL DEFAULT '',
    foto_url           VARCHAR(500),
    creado_en          TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE perfil_enlaces (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    perfil_id   UUID NOT NULL REFERENCES perfiles(id) ON DELETE CASCADE,
    etiqueta    VARCHAR(50) NOT NULL,
    url         VARCHAR(300) NOT NULL,
    orden       INT NOT NULL DEFAULT 0
);

CREATE TABLE perfil_obras_destacadas (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    perfil_id   UUID NOT NULL REFERENCES perfiles(id) ON DELETE CASCADE,
    titulo      VARCHAR(120) NOT NULL,
    imagen_url  VARCHAR(500) NOT NULL,
    orden       INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_perfil_enlaces_perfil ON perfil_enlaces(perfil_id);
CREATE INDEX idx_perfil_obras_perfil ON perfil_obras_destacadas(perfil_id);

CREATE TABLE lienzos (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comunidad_id        UUID NOT NULL UNIQUE REFERENCES comunidades(id) ON DELETE CASCADE,
    contador_secuencia  BIGINT NOT NULL DEFAULT 0,
    creado_en           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE banners_comunitarios (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comunidad_id        UUID NOT NULL UNIQUE REFERENCES comunidades(id) ON DELETE CASCADE,
    tamano              INT NOT NULL DEFAULT 1000,
    tamano_region       INT NOT NULL DEFAULT 100,
    creado_en           TIMESTAMPTZ NOT NULL DEFAULT now()
);

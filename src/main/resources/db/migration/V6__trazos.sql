CREATE TABLE trazos (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lienzo_id       UUID NOT NULL REFERENCES lienzos(id) ON DELETE CASCADE,
    autor_id        UUID NOT NULL REFERENCES usuarios(id),
    secuencia       BIGINT NOT NULL,
    tipo_pincel     VARCHAR(20) NOT NULL,
    color_hex       VARCHAR(9) NOT NULL,
    grosor          DOUBLE PRECISION NOT NULL,
    opacidad        DOUBLE PRECISION NOT NULL,
    capa            INT NOT NULL DEFAULT 0,
    puntos          TEXT NOT NULL,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (lienzo_id, secuencia)
);

CREATE INDEX idx_trazos_lienzo_secuencia ON trazos(lienzo_id, secuencia);

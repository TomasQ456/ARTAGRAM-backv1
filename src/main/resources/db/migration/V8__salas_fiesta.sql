CREATE TABLE salas_fiesta (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo                  VARCHAR(8) NOT NULL UNIQUE,
    anfitrion_id            UUID NOT NULL REFERENCES usuarios(id),
    duracion_ronda_segundos INT NOT NULL,
    cupo_maximo             INT NOT NULL,
    estado                  VARCHAR(20) NOT NULL DEFAULT 'ESPERANDO',
    numero_ronda            INT NOT NULL DEFAULT 0,
    pincel_actual           VARCHAR(20),
    color_actual            VARCHAR(9),
    grosor_actual           DOUBLE PRECISION,
    opacidad_actual         DOUBLE PRECISION,
    creado_en               TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE participantes_sala (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sala_id     UUID NOT NULL REFERENCES salas_fiesta(id) ON DELETE CASCADE,
    usuario_id  UUID NOT NULL REFERENCES usuarios(id),
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (sala_id, usuario_id)
);

CREATE TABLE party_ronda_estado (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sala_id             UUID NOT NULL REFERENCES salas_fiesta(id) ON DELETE CASCADE,
    numero_ronda        INT NOT NULL,
    contador_secuencia  BIGINT NOT NULL DEFAULT 0,
    UNIQUE (sala_id, numero_ronda)
);

CREATE TABLE party_trazos (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sala_id         UUID NOT NULL REFERENCES salas_fiesta(id) ON DELETE CASCADE,
    numero_ronda    INT NOT NULL,
    secuencia       BIGINT NOT NULL,
    autor_id        UUID NOT NULL REFERENCES usuarios(id),
    borrador        BOOLEAN NOT NULL DEFAULT FALSE,
    tipo_pincel     VARCHAR(20),
    color_hex       VARCHAR(9),
    grosor          DOUBLE PRECISION NOT NULL,
    opacidad        DOUBLE PRECISION NOT NULL,
    puntos          TEXT NOT NULL,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (sala_id, numero_ronda, secuencia)
);

CREATE INDEX idx_party_trazos_ronda ON party_trazos(sala_id, numero_ronda, secuencia);
CREATE INDEX idx_participantes_sala_usuario ON participantes_sala(usuario_id);

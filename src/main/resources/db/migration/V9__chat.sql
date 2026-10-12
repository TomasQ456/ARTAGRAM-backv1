CREATE SEQUENCE mensajes_orden_seq;

CREATE TABLE mensajes_personales (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    orden               BIGINT NOT NULL DEFAULT nextval('mensajes_orden_seq'),
    remitente_id        UUID NOT NULL REFERENCES usuarios(id),
    destinatario_id     UUID NOT NULL REFERENCES usuarios(id),
    contenido           VARCHAR(2000) NOT NULL,
    entregado           BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_mp_remitente ON mensajes_personales(remitente_id, orden);
CREATE INDEX idx_mp_destinatario ON mensajes_personales(destinatario_id, orden);

CREATE TABLE mensajes_comunidad (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    orden           BIGINT NOT NULL DEFAULT nextval('mensajes_orden_seq'),
    comunidad_id    UUID NOT NULL REFERENCES comunidades(id) ON DELETE CASCADE,
    remitente_id    UUID NOT NULL REFERENCES usuarios(id),
    contenido       VARCHAR(2000) NOT NULL,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_mc_comunidad ON mensajes_comunidad(comunidad_id, orden);

CREATE TABLE uso_cuenta (
    usuario_id            UUID PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
    comunidades_creadas   INT NOT NULL DEFAULT 0,
    salas_fiesta_activas  INT NOT NULL DEFAULT 0,
    actualizado_en        TIMESTAMPTZ NOT NULL DEFAULT now()
);

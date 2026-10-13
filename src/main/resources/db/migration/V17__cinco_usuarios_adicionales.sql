-- V17__cinco_usuarios_adicionales.sql
-- Creación de 5 usuarios de ejemplo adicionales con contraseña 'clavesegura123' y verificación activa

INSERT INTO usuarios (id, correo, nombre_artista, password_hash, verificado, rol_global, premium, creado_en, actualizado_en)
VALUES
  ('a1b2c3d4-0001-4000-8000-000000000001', 'camila.oleo@correo.com', 'camila_oleo', '$2a$10$S9rlsaNry9vtuKjJlDWXY.Mi0ge1JFY9i.m6LwbYFRoJGxoi9VDie', TRUE, 'USUARIO', FALSE, now(), now()),
  ('a1b2c3d4-0002-4000-8000-000000000002', 'mateo.3d@correo.com', 'mateo_3d', '$2a$10$S9rlsaNry9vtuKjJlDWXY.Mi0ge1JFY9i.m6LwbYFRoJGxoi9VDie', TRUE, 'USUARIO', FALSE, now(), now()),
  ('a1b2c3d4-0003-4000-8000-000000000003', 'valeria.botanica@correo.com', 'valeria_botanica', '$2a$10$S9rlsaNry9vtuKjJlDWXY.Mi0ge1JFY9i.m6LwbYFRoJGxoi9VDie', TRUE, 'USUARIO', FALSE, now(), now()),
  ('a1b2c3d4-0004-4000-8000-000000000004', 'andres.concept@correo.com', 'andres_concept', '$2a$10$S9rlsaNry9vtuKjJlDWXY.Mi0ge1JFY9i.m6LwbYFRoJGxoi9VDie', TRUE, 'USUARIO', FALSE, now(), now()),
  ('a1b2c3d4-0005-4000-8000-000000000005', 'elena.collage@correo.com', 'elena_collage', '$2a$10$S9rlsaNry9vtuKjJlDWXY.Mi0ge1JFY9i.m6LwbYFRoJGxoi9VDie', TRUE, 'USUARIO', FALSE, now(), now())
ON CONFLICT (correo) DO NOTHING;

INSERT INTO perfiles (id, usuario_id, biografia, foto_url, creado_en, actualizado_en)
VALUES
  (gen_random_uuid(), 'a1b2c3d4-0001-4000-8000-000000000001', 'Pintora al óleo sobre lienzo. Retratos figurativos y texturas empastadas. 🎨', NULL, now(), now()),
  (gen_random_uuid(), 'a1b2c3d4-0002-4000-8000-000000000002', 'Modelado orgánico, renders conceptuales y escultura digital. ✨', NULL, now(), now()),
  (gen_random_uuid(), 'a1b2c3d4-0003-4000-8000-000000000003', 'Ilustración científica y botánica en acuarela y gouache. Flores de los Andes. 🌸', NULL, now(), now()),
  (gen_random_uuid(), 'a1b2c3d4-0004-4000-8000-000000000004', 'Concept artist para animación y videojuegos. Creación de mundos sci-fi. 🚀', NULL, now(), now()),
  (gen_random_uuid(), 'a1b2c3d4-0005-4000-8000-000000000005', 'Técnicas mixtas, collage análogo y fanzines independientes. Experimentación visual. ✂️', NULL, now(), now())
ON CONFLICT (usuario_id) DO NOTHING;

INSERT INTO uso_cuenta (usuario_id, comunidades_creadas, salas_fiesta_activas, actualizado_en)
VALUES
  ('a1b2c3d4-0001-4000-8000-000000000001', 0, 0, now()),
  ('a1b2c3d4-0002-4000-8000-000000000002', 0, 0, now()),
  ('a1b2c3d4-0003-4000-8000-000000000003', 0, 0, now()),
  ('a1b2c3d4-0004-4000-8000-000000000004', 0, 0, now()),
  ('a1b2c3d4-0005-4000-8000-000000000005', 0, 0, now())
ON CONFLICT (usuario_id) DO NOTHING;

-- Unir los nuevos usuarios a las comunidades existentes para facilitar pruebas
INSERT INTO membresias_comunidad (id, comunidad_id, usuario_id, rol, activo, creado_en, actualizado_en)
SELECT gen_random_uuid(), c.id, u.id, 'MIEMBRO', TRUE, now(), now()
FROM comunidades c
CROSS JOIN usuarios u
WHERE u.correo IN (
  'camila.oleo@correo.com',
  'mateo.3d@correo.com',
  'valeria.botanica@correo.com',
  'andres.concept@correo.com',
  'elena.collage@correo.com'
)
ON CONFLICT (comunidad_id, usuario_id) DO NOTHING;

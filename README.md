# Artagram — Backend

API REST + WebSocket (STOMP) del backend de **Artagram**, una red social colaborativa
para artistas visuales: publicación de obras con su proceso, comunidades con un lienzo
compartido en tiempo real, banner mural comunitario, "modo fiesta" (rondas cronometradas
de dibujo colectivo) y chat. Este repo es 100% Java — sin scripts Node/Python en ningún
punto del árbol de fuentes, ni siquiera para poblar datos de ejemplo (ver [Datos de
demostración](#datos-de-demostración)).

Este repo se puede levantar solo (con Postgres y Redis) para desarrollar o probar el
backend de forma aislada. Para correr el sistema completo (con el frontend), ver el
repo [`ARTAGRAM-Infraestructure`](https://github.com/Cam1lo27/ARTAGRAM-Infraestructure).

## Tecnologías

| Categoría | Tecnología |
|---|---|
| Lenguaje / plataforma | Java 21, Maven |
| Framework | Spring Boot 3.3.4 (Web MVC, WebSocket/STOMP, Data JPA, Security, Validation, Actuator, Mail, Cache) |
| Base de datos | PostgreSQL 16 + Flyway (migraciones versionadas) |
| Coordinación distribuida | Redis 7 + Redisson (locks, rate limiter, Pub/Sub para tiempo real multi-instancia) |
| Autenticación | JWT (io.jsonwebtoken / jjwt), BCrypt (costo 12) |
| Documentación de API | springdoc-openapi (Swagger UI + OpenAPI 3) |
| Observabilidad | Micrometer + Prometheus, Logback + `logstash-logback-encoder` (logs JSON) |
| Detección de tipo de archivo | Apache Tika (magic bytes, no solo extensión) |
| Almacenamiento de archivos | Disco local (dev) o Azure Blob Storage (producción), intercambiable por configuración |
| Testing | JUnit 5, Mockito, Testcontainers (Postgres + Redis reales), cliente STOMP real para pruebas de tiempo real |
| Calidad de código | JaCoCo (cobertura), SpotBugs (análisis estático) |
| Contenedores | Docker multi-stage build (`maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre-alpine`) |

## Arquitectura

### Organización por dominio (no por capa técnica)

```
src/main/java/com/artagram/platform/
  auth/            registro, verificación de correo, login, JWT
  profile/         perfil público del artista, almacenamiento de archivos con URL firmada
  community/       comunidades, membresías, roles, autorización de canales STOMP por comunidad
  canvas/          lienzo colaborativo, catálogo de pinceles, bloqueo de trazo, banner mural por regiones
  party/           modo fiesta: salas, rondas cronometradas, borrador como recurso disputado
  chat/            chat personal y de comunidad
  feed/            publicaciones (obra + proceso), feed paginado, likes atómicos, seguidores
  billing/         cuotas del plan gratuito (comunidades y salas de fiesta simultáneas)
  observability/   KPIs de negocio, dashboard de administración
  realtime/        infraestructura transversal de STOMP (autorización de canal, presencia, métricas de sesión)
  common/          manejo de errores, configuración de seguridad, tracing, locks distribuidos genéricos
  seed/            DemoDataSeeder (Java puro) — puebla la base con datos de ejemplo, ver más abajo
```

Cada paquete de dominio trae su propio controlador, servicio, repositorio, entidades y
DTOs — no hay una capa `service`/`repository` transversal a todo el proyecto. `common` y
`realtime` son la única infraestructura compartida entre dominios.

### Decisiones de arquitectura clave

- **Postgres es el estado autoritativo** de todo: usuarios, comunidades, trazos
  append-only, salas de fiesta, cuotas, eventos de negocio. Redis solo coordina
  concurrencia entre peticiones/instancias — nunca es la única copia de un dato.
- **Locks distribuidos propios** (`DistributedLockService`, compare-and-set con
  `SET NX PX` + Lua para renovar/liberar) en vez de `Redisson RLock`: `RLock` está
  pensado para reentrancia por hilo de la JVM, no para un flujo adquirir → renovar →
  liberar que cruza varias peticiones HTTP/STOMP independientes de un mismo usuario.
  Se usa para el bloqueo de edición de un trazo (TTL 30s, con heartbeat) y para la
  exclusión mutua del borrador del modo fiesta (TTL 10s, `tryLock` no bloqueante: el
  primer `intento` gana, el resto falla al instante).
- **`RRateLimiter`** (Redisson) limita a 100 operaciones/segundo por usuario sobre el
  banner mural, para que un cliente con un script no ahogue al resto.
- **Contadores atómicos, nunca leer-sumar-escribir**: los likes se actualizan con un
  `UPDATE` JPQL en bloque (`@Modifying(clearAutomatically = true)`); 200 usuarios dando
  like a la vez dejan el contador exacto en 200 (ver `LikeConcurrencyIT`).
- **Secuencia de trazos determinista**: cada lienzo, cada región del banner y cada sala
  de fiesta tiene una fila contadora con `SELECT ... FOR UPDATE`, así el orden de los
  trazos queda consistente sin depender de un segundo sistema (ni de reloj de pared).
- **JWT compartido entre REST y STOMP**: mismo token, misma llave. En el *handshake* de
  WebSocket viaja como `?token=` en la URL (un navegador no puede mandar headers propios
  en el handshake nativo) y se valida antes de aceptar la conexión; el `Principal`
  resultante persiste durante toda la sesión STOMP.
- **Autorización por canal, no solo al conectar**: un `ChannelInterceptor` revisa cada
  `SUBSCRIBE` y cada mensaje entrante contra la membresía real en Postgres/Redis — quien
  ya no es miembro de una comunidad (o fue expulsado) deja de recibir y de poder escribir
  de inmediato, no solo en la próxima reconexión.
- **`AuthenticationEntryPoint` explícito**: sin él, Spring Security devuelve 403 para
  *cualquier* petición sin autenticar (token ausente, inválido o vencido), lo cual no le
  permite al frontend distinguir "tu sesión expiró" (debería mandarte a `/login`) de
  "no tenés permiso para esto" (sesión válida, rol insuficiente). Ahora el primer caso
  responde 401 `NO_AUTENTICADO` y el segundo 403 `SIN_PERMISO`.
- **Tiempo real multi-instancia con `RealtimeBroadcaster`**: el broker STOMP sigue siendo
  un `SimpleBroker` (solo entrega a las sesiones conectadas a esa misma instancia), pero
  ningún componente de dominio lo usa directo — todos pasan por `RealtimeBroadcaster`
  (paquete `realtime`), que publica cada mensaje en un `RTopic` de Redis y cada instancia
  (incluida la que lo originó) lo recibe y hace la entrega local real. Así, correr el
  backend detrás de un load balancer con más de una instancia ya no rompe el lienzo
  colaborativo, el banner, el modo fiesta ni el chat — se eligió Redis Pub/Sub en vez de
  un *broker relay* dedicado (RabbitMQ/ActiveMQ, la forma "de libro" vía
  `enableStompBrokerRelay`) porque reusa el Redis que el proyecto ya tiene para locks y
  rate-limiting, sin agregar infraestructura ni costo nuevo.
- **Almacenamiento con `StorageProvider`**: `StorageService` (validación de tamaño/tipo
  con Tika, firma HMAC de las URLs) es agnóstico de dónde viven los bytes — delega en un
  `StorageProvider` intercambiable por configuración (`app.storage.provider`, sin tocar
  código): `DiskStorageProvider` (disco local, el default para dev/Docker Compose) o
  `AzureBlobStorageProvider` (Azure Blob Storage, para producción — ver
  [Despliegue en Azure](#despliegue-en-azure)). Antes de esta abstracción, los archivos
  subidos solo vivían en disco, lo cual no sobrevive un redeploy/reinicio en un entorno
  con contenedores efímeros ni es visible entre instancias.
- **Observabilidad de punta a punta**: un `Filter` HTTP + un `ChannelInterceptor` STOMP
  generan/propagan un `traceId` vía MDC en cada log; Logback lo serializa como JSON
  (`logstash-logback-encoder`); Micrometer expone métricas técnicas en
  `/actuator/prometheus`; una tabla de eventos de negocio (`EventoNegocio`) alimenta 4
  KPIs calculados bajo demanda con fórmula, período y tendencia.

## Cómo correr el backend solo

Necesita Postgres y Redis corriendo (local o vía Docker). Forma más simple, desde este
repo:

```bash
docker run -d --name artagram-postgres -p 5432:5432 \
  -e POSTGRES_DB=artagram -e POSTGRES_USER=artagram -e POSTGRES_PASSWORD=artagram \
  postgres:16-alpine

docker run -d --name artagram-redis -p 6379:6379 redis:7-alpine

mvn spring-boot:run
```

Por defecto arranca en `http://localhost:8080` con la configuración de
`src/main/resources/application.yml` (los defaults ya apuntan a `localhost:5432` /
`localhost:6379` con las credenciales de arriba). Variables de entorno relevantes:

| Variable | Uso | Default |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | conexión a Postgres | `localhost` / `5432` / `artagram` / `artagram` / `artagram` |
| `REDIS_HOST` / `REDIS_PORT` | conexión a Redis | `localhost` / `6379` |
| `JWT_SECRET` | llave HMAC para firmar los JWT (REST + STOMP) | secreto de desarrollo — **cambiar en producción** |
| `MEDIA_SIGNING_SECRET` | HMAC de las URLs firmadas de `/media/**` | secreto de desarrollo |
| `CORS_ORIGINS` | orígenes permitidos | `http://localhost:5173` |
| `APP_BASE_URL` | base para el enlace de verificación de correo | `http://localhost:5173` |
| `APP_API_BASE_URL` | base pública de este backend (debe coincidir con el puerto real expuesto, si no las URLs firmadas de imágenes quedan mal) | `http://localhost:8080` |
| `STORAGE_ROOT` | carpeta donde se guardan los archivos subidos | `./data/uploads` |
| `EMAIL_MODE` | `log` (loguea el enlace de verificación, sin SMTP real) o `smtp` | `log` |
| `SPRING_PROFILES_ACTIVE` | agregar `seed` para poblar datos de ejemplo al arrancar | (vacío) |

### Documentación interactiva de la API

Con el backend corriendo: **Swagger UI en `http://localhost:8080/swagger-ui.html`** —
todos los endpoints REST agrupados por módulo (Autenticación, Perfiles, Comunidades,
Lienzo colaborativo, Banner mural, Modo fiesta, Chat, Feed, Administración, Archivos),
con botón "Authorize" para pegar el JWT y probar en vivo. Los canales de tiempo real
(STOMP sobre `/ws`) no quedan documentados ahí porque no son REST — ver la tabla de
destinos STOMP más abajo.

Especificación cruda en `http://localhost:8080/v3/api-docs`.

### Datos de demostración

`DemoDataSeeder` (`com.artagram.platform.seed`, Java puro — sin scripts externos) crea
6 usuarios + 1 admin, 3 comunidades, 6 publicaciones con imágenes de ejemplo (empaquetadas
en `src/main/resources/seed-images/`), likes, seguidores, mensajes de chat y banners ya
pintados. Es idempotente (si ya corrió antes, no duplica nada) y solo se activa con el
profile `seed`:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=seed
```

El usuario `prueba.artagram@correo.com` (contraseña `clavesegura123`, igual que el resto
de las cuentas sembradas) queda promovido a rol `ADMIN`.

## Pruebas

```bash
mvn test      # unitarias (JUnit 5 + Mockito) — no necesitan Docker
mvn verify    # + integración: Testcontainers levanta Postgres y Redis reales,
              # y las pruebas de tiempo real usan un cliente STOMP real (StompTestClients),
              # nunca un mock de WebSocket ni de Redis
```

**59 pruebas** (10 unitarias + 49 de integración), 0 fallos, repartidas en 21 clases —
desde las de dominio "normal" (registro, perfil, feed, eliminar una publicación propia o
intentar eliminar la de otro) hasta las de concurrencia real (200 likes simultáneos, 6
usuarios peleando un lock, 30 pintando el banner a la vez), las de tiempo real con
cliente STOMP real (lienzo colaborativo, modo fiesta, chat — las mismas que validan que
`RealtimeBroadcaster` reparte todo correctamente vía Redis) y la de `DiskStorageProvider`
(guardar → existe → leer, byte a byte).

- **Cobertura**: **81% de líneas** (JaCoCo), por encima del 70% acordado en el DoD.
  Reporte navegable en `target/site/jacoco/index.html` tras `mvn verify`. Incluye
  cobertura del propio `DemoDataSeeder` (`DemoDataSeederIT`, arranca el contexto con el
  profile `seed` real y verifica también que correrlo dos veces no duplica nada) y de los
  manejadores de excepción de framework (`GlobalExceptionHandlerIT`: ruta inexistente →
  404, verbo no soportado → 405, JSON mal formado → 400, sin token → 401).
  `AzureBlobStorageProvider` no tiene prueba automatizada (no hay cuenta de Azure real en
  este entorno) — revisado a mano, igual que `SmtpEmailService`.
- **Análisis estático**: `mvn spotbugs:check` (o dentro de `mvn verify`), `effort=Max`,
  `threshold=Medium` — **4 hallazgos, todos Medium, ninguno High**, revisados uno por uno
  (3 falsos positivos de null-check sobre valores que el propio contrato de la API
  garantiza no-null; 1 `CT_CONSTRUCTOR_THROW` genérico de bajo riesgo real). El filtro
  `spotbugs-exclude.xml` documenta por qué se suprime `EI_EXPOSE_REP`/`EI_EXPOSE_REP2`
  (idiomático en Spring/JPA con inyección de dependencias y entidades relacionadas — sin
  excluirlo, 116 de 120 hallazgos serían ruido de ese único patrón).
- Trazabilidad completa historia de usuario → escenario Gherkin → prueba automatizada:
  ver `docs/gherkin-coverage.md` en el repo
  [`ARTAGRAM-Infraestructure`](https://github.com/Cam1lo27/ARTAGRAM-Infraestructure).

## Logging

Logs en JSON (Logback + `logstash-logback-encoder`) con un `traceId` por request
propagado vía MDC, tanto en HTTP (`TraceIdFilter`) como en STOMP (`StompTraceInterceptor`)
— un mismo `traceId` permite seguir una operación de punta a punta en los logs aunque
cruce varios servicios/hilos. Niveles usados con criterio: `DEBUG` para eventos de alta
frecuencia (like individual, trazo finalizado, intento de lock), `INFO` para hechos de
negocio significativos (comunidad creada, ronda iniciada, publicación creada), `WARN`
para intentos de acceso denegado o locks no renovados, `ERROR` con stack trace completo
solo para fallos reales de infraestructura (ej. no se pudo escribir un archivo).

## CI/CD

`.github/workflows/docker-publish.yml`: en cada push a `main`/`master`, compila, corre
`mvn verify` (con Testcontainers) y publica la imagen Docker en
`ghcr.io/<owner>/artagram-backend:latest` usando el `GITHUB_TOKEN` que inyecta GitHub
Actions automáticamente (no requiere secrets adicionales).

## Despliegue en Azure

La imagen ya se publica sola en `ghcr.io` con cada push (ver CI/CD arriba), así que la
forma más directa es **Azure App Service para contenedores (Linux)** apuntando
directo a esa imagen — sin build ni registry propios de Azure.

### 1. Servicios administrados que hacen falta

- **Azure Database for PostgreSQL — Flexible Server** (el estado autoritativo).
- **Azure Cache for Redis** (coordinación de locks/rate-limiter — no guarda datos de
  negocio, así que el tier más económico alcanza).
- **App Service Plan** (Linux) + **Web App** configurada para contenedor Docker.

### 2. Configurar la Web App

En **Configuration → General settings**:

| Ajuste | Valor |
|---|---|
| Stack | Docker Container |
| Imagen | `ghcr.io/<tu-usuario>/artagram-backend:latest` (público) o con credenciales si el paquete quedó privado — ver [el README de infra](https://github.com/Cam1lo27/ARTAGRAM-Infraestructure#c%C3%B3mo-est%C3%A1n-conectados-los-3-repositorios) para hacerlo público |
| **Web sockets** | **On** — apagado por defecto en Azure; sin esto, el modo fiesta, el lienzo colaborativo y el chat en vivo no funcionan (STOMP corre sobre WebSocket) |
| **Always On** | On (requiere tier Basic o superior) — sin esto, Azure "duerme" la app en free tier y el `@Scheduled` que manda el reloj del modo fiesta cada 500ms se detiene entre peticiones |
| Health check path | `/actuator/health` |

En **Configuration → Application settings** (variables de entorno), agregar:

| Variable | Valor |
|---|---|
| `WEBSITES_PORT` | `8080` (el puerto que expone el contenedor — Azure espera 80 si no se le dice otra cosa) |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | los del Flexible Server de Postgres |
| `DB_SSLMODE` | `require` — **obligatorio**: Azure Database for PostgreSQL exige TLS y por defecto la app se conecta con `sslmode=disable` (pensado para Postgres local/Docker) |
| `REDIS_HOST` | el host del Azure Cache for Redis |
| `REDIS_PORT` | `6380` (puerto TLS de Azure Cache, no el `6379` sin cifrar) |
| `SPRING_DATA_REDIS_PASSWORD` | la *access key* del Azure Cache — **usar exactamente este nombre** (la propiedad nativa de Spring Boot, no una variable propia): así no hace falta tocar `application.yml` para pasarla |
| `SPRING_DATA_REDIS_SSL_ENABLED` | `true` |
| `JWT_SECRET` / `MEDIA_SIGNING_SECRET` | secretos nuevos y reales — **nunca** los de desarrollo del `application.yml` |
| `CORS_ORIGINS` | la URL de producción del frontend en Vercel, ej. `https://artagram.vercel.app` |
| `APP_BASE_URL` | la misma URL de Vercel (se usa para armar el link de verificación de correo) |
| `APP_API_BASE_URL` | la URL pública de esta misma Web App, ej. `https://artagram-backend.azurewebsites.net` — si queda mal, las URLs firmadas de imágenes apuntan al lugar equivocado |
| `EMAIL_MODE` | `log` para la demo (el link de verificación queda en los logs de la Web App, en *Log stream*), o `smtp` con `SMTP_HOST`/`SMTP_PORT`/`SMTP_USER`/`SMTP_PASSWORD` reales |
| `SPRING_PROFILES_ACTIVE` | opcional: `seed` en el primer arranque para poblar datos de ejemplo (ver [Datos de demostración](#datos-de-demostración)) — quitarlo después, no hace falta dejarlo activo |
| `STORAGE_PROVIDER` | `blob` — **importante**: sin esto, los archivos subidos (obras, fotos de perfil) se guardan en el disco del contenedor, que en Azure App Service no es persistente entre reinicios ni visible entre instancias si se escala horizontalmente |
| `AZURE_STORAGE_CONNECTION_STRING` | connection string de la cuenta de Storage (Azure Portal → tu cuenta de Storage → *Access keys*) |
| `AZURE_STORAGE_CONTAINER` | nombre del contenedor de blobs (se crea solo en el primer arranque si no existe) — default `artagram-uploads` |

### 3. Migraciones y arranque

Flyway corre las migraciones automáticamente contra la base de Azure la primera vez que
arranca el contenedor — no hace falta correr nada a mano.

### 4. Verificar el despliegue

`https://<tu-web-app>.azurewebsites.net/actuator/health` debería responder
`{"status":"UP"}`, y `https://<tu-web-app>.azurewebsites.net/swagger-ui.html` debería
cargar la documentación de la API.

# Guía de Despliegue en AWS EC2 con GitHub Actions

Esta guía explica paso a paso cómo desplegar el backend de **Artagram** (junto con PostgreSQL 16 y Redis 7) en una instancia **AWS EC2** utilizando integración y entrega continua (CI/CD) con **GitHub Actions**.

---

## 1. Crear la Instancia en AWS (Consola Web)

1. Inicia sesión en la consola de AWS y ve al servicio **EC2**.
2. Haz clic en **Launch Instance** (Lanzar instancia):
   - **Name**: `artagram-backend`
   - **OS / AMI**: **Ubuntu Server 24.04 LTS** (o 22.04 LTS), arquitectura `x86_64`.
   - **Instance type**:
     - Recomendado: `t3.small` (2 vCPU, 2 GB RAM — ideal para Java 21, Postgres y Redis).
     - Si usas Free Tier: `t2.micro` (1 GB RAM — el script de setup añade 2 GB de Swap para evitar falta de memoria).
   - **Key pair (login)**:
     - Crea un par de claves nuevo o selecciona uno existente (ej. `artagram-key`).
     - Tipo: `RSA`, Formato: `.pem`.
     - **Descarga y guarda el archivo `.pem`** (lo necesitarás para SSH y GitHub Actions).
   - **Network settings / Security Group**:
     - Permite tráfico SSH: Puerto `22` desde cualquier IP (`0.0.0.0/0`) o desde tu IP.
     - Agrega una regla personalizada: Puerto `8080` (TCP), origen `0.0.0.0/0` (para la API REST, Swagger y WebSockets).
     - *(Opcional)* Puerto `80` y `443` si usas un dominio o proxy inverso Nginx.
   - **Configure storage**:
     - `20 GiB` o `30 GiB` gp3 (incluido en el nivel gratuito de AWS EBS).
3. Haz clic en **Launch Instance**.
4. Una vez esté en estado `Running`, copia la **Dirección IPv4 pública** (ej. `54.210.123.45`).

---

## 2. Preparar el Servidor EC2 (Solo una vez)

Conéctate por SSH a la instancia desde tu terminal local:

```bash
chmod 400 artagram-key.pem
ssh -i artagram-key.pem ubuntu@<IP_PUBLICA_EC2>
```

Una vez dentro de la máquina EC2, ejecuta el script de configuración automática:

```bash
curl -fsSL https://raw.githubusercontent.com/TomasQ456/ARTAGRAM-backv1/main/deploy/setup-ec2.sh | bash
```

*(O si prefieres, clona el repositorio o copia el contenido de `deploy/setup-ec2.sh` y ejecútalo).*

El script realiza automáticamente:
1. Configuración de **2 GB de memoria Swap** (previene caídas de memoria en la JVM).
2. Instalación de **Docker** y **Docker Compose**.
3. Creación del directorio `~/artagram`.
4. Creación del archivo `~/artagram/.env` con claves secretas seguras y la IP pública configurada.

---

## 3. Configurar GitHub Packages (Hacer pública la imagen)

Dado que la imagen se publica en GitHub Container Registry (`ghcr.io`):
1. Tras el primer push a `main`, la imagen se publicará en GitHub.
2. Ve a tu perfil u organización de GitHub:
   - **Packages** -> Selecciona `artagram-backend`.
   - **Package settings** -> Abajo en **Danger Zone** -> **Change package visibility** -> Selecciona **Public**.
3. *(Alternativa privada)*: Si prefieres mantenerla privada, crea un Personal Access Token (PAT) con permiso `read:packages` y agrégalo como secreto `GHCR_PAT` en GitHub.

---

## 4. Configurar los Secretos en GitHub Actions

En tu repositorio de GitHub (`ARTAGRAM-BackEnd`):
1. Ve a **Settings** -> **Secrets and variables** -> **Actions**.
2. Haz clic en **New repository secret** y agrega los siguientes 3 secretos:

| Nombre del Secreto | Obligatorio | Descripción |
|---|---|---|
| `EC2_HOST` | Sí (para despliegue) | La IP pública de tu instancia EC2 (ej. `54.210.123.45`). |
| `EC2_USER` | Sí (para despliegue) | `ubuntu` |
| `EC2_SSH_KEY` | Sí (para despliegue) | El contenido completo del archivo `.pem` (incluyendo `-----BEGIN RSA PRIVATE KEY-----` y `-----END ...`). |
| `SONAR_TOKEN` | Opcional | Token de análisis generado en SonarCloud o SonarQube. |
| `SONAR_ORGANIZATION` | Opcional | Organización de SonarCloud (por defecto usa el usuario de GitHub, ej. `TomasQ456`). |
| `SONAR_PROJECT_KEY` | Opcional | Clave del proyecto en Sonar (por defecto `TomasQ456_ARTAGRAM-backv1`). |
| `SONAR_HOST_URL` | Opcional | URL del servidor Sonar (por defecto `https://sonarcloud.io`). |
| `GHCR_PAT` | Opcional | Solo necesario si dejaste el paquete de Docker como privado. |

> ℹ️ **Nota de resiliencia**: Si aún no tienes permisos de administrador en el repositorio para configurar los secretos, el pipeline de CI/CD **no fallará**. Ejecutará la compilación, las pruebas con Testcontainers y la publicación del contenedor Docker, omitiendo Sonar y el despliegue a AWS con un mensaje informativo hasta que el administrador agregue las claves.

---

## 5. Análisis Estático con SonarCloud

El pipeline incluye análisis de calidad de código integrado con JaCoCo y SpotBugs:
1. Crea una cuenta gratuita en [sonarcloud.io](https://sonarcloud.io) vinculada a GitHub.
2. Genera un token en **My Account > Security > Generate Token**.
3. Pídele al administrador del repositorio que guarde el token en los secretos de GitHub como `SONAR_TOKEN`.

---

## 6. Despliegue Automático (CI/CD)

A partir de ahora, cada vez que hagas `git push` a la rama `main` o `master`:
1. **test-and-quality**: GitHub Actions ejecuta las pruebas unitarias y de integración (`mvn verify` con Testcontainers) y el análisis de calidad con Sonar si `SONAR_TOKEN` está presente.
2. **build-and-push**: Construye la imagen Docker optimizada y la sube a `ghcr.io`.
3. **deploy**: Se conecta a la EC2 por SSH, sube el `docker-compose.yml`, descarga la nueva imagen y reinicia el contenedor sin afectar los volúmenes de PostgreSQL ni Redis.

---

## 7. Verificación del Despliegue

Abre en tu navegador:
- **Salud del servicio**: `http://<IP_PUBLICA_EC2>:8080/actuator/health` (debe responder `{"status":"UP"}`)
- **Swagger UI**: `http://<IP_PUBLICA_EC2>:8080/swagger-ui.html`
- **Métricas Prometheus**: `http://<IP_PUBLICA_EC2>:8080/actuator/prometheus`

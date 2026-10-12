#!/usr/bin/env bash
set -e

echo "=========================================================="
echo "  Artagram - Script de inicialización para AWS EC2        "
echo "=========================================================="

# 1. Configurar Swap (2GB) para evitar OOM con Java + Postgres + Redis
if [ ! -f /swapfile ]; then
    echo "[1/5] Creando archivo Swap de 2GB..."
    sudo fallocate -l 2G /swapfile || sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile
    sudo swapon /swapfile
    echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
    echo "Swap configurado correctamente."
else
    echo "[1/5] Swap ya existe, continuando..."
fi

# 2. Instalar Docker y docker compose plugin
echo "[2/5] Instalando Docker y Docker Compose..."
sudo apt-get update -y
sudo apt-get install -y ca-certificates curl gnupg

sudo install -m 0755 -d /etc/apt/keyrings
if [ ! -f /etc/apt/keyrings/docker.gpg ]; then
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    sudo chmod a+r /etc/apt/keyrings/docker.gpg
fi

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update -y
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 3. Asignar permisos al usuario actual
CURRENT_USER=$(whoami)
echo "[3/5] Agregando usuario '${CURRENT_USER}' al grupo docker..."
sudo usermod -aG docker "${CURRENT_USER}"

# 4. Crear carpeta de trabajo
APP_DIR="/home/${CURRENT_USER}/artagram"
echo "[4/5] Creando directorio ${APP_DIR}..."
mkdir -p "${APP_DIR}"
cd "${APP_DIR}"

# 5. Generar archivo .env base si no existe
if [ ! -f "${APP_DIR}/.env" ]; then
    echo "[5/5] Generando archivo .env inicial con credenciales seguras..."
    PUBLIC_IP=$(curl -s http://checkip.amazonaws.com || curl -s ifconfig.me || echo "localhost")
    JWT_SEC=$(openssl rand -base64 32 | tr -d '\n')
    MEDIA_SEC=$(openssl rand -base64 32 | tr -d '\n')
    DB_PASS=$(openssl rand -base64 16 | tr -dc 'a-zA-Z0-9')

    cat <<EOF > "${APP_DIR}/.env"
# Configuración del entorno en AWS EC2
GHCR_OWNER=tomasq456
POSTGRES_PASSWORD=${DB_PASS}
JWT_SECRET=${JWT_SEC}
MEDIA_SIGNING_SECRET=${MEDIA_SEC}
APP_API_BASE_URL=http://${PUBLIC_IP}:8080
APP_BASE_URL=http://${PUBLIC_IP}:5173
CORS_ORIGINS=*
EMAIL_MODE=log
SPRING_PROFILES_ACTIVE=seed
EOF
    echo "Archivo .env creado en ${APP_DIR}/.env"
else
    echo "[5/5] Archivo .env ya existe, preservando configuración."
fi

echo "=========================================================="
echo "  Instalación completada con éxito!                       "
echo "  Directorio de la app: ${APP_DIR}                        "
echo "  Importante: Para usar docker sin sudo, ejecuta:         "
echo "    newgrp docker                                         "
echo "=========================================================="

#!/usr/bin/env bash
# ==============================================================================
# RemoteDesk — Script de Inicialização da Infraestrutura Pública (VPS)
# ==============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "======================================================================"
echo "  REMOTEDESK — INICIALIZAÇÃO DA INFRAESTRUTURA EM NUVEM"
echo "======================================================================"

if [ ! -f ".env" ]; then
    if [ -f ".env.example" ]; then
        echo "[AVISO] Arquivo .env não encontrado. Criando a partir de .env.example..."
        cp .env.example .env
        echo "[INFORMAÇÃO] Edite o arquivo .env com seu domínio e IP antes de produção."
    else
        echo "[ERRO] Arquivo .env.example não encontrado."
        exit 1
    fi
fi

if ! command -v docker >/dev/null 2>&1; then
    echo "[ERRO] Docker não está instalado neste servidor. Instale o Docker e o docker-compose."
    exit 1
fi

echo "[1/3] Construindo imagens e iniciando contêineres..."
docker compose up -d --build

echo "[2/3] Aguardando inicialização dos serviços (5 segundos)..."
sleep 5

echo "[3/3] Verificando integridade..."
./verify_deployment.sh || true

echo "======================================================================"
echo "  INFRAESTRUTURA INICIADA COM SUCESSO!"
echo "======================================================================"

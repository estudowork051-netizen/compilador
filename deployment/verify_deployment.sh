#!/usr/bin/env bash
# ==============================================================================
# RemoteDesk — Script de Verificação de Saúde da Infraestrutura Pública
# ==============================================================================
set -e

BASE_URL="${1:-http://localhost:5000}"

echo "======================================================================"
echo "  REMOTEDESK — VERIFICAÇÃO DE SAÚDE DA INFRAESTRUTURA"
echo "  Testando destino: $BASE_URL"
echo "======================================================================"

echo -n "[1/4] Testando endpoint raiz (/)........................ "
ROOT_RES=$(curl -s "$BASE_URL/" || true)
if echo "$ROOT_RES" | grep -q "RemoteDesk"; then
    echo "OK"
else
    echo "FALHA (Resposta: $ROOT_RES)"
fi

echo -n "[2/4] Testando verificação de saúde (/health)............ "
HEALTH_RES=$(curl -s "$BASE_URL/health" || true)
if echo "$HEALTH_RES" | grep -q "Healthy"; then
    echo "OK"
else
    echo "FALHA (Resposta: $HEALTH_RES)"
fi

echo -n "[3/4] Testando servidores ICE (/api/signaling/ice-servers) "
ICE_RES=$(curl -s "$BASE_URL/api/signaling/ice-servers" || true)
if echo "$ICE_RES" | grep -q "iceServers"; then
    echo "OK"
else
    echo "FALHA (Resposta: $ICE_RES)"
fi

echo -n "[4/4] Testando listagem de hosts (/api/signaling/hosts).. "
HOSTS_RES=$(curl -s "$BASE_URL/api/signaling/hosts" || true)
if echo "$HOSTS_RES" | grep -q "hosts"; then
    echo "OK"
else
    echo "FALHA (Resposta: $HOSTS_RES)"
fi

echo "======================================================================"
echo "  DIAGNÓSTICO CONCLUÍDO"
echo "======================================================================"

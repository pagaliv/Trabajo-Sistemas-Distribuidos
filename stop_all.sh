#!/usr/bin/env bash
set -euo pipefail

# Detiene procesos Java que correspondan al servidor y clientes lanzados por run_all.sh
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$ROOT_DIR/logs"

echo "Buscando procesos Java relacionados con este proyecto..."
ps aux | grep java | grep -E "main\.Servidor|main\.Cliente" | grep -v grep || true

PIDS=$(ps aux | grep java | grep -E "main\.Servidor|main\.Cliente" | grep -v grep | awk '{print $2}') || true
if [ -z "$PIDS" ]; then
  echo "No se encontraron procesos a detener."
  exit 0
fi

echo "Deteniendo PIDs: $PIDS"
for pid in $PIDS; do
  kill "$pid" || true
done

echo "Verificando..."
sleep 1
ps aux | grep java | grep -E "main\.Servidor|main\.Cliente" | grep -v grep || echo "Todos detenidos"

echo "Logs disponibles en: $LOG_DIR"

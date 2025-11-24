#!/usr/bin/env bash
set -euo pipefail

# Script para compilar, arrancar el servidor y 4 clientes del Mus (nombres automáticos)
# Uso: ./run_all.sh [name1 name2 name3 name4]

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT_DIR="$ROOT_DIR/out"
LOG_DIR="$ROOT_DIR/logs"
mkdir -p "$OUT_DIR" "$LOG_DIR"

PORT=12346

echo "Compilando fuentes..."
javac -d "$OUT_DIR" $(find "$ROOT_DIR/src" -name "*.java")

is_listening() {
  ss -ltnp 2>/dev/null | grep -q ":$1 " || return 1
  return 0
}

if is_listening "$PORT"; then
  echo "Puerto $PORT ya está en uso. No arrancaré otra instancia del servidor."
else
  echo "Arrancando servidor en background (puerto $PORT)..."
  nohup java -cp "$OUT_DIR" main.Servidor > "$LOG_DIR/server.log" 2>&1 &
  SERVER_PID=$!
  echo "Servidor arrancado con PID $SERVER_PID (logs: $LOG_DIR/server.log)"
  # Esperar a que el server empiece a escuchar
  for i in {1..20}; do
    if is_listening "$PORT"; then
      echo "Servidor escuchando en $PORT"
      break
    fi
    sleep 0.5
  done
fi

# Nombres por defecto si no se pasan como argumentos
DEFAULT_NAMES=(Alice Bob Carol Dave)
NAMES=()
if [ "$#" -ge 4 ]; then
  NAMES=("$1" "$2" "$3" "$4")
else
  NAMES=(${DEFAULT_NAMES[@]})
fi

echo "Lanzando 4 clientes en terminales separadas..."

# Detectar emulador de terminal disponible
TERMS=("gnome-terminal" "konsole" "xfce4-terminal" "xterm" "mate-terminal" "terminator" "alacritty")
TERMCMD=""
for t in "${TERMS[@]}"; do
  if command -v "$t" >/dev/null 2>&1; then
    TERMCMD="$t"
    break
  fi
done

if [ -z "$TERMCMD" ]; then
  echo "No se encontró un emulador de terminal soportado. Los clientes se lanzarán en background (no interactivos)."
  CLIENT_PIDS=()
  for i in {0..3}; do
    name=${NAMES[i]}
    logfile="$LOG_DIR/client_$i.log"
    echo " - Cliente $i -> nombre: $name (log: $logfile)"
    nohup bash -c "(printf '%s\n' \"$name\"; cat) | java -cp \"$OUT_DIR\" main.Cliente" > "$logfile" 2>&1 &
    CLIENT_PIDS+=("$!")
  done
  echo "Clientes lanzados en background. PIDs: ${CLIENT_PIDS[*]}"
  echo "Logs: $LOG_DIR"
  echo "Para detener los procesos lanzados por este script, ejecuta ./stop_all.sh"
else
  echo "Usando emulador de terminal: $TERMCMD"
  for i in {0..3}; do
    name=${NAMES[i]}
    logfile="$LOG_DIR/client_$i.log"
    echo " - Cliente $i -> nombre: $name (log: $logfile)"
    case "$TERMCMD" in
      gnome-terminal)
        "$TERMCMD" -- bash -lc "(printf '%s\n' \"$name\"; cat) | java -cp \"$OUT_DIR\" main.Cliente; exec bash" &
        ;;
      konsole)
        "$TERMCMD" -e bash -lc "(printf '%s\n' \"$name\"; cat) | java -cp \"$OUT_DIR\" main.Cliente; exec bash" &
        ;;
      xfce4-terminal)
        "$TERMCMD" --command= bash -lc "(printf '%s\n' \"$name\"; cat) | java -cp \"$OUT_DIR\" main.Cliente; exec bash" &
        ;;
      mate-terminal)
        "$TERMCMD" -- bash -lc "(printf '%s\n' \"$name\"; cat) | java -cp \"$OUT_DIR\" main.Cliente; exec bash" &
        ;;
      terminator)
        "$TERMCMD" -x bash -lc "(printf '%s\n' \"$name\"; cat) | java -cp \"$OUT_DIR\" main.Cliente; exec bash" &
        ;;
      xterm)
        "$TERMCMD" -hold -e bash -lc "(printf '%s\n' \"$name\"; cat) | java -cp \"$OUT_DIR\" main.Cliente" &
        ;;
      alacritty)
        "$TERMCMD" -e bash -lc "(printf '%s\n' \"$name\"; cat) | java -cp \"$OUT_DIR\" main.Cliente; exec bash" &
        ;;
      *)
        nohup bash -c "(printf '%s\n' \"$name\"; cat) | java -cp \"$OUT_DIR\" main.Cliente" > "$logfile" 2>&1 &
        ;;
    esac
    sleep 0.2
  done
  echo "Clientes lanzados en terminales. Busca las ventanas abiertas y usa sus consolas para interactuar."
  echo "Logs: $LOG_DIR"
fi

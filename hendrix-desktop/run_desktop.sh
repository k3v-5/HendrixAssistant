#!/usr/bin/env bash
# ==============================================================================
# Hendrix Desktop Companion - Script de Inicio para Linux
# ==============================================================================
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "=========================================================="
echo "⚡ Iniciando Hendrix Desktop Companion en Linux..."
echo "=========================================================="

# Verificar entorno virtual Python
if [ ! -d ".venv" ]; then
    echo "📦 Creando entorno virtual Python en .venv..."
    python3 -m venv .venv
    echo "📥 Instalando dependencias desde requirements.txt..."
    .venv/bin/pip install --upgrade pip
    .venv/bin/pip install -r requirements.txt
fi
# Auto-detección de pantalla DISPLAY para Wayland / Xwayland / X11
if [ -z "$DISPLAY" ]; then
    CURRENT_UID="$(id -u)"
    for sock in /tmp/.X11-unix/X*; do
        if [ -S "$sock" ]; then
            if [ "$(stat -c '%u' "$sock" 2>/dev/null)" = "$CURRENT_UID" ]; then
                export DISPLAY=":${sock##*/X}"
                break
            fi
        fi
    done
    if [ -z "$DISPLAY" ] && [ -e /tmp/.X11-unix/X0 ]; then
        export DISPLAY=":0"
    fi
fi

if ! .venv/bin/python -c "import tkinter" 2>/dev/null; then
    echo "ℹ️  Tkinter no detectado (falta el paquete 'tk' del sistema)."
    echo "💡 Si deseas la ventana gráfica OLED, puedes instalarlo con:"
    echo "   sudo pacman -S tk   (en Arch / EndeavourOS)"
    echo "   sudo apt install python3-tk   (en Debian / Ubuntu)"
    echo "🚀 Iniciando Hendrix Desktop en MODO SERVICIO (WebSocket, AirSync, Control)..."
    echo "=========================================================="
fi

# Ejecutar aplicación principal
exec .venv/bin/python main.py "$@"

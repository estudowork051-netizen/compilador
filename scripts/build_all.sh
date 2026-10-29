#!/bin/bash
# Script de conveniência para compilar e gerar logs de todos os componentes
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
python3 "$SCRIPT_DIR/build_runner.py" --all

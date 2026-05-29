#!/usr/bin/env bash
# atalho para abrir o simulador com interface grafica
# uso: ./run-ui.sh [entrada/entrada_basica.txt]
set -e
cd "$(dirname "$0")"
if [ ! -d out ]; then
  ./build.sh
fi
java -cp out sosim.ui.MainUI "$@"

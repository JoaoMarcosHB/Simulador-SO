#!/usr/bin/env bash
# atalho para rodar o simulador apos compilado
# uso: ./run.sh entrada/entrada_basica.txt
set -e
cd "$(dirname "$0")"
if [ ! -d out ]; then
  ./build.sh
fi
java -cp out sosim.Main "$@"

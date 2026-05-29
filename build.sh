#!/usr/bin/env bash
# compila o simulador (core + UI)
# uso: ./build.sh
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -d out src/sosim/*.java src/sosim/ui/*.java
echo "Compilado com sucesso. Classes em ./out"
echo
echo "Para executar em modo terminal:"
echo "  java -cp out sosim.Main entrada/entrada_basica.txt"
echo
echo "Para executar com interface grafica:"
echo "  java -cp out sosim.ui.MainUI [entrada/entrada_basica.txt]"

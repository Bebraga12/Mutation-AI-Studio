#!/usr/bin/env bash
set -euo pipefail

TARGET_PROJECT="${1:-$HOME/Documentos/Faculdade/TCC/projetos-testes/Spring-livros}"
MUTATION_DIR="$TARGET_PROJECT/.mutation-ai"
FAILED_DIR="$MUTATION_DIR/generated/failed"
PROMPTS_DIR="$MUTATION_DIR/prompts"

if [[ ! -d "$MUTATION_DIR" ]]; then
  echo "Diretório .mutation-ai não encontrado em: $TARGET_PROJECT"
  exit 1
fi

latest_failed=""
if [[ -d "$FAILED_DIR" ]]; then
  latest_failed=$(find "$FAILED_DIR" -mindepth 1 -maxdepth 1 -type d | sort | tail -n 1)
fi

latest_prompt=""
if [[ -d "$PROMPTS_DIR" ]]; then
  latest_prompt=$(find "$PROMPTS_DIR" -mindepth 1 -maxdepth 1 -type d | sort | tail -n 1)
fi

echo "Projeto alvo: $TARGET_PROJECT"
echo

if [[ -n "$latest_prompt" ]]; then
  echo "Último lote de prompts: $latest_prompt"
else
  echo "Nenhum lote de prompts encontrado."
fi

if [[ -n "$latest_failed" ]]; then
  echo "Último lote de falhas: $latest_failed"
  echo
  echo "Arquivos preservados:"
  find "$latest_failed" -maxdepth 1 -type f | sort
  echo
  for file in "$latest_failed"/*.java; do
    [[ -f "$file" ]] || continue
    echo "===== $file ====="
    sed -n '1,220p' "$file"
    echo
  done
else
  echo "Nenhum lote de falhas preservado encontrado."
fi

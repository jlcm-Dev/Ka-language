#!/bin/bash
# Roda todos os casos .ka e compara com a saída esperada.
# Saída = tokens + contagem de declarações + erros léxicos/sintáticos.

set -u

cd "$(dirname "$0")"

CASOS_DIR="casos"
ESPERADO_DIR="esperado"
TMP_DIR="/tmp/ka-test"

mkdir -p "$TMP_DIR"

VERDE="\033[0;32m"
VERMELHO="\033[0;31m"
AMARELO="\033[0;33m"
RESET="\033[0m"

total=0
passou=0
falhou=0
sem_esperado=0

echo "=========================================="
echo "  Testes do front-end do Ka"
echo "  (Scanner + Parser)"
echo "=========================================="
echo

for caso in "$CASOS_DIR"/*.ka; do
    nome=$(basename "$caso" .ka)
    esperado="$ESPERADO_DIR/$nome.txt"
    total=$((total + 1))

    echo "--- $nome ---"

    saida_arquivo="$TMP_DIR/$nome.saida"
    java -cp "${KA_CP:-..}" com.craftinginterpreters.ka.Ka "$caso" > "$saida_arquivo" 2>&1 || true

    if [ ! -f "$esperado" ]; then
        if [ "${1:-}" = "--gerar" ]; then
            cp "$saida_arquivo" "$esperado"
            echo -e "${AMARELO}[GERADO]${RESET} test/esperado/$nome.txt (confira a saida antes de confiar!)"
        else
            echo -e "${AMARELO}[SEM ESPERADO]${RESET} rode com --gerar para criar $nome.txt"
        fi
        sem_esperado=$((sem_esperado + 1))
        echo
        continue
    fi

    # No Windows o Java imprime \r\n e no Linux so \n: remove \r dos dois lados
    # antes de comparar, senao tudo "falha" mesmo estando igual.
    tr -d '\r' < "$esperado" > "$TMP_DIR/$nome.esperado"
    tr -d '\r' < "$saida_arquivo" > "$TMP_DIR/$nome.saida.lf"

    if diff -q "$TMP_DIR/$nome.esperado" "$TMP_DIR/$nome.saida.lf" > /dev/null; then
        echo -e "${VERDE}[PASSOU]${RESET}"
        passou=$((passou + 1))
    else
        echo -e "${VERMELHO}[FALHOU]${RESET}"
        echo "Diferença (esperado x obtido):"
        diff "$TMP_DIR/$nome.esperado" "$TMP_DIR/$nome.saida.lf" | head -30
        falhou=$((falhou + 1))
    fi
    echo
done

echo "=========================================="
echo "  Total: $total"
echo "  Passou: $passou"
echo "  Falhou: $falhou"
echo "  Sem esperado (recém gerados): $sem_esperado"
echo "=========================================="

if [ $falhou -gt 0 ]; then
    exit 1
fi

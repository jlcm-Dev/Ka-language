#!/bin/bash
# Compila o front-end do Ka (Scanner + Parser + AST).
# Move runtime temporariamente se ainda estiver na pasta.

set -e

cd "$(dirname "$0")/.."

echo "=== Limpando .class ==="
find . -name "*.class" -delete

if [ -d "_runtime" ]; then
    echo "=== Runtime já está em _runtime/ ==="
else
    echo "=== Movendo runtime para _runtime/ ==="
    mkdir -p _runtime
    for f in Interpreter Resolver Environment KaObject KaFunction KaCallable Return RuntimeError; do
        if [ -f "com/craftinginterpreters/ka/$f.java" ]; then
            mv "com/craftinginterpreters/ka/$f.java" "_runtime/"
        fi
    done
fi

echo "=== Compilando front-end ==="
javac com/craftinginterpreters/ka/*.java

echo "=== Build OK ==="

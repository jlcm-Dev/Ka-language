package com.craftinginterpreters.ka;

/*
 * Diferente do erro de SINTAXE (que o Parser detecta, tipo esquecer
 * um ')'), esse é um erro de EXECUÇÃO - só descobrimos que tem
 * problema quando o programa já está RODANDO.
 *
 * Exemplo: "2" + true;  -> sintaticamente válido (é um Binary normal),
 * mas semanticamente não faz sentido, e só percebemos isso quando o
 * Interpreter tenta de fato somar esses dois valores.
 *
 * Guardamos o "token" que causou o erro, pra poder reportar em
 * qual linha do código o problema aconteceu.
 */
class RuntimeError extends RuntimeException {
    final Token token;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}

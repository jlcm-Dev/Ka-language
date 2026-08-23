package com.craftinginterpreters.ka;

/*
 * Isso NÃO representa um erro de verdade. É um truque: usamos o
 * mecanismo de exceção do Java só pra conseguir "pular" de qualquer
 * profundidade de código (dentro de vários if/while aninhados) direto
 * de volta pro ponto onde a função foi CHAMADA (LoxFunction.call()).
 *
 * Os "null, null, false, false" no super() desligam coisas que o
 * Java normalmente monta pra exceções de erro de verdade (stack
 * trace, supressão, etc.) - não precisamos disso aqui, e monta-los
 * custaria desempenho à toa.
 */
class Return extends RuntimeException {
    final Object value;

    Return(Object value) {
        super(null, null, false, false);
        this.value = value;
    }
}

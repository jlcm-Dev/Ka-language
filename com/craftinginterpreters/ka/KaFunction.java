package com.craftinginterpreters.ka;

import java.util.List;

/*
 * Diferente do LoxFunction (que vinculava "this" no momento em que
 * o método era PEGO de uma instância, via bind() guardado no
 * closure), aqui "this" e vinculado no momento da CHAMADA, pelo
 * Interpreter, baseado em COMO a função foi chamada:
 *
 *   obj.metodo()        -> "this" = obj (vinculado nessa chamada)
 *   var m = obj.metodo;
 *   m()                 -> "this" = nil (a referencia foi "solta"
 *                           do objeto - perdeu o vinculo, igual
 *                           acontece de verdade em JavaScript)
 *
 * Essa e a diferenca central entre "bound methods" (Lox, capitulo
 * 12) e "funcoes soltas usadas como metodo" (estilo prototipo).
 */
class KaFunction implements KaCallable {
    private final String name; // pode ser null (função anônima)
    private final List<Token> params;
    private final List<Stmt> body;
    private final Environment closure;
    private final Object boundReceiver; // null = sem "this" vinculado

    KaFunction(String name, List<Token> params, List<Stmt> body, Environment closure) {
        this(name, params, body, closure, null);
    }

    private KaFunction(String name, List<Token> params, List<Stmt> body,
                        Environment closure, Object boundReceiver) {
        this.name = name;
        this.params = params;
        this.body = body;
        this.closure = closure;
        this.boundReceiver = boundReceiver;
    }

    // Devolve uma CÓPIA dessa função com "this" fixado no receptor
    // dado - usado pelo Interpreter na hora de uma chamada estilo
    // "objeto.metodo()".
    KaFunction bind(KaObject receiver) {
        return new KaFunction(name, params, body, closure, receiver);
    }

    @Override
    public int arity() {
        return params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);

        // "this" sempre existe como uma variável no ambiente da
        // chamada (igual um parâmetro implícito) - vale o receptor
        // vinculado, ou nil se a função foi chamada "solta".
        environment.define("this", boundReceiver);

        for (int i = 0; i < params.size(); i++) {
            environment.define(params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }

        return null;
    }

    @Override
    public String toString() {
        return "<fn " + (name != null ? name : "anonima") + ">";
    }
}

package com.craftinginterpreters.ka;

import java.util.HashMap;
import java.util.Map;

/*
 * Em Ka NAO existe "classe" separada de "instancia". So existe
 * OBJETO: um saco de campos (fields), mais um ponteiro opcional
 * pra outro objeto - seu PROTOTIPO. "Herdar" comportamento
 * significa simplesmente: "se eu nao tenho esse campo, pergunta
 * pro meu prototipo".
 *
 * Metodos nao sao uma categoria especial - sao so campos cujo
 * valor e uma funcao (KaFunction). Quem decide se "this" e
 * vinculado e o Interpreter, na hora da CHAMADA (visitCallExpr),
 * nao aqui.
 */
class KaObject {
    // Pode ser null (topo da cadeia de protótipos).
    KaObject prototype;
    final Map<String, Object> fields = new HashMap<>();

    KaObject(KaObject prototype) {
        this.prototype = prototype;
    }

    // Busca uma propriedade, subindo a cadeia de protótipos se
    // necessário. "__proto__" é um nome especial reservado que
    // sempre devolve o protótipo atual (ou nil se não tiver).
    Object get(Token name) {
        if (name.lexeme.equals("__proto__")) {
            return prototype;
        }

        KaObject current = this;
        while (current != null) {
            if (current.fields.containsKey(name.lexeme)) {
                return current.fields.get(name.lexeme);
            }
            current = current.prototype;
        }

        throw new RuntimeError(name, "Propriedade indefinida '" + name.lexeme + "'.");
    }

    // Define/sobrescreve um campo SEMPRE no próprio objeto (nunca
    // no protótipo) - igual JavaScript: escrever nunca muta o
    // protótipo, só cria/atualiza uma propriedade própria.
    // "__proto__" é especial: reatribui o próprio ponteiro de protótipo.
    Object set(Token name, Object value) {
        if (name.lexeme.equals("__proto__")) {
            if (value == null || value instanceof KaObject) {
                this.prototype = (KaObject) value;
                return value;
            }
            throw new RuntimeError(name, "__proto__ deve ser um objeto ou nil.");
        }
        fields.put(name.lexeme, value);
        return value;
    }

    @Override
    public String toString() {
        return "<object>";
    }
}

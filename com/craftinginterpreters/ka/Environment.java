package com.craftinginterpreters.ka;

import java.util.HashMap;
import java.util.Map;

/*
 * O AMBIENTE (Environment)
 * ========================
 * É aqui que as variáveis "moram" de verdade em tempo de execução.
 * Pense nele como um dicionário: nome da variável (String) -> valor.
 *
 * "values" usa String (não Token) como chave, porque dois tokens
 * IDENTIFIER diferentes, em lugares diferentes do código, mas com
 * o mesmo texto (ex: duas aparições de "idade"), devem se referir
 * à MESMA variável.
 */
class Environment {

    // Referência pro ambiente "de fora" (o escopo que contém esse).
    // Se for null, esse é o ambiente GLOBAL (não tem ninguém acima dele).
    final Environment enclosing;

    private final Map<String, Object> values = new HashMap<>();

    // Construtor pro ambiente GLOBAL (não tem escopo pai)
    Environment() {
        enclosing = null;
    }

    // Construtor pra um escopo LOCAL, aninhado dentro de outro
    // (ex: o corpo de um bloco { }, que "enxerga" o escopo de fora)
    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    // Cria uma variável NOVA nesse ambiente (usado por "var x = ...").
    // Repare: não checa se já existe - "var x" duas vezes seguidas
    // é permitido, a segunda simplesmente sobrescreve a primeira.
    void define(String name, Object value) {
        values.put(name, value);
    }

    // Busca o VALOR de uma variável já existente.
    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        // Não achou aqui? Tenta no escopo de FORA (recursivamente).
        // É assim que uma variável global continua visível de
        // dentro de um bloco aninhado.
        if (enclosing != null) return enclosing.get(name);

        // Não achou em lugar nenhum -> erro em tempo de EXECUÇÃO
        // (não é erro de sintaxe - o código é válido, só está usando
        // um nome que nunca foi declarado)
        throw new RuntimeError(name,
            "Variavel nao definida '" + name.lexeme + "'.");
    }

    // ATRIBUI um novo valor a uma variável que JÁ EXISTE
    // (usado por "x = novoValor", diferente de "var x = valor").
    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        // Também não achou aqui? Tenta atribuir no escopo de fora.
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        // Diferente de "define()": atribuição NUNCA cria uma
        // variável nova. Se não existe em lugar nenhum, é erro.
        throw new RuntimeError(name,
            "Variavel nao definida '" + name.lexeme + "'.");
    }

    // -----------------------------------------------------------
    // ACESSO "DIRETO" - usado quando o RESOLVER já calculou
    // antecipadamente a distância exata até o ambiente certo.
    // Diferente de get()/assign(), esses NÃO precisam checar se
    // a variável existe (o Resolver já garantiu que existe) nem
    // percorrer a cadeia procurando - vamos direto no alvo.
    // -----------------------------------------------------------

    // Pula exatamente "distance" ambientes pra cima na cadeia
    // "enclosing" e devolve o Environment encontrado lá.
    Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }
        return environment;
    }

    // Busca o valor de uma variável, mas já sabendo EXATAMENTE
    // a quantos níveis de distância ela está (calculado pelo Resolver).
    Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    // Mesma ideia, mas pra atribuição.
    void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }
}

package com.craftinginterpreters.ka;

/*
 * Um Symbol representa o NOME de algo dentro da arvore de sintaxe (AST),
 * junto com a linha onde ele foi declarado/referenciado: nome de variavel,
 * nome de funcao, nome de parametro, chave de um literal de objeto, ou a
 * palavra "this"/"return".
 *
 * Por que isso existe, e por que nao usamos Token direto como antes:
 *
 *   Token  -> pertence a fase LEXICA. Carrega type, lexeme, literal, line.
 *   Symbol -> pertence a fase SINTATICA (a AST). Carrega so name + line,
 *             que e o unico que a arvore realmente precisa pra identificar
 *             "quem" uma variavel/funcao/propriedade e, e onde apontar
 *             um erro relacionado a ela.
 *
 * Antes, nos de AST como Expr.Variable, Stmt.Var, Expr.This etc. guardavam
 * um Token inteiro so pra usar o .lexeme (texto) e o .line (linha do erro).
 * Isso acopla a AST a um conceito de uma fase anterior (o Scanner) que ela
 * nao deveria precisar conhecer - se o Scanner mudar a estrutura de Token
 * amanha (adicionar coluna, remover literal, etc.), a AST nao deveria
 * sentir nada disso. Com Symbol, a AST so depende do que ela de fato usa.
 *
 * O construtor Symbol(Token) existe so para facilitar a conversao dentro
 * do Parser: o Parser recebe Token do Scanner e, ao montar um no da AST,
 * empacota so o que interessa num Symbol.
 */
class Symbol {
    final String name;
    final int line;

    Symbol(String name, int line) {
        this.name = name;
        this.line = line;
    }

    // Conveniencia: constroi um Symbol diretamente a partir de um Token
    // (e o caminho mais comum, usado o tempo todo dentro do Parser).
    Symbol(Token token) {
        this(token.lexeme, token.line);
    }

    @Override
    public String toString() {
        return name;
    }
}

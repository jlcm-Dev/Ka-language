package com.craftinginterpreters.ka;

/*
 * Um Token é o "pacotinho de informação" que o Scanner produz pra cada
 * lexema que ele reconhece no código fonte.
 *
 * Lembrando a diferença:
 * - LEXEMA = o pedaço de texto cru, tipo "var" ou "123" ou "+"
 * - TOKEN  = o lexema + informação extra útil (que tipo é, qual o valor
 *            real, em que linha apareceu)
 *
 * Essa classe não tem lógica nenhuma, é só uma "caixinha" de dados
 * (em Java isso é comum chamar de "POJO" - Plain Old Java Object).
 */
class Token {
    final TokenType type;   // o tipo do token, ex: PLUS, STRING, IDENTIFIER...
    final String lexeme;    // o texto cru original, ex: "+", "\"oi\"", "idade"
    final Object literal;   // o valor já convertido, ex: 123.0 (double), "oi" (String)
                             // pra tokens que não são literais (tipo "+"), fica null
    final int line;         // em qual linha do código fonte esse token apareceu
                             // (usamos isso pra reportar erros de forma útil)

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    // Isso só serve pra quando a gente faz System.out.println(token),
    // pra imprimir ele de um jeito legível, tipo: "PLUS + null"
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}

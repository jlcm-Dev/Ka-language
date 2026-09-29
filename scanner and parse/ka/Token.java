package ka;

/*
 * Um Token é o "pacotinho de informação" que o Scanner produz para
 * cada lexema que ele reconhece no código fonte.
 *
 * Terminologia importante para a apresentação:
 *   LEXEMA = o pedaço de texto CRU, exatamente como está no arquivo.
 *            Ex: "var", "123", "+", "\"oi\""
 *   TOKEN  = o lexema + informação estruturada extra:
 *              - qual é o TIPO dele (TokenType)
 *              - qual é o VALOR já convertido (literal), quando aplicável
 *              - em que LINHA do arquivo ele apareceu
 *
 * Esta classe não tem lógica nenhuma: é só uma "caixinha" imutável de
 * dados (um "POJO" - Plain Old Java Object). Quem cria os Tokens é o
 * Scanner; quem consome é o Parser.
 */
class Token {
    final TokenType type;    // o tipo do token, ex: PLUS, STRING, IDENTIFIER...
    final String lexeme;     // o texto cru original, ex: "+", "\"oi\"", "idade"
    final Object literal;    // o valor já convertido, ex: 123.0 (Double), "oi" (String)
                              // para tokens que não são literais (tipo "+"), fica null
    final int line;          // em qual linha do código fonte esse token apareceu
                              // (usado para reportar erros de forma útil)

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    // Representação legível para debug, ex: "PLUS + null" ou "NUMBER 123.0 123.0"
    public String toString() {
        return type + " '" + lexeme + "'" + (literal != null ? " (" + literal + ")" : "");
    }
}

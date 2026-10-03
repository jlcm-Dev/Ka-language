package com.craftinginterpreters.ka;

/*
 * Operator representa um OPERADOR dentro da AST: o "+" de Expr.Binary,
 * o "!" de Expr.Unary, o "and"/"or" de Expr.Logical.
 *
 * Essa classe espelha de proposito a mesma relacao que Token tem com
 * TokenType - uma classe "de fora" guardando um enum "de dentro":
 *
 *   Token    { TokenType type, String lexeme, Object literal, int line }
 *   Operator { Kind kind,                                      int line }
 *
 * Por que separar de Token, igual fizemos com Symbol para nomes:
 *
 *   Token tem campos que nao fazem sentido pra um operador dentro da
 *   AST - "literal" nunca e preenchido para "+" ou "==", e o "lexeme"
 *   e so uma representacao textual que da pra recalcular a partir do
 *   proprio Kind (ver metodo lexeme() abaixo). O unico motivo de ainda
 *   guardar algo alem do Kind e a LINHA, usada em mensagens de erro em
 *   tempo de execucao (ex: "Operando deve ser numero. [linha 7]").
 *
 * Diferenca de Operator para TokenType: TokenType cobre TODOS os tokens
 * possiveis da linguagem (palavras reservadas, pontuacao, EOF...).
 * Operator.Kind cobre SO os operadores que realmente aparecem dentro de
 * Expr.Binary, Expr.Logical e Expr.Unary. Isso deixa explicito, so
 * olhando o tipo, quais operadores a AST aceita - nao da pra, por
 * engano, colocar um TokenType.VAR dentro de um Expr.Binary, porque o
 * compilador Java nem deixa compilar.
 */
class Operator {

    // Os unicos operadores que aparecem em Expr.Binary, Expr.Logical
    // e Expr.Unary. Cada valor corresponde a exatamente um lexema.
    enum Kind {
        // Aritmeticos (Expr.Binary)
        PLUS, MINUS, STAR, SLASH,
        // Comparacao (Expr.Binary)
        GREATER, GREATER_EQUAL, LESS, LESS_EQUAL,
        // Igualdade (Expr.Binary)
        BANG_EQUAL, EQUAL_EQUAL,
        // Unario (Expr.Unary) - BANG so aparece aqui; MINUS e
        // reaproveitado tanto em Expr.Binary ("a - b") quanto em
        // Expr.Unary ("-a"), por isso ja esta listado acima.
        BANG,
        // Logicos, com curto-circuito (Expr.Logical)
        AND, OR
    }

    final Kind kind;
    final int line;

    Operator(Kind kind, int line) {
        this.kind = kind;
        this.line = line;
    }

    // Conveniencia: constroi um Operator a partir do Token que o
    // Scanner produziu. E aqui que a traducao TokenType -> Kind
    // acontece - a unica ponte entre a fase lexica e este tipo.
    Operator(Token token) {
        this(kindOf(token.type), token.line);
    }

    private static Kind kindOf(TokenType type) {
        switch (type) {
            case PLUS:          return Kind.PLUS;
            case MINUS:         return Kind.MINUS;
            case STAR:          return Kind.STAR;
            case SLASH:         return Kind.SLASH;
            case GREATER:       return Kind.GREATER;
            case GREATER_EQUAL: return Kind.GREATER_EQUAL;
            case LESS:          return Kind.LESS;
            case LESS_EQUAL:    return Kind.LESS_EQUAL;
            case BANG_EQUAL:    return Kind.BANG_EQUAL;
            case EQUAL_EQUAL:   return Kind.EQUAL_EQUAL;
            case BANG:          return Kind.BANG;
            case AND:           return Kind.AND;
            case OR:            return Kind.OR;
            default:
                // Nunca deveria acontecer: o Parser so chama este
                // construtor depois de um match() que ja garantiu que
                // o token e um dos tipos acima. Se isso disparar, e
                // bug de programacao (um TokenType novo usado como
                // operador sem ser cadastrado aqui), nao erro do usuario.
                throw new IllegalArgumentException(
                    "TokenType nao corresponde a nenhum Operator.Kind: " + type);
        }
    }

    // Reconstroi o texto do operador a partir do Kind - usado em
    // mensagens de erro e em qualquer impressao da AST, sem precisar
    // guardar o lexeme original.
    String lexeme() {
        switch (kind) {
            case PLUS:          return "+";
            case MINUS:         return "-";
            case STAR:          return "*";
            case SLASH:         return "/";
            case GREATER:       return ">";
            case GREATER_EQUAL: return ">=";
            case LESS:          return "<";
            case LESS_EQUAL:    return "<=";
            case BANG_EQUAL:    return "!=";
            case EQUAL_EQUAL:   return "==";
            case BANG:          return "!";
            case AND:           return "and";
            case OR:            return "or";
            default:            return kind.toString();
        }
    }

    @Override
    public String toString() {
        return lexeme();
    }
}

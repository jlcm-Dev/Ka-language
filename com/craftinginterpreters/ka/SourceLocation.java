package com.craftinginterpreters.ka;

/*
 * Hoje guarda so a linha. A ideia e evoluir pra incluir coluna e arquivo
 * sem refatorar a AST - mudar so esta classe basta, porque todos os nos
 * que a usam ganham os campos novos de graca.  
 */
class SourceLocation {
    final int line;

    SourceLocation(int line) {
        this.line = line;
    }

    SourceLocation(Token token) {
        this(token.line);
    }

    @Override
    public String toString() {
        return "linha " + line;
    }
}
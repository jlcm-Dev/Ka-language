package com.craftinginterpreters.ka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.craftinginterpreters.ka.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    // declaration → funDecl | varDecl | statement ;
    // (sem classDecl - Ka nao tem "class")
    private Stmt declaration() {
        try {
            if (match(FUN)) return function("function");
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Esperado o nome da " + kind + ".");
        consume(LEFT_PAREN, "Esperado '(' depois do nome da " + kind + ".");
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Uma funcao nao pode ter mais de 255 parametros.");
                }
                parameters.add(consume(IDENTIFIER, "Esperado o nome do parametro."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Esperado ')' depois dos parametros.");
        consume(LEFT_BRACE, "Esperado '{' antes do corpo da " + kind + ".");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    // Igual function(), mas SEM nome - usada para "fun (params) { corpo }"
    // em posição de EXPRESSÃO (função anônima / lambda).
    private Expr functionExpression() {
        consume(LEFT_PAREN, "Esperado '(' depois de 'fun'.");
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Uma funcao nao pode ter mais de 255 parametros.");
                }
                parameters.add(consume(IDENTIFIER, "Esperado o nome do parametro."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Esperado ')' depois dos parametros.");
        consume(LEFT_BRACE, "Esperado '{' antes do corpo da funcao.");
        List<Stmt> body = block();
        return new Expr.Function(parameters, body);
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Esperado o nome da variavel.");
        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Esperado ';' depois da declaracao da variavel.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt statement() {
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(RETURN)) return returnStatement();
        if (match(WHILE)) return whileStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        return expressionStatement();
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }
        consume(SEMICOLON, "Esperado ';' depois do valor de retorno.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Esperado '(' depois de 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) condition = expression();
        consume(SEMICOLON, "Esperado ';' depois da condicao do loop.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) increment = expression();
        consume(RIGHT_PAREN, "Esperado ')' depois das clausulas do for.");

        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }
        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);
        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }
        return body;
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Esperado '(' depois de 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Esperado ')' depois da condicao do if.");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) elseBranch = statement();
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Esperado ';' depois do valor.");
        return new Stmt.Print(value);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Esperado '(' depois de 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Esperado ')' depois da condicao.");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Esperado ';' depois da expressao.");
        return new Stmt.Expression(expr);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Esperado '}' depois do bloco.");
        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    // assignment → ( call "." )? IDENTIFIER "=" assignment | logic_or ;
    private Expr assignment() {
        Expr expr = or();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get) expr;
                return new Expr.Set(get.object, get.name, value);
            }

            error(equals, "Alvo de atribuicao invalido.");
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();
        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr and() {
        Expr expr = equality();
        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                Token name = consume(IDENTIFIER, "Esperado o nome da propriedade depois de '.'.");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Uma chamada nao pode ter mais de 255 argumentos.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }
        Token paren = consume(RIGHT_PAREN, "Esperado ')' depois dos argumentos.");
        return new Expr.Call(callee, paren, arguments);
    }

    // primary → ... | "fun" "(" ... | "{" objectLiteral "}" | ...
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(THIS)) return new Expr.This(previous());

        // Função anônima: "fun (a, b) { ... }" como EXPRESSÃO
        // (diferente de "fun nome(...) {...}" que é statement).
        if (match(FUN)) return functionExpression();

        // Literal de objeto: "{ chave: valor, chave2: valor2 }"
        // Só entra aqui dentro de contexto de EXPRESSÃO - "{" no
        // início de um statement continua sendo bloco (sem ambiguidade,
        // já que statement() checa LEFT_BRACE antes de cair aqui).
        if (match(LEFT_BRACE)) return objectLiteral();

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Esperado ')' depois da expressao.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Esperada uma expressao.");
    }

    // objectLiteral → "{" ( IDENTIFIER ":" expression ( "," IDENTIFIER ":" expression )* )? "}" ;
    private Expr objectLiteral() {
        List<Token> keys = new ArrayList<>();
        List<Expr> values = new ArrayList<>();

        if (!check(RIGHT_BRACE)) {
            do {
                Token key = consume(IDENTIFIER, "Esperado o nome da propriedade.");
                consume(COLON, "Esperado ':' depois do nome da propriedade.");
                Expr value = expression();
                keys.add(key);
                values.add(value);
            } while (match(COMMA));
        }

        consume(RIGHT_BRACE, "Esperado '}' depois do literal de objeto.");
        return new Expr.ObjectLiteral(keys, values);
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Ka.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;
            switch (peek().type) {
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }
}

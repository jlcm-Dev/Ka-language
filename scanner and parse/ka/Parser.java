package ka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ka.TokenType.*;

/*
 * ==========================================================================
 *  PARSER (ANÁLISE SINTÁTICA) - Recursive Descent, Top-Down, Preditivo
 * ==========================================================================
 * Responsabilidade: transformar a List<Token> (produzida pelo Scanner)
 * em uma árvore de sintaxe (List<Stmt>, cada Stmt podendo conter Expr).
 *
 * Técnica: "recursive descent parsing".
 *   - TOP-DOWN: começa pela regra mais geral (program/declaration) e
 *     desce até as mais específicas (primary).
 *   - PREDITIVO: em cada ponto de decisão, olha SOMENTE o token atual
 *     (lookahead de 1) e já sabe, sem ambiguidade, qual regra aplicar.
 *     Nunca precisa "voltar atrás" (backtracking) - a gramática de Ka
 *     foi desenhada para ser LL(1).
 *
 * Cada método abaixo corresponde a UMA regra da gramática. A gramática
 * completa (em notação BNF simplificada) está documentada logo acima
 * de cada método. Do mais geral para o mais específico:
 *
 *   program        -> declaration* EOF
 *   declaration    -> funDecl | varDecl | statement
 *   funDecl        -> "fun" IDENTIFIER "(" parameters? ")" block
 *   varDecl        -> "var" IDENTIFIER ( "=" expression )? ";"
 *   statement      -> exprStmt | forStmt | ifStmt | printStmt
 *                    | returnStmt | whileStmt | block
 *   exprStmt       -> expression ";"
 *   forStmt        -> "for" "(" ... ")" statement      (desnaturado em while)
 *   ifStmt         -> "if" "(" expression ")" statement ( "else" statement )?
 *   printStmt      -> "print" expression ";"
 *   returnStmt     -> "return" expression? ";"
 *   whileStmt      -> "while" "(" expression ")" statement
 *   block          -> "{" declaration* "}"
 *
 *   expression     -> assignment
 *   assignment     -> ( call "." )? IDENTIFIER "=" assignment | logic_or
 *   logic_or       -> logic_and ( "or" logic_and )*
 *   logic_and      -> equality ( "and" equality )*
 *   equality       -> comparison ( ( "!=" | "==" ) comparison )*
 *   comparison     -> term ( ( ">" | ">=" | "<" | "<=" ) term )*
 *   term           -> factor ( ( "-" | "+" ) factor )*
 *   factor         -> unary ( ( "/" | "*" ) unary )*
 *   unary          -> ( "!" | "-" ) unary | call
 *   call           -> primary ( "(" arguments? ")" | "." IDENTIFIER )*
 *   primary        -> NUMBER | STRING | "true" | "false" | "nil" | "this"
 *                    | IDENTIFIER | "(" expression ")"
 *                    | "fun" "(" parameters? ")" block      (lambda)
 *                    | "{" objectLiteral "}"
 *   objectLiteral  -> ( IDENTIFIER ":" expression ( "," IDENTIFIER ":" expression )* )?
 *
 * Repare como a gramática de expressões está estratificada por
 * PRECEDÊNCIA: cada regra chama a de precedência imediatamente maior
 * antes de si mesma. Isso é o que garante que "2 + 3 * 4" vire
 * "2 + (3 * 4)" e não "(2 + 3) * 4" - sem precisar de nenhuma tabela
 * de precedência explícita, só a estrutura recursiva das chamadas.
 * ==========================================================================
 */
class Parser {

    // Exceção interna usada só para "desviar" a execução até o ponto
    // de sincronização, quando um erro de sintaxe é encontrado.
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    // Ativa impressão de cada regra gramatical visitada, com
    // indentação proporcional à profundidade da recursão - isso deixa
    // BEM visível a estrutura top-down do parser.
    static boolean DEBUG = false;
    private int depth = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // Imprime uma linha de trace indentada de acordo com a profundidade
    // atual da recursão. Chamado no início de cada regra gramatical.
    private void trace(String regra) {
        if (!DEBUG) return;
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) indent.append("  ");
        System.out.println("[PARSER] " + indent + "-> " + regra +
            "   (token atual: " + peek() + ")");
    }

    // ---- Ponto de entrada -------------------------------------------------

    // program -> declaration* EOF
    List<Stmt> parse() {
        if (DEBUG) System.out.println("=== [PARSER] Iniciando analise sintatica ===");
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        if (DEBUG) System.out.println("=== [PARSER] Analise concluida: " +
            statements.size() + " comando(s) de topo ===");
        return statements;
    }

    // ---- Declarations -------------------------------------------------

    // declaration -> funDecl | varDecl | statement
    private Stmt declaration() {
        trace("declaration");
        depth++;
        try {
            if (match(FUN)) { depth--; return function("funcao"); }
            if (match(VAR)) { depth--; return varDeclaration(); }
            Stmt s = statement();
            depth--;
            return s;
        } catch (ParseError error) {
            depth--;
            synchronize();
            return null;
        }
    }

    // funDecl -> "fun" IDENTIFIER "(" parameters? ")" block
    private Stmt.Function function(String kind) {
        trace("function (" + kind + ")");
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

    // Igual a function(), porém SEM NOME - usada quando "fun (...) {...}"
    // aparece em posição de EXPRESSÃO (função anônima / lambda), em vez
    // de posição de DECLARAÇÃO. Repare que "fun" sozinho no início de um
    // statement é sempre uma DECLARAÇÃO (tratado em declaration()); "fun"
    // dentro de uma expressão (ex: "var f = fun(x) {...};") é uma lambda.
    // Essa distinção nunca é ambígua porque o parser já está em contextos
    // sintáticos diferentes (statement vs expression) - lookahead de 1
    // token (o "fun") continua bastando.
    private Expr functionExpression() {
        trace("functionExpression (lambda)");
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

    // varDecl -> "var" IDENTIFIER ( "=" expression )? ";"
    private Stmt varDeclaration() {
        trace("varDeclaration");
        Token name = consume(IDENTIFIER, "Esperado o nome da variavel.");
        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Esperado ';' depois da declaracao da variavel.");
        return new Stmt.Var(name, initializer);
    }

    // ---- Statements -------------------------------------------------

    // statement -> exprStmt | forStmt | ifStmt | printStmt | returnStmt
    //            | whileStmt | block
    private Stmt statement() {
        trace("statement");
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(RETURN)) return returnStatement();
        if (match(WHILE)) return whileStatement();
        if (match(LEFT_BRACE)) {
            depth++;
            List<Stmt> stmts = block();
            depth--;
            return new Stmt.Block(stmts);
        }
        return expressionStatement();
    }

    // returnStmt -> "return" expression? ";"
    private Stmt returnStatement() {
        trace("returnStatement");
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }
        consume(SEMICOLON, "Esperado ';' depois do valor de retorno.");
        return new Stmt.Return(keyword, value);
    }

    // forStmt -> "for" "(" ( varDecl | exprStmt | ";" )
    //                      expression? ";"
    //                      expression? ")" statement
    //
    // IMPORTANTE (desugaring / "açucar sintático"): NÃO existe um
    // Stmt.For na AST. O "for" é inteiramente reescrito aqui, em tempo
    // de parsing, como um "while" equivalente usando os nós que já
    // existem (Block, While, Expression). Isso é uma técnica clássica:
    // em vez de dar suporte especial a cada construção sintática em
    // TODAS as fases seguintes (interpretador, resolvedor de nomes...),
    // reduzimos "for" a algo que essas fases já sabem tratar.
    private Stmt forStatement() {
        trace("forStatement (desnaturado em while)");
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

        // Monta de dentro para fora: primeiro anexa o incremento ao
        // final do corpo, depois embrulha em While, depois antepõe o
        // inicializador.
        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }
        if (condition == null) condition = new Expr.Literal(true); // "for (;;)" = laço infinito
        body = new Stmt.While(condition, body);
        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }
        return body;
    }

    // ifStmt -> "if" "(" expression ")" statement ( "else" statement )?
    private Stmt ifStatement() {
        trace("ifStatement");
        consume(LEFT_PAREN, "Esperado '(' depois de 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Esperado ')' depois da condicao do if.");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) elseBranch = statement();
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    // printStmt -> "print" expression ";"
    private Stmt printStatement() {
        trace("printStatement");
        Expr value = expression();
        consume(SEMICOLON, "Esperado ';' depois do valor.");
        return new Stmt.Print(value);
    }

    // whileStmt -> "while" "(" expression ")" statement
    private Stmt whileStatement() {
        trace("whileStatement");
        consume(LEFT_PAREN, "Esperado '(' depois de 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Esperado ')' depois da condicao.");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    // exprStmt -> expression ";"
    private Stmt expressionStatement() {
        trace("expressionStatement");
        Expr expr = expression();
        consume(SEMICOLON, "Esperado ';' depois da expressao.");
        return new Stmt.Expression(expr);
    }

    // block -> "{" declaration* "}"
    private List<Stmt> block() {
        trace("block");
        List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Esperado '}' depois do bloco.");
        return statements;
    }

    // ---- Expressions, da menor para a maior precedencia -------------------

    // expression -> assignment
    private Expr expression() {
        trace("expression");
        return assignment();
    }

    // assignment -> ( call "." )? IDENTIFIER "=" assignment | logic_or
    //
    // Truque clássico do livro: em vez de tentar prever de antemão se
    // estamos vendo uma atribuição, o parser primeiro analisa o lado
    // esquerdo como se fosse uma expressão normal (or()) e SÓ DEPOIS,
    // se encontrar um "=", reinterpreta essa árvore já construída como
    // um "alvo" de atribuição (Variable -> Assign, Get -> Set).
    private Expr assignment() {
        trace("assignment");
        depth++;
        Expr expr = or();
        depth--;

        if (match(EQUAL)) {
            Token equals = previous();
            depth++;
            Expr value = assignment(); // associatividade à direita: a = b = c
            depth--;

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

    // logic_or -> logic_and ( "or" logic_and )*
    private Expr or() {
        trace("or");
        depth++;
        Expr expr = and();
        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        depth--;
        return expr;
    }

    // logic_and -> equality ( "and" equality )*
    private Expr and() {
        trace("and");
        depth++;
        Expr expr = equality();
        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        depth--;
        return expr;
    }

    // equality -> comparison ( ( "!=" | "==" ) comparison )*
    private Expr equality() {
        trace("equality");
        depth++;
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        depth--;
        return expr;
    }

    // comparison -> term ( ( ">" | ">=" | "<" | "<=" ) term )*
    private Expr comparison() {
        trace("comparison");
        depth++;
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        depth--;
        return expr;
    }

    // term -> factor ( ( "-" | "+" ) factor )*
    private Expr term() {
        trace("term");
        depth++;
        Expr expr = factor();
        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        depth--;
        return expr;
    }

    // factor -> unary ( ( "/" | "*" ) unary )*
    private Expr factor() {
        trace("factor");
        depth++;
        Expr expr = unary();
        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        depth--;
        return expr;
    }

    // unary -> ( "!" | "-" ) unary | call
    private Expr unary() {
        trace("unary");
        if (match(BANG, MINUS)) {
            Token operator = previous();
            depth++;
            Expr right = unary(); // recursão: permite "!!x", "--x", etc.
            depth--;
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    // call -> primary ( "(" arguments? ")" | "." IDENTIFIER )*
    // Permite encadear chamadas e acessos: obj.metodo().outraCoisa
    private Expr call() {
        trace("call");
        depth++;
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
        depth--;
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

    // primary -> NUMBER | STRING | "true" | "false" | "nil" | "this"
    //          | IDENTIFIER | "(" expression ")"
    //          | "fun" "(" parameters? ")" block
    //          | "{" objectLiteral "}"
    //
    // Esta é a regra "folha" da gramática de expressões - toda a
    // recursão de equality/comparison/term/factor/unary/call acaba
    // aqui, no caso base. Note que cada alternativa é escolhida por
    // um ÚNICO token de lookahead (LL(1)) - nunca há ambiguidade.
    private Expr primary() {
        trace("primary");
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(THIS)) return new Expr.This(previous());

        // "fun" aqui dentro de uma expressão = função anônima (lambda).
        if (match(FUN)) return functionExpression();

        // "{" aqui dentro de uma expressão = literal de objeto.
        // Não há ambiguidade com bloco porque statement() já intercepta
        // LEFT_BRACE ANTES de qualquer expressão ser tentada - "{" só
        // chega até aqui quando já estamos dentro do contexto de uma
        // expressão (ex: "var obj = { ... };").
        if (match(LEFT_BRACE)) return objectLiteral();

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            depth++;
            Expr expr = expression();
            depth--;
            consume(RIGHT_PAREN, "Esperado ')' depois da expressao.");
            return new Expr.Grouping(expr);
        }

        // Nenhuma alternativa bateu -> erro de sintaxe.
        throw error(peek(), "Esperada uma expressao.");
    }

    // objectLiteral -> ( IDENTIFIER ":" expression ( "," IDENTIFIER ":" expression )* )?
    private Expr objectLiteral() {
        trace("objectLiteral");
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

    // ---- Utilitários de baixo nivel do parser -------------------------------------------------
    // Estes são os únicos métodos que de fato tocam a lista de tokens.
    // Tudo acima é construído só a partir destes primitivos.

    // Se o token atual for de algum dos tipos dados, CONSOME e retorna
    // true. Senão, não mexe em nada e retorna false. É a base de todo
    // o "lookahead de 1" do parser.
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    // Como match(), mas EXIGE que bata - se não bater, é erro de sintaxe.
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    // Olha o tipo do token atual SEM consumir.
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

    // Recuperação de erro: depois de um erro de sintaxe, o parser não
    // para tudo - ele "sincroniza", descartando tokens até um ponto
    // razoável para recomeçar (início de um novo statement). Isso
    // permite reportar VÁRIOS erros de uma vez em vez de parar no
    // primeiro, que é bem melhor para quem está programando.
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

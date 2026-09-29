package ka;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ka.TokenType.*;

/*
 * ==========================================================================
 *  SCANNER (ANÁLISE LÉXICA)
 * ==========================================================================
 * Responsabilidade: transformar uma String bruta (o código fonte) em uma
 * List<Token>. É a primeira etapa do interpretador.
 *
 * Estratégia: NÃO usamos autômatos gerados a partir de expressões
 * regulares (o clássico algoritmo de Thompson regex -> NFA -> DFA que
 * ferramentas tipo lex/flex usam). Em vez disso, escrevemos um scanner
 * "à mão", com um laço e um switch sobre o caractere atual. Isso é, na
 * prática, um autômato de estados finitos IMPLÍCITO, codificado
 * diretamente como fluxo de controle Java. Mais simples de entender e
 * debugar, e rápido o suficiente para os nossos propósitos.
 *
 * O Scanner varre o texto UMA VEZ, da esquerda para a direita, mantendo
 * três ponteiros:
 *   start   - onde o lexema atual começou
 *   current - o próximo caractere ainda não consumido
 *   line    - em que linha estamos (para mensagens de erro)
 * ==========================================================================
 */
class Scanner {

    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;

    // Ativa impressão de cada passo do scanner (caractere a caractere
    // e token a token). Controlado externamente por Ka.DEBUG_SCANNER.
    static boolean DEBUG = false;

    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("and",    AND);
        keywords.put("else",   ELSE);
        keywords.put("false",  FALSE);
        keywords.put("for",    FOR);
        keywords.put("fun",    FUN);
        keywords.put("if",     IF);
        keywords.put("nil",    NIL);
        keywords.put("or",     OR);
        keywords.put("print",  PRINT);
        keywords.put("return", RETURN);
        keywords.put("this",   THIS);
        keywords.put("true",   TRUE);
        keywords.put("var",    VAR);
        keywords.put("while",  WHILE);
        // Sem "class" nem "super" - decisão de design: Ka usa
        // protótipos (Object.create), não classes.
    }

    Scanner(String source) {
        this.source = source;
    }

    // Ponto de entrada: consome todo o "source" e devolve a lista de tokens.
    List<Token> scanTokens() {
        if (DEBUG) {
            System.out.println("=== [SCANNER] Iniciando varredura lexica ===");
            System.out.println("Codigo fonte (" + source.length() + " caracteres):");
            System.out.println("----------------------------------------");
            System.out.println(source);
            System.out.println("----------------------------------------");
        }

        while (!isAtEnd()) {
            // No começo de cada iteração estamos no início do próximo lexema.
            start = current;
            scanToken();
        }

        // Token sentinela de fim de arquivo. Simplifica o Parser: ele
        // nunca precisa de um caso especial para "acabaram os tokens".
        tokens.add(new Token(EOF, "", null, line));

        if (DEBUG) {
            System.out.println("=== [SCANNER] Varredura concluida: " + tokens.size() + " tokens ===");
        }

        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    // Reconhece UM lexema a partir da posição "start" / "current".
    private void scanToken() {
        char c = advance();

        if (DEBUG) {
            System.out.println("[linha " + line + "] le caractere '" +
                (c == '\n' ? "\\n" : c) + "'");
        }

        switch (c) {
            // --- símbolos de um caractere ---
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case ':': addToken(COLON); break;

            // --- símbolos que podem virar dois caracteres ---
            // match('=') espia o PRÓXIMO caractere sem "gastar" nada se
            // não bater - é o lookahead de 1 caractere do scanner.
            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;

            // --- barra: pode ser divisão OU comentário de linha ---
            case '/':
                if (match('/')) {
                    if (DEBUG) System.out.println("        -> comentario de linha, ignorando ate '\\n'");
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;

            // --- espaços em branco: não geram token, só são pulados ---
            case ' ':
            case '\r':
            case '\t':
                if (DEBUG) System.out.println("        -> espaco em branco, ignorado");
                break;
            case '\n':
                line++;
                if (DEBUG) System.out.println("        -> quebra de linha, agora na linha " + line);
                break;

            // --- literais de string ---
            case '"': string(); break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    // Caractere que não pertence a Ka. Reporta erro mas
                    // CONTINUA escaneando (não trava o compilador inteiro
                    // por causa de um caractere ruim).
                    Ka.error(line, "Caractere inesperado: '" + c + "'.");
                }
                break;
        }
    }

    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    // Olha o caractere atual SEM consumi-lo (lookahead de 1).
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    // Olha um caractere além do atual (lookahead de 2, usado para
    // distinguir "123" de "123.45" - só entra no "." se depois dele
    // vier um dígito).
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    // Consome o caractere atual SE ele for o esperado. Usado para
    // reconhecer operadores de dois caracteres (==, !=, <=, >=).
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    // Consome uma string entre aspas duplas: "texto".
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++; // Ka permite strings multi-linha.
            advance();
        }

        if (isAtEnd()) {
            Ka.error(line, "String nao foi fechada (faltou aspas).");
            return;
        }

        advance(); // consome a aspas de fechamento

        // O valor literal NÃO inclui as aspas (por isso start+1 / current-1).
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    // Consome um número: 123 ou 123.45 (sempre armazenado como Double).
    private void number() {
        while (isDigit(peek())) advance();

        // Parte fracionária, só se houver um dígito depois do ponto
        // (isso evita consumir o "." de uma chamada de método como "1.metodo()").
        if (peek() == '.' && isDigit(peekNext())) {
            advance(); // consome o '.'
            while (isDigit(peek())) advance();
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    // Consome um identificador (nome de variável/função) OU uma
    // palavra-chave - a diferença só é decidida NO FINAL, checando
    // o texto contra o mapa "keywords".
    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER; // não é palavra reservada

        addToken(type);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        Token token = new Token(type, text, literal, line);
        tokens.add(token);

        if (DEBUG) {
            System.out.println("        -> TOKEN gerado: " + token);
        }
    }
}

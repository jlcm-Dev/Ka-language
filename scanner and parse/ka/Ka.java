package ka;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/*
 * ==========================================================================
 *  Ka - Ponto de entrada (versao "so scanner + parser" para apresentacao)
 * ==========================================================================
 * Esta versao do interpretador FAZ DE PROPOSITO só duas coisas:
 *   1) Analise LEXICA  (Scanner: texto -> tokens)
 *   2) Analise SINTATICA (Parser: tokens -> arvore de sintaxe / AST)
 *
 * Ela NAO interpreta/executa o programa (não há Interpreter.java nem
 * Resolver.java aqui) - isso é conteúdo da próxima etapa do projeto.
 * O objetivo é deixar essas duas primeiras fases bem visíveis e
 * bem instrumentadas com saída de debug, para fins de apresentação.
 *
 * MODO DE USO:
 *   java Ka arquivo.ka              -> roda normalmente
 *   java Ka arquivo.ka --tokens     -> mostra a lista de tokens gerada
 *   java Ka arquivo.ka --trace      -> mostra o rastro do scanner E do parser
 *                                      (cada caractere, cada regra gramatical)
 *   java Ka arquivo.ka --arvore     -> mostra a AST final, indentada
 *   java Ka arquivo.ka --tudo       -> ativa tokens + trace + arvore juntos
 *   java Ka                         -> modo REPL (interativo)
 *   java Ka --tudo                  -> modo REPL com debug completo
 * ==========================================================================
 */
public class Ka {

    static boolean hadError = false;

    // Flags de depuração, controladas pelos argumentos de linha de comando.
    private static boolean mostrarTokens = false;
    private static boolean mostrarArvore = true; // ligado por padrão: é o produto final desta etapa

    public static void main(String[] args) throws IOException {
        String caminhoArquivo = null;

        for (String arg : args) {
            switch (arg) {
                case "--tokens":
                    mostrarTokens = true;
                    break;
                case "--trace":
                    Scanner.DEBUG = true;
                    Parser.DEBUG = true;
                    break;
                case "--arvore":
                    mostrarArvore = true;
                    break;
                case "--tudo":
                    mostrarTokens = true;
                    mostrarArvore = true;
                    Scanner.DEBUG = true;
                    Parser.DEBUG = true;
                    break;
                default:
                    caminhoArquivo = arg;
            }
        }

        if (caminhoArquivo != null) {
            runFile(caminhoArquivo);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if (hadError) System.exit(65);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        System.out.println("Ka REPL - modo scanner + parser (sem execucao)");
        System.out.println("Use --tudo ao iniciar para ver todo o rastro de debug.");
        for (;;) {
            System.out.print("ka> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    // Orquestra as duas fases: scan -> parse -> (opcionalmente) imprime.
    private static void run(String source) {
        // ---------- FASE 1: ANALISE LEXICA ----------
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        if (mostrarTokens) {
            System.out.println("--- TOKENS (" + tokens.size() + ") ---");
            for (Token token : tokens) {
                System.out.println("  " + token);
            }
        }

        if (hadError) return; // erro lexico -> nem tenta parsear

        // ---------- FASE 2: ANALISE SINTATICA ----------
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        if (hadError) return; // erro sintatico -> nao imprime arvore incompleta/invalida

        if (mostrarArvore) {
            new ImpressoraAst().print(statements);
        }
    }

    // ---------- Relato de erros ----------
    // Usado pelo Scanner (erro léxico: caractere inválido, string não fechada)
    static void error(int line, String message) {
        report(line, "", message);
    }

    // Usado pelo Parser (erro sintático: token inesperado)
    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " no fim do arquivo", message);
        } else {
            report(token.line, " em '" + token.lexeme + "'", message);
        }
    }

    private static void report(int line, String where, String message) {
        System.err.println("[linha " + line + "] Erro" + where + ": " + message);
        hadError = true;
    }
}

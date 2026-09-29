# Ka - Scanner + Parser (versão para apresentação)

Esta é uma versão **reduzida e fortemente instrumentada** do interpretador
Ka, contendo apenas as duas primeiras fases de um compilador/interpretador:

1. **Análise léxica** (`Scanner.java`) — transforma o texto em uma lista de `Token`
2. **Análise sintática** (`Parser.java`) — transforma os tokens em uma árvore de sintaxe (AST)

Não há execução (`Interpreter.java`) nem resolução de nomes (`Resolver.java`)
nesta versão — propositalmente, para focar a apresentação nessas duas etapas.

## Estrutura de arquivos

| Arquivo | Fase | O que faz |
|---|---|---|
| `TokenType.java` | Léxica | Enum com todos os tipos de token possíveis |
| `Token.java` | Léxica | Estrutura de dados de um token (tipo + lexema + valor + linha) |
| `Scanner.java` | Léxica | Percorre o código caractere a caractere e produz tokens |
| `Expr.java` | Sintática | Classes da AST para expressões (Binary, Unary, Call, Get, etc.) |
| `Stmt.java` | Sintática | Classes da AST para comandos (Var, If, While, Print, etc.) |
| `Parser.java` | Sintática | Recursive descent parser: tokens → AST |
| `ImpressoraAst.java` | Depuração | Imprime a AST de forma indentada e legível |
| `Ka.java` | — | Ponto de entrada, orquestra tudo |

## Como compilar

Pré-requisito: JDK instalado (`javac -version` deve funcionar).

```bash
javac -encoding UTF-8 -d . ka/*.java
```

Isso compila tudo dentro da pasta `ka/` (o pacote Java).

## Como rodar

```bash
# Rodar um arquivo .ka, mostrando só a arvore final (comportamento padrão)
java ka.Ka teste_simples.ka

# Mostrar também a lista de tokens gerada pelo scanner
java ka.Ka teste_simples.ka --tokens

# Mostrar o rastro DETALHADO: cada caractere lido pelo scanner e
# cada regra gramatical visitada pelo parser (ótimo para apresentação)
java ka.Ka teste_simples.ka --trace

# Ativar tudo de uma vez (tokens + trace + arvore)
java ka.Ka teste_simples.ka --tudo

# Modo REPL interativo (sem arquivo)
java ka.Ka
java ka.Ka --tudo
```

## Arquivos de teste incluídos

- `teste_simples.ka` — uma expressão aritmética simples, boa para
  mostrar precedência de operadores (`(2 + 3) * 4`)
- `teste_completo.ka` — objetos com protótipos, funções, `if/else`,
  `while`, acesso a propriedades encadeado

## Roteiro sugerido para a apresentação

1. **Mostrar o código fonte** de `teste_simples.ka`.
2. Rodar `java ka.Ka teste_simples.ka --tokens` e explicar a tabela de
   tokens: cada palavra virou um "pacotinho" com tipo + texto + valor + linha.
3. Rodar `java ka.Ka teste_simples.ka` (sem flags) e mostrar a árvore
   final: apontar que `*` está na raiz (executa por último / tem menor
   precedência que `+` dentro do grupo).
4. Rodar `java ka.Ka teste_simples.ka --trace` e mostrar o rastro do
   parser: a cascata de chamadas `expression -> assignment -> or -> and
   -> equality -> comparison -> term -> factor -> unary -> call -> primary`
   descendo até achar o número. Isso demonstra visualmente o que é um
   "recursive descent parser" **preditivo** — decide tudo olhando 1
   token à frente, nunca precisa desfazer decisões.
5. Rodar com `teste_completo.ka` para mostrar que a mesma técnica dá
   conta de uma gramática bem mais rica (objetos, funções, controle de
   fluxo), sem mudar a estratégia.
6. (Opcional) Rodar um arquivo com erro de sintaxe proposital e mostrar
   que o parser reporta e **continua**, achando vários erros de uma vez
   (mecanismo de sincronização em `Parser.synchronize()`).

## Sobre a gramática implementada

```
program        -> declaration* EOF
declaration    -> funDecl | varDecl | statement
funDecl        -> "fun" IDENTIFIER "(" parameters? ")" block
varDecl        -> "var" IDENTIFIER ( "=" expression )? ";"
statement      -> exprStmt | forStmt | ifStmt | printStmt
                 | returnStmt | whileStmt | block
block          -> "{" declaration* "}"

expression     -> assignment
assignment     -> ( call "." )? IDENTIFIER "=" assignment | logic_or
logic_or       -> logic_and ( "or" logic_and )*
logic_and      -> equality ( "and" equality )*
equality       -> comparison ( ( "!=" | "==" ) comparison )*
comparison     -> term ( ( ">" | ">=" | "<" | "<=" ) term )*
term           -> factor ( ( "-" | "+" ) factor )*
factor         -> unary ( ( "/" | "*" ) unary )*
unary          -> ( "!" | "-" ) unary | call
call           -> primary ( "(" arguments? ")" | "." IDENTIFIER )*
primary        -> NUMBER | STRING | "true" | "false" | "nil" | "this"
                 | IDENTIFIER | "(" expression ")"
                 | "fun" "(" parameters? ")" block        (lambda)
                 | "{" objectLiteral "}"
```

O parser é **top-down** (desce da regra mais geral até a mais específica)
e **preditivo** (LL(1): decide cada alternativa olhando só o token atual,
sem nunca precisar de backtracking).

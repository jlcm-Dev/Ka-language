package ka;

/*
 * TokenType enumera TODAS as categorias de "palavra" que o Scanner
 * consegue reconhecer no código fonte de Ka.
 *
 * Cada valor aqui é um "tipo" de lexema. Por exemplo, os caracteres
 * "+" e "print" e "123" viram, respectivamente, os tipos PLUS, PRINT
 * e NUMBER. O tipo é o que permite ao Parser, mais tarde, decidir
 * qual regra gramatical aplicar SEM precisar reexaminar o texto cru.
 */
enum TokenType {
    // --- Um único caractere ---
    LEFT_PAREN, RIGHT_PAREN,   // ( )
    LEFT_BRACE, RIGHT_BRACE,   // { }
    COMMA, DOT, MINUS, PLUS,   // , . - +
    SEMICOLON, SLASH, STAR,    // ; / *
    COLON,                     // :  (usado em literais de objeto { chave: valor })

    // --- Um ou dois caracteres (dependem do próximo caractere) ---
    BANG, BANG_EQUAL,          // !  !=
    EQUAL, EQUAL_EQUAL,        // =  ==
    GREATER, GREATER_EQUAL,    // >  >=
    LESS, LESS_EQUAL,          // <  <=

    // --- Literais (têm um valor associado além do texto) ---
    IDENTIFIER,   // nomes de variáveis/funções, ex: idade, calcular
    STRING,       // "texto entre aspas"
    NUMBER,       // 123, 45.67

    // --- Palavras reservadas ---
    // OBS: propositalmente NÃO existe CLASS nem SUPER - Ka não tem
    // sintaxe de classes, usa protótipos (Object.create) em vez disso.
    AND, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, THIS, TRUE, VAR, WHILE,

    // --- Fim de arquivo ---
    // O Scanner sempre adiciona um token EOF ao final da lista.
    // Isso simplifica MUITO o Parser: ele nunca precisa checar
    // "acabaram os tokens?" com um caso especial - EOF já é um
    // token normal que as regras de parsing tratam como "não bati
    // com nenhuma alternativa válida".
    EOF
}

package com.craftinginterpreters.ka;

enum TokenType {
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR, COLON,

    BANG, BANG_EQUAL,  
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    IDENTIFIER, STRING, NUMBER,

    AND, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, THIS, TRUE, VAR, WHILE,
    EOF
}
//estender o livro para aproximar/melhorar a nossa linguagem consolidadas.
//switch case,  dual. 
//number -> int, float.

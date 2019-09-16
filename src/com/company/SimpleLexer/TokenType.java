package com.company.SimpleLexer;

public enum TokenType {
    Identifier,
    IntLiteral,

    GE, // >=
    GT, // >
    EQ, // ==
    LE, // <=
    LT, // <

    SemiColon, // ;
    LeftParen, // (
    RightParen, // )

    Assignment, // =

    Int,

    Plus,
    Minus,
    Star,
    Slash;
}

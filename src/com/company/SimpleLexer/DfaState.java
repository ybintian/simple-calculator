package com.company.SimpleLexer;

public enum DfaState {
    Initial,
    Id,

    IntLiteral,

    Assignment,

    GT,
    GE,

    SemiColon,
    LeftParen,
    RightParen,

    Id_int1,
    Id_int2,
    Id_int3,

    Plus,
    Minus,
    Star,
    Slash;
}

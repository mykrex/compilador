package com.babyduck.semantic;

/**
 * Operadores que consulta el cubo semántico
 */
public enum Op {
    PLUS("+"),
    MINUS("-"),
    MULT("*"),
    DIV("/"),
    GT(">"),
    LT("<"),
    EQ("=="),
    NEQ("!="),
    ASSIGN("=");

    private final String symbol;

    Op(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    /** Convierte el texto del token ("+", "==", ...) en el operador. */
    public static Op fromSymbol(String symbol) {
        for (Op op : values()) {
            if (op.symbol.equals(symbol)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Operador desconocido: " + symbol);
    }
}
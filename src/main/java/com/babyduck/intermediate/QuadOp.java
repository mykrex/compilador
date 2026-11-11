package com.babyduck.intermediate;

import com.babyduck.semantic.Op;

/**
 * Operadores de los cuádruplos (instrucciones del código intermedio)
 *
 * Es un enum aparte de {@link Op}: Op son los operadores del lenguaje que consulta
 * el cubo semántico; QuadOp son las instrucciones que ejecutará la máquina virtual
 *
 * Cada operador tiene un código numérico fijo, escrito a mano (no se usa ordinal(),
 * que cambiaría si alguien reordena el enum). En memoria el cuádruplo guarda el enum;
 * el código se usará al escribir el archivo que lee la máquina virtual
 *
 * Los códigos 15 en adelante quedan para los saltos y las funciones (entregas siguientes)
 */
public enum QuadOp {
    PLUS(1, "+"),
    MINUS(2, "-"),
    MULT(3, "*"),
    DIV(4, "/"),
    GT(5, ">"),
    LT(6, "<"),
    EQ(7, "=="),
    NEQ(8, "!="),
    ASSIGN(9, "="),
    UMINUS(10, "UMINUS"),
    PRINT(11, "PRINT"),
    PRINTLN(12, "PRINTLN"),
    READ(13, "READ"),
    END(14, "END");

    private final int code;
    private final String symbol;

    QuadOp(int code, String symbol) {
        this.code = code;
        this.symbol = symbol;
    }

    /** Código numérico fijo del operador */
    public int code() {
        return code;
    }

    /** Texto con el que se imprime en la fila */
    public String symbol() {
        return symbol;
    }

    /** Instrucción que corresponde a un operador binario del lenguaje (+ - * / > < == != =) */
    public static QuadOp from(Op op) {
        return switch (op) {
            case PLUS -> PLUS;
            case MINUS -> MINUS;
            case MULT -> MULT;
            case DIV -> DIV;
            case GT -> GT;
            case LT -> LT;
            case EQ -> EQ;
            case NEQ -> NEQ;
            case ASSIGN -> ASSIGN;
        };
    }
}

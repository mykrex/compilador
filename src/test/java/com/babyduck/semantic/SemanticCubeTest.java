package com.babyduck.semantic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del cubo semántico. Cada fila de {@code @CsvSource} es una entrada de la
 * "Tabla de consideraciones semánticas" de la documentación.
 */
class SemanticCubeTest {

    private final SemanticCube cube = new SemanticCube();

    @ParameterizedTest(name = "{0} {1} {2} -> {3}")
    @CsvSource({
            // ---- Aritmética + - *
            "INT,   PLUS,  INT,   INT",
            "INT,   MINUS, FLOAT, FLOAT",
            "FLOAT, MULT,  INT,   FLOAT",
            "FLOAT, PLUS,  FLOAT, FLOAT",
            // ---- División siempre float
            "INT,   DIV,   INT,   FLOAT",
            "FLOAT, DIV,   INT,   FLOAT",
            // ---- Relacionales
            "INT,   LT,    INT,   BOOL",
            "INT,   GT,    FLOAT, BOOL",
            "FLOAT, EQ,    INT,   BOOL",
            "FLOAT, NEQ,   FLOAT, BOOL",
            // ---- Asignación destino <- valor
            "INT,   ASSIGN, INT,   INT",
            "FLOAT, ASSIGN, INT,   FLOAT",
            "FLOAT, ASSIGN, FLOAT, FLOAT",
            "INT,   ASSIGN, FLOAT, ERROR",
            // ---- BOOL no participa en aritmética, comparación ni asignación
            "BOOL,  PLUS,  INT,   ERROR",
            "INT,   MULT,  BOOL,  ERROR",
            "BOOL,  EQ,    BOOL,  ERROR",
            "INT,   ASSIGN, BOOL, ERROR",
            // ---- VOID nunca es operando válido
            "VOID,  PLUS,  INT,   ERROR",
            "INT,   LT,    VOID,  ERROR",
            "INT,   ASSIGN, VOID, ERROR",
            // ---- ERROR se propaga
            "ERROR, PLUS,  INT,   ERROR"
    })
    void binaryRules(Type left, Op op, Type right, Type expected) {
        assertEquals(expected, cube.result(left, op, right));
    }

    @ParameterizedTest(name = "{0} {1} -> {2}")
    @CsvSource({
            "MINUS, INT,   INT",
            "MINUS, FLOAT, FLOAT",
            "PLUS,  INT,   INT",
            "MINUS, BOOL,  ERROR",
            "MINUS, VOID,  ERROR",
            "MULT,  INT,   ERROR"   // el * no es operador unario
    })
    void unaryRules(Op op, Type operand, Type expected) {
        assertEquals(expected, cube.unaryResult(op, operand));
    }

    @Test
    void canAssignFollowsCube() {
        assertTrue(cube.canAssign(Type.FLOAT, Type.INT));
        assertFalse(cube.canAssign(Type.INT, Type.FLOAT));
    }

    /** Todas las operaciones de expresión son simétricas en tipos, la asignación no */
    @Test
    void expressionOperatorsAreTypeSymmetric() {
        for (Op op : Op.values()) {
            if (op == Op.ASSIGN) continue;
            for (Type a : Type.values()) {
                for (Type b : Type.values()) {
                    assertEquals(cube.result(a, op, b), cube.result(b, op, a),
                            () -> a + " " + op + " " + b);
                }
            }
        }
        assertNotEquals(cube.result(Type.INT, Op.ASSIGN, Type.FLOAT),
                cube.result(Type.FLOAT, Op.ASSIGN, Type.INT));
    }

    /** Solo las combinaciones numéricas son válidas: 9 operadores × 5 × 5 = 225 casillas. */
    @Test
    void onlyNumericCombinationsAreValid() {
        int valid = 0;
        for (Type a : Type.values())
            for (Op op : Op.values())
                for (Type b : Type.values())
                    if (cube.result(a, op, b) != Type.ERROR) valid++;
        // 8 operadores de expresión × 4 combinaciones numéricas + 3 asignaciones válidas
        assertEquals(8 * 4 + 3, valid);
    }

    @Test
    void symbolAndKeywordConversions() {
        assertEquals(Op.EQ, Op.fromSymbol("=="));
        assertEquals(Op.ASSIGN, Op.fromSymbol("="));
        assertEquals(Type.FLOAT, Type.fromKeyword("float"));
        assertEquals(Type.VOID, Type.fromKeyword("void"));
        assertThrows(IllegalArgumentException.class, () -> Type.fromKeyword("bool"));
    }
}
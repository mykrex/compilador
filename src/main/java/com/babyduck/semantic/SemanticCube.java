package com.babyduck.semantic;

import java.util.EnumMap;
import java.util.Map;

/**
 * Cubo semántico
 *
 * Estructura: {@code EnumMap<Type, EnumMap<Op, EnumMap<Type, Type>>>}, es decir
 * tipo izquierdo → operador → tipo derecho → tipo resultante
 *
 * Cualquier combinación que no esté registrada devuelve {@link Type#ERROR}; así,
 * {@code BOOL} en aritmética y {@code VOID} en cualquier expresión quedan rechazados
 * sin escribir reglas adicionales
 *
 * Para {@code ASSIGN}, el tipo izquierdo es el 'destino' y el derecho el 'valor';
 * si la asignación es válida, el resultado es el tipo del destino
 */
public final class SemanticCube {

    private final Map<Type, Map<Op, Map<Type, Type>>> binary = new EnumMap<>(Type.class);
    private final Map<Op, Map<Type, Type>> unary = new EnumMap<>(Op.class);

    public SemanticCube() {
        fillArithmetic();
        fillRelational();
        fillAssignment();
        fillUnary();
    }

    // Consultas

    /** Tipo resultante de {@code left op right} o {@link Type#ERROR} si no es válido */
    public Type result(Type left, Op op, Type right) {
        Map<Op, Map<Type, Type>> byOp = binary.get(left);
        if (byOp == null) return Type.ERROR;
        Map<Type, Type> byRight = byOp.get(op);
        if (byRight == null) return Type.ERROR;
        return byRight.getOrDefault(right, Type.ERROR);
    }

    /** Tipo resultante de un signo unario ({@code +x}, {@code -x}) */
    public Type unaryResult(Op op, Type operand) {
        Map<Type, Type> byOperand = unary.get(op);
        if (byOperand == null) return Type.ERROR;
        return byOperand.getOrDefault(operand, Type.ERROR);
    }

    /** {@code true} si un valor de tipo {@code value} puede guardarse en un destino de tipo {@code target} */
    public boolean canAssign(Type target, Type value) {
        return result(target, Op.ASSIGN, value) != Type.ERROR;
    }

    // Llenado

    /** { + - * } conservan int con int; { / } devuelve float */
    private void fillArithmetic() {
        for (Op op : new Op[]{Op.PLUS, Op.MINUS, Op.MULT}) {
            put(Type.INT, op, Type.INT, Type.INT);
            put(Type.INT, op, Type.FLOAT, Type.FLOAT);
            put(Type.FLOAT, op, Type.INT, Type.FLOAT);
            put(Type.FLOAT, op, Type.FLOAT, Type.FLOAT);
        }
        put(Type.INT, Op.DIV, Type.INT, Type.FLOAT);
        put(Type.INT, Op.DIV, Type.FLOAT, Type.FLOAT);
        put(Type.FLOAT, Op.DIV, Type.INT, Type.FLOAT);
        put(Type.FLOAT, Op.DIV, Type.FLOAT, Type.FLOAT);
    }

    /** Cualquier combinación numérica produce bool, bool no se compara */
    private void fillRelational() {
        for (Op op : new Op[]{Op.GT, Op.LT, Op.EQ, Op.NEQ}) {
            put(Type.INT, op, Type.INT, Type.BOOL);
            put(Type.INT, op, Type.FLOAT, Type.BOOL);
            put(Type.FLOAT, op, Type.INT, Type.BOOL);
            put(Type.FLOAT, op, Type.FLOAT, Type.BOOL);
        }
    }

    /** Asignación, destino ← valor. Se permite int a float, no se permite float a int */
    private void fillAssignment() {
        put(Type.INT, Op.ASSIGN, Type.INT, Type.INT);
        put(Type.FLOAT, Op.ASSIGN, Type.FLOAT, Type.FLOAT);
        put(Type.FLOAT, Op.ASSIGN, Type.INT, Type.FLOAT);
    }

    /** El signo conserva el tipo numérico. */
    private void fillUnary() {
        for (Op op : new Op[]{Op.PLUS, Op.MINUS}) {
            Map<Type, Type> rules = new EnumMap<>(Type.class);
            rules.put(Type.INT, Type.INT);
            rules.put(Type.FLOAT, Type.FLOAT);
            unary.put(op, rules);
        }
    }

    private void put(Type left, Op op, Type right, Type result) {
        binary.computeIfAbsent(left, k -> new EnumMap<>(Op.class))
                .computeIfAbsent(op, k -> new EnumMap<>(Type.class))
                .put(right, result);
    }
}
package com.babyduck.intermediate;

/**
 * Un cuádruplo: (operador, operando izquierdo, operando derecho, resultado)
 *
 * El operador es un enum y los operandos son objetos; un campo que no aplica es null
 * (por ejemplo, el derecho de una asignación o los tres de END)
 */
public record Quadruple(QuadOp op, Operand left, Operand right, Operand result) {
}

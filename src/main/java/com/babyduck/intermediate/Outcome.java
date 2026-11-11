package com.babyduck.intermediate;

import com.babyduck.semantic.Op;
import com.babyduck.semantic.Type;

/**
 * Resultado de una operación del generador (binaria, signo o asignación)
 *
 * El generador detecta los errores pero no los reporta: no conoce el árbol ni el
 * ErrorCollector. SemanticAnalyzer revisa {@link #failed()} y arma el mensaje
 *
 * @param op     operador (para el signo: PLUS o MINUS; para la asignación: ASSIGN)
 * @param left   tipo izquierdo (destino en la asignación); null en el signo unario
 * @param right  tipo derecho (valor en la asignación; el operando en el signo)
 * @param result tipo resultante según el cubo
 * @param line   línea del operador (donde se reporta el error, si lo hay)
 * @param column columna del operador
 */
public record Outcome(Op op, Type left, Type right, Type result, int line, int column) {

    /**
     * true solo si el cubo rechazó una combinación de operandos válidos. Si algún
     * operando ya era ERROR, ese error ya se reportó y no se reporta otro
     */
    public boolean failed() {
        return result == Type.ERROR && left != Type.ERROR && right != Type.ERROR;
    }
}

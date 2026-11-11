package com.babyduck.intermediate;

import com.babyduck.semantic.Type;

/**
 * Lo que regresa {@link QuadrupleGenerator#popOperand()}: el operando y el tipo que
 * salió de la pila de tipos. El tipo de la pila es el que se usa para consultar el cubo
 */
public record TypedOperand(Operand operand, Type type) {

    public boolean isError() {
        return type == Type.ERROR;
    }
}

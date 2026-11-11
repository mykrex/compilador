package com.babyduck.intermediate;

import com.babyduck.semantic.Type;

/**
 * Operando de un cuádruplo: una variable, un temporal, una constante o un letrero
 *
 * Los cuádruplos guardan objetos Operand, no texto; el texto solo se usa al imprimir
 *
 * El constructor no es público: los operandos solo se crean con los métodos de fábrica
 * de {@link QuadrupleGenerator} (variable, constant, string, newTemp). Así, para la
 * memoria virtual basta con cambiar esos cuatro métodos para que cada operando reciba
 * su dirección, y el compilador de Java impide crear operandos por fuera de ellos
 *
 * El tipo guardado aquí es una copia: durante la generación, el tipo con el que se
 * consulta el cubo es el que sale de la pila de tipos
 */
public final class Operand {

    /** Marca de una expresión con error, ya reportado; evita errores en cadena */
    public static final Operand ERROR = new Operand(OperandKind.ERROR, "<error>", Type.ERROR, null);

    private final OperandKind kind;
    private final String name;
    private final Type type;
    private final Object value;

    /**
     * @param name  texto con el que se muestra: _X_, t1, -2, 3.14, "hola"
     * @param value valor ya convertido de una constante (Integer o Double) o el contenido
     *              de un letrero (sin comillas); null para variables y temporales
     */
    Operand(OperandKind kind, String name, Type type, Object value) {
        this.kind = kind;
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public OperandKind kind() {
        return kind;
    }

    public String name() {
        return name;
    }

    public Type type() {
        return type;
    }

    public Object value() {
        return value;
    }

    public boolean isError() {
        return kind == OperandKind.ERROR;
    }

    @Override
    public String toString() {
        return name;
    }
}

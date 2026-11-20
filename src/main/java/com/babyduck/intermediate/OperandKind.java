package com.babyduck.intermediate;

/**
 * Clase de un operando de los cuádruplos
 *
 * Las variables, los temporales, las constantes y los letreros tienen su propio rango
 * de direcciones virtuales (ver {@link VirtualMemory})
 */
public enum OperandKind {
    /** Variable o parámetro declarado por el programador */
    VARIABLE,
    /** Resultado intermedio creado por el compilador (t1, t2, ...) */
    TEMPORARY,
    /** Constante numérica escrita en el programa (42, -2, 3.14) */
    CONSTANT,
    /** Letrero de print ("hola") */
    STRING,
    /** Número de cuádruplo: destino de un GOTO o GOTOF (no es una dirección de memoria) */
    QUAD_INDEX,
    /** Nombre de una función: operando de ERA y GOSUB (no es una dirección de memoria) */
    FUNCTION,
    /** Número de parámetro de PARAM, desde 1 (no es una dirección de memoria) */
    PARAM_INDEX,
    /**
     * Marca interna de una expresión con error; nunca aparece en una fila entregada
     * (si hubo errores, la fila se descarta)
     */
    ERROR
}

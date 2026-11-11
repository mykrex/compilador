package com.babyduck.intermediate;

/**
 * Clase de un operando de los cuádruplos
 *
 * Con la memoria virtual cada clase tendrá su propio rango de direcciones
 * (variables globales/locales, temporales, constantes y letreros)
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
    /** Marca interna de una expresión con error; nunca aparece en un cuádruplo */
    ERROR
}

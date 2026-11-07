package com.babyduck.semantic;

/**
 * Tipos internos del compilador
 *
 * {@code INT}, {@code FLOAT}: tipos declarables por el programador
 * {@code BOOL}: solo existe como resultado de {@code > < == !=}; no se puede declarar
 * {@code VOID}: tipo de retorno de funciones sin valor; nunca es válido dentro de una expresión
 * {@code ERROR}: resultado de cualquier combinación inválida en el cubo semántico
 *
 */
public enum Type {
    INT,
    FLOAT,
    BOOL,
    VOID,
    ERROR;

    /**
     * Convierte la palabra reservada del código fuente ("int", "float", "void") en su tipo.
     * Se usará al recorrer {@code type} y {@code funcType} del árbol sintáctico.
     */
    public static Type fromKeyword(String keyword) {
        return switch (keyword) {
            case "int" -> INT;
            case "float" -> FLOAT;
            case "void" -> VOID;
            default -> throw new IllegalArgumentException("Tipo desconocido: " + keyword);
        };
    }

    /** Nombre como aparece en BabyDuck, para mensajes de error. */
    public String display() {
        return name().toLowerCase();
    }
}
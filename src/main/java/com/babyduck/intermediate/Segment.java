package com.babyduck.intermediate;

/**
 * Segmentos de la memoria virtual. Cada segmento tiene un rango de direcciones por tipo
 * (ver {@link VirtualMemory})
 */
public enum Segment {
    /** Variables globales (viven todo el programa) */
    GLOBAL("variables globales"),
    /** Parámetros y variables locales de la función actual */
    LOCAL("variables locales"),
    /** Temporales de la función actual o de main */
    TEMPORARY("temporales"),
    /** Constantes numéricas y letreros (una dirección por valor) */
    CONSTANT("constantes");

    private final String description;

    Segment(String description) {
        this.description = description;
    }

    /** Nombre en español, para los mensajes de error */
    public String description() {
        return description;
    }
}

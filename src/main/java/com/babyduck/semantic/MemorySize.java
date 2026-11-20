package com.babyduck.semantic;

/**
 * Cuántas direcciones de cada tipo usa un segmento (por ejemplo, las locales o los
 * temporales de una función). Con esto la máquina virtual crea en cada llamada solo la
 * memoria necesaria
 */
public record MemorySize(int ints, int floats, int bools) {

    public static final MemorySize EMPTY = new MemorySize(0, 0, 0);

    @Override
    public String toString() {
        return "int " + ints + ", float " + floats + ", bool " + bools;
    }
}

package com.babyduck.vm;

import com.babyduck.semantic.MemorySize;
import com.babyduck.semantic.Type;

import java.util.BitSet;

/**
 * Casillas de un segmento de la memoria de ejecución: un arreglo por tipo
 *
 * Cada arreglo tiene un BitSet que marca qué casillas ya tienen valor: escribir marca la
 * casilla y {@link #hasValue} permite detectar una lectura sin valor. Los letreros solo
 * existen en el bloque de constantes
 *
 * El bloque no conoce direcciones: recibe la casilla ya calculada (dirección - base)
 */
public final class MemoryBlock {

    private final int[] ints;
    private final double[] floats;
    private final boolean[] bools;
    private final String[] strings;

    private final BitSet intSet = new BitSet();
    private final BitSet floatSet = new BitSet();
    private final BitSet boolSet = new BitSet();
    private final BitSet stringSet = new BitSet();

    public MemoryBlock(int ints, int floats, int bools, int strings) {
        this.ints = new int[ints];
        this.floats = new double[floats];
        this.bools = new boolean[bools];
        this.strings = new String[strings];
    }

    /** Bloque del tamaño guardado en el directorio (sin letreros) */
    public static MemoryBlock of(MemorySize size) {
        return new MemoryBlock(size.ints(), size.floats(), size.bools(), 0);
    }

    /** true si la casilla de ese tipo ya recibió un valor */
    public boolean hasValue(Type type, int cell) {
        return switch (type) {
            case INT -> intSet.get(cell);
            case FLOAT -> floatSet.get(cell);
            case BOOL -> boolSet.get(cell);
            case STRING -> stringSet.get(cell);
            default -> throw new IllegalArgumentException("La memoria no guarda el tipo " + type);
        };
    }

    public int readInt(int cell) {
        return ints[cell];
    }

    public double readFloat(int cell) {
        return floats[cell];
    }

    public boolean readBool(int cell) {
        return bools[cell];
    }

    public String readString(int cell) {
        return strings[cell];
    }

    public void writeInt(int cell, int value) {
        ints[cell] = value;
        intSet.set(cell);
    }

    public void writeFloat(int cell, double value) {
        floats[cell] = value;
        floatSet.set(cell);
    }

    public void writeBool(int cell, boolean value) {
        bools[cell] = value;
        boolSet.set(cell);
    }

    public void writeString(int cell, String value) {
        strings[cell] = value;
        stringSet.set(cell);
    }
}

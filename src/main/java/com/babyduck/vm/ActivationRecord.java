package com.babyduck.vm;

import com.babyduck.semantic.MemorySize;

/**
 * Registro de activación: la memoria de una llamada
 *
 * Tiene un bloque para sus locales (parámetros incluidos) y otro para sus temporales, del
 * tamaño guardado en el directorio, y el número de cuádruplo al que se regresa al terminar.
 * El de main solo tiene temporales
 */
public final class ActivationRecord {

    private final MemoryBlock locals;
    private final MemoryBlock temps;
    private int returnQuad = -1;

    public ActivationRecord(MemorySize localSize, MemorySize tempSize) {
        this.locals = MemoryBlock.of(localSize);
        this.temps = MemoryBlock.of(tempSize);
    }

    public MemoryBlock locals() {
        return locals;
    }

    public MemoryBlock temps() {
        return temps;
    }

    /** Cuádruplo siguiente al GOSUB que creó la llamada */
    public int returnQuad() {
        return returnQuad;
    }

    public void setReturnQuad(int quad) {
        this.returnQuad = quad;
    }
}

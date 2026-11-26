package com.babyduck.vm;

import com.babyduck.intermediate.ConstantTable;
import com.babyduck.intermediate.Segment;
import com.babyduck.intermediate.VirtualMemory;
import com.babyduck.semantic.MemorySize;
import com.babyduck.semantic.Type;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Memoria de ejecución: traduce cada dirección virtual a una casilla real
 *
 * Bloques:
 *   global      un solo bloque, del tamaño del segmento global (variables y direcciones de regreso)
 *   constantes  un solo bloque, cargado desde la tabla de constantes antes de ejecutar
 *   pila        un registro de activación por llamada; la cima es la llamada actual
 *   pendiente   el registro que crea ERA y llenan los PARAM; GOSUB lo mete a la pila
 *
 * Traducción de una dirección: {@code dirección / 1000} da el segmento y el tipo
 * ({@link VirtualMemory#rangeOf}) y {@code dirección - base} la casilla. Las globales y las
 * constantes van a su bloque fijo; las locales y los temporales, al registro de la cima.
 * Por eso la misma dirección local (por ejemplo 3000) es una casilla distinta en cada llamada
 *
 * Solo recibe direcciones y tamaños: nunca nombres
 */
public final class ExecutionMemory {

    /** Llamadas anidadas permitidas (incluida main); más que esto es una recursión sin fin */
    public static final int MAX_CALLS = 10_000;

    private final MemoryBlock global;
    private final MemoryBlock constants;
    private final Deque<ActivationRecord> stack = new ArrayDeque<>();
    private ActivationRecord pending;

    /**
     * @param globalSize   tamaño del segmento global (del directorio)
     * @param mainTempSize temporales de main (del directorio)
     * @param table        tabla de constantes; cada valor se escribe en su dirección
     */
    public ExecutionMemory(MemorySize globalSize, MemorySize mainTempSize, ConstantTable table) {
        this.global = MemoryBlock.of(globalSize);
        this.constants = new MemoryBlock(table.size(Type.INT), table.size(Type.FLOAT), 0,
                table.size(Type.STRING));
        table.forEach(this::loadConstant);
        stack.push(new ActivationRecord(MemorySize.EMPTY, mainTempSize));
    }

    private void loadConstant(int address, Object value) {
        int cell = VirtualMemory.cellOf(address);
        switch (typeOf(address)) {
            case INT -> constants.writeInt(cell, (Integer) value);
            case FLOAT -> constants.writeFloat(cell, (Double) value);
            case STRING -> constants.writeString(cell, (String) value);
            default -> throw new IllegalStateException("Constante con tipo inválido en " + address);
        }
    }

    // Lectura y escritura por dirección

    /** Tipo de una dirección, según su rango */
    public Type typeOf(int address) {
        return range(address).type();
    }

    public int readInt(int address) {
        MemoryBlock block = blockToRead(address, Type.INT);
        return block.readInt(VirtualMemory.cellOf(address));
    }

    /** Valor numérico como float: una dirección int se promueve (int → float) */
    public double readFloat(int address) {
        if (typeOf(address) == Type.INT) {
            return readInt(address);
        }
        MemoryBlock block = blockToRead(address, Type.FLOAT);
        return block.readFloat(VirtualMemory.cellOf(address));
    }

    public boolean readBool(int address) {
        MemoryBlock block = blockToRead(address, Type.BOOL);
        return block.readBool(VirtualMemory.cellOf(address));
    }

    public String readString(int address) {
        MemoryBlock block = blockToRead(address, Type.STRING);
        return block.readString(VirtualMemory.cellOf(address));
    }

    public void writeInt(int address, int value) {
        blockToWrite(address, Type.INT, stack.peek()).writeInt(VirtualMemory.cellOf(address), value);
    }

    public void writeFloat(int address, double value) {
        blockToWrite(address, Type.FLOAT, stack.peek()).writeFloat(VirtualMemory.cellOf(address), value);
    }

    public void writeBool(int address, boolean value) {
        blockToWrite(address, Type.BOOL, stack.peek()).writeBool(VirtualMemory.cellOf(address), value);
    }

    // Llamadas

    /** ERA: crea el registro pendiente con el tamaño de la función */
    public void prepareCall(MemorySize localSize, MemorySize tempSize) {
        pending = new ActivationRecord(localSize, tempSize);
    }

    /**
     * PARAM: lee el argumento en la memoria actual y lo escribe en la dirección del parámetro
     * dentro del registro pendiente (con promoción int → float si el parámetro es float)
     */
    public void writeParam(int paramAddress, int argumentAddress) {
        if (pending == null) {
            throw new IllegalStateException("PARAM sin ERA");
        }
        int cell = VirtualMemory.cellOf(paramAddress);
        MemoryBlock target = blockToWrite(paramAddress, typeOf(paramAddress), pending);
        if (typeOf(paramAddress) == Type.INT) {
            target.writeInt(cell, readInt(argumentAddress));
        } else {
            target.writeFloat(cell, readFloat(argumentAddress));
        }
    }

    /**
     * GOSUB: guarda el cuádruplo de regreso en el registro pendiente y lo mete a la pila;
     * desde aquí las locales y los temporales son los de la llamada nueva
     */
    public void enterCall(int returnQuad) {
        if (pending == null) {
            throw new IllegalStateException("GOSUB sin ERA");
        }
        if (stack.size() >= MAX_CALLS) {
            throw new RuntimeError("se excedió el límite de " + MAX_CALLS + " llamadas anidadas");
        }
        pending.setReturnQuad(returnQuad);
        stack.push(pending);
        pending = null;
    }

    /** RETURN o ENDFUNC: borra la memoria de la llamada actual y regresa el cuádruplo de regreso */
    public int exitCall() {
        if (stack.size() <= 1) {
            throw new IllegalStateException("ENDFUNC fuera de una función");
        }
        return stack.pop().returnQuad();
    }

    /** Registros en la pila (main cuenta como 1) */
    public int depth() {
        return stack.size();
    }

    // Traducción de direcciones

    private VirtualMemory.Range range(int address) {
        VirtualMemory.Range range = VirtualMemory.rangeOf(address);
        if (range == null) {
            throw new IllegalStateException("Dirección fuera de los rangos: " + address);
        }
        return range;
    }

    /** Bloque donde vive la dirección: global, constantes, o locales/temporales del registro dado */
    private MemoryBlock block(VirtualMemory.Range range, ActivationRecord record) {
        return switch (range.segment()) {
            case GLOBAL -> global;
            case CONSTANT -> constants;
            case LOCAL -> record.locals();
            case TEMPORARY -> record.temps();
        };
    }

    /** Bloque para leer; si la casilla no tiene valor, error de ejecución */
    private MemoryBlock blockToRead(int address, Type expected) {
        VirtualMemory.Range range = range(address);
        checkType(address, range, expected);
        MemoryBlock block = block(range, stack.peek());
        if (!block.hasValue(expected, VirtualMemory.cellOf(address))) {
            throw new RuntimeError("la dirección " + address + " no tiene valor");
        }
        return block;
    }

    /** Bloque para escribir; las constantes no se escriben durante la ejecución */
    private MemoryBlock blockToWrite(int address, Type expected, ActivationRecord record) {
        VirtualMemory.Range range = range(address);
        checkType(address, range, expected);
        if (range.segment() == Segment.CONSTANT) {
            throw new IllegalStateException("No se puede escribir en la constante " + address);
        }
        return block(range, record);
    }

    /** Un tipo distinto al del rango es un error del compilador, no del programa */
    private static void checkType(int address, VirtualMemory.Range range, Type expected) {
        if (range.type() != expected) {
            throw new IllegalStateException("La dirección " + address + " es " + range.type().display()
                    + ", no " + expected.display());
        }
    }
}

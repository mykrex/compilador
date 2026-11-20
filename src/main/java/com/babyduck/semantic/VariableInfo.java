package com.babyduck.semantic;

/**
 * Una variable o un parámetro declarado, con su dirección virtual
 *
 * La dirección se asigna al declararla (global o local, según el alcance)
 */
public record VariableInfo(String name, Type type, int address) {

    /** Dirección sin asignar: solo la usan las pruebas de las tablas */
    public static final int NO_ADDRESS = -1;

    /**
     * Variable sin dirección, solo para pruebas de las tablas. La fábrica de operandos la
     * rechaza: ninguna variable sin dirección puede llegar a un cuádruplo
     */
    public VariableInfo(String name, Type type) {
        this(name, type, NO_ADDRESS);
    }

    @Override
    public String toString() {
        return name + ":" + type.display() + (address >= 0 ? "@" + address : "");
    }
}
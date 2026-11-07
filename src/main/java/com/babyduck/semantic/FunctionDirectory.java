package com.babyduck.semantic;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Directorio de funciones del programa.
 *
 * <p>Las variables globales viven en un campo aparte, no como una entrada
 * especial del mapa de funciones: así cada {@link FunctionInfo} describe una función real</p>
 *
 * <p>Las estructuras solo guardan y buscan. Las reglas de nombres y los
 * mensajes de error los aplica el recorrido del árbol, que conoce línea y columna</p>
 */
public final class FunctionDirectory {

    private final VariableTable globals = new VariableTable();
    private final Map<String, FunctionInfo> functions = new LinkedHashMap<>();

    /** Tabla de variables globales (también es la que usa {@code main}) */
    public VariableTable globals() {
        return globals;
    }

    /**
     * Agrega la función si su nombre no existe en el directorio
     *
     * @return {@code false} si ya había una función con ese nombre
     */
    public boolean addFunction(FunctionInfo function) {
        return functions.putIfAbsent(function.name(), function) == null;
    }

    /** La función con ese nombre, o {@code null} si no existe */
    public FunctionInfo getFunction(String name) {
        return functions.get(name);
    }

    public boolean containsFunction(String name) {
        return functions.containsKey(name);
    }

    /** Todas las funciones, en orden de declaración (solo lectura) */
    public Collection<FunctionInfo> functions() {
        return Collections.unmodifiableCollection(functions.values());
    }

    /**
     * Busca una variable desde el alcance actual: primero en la función y luego en las globales
     *
     * @param current la función que se está analizando, o {@code null} si se está en {@code main}
     *                (que no tiene variables propias y solo ve las globales)
     * @return la variable, o {@code null} si no está declarada
     */
    public VariableInfo lookupVariable(String name, FunctionInfo current) {
        if (current != null) {
            VariableInfo local = current.locals().get(name);
            if (local != null) {
                return local;
            }
        }
        return globals.get(name);
    }

    /** Vista legible del directorio (evidencia y depuración) */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Globales: ").append(globals);
        for (FunctionInfo f : functions.values()) {
            sb.append('\n').append(f);
        }
        return sb.toString();
    }
}
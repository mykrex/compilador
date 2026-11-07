package com.babyduck.semantic;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tabla de variables de un alcance (global o una función)
 *
 * Se implementa con {@code LinkedHashMap} : búsqueda e inserción O(1) por nombre,
 * como un {@code HashMap}, y además conserva el orden de declaración al recorrerla
 */
public final class VariableTable {

    private final Map<String, VariableInfo> variables = new LinkedHashMap<>();

    /**
     * Agrega la variable si su nombre no existe en esta tabla.
     *
     * @return {@code true} si se agregó, {@code false} si ya había una con ese nombre
     * la tabla no cambia, se conserva la primera declaración en ese caso
     */
    public boolean add(VariableInfo variable) {
        return variables.putIfAbsent(variable.name(), variable) == null;
    }

    /** La variable con ese nombre, o {@code null} si no está en esta tabla */
    public VariableInfo get(String name) {
        return variables.get(name);
    }

    public boolean contains(String name) {
        return variables.containsKey(name);
    }

    public int size() {
        return variables.size();
    }

    /** Todas las variables, en orden de declaración (solo lectura) */
    public Collection<VariableInfo> all() {
        return Collections.unmodifiableCollection(variables.values());
    }

    @Override
    public String toString() {
        return variables.values().toString();
    }
}
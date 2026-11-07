package com.babyduck.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entrada del directorio de funciones.
 *
 * <p>Cada parámetro se referencia desde dos lugares:</p>
 * <ul>
 *   <li>{@code params}: lista ordenada, para validar llamadas por posición
 *       (número de argumentos y tipo de cada uno)</li>
 *   <li>{@code locals}: tabla de la función, para buscarlo por nombre dentro del cuerpo
 *       y detectar si una variable local repite su nombre</li>
 * </ul>
 * <p>Ambas guardan el <b>mismo objeto</b> {@link VariableInfo}, no una copia</p>
 */
public final class FunctionInfo {

    private final String name;
    private final Type returnType;
    private final List<VariableInfo> params = new ArrayList<>();
    private final VariableTable locals = new VariableTable();

    public FunctionInfo(String name, Type returnType) {
        this.name = name;
        this.returnType = returnType;
    }

    /**
     * Registra un parámetro en la tabla local y, si su nombre no estaba repetido,
     * al final de la lista ordenada
     *
     * @return {@code false} si otro parámetro ya tenía ese nombre
     */
    public boolean addParam(VariableInfo param) {
        if (!locals.add(param)) {
            return false;
        }
        params.add(param);
        return true;
    }

    /**
     * Registra una variable local ({@code var} dentro de la función)
     *
     * @return {@code false} si ya existe un parámetro o una local con ese nombre
     */
    public boolean addLocal(VariableInfo variable) {
        return locals.add(variable);
    }

    public String name() {
        return name;
    }

    public Type returnType() {
        return returnType;
    }

    /** Parámetros en el orden en que se declararon (solo lectura) */
    public List<VariableInfo> params() {
        return Collections.unmodifiableList(params);
    }

    public int paramCount() {
        return params.size();
    }

    /** Tabla con parámetros y variables locales */
    public VariableTable locals() {
        return locals;
    }

    @Override
    public String toString() {
        return name + " -> " + returnType.display()
                + "\n  params:  " + params
                + "\n  locales: " + locals;
    }
}
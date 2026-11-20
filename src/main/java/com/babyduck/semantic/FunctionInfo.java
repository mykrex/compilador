package com.babyduck.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entrada del directorio de funciones.
 *
 * Cada parámetro se referencia desde dos lugares:
 *  -{@code params}: lista ordenada, para validar llamadas por posición
 *       (número de argumentos y tipo de cada uno)
 *  -{@code locals}: tabla de la función, para buscarlo por nombre dentro del cuerpo
 *       y detectar si una variable local repite su nombre
 * Ambas guardan el "mismo objeto" {@link VariableInfo}, no una copia
 *
 * Al terminar de compilar la función se guardan su cuádruplo de inicio, la cantidad de
 * locales y temporales por tipo, y se libera la tabla local: lo que necesita una llamada
 * (parámetros con su dirección, inicio, tamaño y dirección de regreso) queda aquí
 */
public final class FunctionInfo {

    /** Valor de un dato que no aplica o no se ha guardado (por ejemplo, la dirección de regreso de una void) */
    public static final int UNKNOWN = -1;

    private final String name;
    private final Type returnType;
    private final List<VariableInfo> params = new ArrayList<>();
    private final VariableTable locals = new VariableTable();
    private int returnAddress = UNKNOWN;      // dirección global donde deja su valor (no void)
    private int startQuad = UNKNOWN;          // número del primer cuádruplo de su cuerpo
    private MemorySize localSize = MemorySize.EMPTY;
    private MemorySize tempSize = MemorySize.EMPTY;

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

    /** Tabla con parámetros y variables locales (vacía después de {@link #releaseLocals()}) */
    public VariableTable locals() {
        return locals;
    }

    public int returnAddress() {
        return returnAddress;
    }

    public void setReturnAddress(int address) {
        this.returnAddress = address;
    }

    public int startQuad() {
        return startQuad;
    }

    public void setStartQuad(int quad) {
        this.startQuad = quad;
    }

    public MemorySize localSize() {
        return localSize;
    }

    public MemorySize tempSize() {
        return tempSize;
    }

    /** Guarda cuántas locales (incluye parámetros) y temporales de cada tipo usa la función */
    public void setSizes(MemorySize locals, MemorySize temps) {
        this.localSize = locals;
        this.tempSize = temps;
    }

    /**
     * Libera la tabla local al terminar de compilar la función. Los parámetros siguen en la
     * lista ordenada, con su dirección, para validar y generar las llamadas
     */
    public void releaseLocals() {
        locals.clear();
    }

    @Override
    public String toString() {
        return name + " -> " + returnType.display()
                + (returnAddress != UNKNOWN ? " (regreso en " + returnAddress + ")" : "")
                + "\n  inicio:      " + startQuad
                + "\n  params:      " + params
                + "\n  locales:     " + localSize
                + "\n  temporales:  " + tempSize;
    }
}
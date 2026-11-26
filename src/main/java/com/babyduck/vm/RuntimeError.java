package com.babyduck.vm;

/**
 * Error del programa durante la ejecución (división entre cero, casilla sin valor, entrada
 * inválida, demasiadas llamadas anidadas). Detiene la máquina virtual
 *
 * La memoria no conoce el cuádruplo que se está ejecutando: lanza el error solo con el
 * detalle y la máquina virtual le agrega el número de cuádruplo con {@link #atQuad(int)}
 */
public final class RuntimeError extends RuntimeException {

    private final String detail;

    public RuntimeError(String detail) {
        super(detail);
        this.detail = detail;
    }

    private RuntimeError(String message, String detail) {
        super(message);
        this.detail = detail;
    }

    /** El mismo error con el mensaje completo: "Error de ejecución en el cuádruplo N: detalle" */
    public RuntimeError atQuad(int quad) {
        return new RuntimeError("Error de ejecución en el cuádruplo " + quad + ": " + detail, detail);
    }

    /** Solo el detalle, sin el número de cuádruplo */
    public String detail() {
        return detail;
    }
}

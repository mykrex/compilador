package com.babyduck.vm;

import com.babyduck.intermediate.ConstantTable;
import com.babyduck.intermediate.Quadruple;
import com.babyduck.intermediate.QuadrupleQueue;
import com.babyduck.semantic.FunctionDirectory;
import com.babyduck.semantic.FunctionInfo;
import com.babyduck.semantic.MemorySize;
import com.babyduck.semantic.Type;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Máquina virtual: ejecuta la fila de cuádruplos
 *
 * Al crearla se carga lo que necesita: los cuádruplos, la memoria de ejecución (global,
 * constantes y la memoria de main) y, por cada función, su inicio con sus tamaños. Del
 * directorio solo toma tamaños e inicios; de cada cuádruplo, el código de operación y los
 * números de sus operandos ({@code address()}). Nunca usa nombres
 *
 * Ciclo: leer el cuádruplo del IP, ejecutarlo según su código (switch) y pasar al
 * siguiente, o al que indique un salto, hasta END
 */
public final class VirtualMachine {

    /** Tamaños de una función, para crear su registro de activación en ERA */
    private record FunctionSize(MemorySize locals, MemorySize temps) { }

    /** IP que regresa END: termina el ciclo */
    private static final int HALT = -1;

    private final List<Quadruple> code;
    private final ExecutionMemory memory;
    private final Map<Integer, FunctionSize> functions = new HashMap<>();   // inicio → tamaños
    private final BufferedReader input;
    private final PrintStream output;

    /**
     * @param input  de donde lee READ (una línea por dato)
     * @param output donde escriben PRINT y PRINTLN
     */
    public VirtualMachine(QuadrupleQueue quadruples, ConstantTable constants, FunctionDirectory directory,
                          BufferedReader input, PrintStream output) {
        this.code = quadruples.quadruples();
        this.memory = new ExecutionMemory(directory.globalSize(), directory.mainTempSize(), constants);
        for (FunctionInfo function : directory.functions()) {
            functions.put(function.startQuad(), new FunctionSize(function.localSize(), function.tempSize()));
        }
        this.input = input;
        this.output = output;
    }

    /**
     * Ejecuta el programa desde el cuádruplo 0 hasta END
     *
     * @throws RuntimeError con el número del cuádruplo donde ocurrió
     */
    public void run() {
        int ip = 0;
        while (ip != HALT) {
            try {
                ip = execute(code.get(ip), ip);
            } catch (RuntimeError e) {
                output.flush();
                throw e.atQuad(ip);
            }
        }
        output.flush();
    }

    /** Ejecuta un cuádruplo y regresa el número del siguiente */
    private int execute(Quadruple q, int ip) {
        switch (q.op().code()) {
            case 1, 2, 3 -> arithmetic(q);                               // + - *
            case 4 -> divide(q);                                         // /
            case 5, 6, 7, 8 -> relational(q);                            // > < == !=
            case 9 -> copy(q.left().address(), q.result().address());   // =
            case 10 -> negate(q);                                        // UMINUS
            case 11 -> print(q.left().address());                        // PRINT
            case 12 -> output.println();                                 // PRINTLN
            case 13 -> read(q.result().address());                       // READ
            case 14 -> {                                                 // END
                return HALT;
            }
            case 15 -> {                                                 // GOTO
                return q.result().address();
            }
            case 16 -> {                                                 // GOTOF
                return memory.readBool(q.left().address()) ? ip + 1 : q.result().address();
            }
            case 17 -> {                                                 // ERA
                FunctionSize size = functions.get(q.left().address());
                memory.prepareCall(size.locals(), size.temps());
            }
            case 18 -> memory.writeParam(q.result().address(), q.left().address());   // PARAM
            case 19 -> {                                                 // GOSUB
                memory.enterCall(ip + 1);
                return q.result().address();
            }
            case 20 -> {                                                 // RETURN
                if (q.left() != null) {
                    copy(q.left().address(), q.result().address());     // valor a la dirección global de regreso
                }
                return memory.exitCall();
            }
            case 21 -> {                                                 // ENDFUNC
                return memory.exitCall();
            }
            default -> throw new IllegalStateException("Código de operación desconocido: " + q.op().code());
        }
        return ip + 1;
    }

    /** + - *: si el resultado es int se opera con int; si es float, con float */
    private void arithmetic(Quadruple q) {
        int left = q.left().address();
        int right = q.right().address();
        int result = q.result().address();
        int code = q.op().code();
        if (memory.typeOf(result) == Type.INT) {
            int a = memory.readInt(left);
            int b = memory.readInt(right);
            memory.writeInt(result, code == 1 ? a + b : code == 2 ? a - b : a * b);
        } else {
            double a = memory.readFloat(left);
            double b = memory.readFloat(right);
            memory.writeFloat(result, code == 1 ? a + b : code == 2 ? a - b : a * b);
        }
    }

    /** /: siempre da float; dividir entre cero es error de ejecución */
    private void divide(Quadruple q) {
        double a = memory.readFloat(q.left().address());
        double b = memory.readFloat(q.right().address());
        if (b == 0) {
            throw new RuntimeError("división entre cero");
        }
        memory.writeFloat(q.result().address(), a / b);
    }

    /** > < == !=: compara como float (exacto también para int) y deja un bool */
    private void relational(Quadruple q) {
        double a = memory.readFloat(q.left().address());
        double b = memory.readFloat(q.right().address());
        boolean value = switch (q.op().code()) {
            case 5 -> a > b;
            case 6 -> a < b;
            case 7 -> a == b;
            default -> a != b;
        };
        memory.writeBool(q.result().address(), value);
    }

    /** Copia un valor al destino según el tipo del destino (int ← int, float ← int o float) */
    private void copy(int from, int to) {
        if (memory.typeOf(to) == Type.INT) {
            memory.writeInt(to, memory.readInt(from));
        } else {
            memory.writeFloat(to, memory.readFloat(from));
        }
    }

    /** UMINUS: cambia el signo, con el tipo del resultado */
    private void negate(Quadruple q) {
        int from = q.left().address();
        int to = q.result().address();
        if (memory.typeOf(to) == Type.INT) {
            memory.writeInt(to, -memory.readInt(from));
        } else {
            memory.writeFloat(to, -memory.readFloat(from));
        }
    }

    /** PRINT: escribe el elemento sin espacio ni salto de línea */
    private void print(int address) {
        switch (memory.typeOf(address)) {
            case INT -> output.print(memory.readInt(address));
            case FLOAT -> output.print(memory.readFloat(address));
            case BOOL -> output.print(memory.readBool(address));
            case STRING -> output.print(unescape(memory.readString(address)));
            default -> throw new IllegalStateException("PRINT con tipo inválido en " + address);
        }
    }

    /**
     * READ: lee una línea y la guarda según el tipo de la variable. Un int acepta solo dígitos
     * (con signo opcional); un float acepta además la parte decimal (un entero se promueve)
     */
    private void read(int address) {
        String line;
        try {
            line = input.readLine();
        } catch (IOException e) {
            throw new RuntimeError("no se pudo leer la entrada");
        }
        if (line == null) {
            throw new RuntimeError("no hay más datos de entrada");
        }
        String text = line.trim();
        if (memory.typeOf(address) == Type.INT) {
            if (!text.matches("-?[0-9]+")) {
                throw new RuntimeError("se esperaba un int y se leyó \"" + text + "\"");
            }
            try {
                memory.writeInt(address, Integer.parseInt(text));
            } catch (NumberFormatException e) {
                throw new RuntimeError("el int \"" + text + "\" está fuera de rango");
            }
        } else {
            if (!text.matches("-?[0-9]+(\\.[0-9]+)?")) {
                throw new RuntimeError("se esperaba un float y se leyó \"" + text + "\"");
            }
            memory.writeFloat(address, Double.parseDouble(text));
        }
    }

    /** Interpreta las secuencias de un letrero: \n, \t, \" y \\ (otra letra después de \ se deja tal cual) */
    private static String unescape(String content) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\\' && i + 1 < content.length()) {
                char next = content.charAt(++i);
                sb.append(switch (next) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    default -> next;
                });
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

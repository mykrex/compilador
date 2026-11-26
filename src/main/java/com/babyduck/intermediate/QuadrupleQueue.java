package com.babyduck.intermediate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fila de cuádruplos
 *
 * Los cuádruplos se agregan al final y se recorren en orden (comportamiento de fila),
 * pero se guardan en un ArrayList y no en una cola pura, para tener acceso por índice
 * a cuádruplos ya generados (por ejemplo, para rellenar el destino de un salto).
 * Se numera desde 0: el número de un cuádruplo es su posición en la lista
 */
public final class QuadrupleQueue {

    /** Formas de imprimir la fila */
    public enum Style {
        /** Solo nombres, una línea por cuádruplo, sin encabezado. Lo usan las pruebas */
        SYMBOLIC,
        /**
         * Código del operador y direcciones virtuales, una línea por cuádruplo, sin encabezado:
         * es lo que verá la máquina virtual. Lo usan las pruebas de direcciones
         */
        ADDRESS,
        /** Tabla con encabezado para la consola; cada operando como dirección(nombre) */
        FULL
    }

    private static final String EMPTY = "_";

    private final List<Quadruple> quadruples = new ArrayList<>();

    /** Agrega un cuádruplo al final y regresa su número */
    int add(Quadruple quadruple) {
        quadruples.add(quadruple);
        return quadruples.size() - 1;
    }

    /**
     * Rellena el destino de un salto ya generado: reemplaza el cuádruplo por uno igual con
     * ese resultado (los cuádruplos son inmutables)
     */
    void fill(int index, Operand target) {
        Quadruple q = quadruples.get(index);
        quadruples.set(index, new Quadruple(q.op(), q.left(), q.right(), target));
    }

    /** Número que tendrá el próximo cuádruplo */
    public int nextIndex() {
        return quadruples.size();
    }

    public int size() {
        return quadruples.size();
    }

    public Quadruple get(int index) {
        return quadruples.get(index);
    }

    /** Todos los cuádruplos, en orden (solo lectura) */
    public List<Quadruple> quadruples() {
        return Collections.unmodifiableList(quadruples);
    }

    /** Único método que convierte la fila en texto */
    public String format(Style style) {
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < quadruples.size(); i++) {
            Quadruple q = quadruples.get(i);
            String op = (style == Style.ADDRESS) ? String.valueOf(q.op().code()) : q.op().symbol();
            rows.add(new String[]{String.valueOf(i), op,
                    show(q.left(), style), show(q.right(), style), show(q.result(), style)});
        }

        StringBuilder sb = new StringBuilder();
        if (style != Style.FULL) {
            for (String[] row : rows) {
                sb.append(String.join(" ", row)).append('\n');
            }
            return sb.toString();
        }

        String[] header = {"#", "op", "izq", "der", "res"};
        int[] width = new int[header.length];
        for (int c = 0; c < header.length; c++) {
            width[c] = header[c].length();
            for (String[] row : rows) {
                width[c] = Math.max(width[c], row[c].length());
            }
        }
        appendRow(sb, header, width);
        for (String[] row : rows) {
            appendRow(sb, row, width);
        }
        return sb.toString();
    }

    private static void appendRow(StringBuilder sb, String[] row, int[] width) {
        StringBuilder line = new StringBuilder();
        for (int c = 0; c < row.length; c++) {
            line.append(String.format("%-" + width[c] + "s", row[c]));
            if (c < row.length - 1) line.append("  ");
        }
        sb.append(line.toString().stripTrailing()).append('\n');
    }

    /**
     * Texto de un operando según el estilo: nombre, dirección, o dirección(nombre).
     * El destino de un salto se imprime igual en los tres estilos. Una función y un número
     * de parámetro se imprimen con su nombre y k, salvo en ADDRESS, donde se imprime el
     * número que guardan: el inicio de la función y la dirección del parámetro
     */
    private static String show(Operand operand, Style style) {
        if (operand == null) {
            return EMPTY;
        }
        if (!operand.hasAddress()) {
            boolean numbered = operand.kind() == OperandKind.FUNCTION
                    || operand.kind() == OperandKind.PARAM_INDEX;
            return (style == Style.ADDRESS && numbered) ? String.valueOf(operand.address()) : operand.name();
        }
        return switch (style) {
            case SYMBOLIC -> operand.name();
            case ADDRESS -> String.valueOf(operand.address());
            case FULL -> operand.address() + "(" + operand.name() + ")";
        };
    }

    @Override
    public String toString() {
        return format(Style.FULL);
    }
}

package com.babyduck.intermediate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fila de cuádruplos
 *
 * Los cuádruplos se agregan al final y se recorren en orden (comportamiento de fila),
 * pero se guardan en un ArrayList y no en una cola pura: en la entrega de estatutos no
 * lineales habrá que rellenar el destino de cuádruplos ya generados (GotoF, Goto),
 * y para eso se necesita acceso por índice. Se numera desde 0: el número de un
 * cuádruplo es su posición en la lista
 *
 * Además guarda las notas que marcan dónde se generará código en entregas siguientes
 */
public final class QuadrupleQueue {

    /** Formas de imprimir la fila */
    public enum Style {
        /**
         * Solo nombres, una línea por cuádruplo, sin encabezado ni notas.
         * Lo usan las pruebas; no cambiará cuando lleguen las direcciones virtuales
         */
        SYMBOLIC,
        /**
         * Tabla con encabezado y notas, para mostrar en consola.
         * En la entrega de memoria mostrará cada operando como dirección(nombre)
         */
        FULL
    }

    private static final String EMPTY = "_";

    private final List<Quadruple> quadruples = new ArrayList<>();
    private final List<Note> notes = new ArrayList<>();

    /** Agrega un cuádruplo al final y regresa su número */
    int add(Quadruple quadruple) {
        quadruples.add(quadruple);
        return quadruples.size() - 1;
    }

    /** Agrega una nota antes del próximo cuádruplo que se genere */
    void addNote(String text) {
        notes.add(new Note(quadruples.size(), text));
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

    /** Todas las notas, en orden (solo lectura) */
    public List<Note> notes() {
        return Collections.unmodifiableList(notes);
    }

    /** Único método que convierte la fila en texto */
    public String format(Style style) {
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < quadruples.size(); i++) {
            Quadruple q = quadruples.get(i);
            rows.add(new String[]{String.valueOf(i), q.op().symbol(),
                    show(q.left(), style), show(q.right(), style), show(q.result(), style)});
        }

        StringBuilder sb = new StringBuilder();
        if (style == Style.SYMBOLIC) {
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
        int nextNote = 0;
        for (int i = 0; i <= rows.size(); i++) {
            while (nextNote < notes.size() && notes.get(nextNote).position() == i) {
                sb.append(" ".repeat(width[0] + 2)).append("· ")
                        .append(notes.get(nextNote).text()).append('\n');
                nextNote++;
            }
            if (i < rows.size()) {
                appendRow(sb, rows.get(i), width);
            }
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

    /** Texto de un operando según el estilo (en la entrega 4, FULL agregará la dirección) */
    private static String show(Operand operand, Style style) {
        if (operand == null) {
            return EMPTY;
        }
        return operand.name();
    }

    @Override
    public String toString() {
        return format(Style.FULL);
    }
}

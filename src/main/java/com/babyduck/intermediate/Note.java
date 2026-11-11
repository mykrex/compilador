package com.babyduck.intermediate;

/**
 * Nota que acompaña a la fila de cuádruplos sin ser un cuádruplo
 *
 * Marca el lugar donde una entrega siguiente generará código (saltos de if y while,
 * llamadas, return, inicio y fin de funciones). Se muestra antes del cuádruplo
 * que ocupe la posición {@code position}
 */
public record Note(int position, String text) {
}

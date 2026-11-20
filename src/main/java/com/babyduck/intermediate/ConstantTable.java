package com.babyduck.intermediate;

import com.babyduck.semantic.Type;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tabla de constantes: cada valor distinto recibe una sola dirección del segmento de constantes
 *
 * Un mapa {@code valor → dirección} por tipo (int, float, letrero). La llave es el valor ya
 * convertido (Integer, Double o el contenido del letrero), no el texto del programa: así
 * {@code 2.5} y {@code 2.50} comparten dirección. Los mapas conservan el orden de aparición
 */
public final class ConstantTable {

    private final VirtualMemory memory;
    private final Map<Type, Map<Object, Integer>> table = new EnumMap<>(Type.class);

    public ConstantTable(VirtualMemory memory) {
        this.memory = memory;
    }

    /**
     * Dirección del valor; si es la primera vez que aparece, se le asigna una
     *
     * @return la dirección, o {@link VirtualMemory#OVERFLOW} si el rango de ese tipo ya está lleno
     */
    public int addressOf(Type type, Object value) {
        Map<Object, Integer> values = table.computeIfAbsent(type, t -> new LinkedHashMap<>());
        Integer address = values.get(value);
        if (address == null) {
            address = memory.allocate(Segment.CONSTANT, type);
            if (address != VirtualMemory.OVERFLOW) {
                values.put(value, address);
            }
        }
        return address;
    }

    /** Cantidad de constantes distintas de ese tipo */
    public int size(Type type) {
        return table.getOrDefault(type, Map.of()).size();
    }

    /** Una línea por constante: dirección, tipo y valor (los letreros, entre comillas) */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Type, Map<Object, Integer>> byType : table.entrySet()) {
            for (Map.Entry<Object, Integer> entry : byType.getValue().entrySet()) {
                Object value = entry.getKey();
                String shown = byType.getKey() == Type.STRING ? "\"" + value + "\"" : String.valueOf(value);
                sb.append(String.format("%-6d %-7s %s%n", entry.getValue(), byType.getKey().display(), shown));
            }
        }
        return sb.toString();
    }
}

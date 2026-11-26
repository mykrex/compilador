package com.babyduck.intermediate;

import com.babyduck.semantic.Type;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Asignación de direcciones virtuales: un rango de 1000 direcciones por segmento y tipo
 *
 * Segmento     int          float        bool         letrero
 * Global       1000-1999    2000-2999    -            -
 * Local        3000-3999    4000-4999    -            -
 * Temporal     5000-5999    6000-6999    7000-7999    -
 * Constante    8000-8999    9000-9999    -            10000-10999
 *
 * Del número de una dirección salen su segmento y su tipo, y su casilla es dirección - base.
 * Cada (segmento, tipo) tiene un contador: la siguiente dirección libre es base + contador
 *
 * Al empezar cada función y main, {@link #startScope()} reinicia los contadores locales y
 * temporales: cada función usa sus direcciones desde la base, porque cada llamada tiene su
 * propia memoria. Los globales y las constantes no se reinician
 *
 * Si un rango se llena no se lanza excepción (es un error del programa, no del compilador):
 * la dirección es {@link #OVERFLOW} y el mensaje queda pendiente en {@link #takeOverflows()}
 */
public final class VirtualMemory {

    /** Direcciones por segmento y tipo */
    public static final int SEGMENT_SIZE = 1000;

    /** Dirección que se regresa cuando el rango ya está lleno */
    public static final int OVERFLOW = -2;

    /** Un rango de direcciones: a qué segmento y tipo pertenece */
    public record Range(Segment segment, Type type) { }

    /**
     * Tabla de rangos: la posición i es el rango que empieza en i * 1000 (la posición 0 no se usa).
     * El compilador la usa para asignar direcciones y la máquina virtual para traducirlas
     */
    private static final Range[] RANGES = {
            null,
            new Range(Segment.GLOBAL, Type.INT),        // 1000
            new Range(Segment.GLOBAL, Type.FLOAT),      // 2000
            new Range(Segment.LOCAL, Type.INT),         // 3000
            new Range(Segment.LOCAL, Type.FLOAT),       // 4000
            new Range(Segment.TEMPORARY, Type.INT),     // 5000
            new Range(Segment.TEMPORARY, Type.FLOAT),   // 6000
            new Range(Segment.TEMPORARY, Type.BOOL),    // 7000
            new Range(Segment.CONSTANT, Type.INT),      // 8000
            new Range(Segment.CONSTANT, Type.FLOAT),    // 9000
            new Range(Segment.CONSTANT, Type.STRING),   // 10000
    };

    private final Map<Segment, Map<Type, Integer>> bases = new EnumMap<>(Segment.class);
    private final Map<Segment, Map<Type, Integer>> counters = new EnumMap<>(Segment.class);
    private final Set<String> reported = new HashSet<>();     // rangos llenos ya avisados
    private final List<String> overflows = new ArrayList<>(); // mensajes pendientes de reportar

    public VirtualMemory() {
        for (int i = 1; i < RANGES.length; i++) {
            base(RANGES[i].segment(), RANGES[i].type(), i * SEGMENT_SIZE);
        }
    }

    /**
     * Segmento y tipo de una dirección: el rango es {@code dirección / 1000}
     *
     * @return el rango, o null si la dirección no pertenece a ninguno
     */
    public static Range rangeOf(int address) {
        int index = address / SEGMENT_SIZE;
        return (address >= 0 && index < RANGES.length) ? RANGES[index] : null;
    }

    /** Casilla de una dirección dentro de su rango: {@code dirección - base} */
    public static int cellOf(int address) {
        return address % SEGMENT_SIZE;
    }

    private void base(Segment segment, Type type, int address) {
        bases.computeIfAbsent(segment, s -> new EnumMap<>(Type.class)).put(type, address);
        counters.computeIfAbsent(segment, s -> new EnumMap<>(Type.class)).put(type, 0);
    }

    /**
     * Siguiente dirección libre del segmento para ese tipo
     *
     * @return la dirección, o {@link #OVERFLOW} si el rango ya está lleno
     * @throws IllegalArgumentException si el segmento no tiene rango para ese tipo
     *         (por ejemplo, una variable bool): es un error del compilador
     */
    public int allocate(Segment segment, Type type) {
        Integer base = bases.get(segment).get(type);
        if (base == null) {
            throw new IllegalArgumentException("El segmento " + segment + " no tiene rango para " + type);
        }
        int used = counters.get(segment).get(type);
        if (used >= SEGMENT_SIZE) {
            String key = segment + " " + type;
            if (reported.add(key)) {
                overflows.add("se excedió el límite de " + SEGMENT_SIZE + " direcciones para "
                        + segment.description() + " de tipo " + type.display());
            }
            return OVERFLOW;
        }
        counters.get(segment).put(type, used + 1);
        return base + used;
    }

    /** Reinicia los contadores locales y temporales (inicio de una función o de main) */
    public void startScope() {
        for (Segment segment : List.of(Segment.LOCAL, Segment.TEMPORARY)) {
            counters.get(segment).replaceAll((type, used) -> 0);
            reported.removeIf(key -> key.startsWith(segment + " "));
        }
    }

    /** Cuántas direcciones se han usado en el segmento para ese tipo (0 si no tiene rango) */
    public int count(Segment segment, Type type) {
        return counters.get(segment).getOrDefault(type, 0);
    }

    /** Mensajes de rangos llenos que no se han reportado; la lista queda vacía */
    public List<String> takeOverflows() {
        List<String> pending = List.copyOf(overflows);
        overflows.clear();
        return pending;
    }
}

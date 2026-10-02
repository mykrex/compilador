package org.example.estructuras;

/**
 * Hash table (clave -> valor)
 */
public class MyHashTable<K, V> {

    private static final int CAPACIDAD_INICIAL = 16;
    private static final double FACTOR_CARGA_MAX = 0.75;

    // Entrada interna: un par clave-valor y la referencia a la siguiente entrada de la misma cubeta
    private static class Entrada<K, V> {
        final K clave;
        V valor;
        Entrada<K, V> siguiente;

        Entrada(K clave, V valor, Entrada<K, V> siguiente) {
            this.clave = clave;
            this.valor = valor;
            this.siguiente = siguiente;
        }
    }

    private Entrada<K, V>[] cubetas;
    private int tamanio; // num de pares clave-valor guardados

    public MyHashTable() {
        this(CAPACIDAD_INICIAL);
    }

    @SuppressWarnings("unchecked")
    public MyHashTable(int capacidadInicial) {
        if (capacidadInicial <= 0) {
            throw new IllegalArgumentException("La capacidad inicial debe ser mayor a 0");
        }
        cubetas = (Entrada<K, V>[]) new Entrada[capacidadInicial];
        tamanio = 0;
    }

    /**
     * Funcion hash: convierte la clave en un indice valido [0, capacidad)
     * - hashCode() lo da Java para cualquier objeto (String, Integer, etc.).
     * - El XOR con los bits altos mezcla mejor el valor.
     * - "& 0x7fffffff" quita el signo (hashCode puede ser negativo).
     */
    private int indice(K clave, int capacidad) {
        int h = clave.hashCode();
        h = h ^ (h >>> 16);
        return (h & 0x7fffffff) % capacidad;
    }

    private void validarClave(K clave) {
        if (clave == null) {
            throw new IllegalArgumentException("La clave no puede ser null");
        }
    }

    // Busca la entrada de una clave, return null si no existe
    private Entrada<K, V> buscarEntrada(K clave) {
        int i = indice(clave, cubetas.length);
        Entrada<K, V> actual = cubetas[i];
        while (actual != null) {
            if (actual.clave.equals(clave)) {
                return actual;
            }
            actual = actual.siguiente;
        }
        return null;
    }

    /**
     * Inserta o actualiza un par clave-valor
     * @return el valor anterior si la clave ya existia, o null si es nueva
     */
    public V put(K clave, V valor) {
        validarClave(clave);

        Entrada<K, V> existente = buscarEntrada(clave);
        if (existente != null) {
            V anterior = existente.valor;
            existente.valor = valor; // la clave ya existe, solo actualizamos
            return anterior;
        }

        // Clave nueva, la insertamos al inicio de la lista de su cubeta
        int i = indice(clave, cubetas.length);
        cubetas[i] = new Entrada<>(clave, valor, cubetas[i]);
        tamanio++;

        if ((double) tamanio / cubetas.length > FACTOR_CARGA_MAX) {
            rehash(cubetas.length * 2);
        }
        return null;
    }

    // Regresa el valor asociado a la clave, o null si no existe
    public V get(K clave) {
        validarClave(clave);
        Entrada<K, V> e = buscarEntrada(clave);
        return (e == null) ? null : e.valor;
    }

    // Indica si la clave existe, sirve aunque el valor guardado sea null
    public boolean containsKey(K clave) {
        validarClave(clave);
        return buscarEntrada(clave) != null;
    }

    /**
     * Elimina la clave de la tabla
     * @return el valor que tenia, o null si la clave no existia
     */
    public V remove(K clave) {
        validarClave(clave);
        int i = indice(clave, cubetas.length);
        Entrada<K, V> anterior = null;
        Entrada<K, V> actual = cubetas[i];

        while (actual != null) {
            if (actual.clave.equals(clave)) {
                if (anterior == null) {
                    cubetas[i] = actual.siguiente;          // era la primera de la cubeta
                } else {
                    anterior.siguiente = actual.siguiente;  // la saltamos en la lista
                }
                tamanio--;
                return actual.valor;
            }
            anterior = actual;
            actual = actual.siguiente;
        }
        return null;
    }

    public int size() {
        return tamanio;
    }

    public boolean isEmpty() {
        return tamanio == 0;
    }

    // Elimina todas las entradas
    public void clear() {
        for (int i = 0; i < cubetas.length; i++) {
            cubetas[i] = null;
        }
        tamanio = 0;
    }

    // Regresa todas las claves en una Queue
    public MyQueue<K> keys() {
        MyQueue<K> claves = new MyQueue<>();
        for (Entrada<K, V> cabeza : cubetas) {
            Entrada<K, V> actual = cabeza;
            while (actual != null) {
                claves.enqueue(actual.clave);
                actual = actual.siguiente;
            }
        }
        return claves;
    }

    // Crea un arreglo mas grande y vuelve a colocar cada entrada segun su nuevo indice
    @SuppressWarnings("unchecked")
    private void rehash(int nuevaCapacidad) {
        Entrada<K, V>[] viejas = cubetas;
        cubetas = (Entrada<K, V>[]) new Entrada[nuevaCapacidad];

        for (Entrada<K, V> cabeza : viejas) {
            Entrada<K, V> actual = cabeza;
            while (actual != null) {
                Entrada<K, V> siguiente = actual.siguiente; // guardamos antes de mover el nodo
                int i = indice(actual.clave, nuevaCapacidad);
                actual.siguiente = cubetas[i];
                cubetas[i] = actual;
                actual = siguiente;
            }
        }
    }

    // Capacidad actual del arreglo de cubetas
    public int capacidad() {
        return cubetas.length;
    }

    // Ejemplo: {x=int, y=float}
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean primero = true;
        for (Entrada<K, V> cabeza : cubetas) {
            Entrada<K, V> actual = cabeza;
            while (actual != null) {
                if (!primero) sb.append(", ");
                sb.append(actual.clave).append("=").append(actual.valor);
                primero = false;
                actual = actual.siguiente;
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
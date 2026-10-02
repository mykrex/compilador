package org.example.estructuras;

/**
 * Stack LIFO: Last In, First Out
 */
public class MyStack<T> {

    private static final int CAPACIDAD_INICIAL = 8;

    private Object[] elementos;
    private int tope; // cantidad de elementos, el elemento del tope esta en [tope - 1]

    public MyStack() {
        this(CAPACIDAD_INICIAL);
    }

    public MyStack(int capacidadInicial) {
        if (capacidadInicial <= 0) {
            throw new IllegalArgumentException("La capacidad inicial debe ser mayor a 0");
        }
        elementos = new Object[capacidadInicial];
        tope = 0;
    }

    // Agregar elemento en el tope de la pila
    public void push(T elemento) {
        if (tope == elementos.length) {
            redimensionar(elementos.length * 2); // arreglo lleno -> duplicamos
        }
        elementos[tope] = elemento;
        tope++;
    }

    // Elimina y regresa el elemento del tope. Lanza excepcion si la pila esta vacia
    @SuppressWarnings("unchecked")
    public T pop() {
        if (isEmpty()) {
            throw new IllegalStateException("pop() sobre una pila vacia (stack underflow)");
        }
        tope--;
        T elemento = (T) elementos[tope];
        elementos[tope] = null; // quitamos la referencia para que el garbage colector pueda liberar memoria

        return elemento;
    }

    // Devuelve el elemento del tope SIN eliminarlo
    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) {
            throw new IllegalStateException("peek() sobre una pila vacía");
        }
        return (T) elementos[tope - 1];
    }

    public boolean isEmpty() {
        return tope == 0;
    }

    public int size() {
        return tope;
    }

    // Vaciar la pila
    public void clear() {
        for (int i = 0; i < tope; i++) {
            elementos[i] = null;
        }
        tope = 0;
    }

    // Copia los elementos a un arreglo nuevo de otra capacidad
    private void redimensionar(int nuevaCapacidad) {
        Object[] nuevo = new Object[nuevaCapacidad];
        for (int i = 0; i < tope; i++) {
            nuevo[i] = elementos[i];
        }
        elementos = nuevo;
    }

    // Ejemplo: [a, b, c] <- tope
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < tope; i++) {
            sb.append(elementos[i]);
            if (i < tope - 1) sb.append(", ");
        }
        sb.append("] <- tope");
        return sb.toString();
    }
}
package org.example.estructuras;

/**
 * Queues FIFO: First In, First Out
 */
public class MyQueue<T> {

    // Nodo interno, guarda un dato y la referencia al siguiente
    private static class Nodo<T> {
        T dato;
        Nodo<T> siguiente;

        Nodo(T dato) {
            this.dato = dato;
            this.siguiente = null;
        }
    }

    private Nodo<T> frente;
    private Nodo<T> fin;
    private int tamanio;

    public MyQueue() {
        frente = null;
        fin = null;
        tamanio = 0;
    }

    // Agrega un elemento al final de la cola
    public void enqueue(T elemento) {
        Nodo<T> nuevo = new Nodo<>(elemento);
        if (isEmpty()) {
            frente = nuevo;           // si estaba vacia, el nuevo nodo es frente y fin a la vez
        } else {
            fin.siguiente = nuevo;    // enlazamos el ultimo nodo con el nuevo
        }
        fin = nuevo;
        tamanio++;
    }

    // Elimina y regresa el elemento del frente, se lanza excepcion si la cola esta vacia
    public T dequeue() {
        if (isEmpty()) {
            throw new IllegalStateException("dequeue() sobre una cola vacía (queue underflow)");
        }
        T dato = frente.dato;
        frente = frente.siguiente;
        tamanio--;
        if (frente == null) {
            fin = null; // Si la cola quedo vacia, 'fin' tampoco debe apuntar a nada
        }
        return dato;
    }

    // Regresa el elemento del frente SIN eliminarlo
    public T peek() {
        if (isEmpty()) {
            throw new IllegalStateException("peek() sobre una cola vacía");
        }
        return frente.dato;
    }

    public boolean isEmpty() {
        return tamanio == 0;
    }

    public int size() {
        return tamanio;
    }

    // Vacia la cola
    // El gc se encarga de los nodos sin referencias
    public void clear() {
        frente = null;
        fin = null;
        tamanio = 0;
    }

    // Ejemplo: frente -> [A, B, C] <- final
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("frente -> [");
        Nodo<T> actual = frente;
        while (actual != null) {
            sb.append(actual.dato);
            if (actual.siguiente != null) sb.append(", ");
            actual = actual.siguiente;
        }
        sb.append("] <- fin");
        return sb.toString();
    }
}
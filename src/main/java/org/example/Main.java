package org.example;

import org.example.estructuras.MyHashTable;
import org.example.estructuras.MyQueue;
import org.example.estructuras.MyStack;

import java.util.Objects;

public class Main {

    private static int pasaron = 0;
    private static int fallaron = 0;

    public static void main(String[] args) {
        demoStack();
        demoQueue();
        demoHashTable();

        System.out.println();
        System.out.println("=============== TEST CASES ===============");
        testsStack();
        testsQueue();
        testsHashTable();

        System.out.println();
        System.out.println("===== RESUMEN: " + pasaron + " pasaron, " + fallaron + " fallaron =====");
    }

    private static void demoStack() {
        titulo("STACK");
        MyStack<Integer> pila = new MyStack<>();
        System.out.println("Pila nueva:      " + pila + "   isEmpty=" + pila.isEmpty());

        pila.push(10);
        System.out.println("push(10):        " + pila);
        pila.push(20);
        System.out.println("push(20):        " + pila);
        pila.push(30);
        System.out.println("push(30):        " + pila + "   size=" + pila.size());

        System.out.println("peek():          " + pila.peek() + "   (solo consulta, no saca)");
        System.out.println("pop():           " + pila.pop() + "   -> queda " + pila);
        System.out.println("pop():           " + pila.pop() + "   -> queda " + pila);
        System.out.println("pop():           " + pila.pop() + "   -> queda " + pila + "   isEmpty=" + pila.isEmpty());
    }

    private static void demoQueue() {
        titulo("QUEUE");
        MyQueue<String> cola = new MyQueue<>();
        System.out.println("Cola nueva:      " + cola + "   isEmpty=" + cola.isEmpty());

        cola.enqueue("int");
        System.out.println("enqueue(int):    " + cola);
        cola.enqueue("x");
        System.out.println("enqueue(x):      " + cola);
        cola.enqueue("=");
        System.out.println("enqueue(=):      " + cola + "   size=" + cola.size());

        System.out.println("peek():          " + cola.peek() + "   (solo consulta, no saca)");
        System.out.println("dequeue():       " + cola.dequeue() + "   -> queda " + cola);
        System.out.println("dequeue():       " + cola.dequeue() + "   -> queda " + cola);
        System.out.println("dequeue():       " + cola.dequeue() + "   -> queda " + cola + "   isEmpty=" + cola.isEmpty());
    }

    private static void demoHashTable() {
        titulo("HASH TABLE");
        MyHashTable<String, String> tabla = new MyHashTable<>();
        System.out.println("Tabla nueva:            " + tabla + "   isEmpty=" + tabla.isEmpty());

        tabla.put("x", "int");
        tabla.put("y", "float");
        tabla.put("activo", "bool");
        System.out.println("put x, y, activo:       " + tabla + "   size=" + tabla.size());

        System.out.println("get(\"y\"):               " + tabla.get("y"));
        System.out.println("containsKey(\"z\"):       " + tabla.containsKey("z"));

        String anterior = tabla.put("x", "char");
        System.out.println("put(\"x\", \"char\"):       antes era " + anterior + " -> " + tabla);

        System.out.println("remove(\"activo\"):       " + tabla.remove("activo") + " -> " + tabla);
        System.out.println("keys():                 " + tabla.keys());
    }

    // TEST CASES

    private static void testsStack() {
        titulo("TESTS STACK");
        MyStack<Integer> pila = new MyStack<>();
        pila.push(1);
        pila.push(2);
        pila.push(3);

        String salida = pila.pop() + ", " + pila.pop() + ", " + pila.pop();
        verificar("S1", "pop() saca en orden LIFO", "3, 2, 1", salida);

        verificarExcepcion("S2", "pop() en pila vacia lanza excepcion", pila::pop);

        for (int i = 0; i < 100; i++) {
            pila.push(i);   // capacidad inicial 8, el arreglo tiene que crecer
        }
        verificar("S3", "100 push: el arreglo crece y conserva el tope", "100 elementos, tope 99",
                pila.size() + " elementos, tope " + pila.peek());
    }

    private static void testsQueue() {
        titulo("TESTS QUEUE");
        MyQueue<String> cola = new MyQueue<>();
        cola.enqueue("A");
        cola.enqueue("B");
        cola.enqueue("C");

        String salida = cola.dequeue() + ", " + cola.dequeue() + ", " + cola.dequeue();
        verificar("Q1", "dequeue() saca en orden FIFO", "A, B, C", salida);

        verificarExcepcion("Q2", "dequeue() en cola vacia lanza excepcion", cola::dequeue);
    }

    private static void testsHashTable() {
        titulo("TESTS HASH TABLE");
        MyHashTable<String, String> tabla = new MyHashTable<>();
        tabla.put("x", "int");
        tabla.put("y", "float");

        verificar("H1", "get(\"x\") regresa su valor", "int", tabla.get("x"));

        tabla.put("x", "char");
        verificar("H2", "put en clave existente actualiza sin duplicar", "char, size 2",
                tabla.get("x") + ", size " + tabla.size());

        tabla.remove("y");
        verificar("H3", "remove(\"y\") elimina la clave", false, tabla.containsKey("y"));

        // "Aa" y "BB" tienen el mismo hashCode en Java: estas claves colisionan siempre
        MyHashTable<String, Integer> colisiones = new MyHashTable<>();
        colisiones.put("AaAa", 1);
        colisiones.put("AaBB", 2);
        colisiones.put("BBBB", 3);
        verificar("H4", "claves con colision se guardan y recuperan bien", "1, 2, 3",
                colisiones.get("AaAa") + ", " + colisiones.get("AaBB") + ", " + colisiones.get("BBBB"));
    }

    // UTILIDADES

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("<" + texto + " >");
    }

    private static void verificar(String id, String descripcion, Object esperado, Object obtenido) {
        boolean ok = Objects.equals(esperado, obtenido);
        if (ok) pasaron++; else fallaron++;
        System.out.printf("[%s] %s %s | esperado: %s | obtenido: %s%n",
                ok ? "PASA" : "FALLA", id, descripcion, esperado, obtenido);
    }

    private static void verificarExcepcion(String id, String descripcion, Runnable accion) {
        try {
            accion.run();
            fallaron++;
            System.out.printf("[FALLA] %s %s | no se lanzo ninguna excepcion%n", id, descripcion);
        } catch (IllegalStateException | IllegalArgumentException e) {
            pasaron++;
            System.out.printf("[PASA] %s %s | mensaje: %s%n", id, descripcion, e.getMessage());
        }
    }
}
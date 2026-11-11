# Código intermedio

## Índice

1. [Overview](overview.md)
2. [Herramientas](herramientas.md)
3. [Análisis léxico](lexico.md)
4. [Análisis sintáctico](sintaxis.md)
5. [Análisis semántico](semantica.md)
6. **Código intermedio** (este capítulo)
7. [Testing](testing.md)
8. [Herramientas de IA](herramientas_ia.md)
9. [Guía de uso](guia_de_uso.md)

> Los nombres de archivo no llevan número; el orden final se fijará en la entrega final.

---

## 1. Objetivo y alcance

El compilador traduce el programa a una representación intermedia basada en **cuádruplos**.
En esta entrega se generan los cuádruplos de las **expresiones aritméticas y relacionales** y de
los **estatutos lineales** de BabyDuck: asignación, `print` y `read`, más un cuádruplo `END` al
final de `main`.

Los saltos de `if` y `while`, las llamadas a funciones, `return` y el inicio y fin de las funciones
se generarán en las entregas siguientes. Mientras tanto, el compilador no falla con esos
estatutos: genera todo lo lineal que contienen y deja una nota en el lugar donde irá el código
que falta (sección 8).

La generación ocurre en la misma pasada que el análisis semántico. Si el programa tiene
cualquier error (léxico, sintáctico o semántico), la fila de cuádruplos se descarta y no se
muestra.

## 2. Representación: cuádruplos

Cada cuádruplo tiene cuatro campos: `(operador, izquierdo, derecho, resultado)`. Un campo que no
aplica queda vacío y se imprime como `_`.

El operador se guarda como un enum (`QuadOp`) y los operandos como objetos (`Operand`), no como
texto. El texto solo se produce al imprimir la fila, en un único método. Así, cuando la memoria
virtual asigne direcciones, los cuádruplos no cambian: solo cambia lo que guarda cada operando y
la forma de imprimirlo.

### 2.1 Operadores

`QuadOp` es un enum distinto de `Op`. `Op` contiene los operadores del lenguaje que consulta el
cubo semántico; `QuadOp` contiene las instrucciones que ejecutará la máquina virtual, que
incluyen algunas sin equivalente en el lenguaje (`UMINUS`, `PRINTLN`, `END`).

Cada operador tiene un código numérico fijo, escrito a mano, como recomienda el curso. No se usa
la posición del valor dentro del enum, porque cambiaría si alguien reordena los valores. En
memoria el cuádruplo guarda el enum, lo que permite revisar los casos con `switch`; el código
numérico se usará al escribir el archivo que lee la máquina virtual.

| Código | Operador | Formato | Significado |
|---|---|---|---|
| 1 | `+` | `(+, izq, der, t)` | Suma |
| 2 | `-` | `(-, izq, der, t)` | Resta |
| 3 | `*` | `(*, izq, der, t)` | Multiplicación |
| 4 | `/` | `(/, izq, der, t)` | División (siempre da `float`) |
| 5 | `>` | `(>, izq, der, t)` | Mayor que (da `bool`) |
| 6 | `<` | `(<, izq, der, t)` | Menor que (da `bool`) |
| 7 | `==` | `(==, izq, der, t)` | Igual a (da `bool`) |
| 8 | `!=` | `(!=, izq, der, t)` | Distinto de (da `bool`) |
| 9 | `=` | `(=, valor, _, destino)` | Asignación |
| 10 | `UMINUS` | `(UMINUS, x, _, t)` | Signo menos sobre un operando que no es constante |
| 11 | `PRINT` | `(PRINT, elemento, _, _)` | Imprime un elemento de `print` |
| 12 | `PRINTLN` | `(PRINTLN, _, _, _)` | Termina la línea al final de cada `print` |
| 13 | `READ` | `(READ, _, _, variable)` | Lee un valor en una variable |
| 14 | `END` | `(END, _, _, _)` | Fin del programa |

Los códigos desde el 15 quedan para los saltos y las funciones.

### 2.2 Operandos

Un operando guarda su clase, su nombre (o el texto de su valor), su tipo y, si es constante o
letrero, su valor ya convertido.

| Clase | Ejemplos | Valor guardado |
|---|---|---|
| Variable | `_X_`, `_K_` | Ninguno |
| Temporal | `t1`, `t2` | Ninguno |
| Constante | `42`, `-2`, `3.14` | `Integer` o `Double` ya convertido |
| Letrero | `"hola"` | El contenido sin comillas; las secuencias de escape se interpretan en la máquina virtual |

La clase de cada operando existe para la memoria virtual, que le dará a cada una su propio rango
de direcciones. Además existe un operando especial de error, que nunca aparece en un cuádruplo
(sección 4).

Los operandos solo se crean con cuatro métodos de fábrica del generador: `variable`, `constant`,
`string` y `newTemp`. El constructor de `Operand` no es público, así que el propio compilador de
Java impide crear operandos por fuera de esos métodos. En la entrega de memoria virtual bastará
con que esos cuatro métodos pidan la dirección correspondiente (y que `constant` reutilice una
constante repetida); el resto del generador no cambiará.

Los temporales se numeran con un contador simple (`t1`, `t2`, …) y no se reutilizan. Reutilizarlos
ahorraría memoria, pero es una optimización que no hace falta en este proyecto.

**Constantes.** El árbol de ANTLR guarda las constantes como texto (`"42"`, `"3.14"`); el método
`constant` las convierte a número. Si el signo va pegado a la constante, se convierte junto con
ella, así que `-2147483648` es válido aunque `2147483648` solo no quepa en un `int`. Una constante
entera que no cabe en `int`, o una flotante tan grande que resulta infinita, es un error semántico
(sección 10).

## 3. Estructuras de datos

| Estructura | Implementación | Contenido |
|---|---|---|
| Pila de operadores | `ArrayDeque<OperatorEntry>` | Operadores pendientes con su línea y columna, y fondos falsos |
| Pila de operandos | `ArrayDeque<Operand>` | Operandos pendientes (variables, constantes, temporales) |
| Pila de tipos | `ArrayDeque<Type>` | El tipo de cada operando de la pila de operandos |
| Fila de cuádruplos | `ArrayList<Quadruple>` | Los cuádruplos generados, en orden |
| Notas | `ArrayList<Note>` | Marcas de código que se generará en entregas siguientes |

Las tres pilas y la fila viven en `QuadrupleGenerator` (paquete `com.babyduck.intermediate`), que no
depende de ANTLR. `SemanticAnalyzer` lo llama desde sus puntos neurálgicos.

### 3.1 Pilas

Las pilas se implementan con `ArrayDeque` y no con la clase `Stack` de Java. `Stack` es una clase
antigua que hereda de `Vector`: sincroniza cada operación aunque el compilador use un solo hilo, y
permite acceder por índice a cualquier elemento, lo que rompe la idea de pila. La documentación de
Java recomienda `Deque` en su lugar; `push`, `pop` y `peek` son O(1).

**Pila de operadores.** Guarda cada operador junto con su línea y columna. La posición sirve para
que el mensaje de error de una operación inválida apunte al operador, igual que antes. También
guarda el **fondo falso**, una marca que separa una expresión anidada de los operadores de afuera
(sección 6).

**Pilas de operandos y de tipos.** Avanzan siempre juntas. La pila de tipos está encapsulada:
ninguna parte del compilador la toca directamente; solo se modifica con `pushOperand`, que mete el
operando y su tipo, y `popOperand`, que saca los dos y verifica que coincidan. El tipo con el que
se consulta el cubo es el que sale de la pila de tipos; el tipo guardado en el operando es una
copia verificada que usará la memoria virtual.

### 3.2 Fila de cuádruplos

Los cuádruplos se agregan al final y se recorren en orden, como en una fila. Aun así se guardan en
un `ArrayList` y no en una cola pura: en la entrega de estatutos no lineales habrá que rellenar el
destino de cuádruplos ya generados (`GotoF`, `Goto`), y para eso se necesita acceso por índice, que
una cola no ofrece.

Los cuádruplos se numeran desde 0, de modo que el número de un cuádruplo es su posición en la lista
y rellenar un salto no requiere ajustar índices.

### 3.3 Operaciones principales

| Operación | Qué hace |
|---|---|
| `pushOperand(op)` | Mete el operando y su tipo |
| `popOperand()` | Saca el operando y su tipo, y verifica que coincidan |
| `pushOperator(op, línea, columna)` | Mete un operador con su posición |
| `pushFakeBottom()` / `popFakeBottom()` | Mete o saca el fondo falso |
| `topIs(ops…)` | Indica si el tope de la pila de operadores es alguno de esos operadores (el fondo falso nunca coincide) |
| `generateBinary()` | Saca un operador y dos operandos, consulta el cubo, genera el cuádruplo y mete el temporal |
| `generateUnary(signo)` | Signo sobre el operando del tope: `-` genera `UMINUS`; `+` no genera nada |
| `generateAssign(destino)` | Saca el valor, revisa la asignación con el cubo y genera `=` |
| `emitPrint`, `emitPrintln`, `emitRead`, `emitEnd` | Generan los cuádruplos de los estatutos lineales |
| `note(texto)` | Deja una nota en la posición actual de la fila |
| `format(estilo)` | Único método que convierte la fila en texto |

### 3.4 Impresión de la fila

Toda la impresión pasa por un solo método, `format`, con dos estilos:

- **FULL**, para la consola: tabla con encabezado y las notas en su lugar. En la entrega de memoria
  virtual mostrará cada operando como `dirección(nombre)`.
- **SYMBOLIC**, para las pruebas: una línea por cuádruplo, solo con nombres, sin encabezado ni
  notas. No cambiará cuando lleguen las direcciones, así que las pruebas de esta entrega seguirán
  sirviendo; las direcciones se probarán aparte.

## 4. Errores durante la generación

Las reglas son tres:

1. **Toda expresión deja exactamente un operando en la pila, aunque tenga un error.** Cuando una
   parte de la expresión es inválida (una variable no declarada, una función `void` usada como
   valor, una operación que el cubo rechaza), se mete el operando especial de error. Así las pilas
   nunca se desbalancean.
2. **Un operando con error no produce más mensajes ni cuádruplos.** Si uno de los operandos de una
   operación ya es de error, el resultado también lo es, sin mensaje nuevo: el error original ya se
   reportó. Lo mismo ocurre en una asignación, un elemento de `print`, un `read`, una condición o un
   argumento. Este es el mismo comportamiento que tenía la semántica de la entrega anterior, ahora
   implementado en las pilas.
3. **El generador detecta y `SemanticAnalyzer` reporta.** El generador no conoce el árbol de ANTLR
   ni la lista de errores. Cada operación regresa un resultado con los tipos involucrados, el tipo
   resultante y la posición del operador; si el cubo rechazó una combinación de operandos válidos,
   `SemanticAnalyzer` arma el mensaje (los mismos de la entrega anterior) y lo reporta en esa
   posición.

Un problema interno de las pilas (sacar de una pila vacía, tipos que no coinciden, un fondo falso
que no está donde debería) no es un error del programa sino del compilador, y detiene la ejecución
con una excepción. Los errores del programa nunca usan excepciones: se juntan todos y se muestran
al final.

## 5. Puntos neurálgicos y diagramas

*Pendiente: se completa en los bloques 2, 3 y 5.*

## 6. Traducción de expresiones

*Pendiente: se completa en el bloque 2.*

## 7. Estatutos lineales

*Pendiente: se completa en el bloque 3.*

## 8. Lo que todavía no genera código

*Pendiente: se completa en los bloques 2 y 3.*

## 9. Programas de prueba y fila resultante

*Pendiente: se completa en el bloque 4.*

## 10. Cambios respecto a la entrega 2

Se permite modificar lo entregado antes si queda documentado qué cambió, por qué y qué beneficio
da en la entrega actual. El capítulo de [análisis semántico](semantica.md) se corrigió donde cambió,
para que siempre describa el compilador actual.

### 10.1 Tipo interno para los letreros

**Qué cambió.** Se agregó el tipo interno `string`. En la entrega anterior se decidió no tener un
tipo para los letreros, porque la gramática solo los permite dentro de `print` y nunca llegan a
una expresión.

**Por qué.** Ahora los letreros son operandos de un cuádruplo (`PRINT`), y cada operando lleva un
tipo. Sin un tipo propio, el de un letrero quedaría vacío, y cualquier parte del compilador que
consultara ese tipo tendría que revisar antes si existe.

**Beneficio.** Todos los operandos tienen un tipo válido, y la memoria virtual podrá darle a los
letreros su propio rango de constantes según su tipo, como a las constantes `int` y `float`. Lo que
la entrega anterior garantizaba se conserva: `string` no se puede declarar, no entra a las pilas
(un letrero va directo a `PRINT`) y no está en el cubo semántico, que rechaza por omisión cualquier
combinación no registrada.

---

## Navegación

- Anterior: [← Análisis semántico](semantica.md)
- Siguiente: [Testing →](testing.md)

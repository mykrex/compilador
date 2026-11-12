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

| Código | Operador  | Formato                   | Significado                                       |
| ------ | --------- | ------------------------- | ------------------------------------------------- |
| 1      | `+`       | `(+, izq, der, t)`        | Suma                                              |
| 2      | `-`       | `(-, izq, der, t)`        | Resta                                             |
| 3      | `*`       | `(*, izq, der, t)`        | Multiplicación                                    |
| 4      | `/`       | `(/, izq, der, t)`        | División (siempre da `float`)                     |
| 5      | `>`       | `(>, izq, der, t)`        | Mayor que (da `bool`)                             |
| 6      | `<`       | `(<, izq, der, t)`        | Menor que (da `bool`)                             |
| 7      | `==`      | `(==, izq, der, t)`       | Igual a (da `bool`)                               |
| 8      | `!=`      | `(!=, izq, der, t)`       | Distinto de (da `bool`)                           |
| 9      | `=`       | `(=, valor, _, destino)`  | Asignación                                        |
| 10     | `UMINUS`  | `(UMINUS, x, _, t)`       | Signo menos sobre un operando que no es constante |
| 11     | `PRINT`   | `(PRINT, elemento, _, _)` | Imprime un elemento de `print`                    |
| 12     | `PRINTLN` | `(PRINTLN, _, _, _)`      | Termina la línea al final de cada `print`         |
| 13     | `READ`    | `(READ, _, _, variable)`  | Lee un valor en una variable                      |
| 14     | `END`     | `(END, _, _, _)`          | Fin del programa                                  |

Los códigos desde el 15 quedan para los saltos y las funciones.

### 2.2 Operandos

Un operando guarda su clase, su nombre (o el texto de su valor), su tipo y, si es constante o
letrero, su valor ya convertido.

| Clase     | Ejemplos           | Valor guardado                                                                           |
| --------- | ------------------ | ---------------------------------------------------------------------------------------- |
| Variable  | `_X_`, `_K_`       | Ninguno                                                                                  |
| Temporal  | `t1`, `t2`         | Ninguno                                                                                  |
| Constante | `42`, `-2`, `3.14` | `Integer` o `Double` ya convertido                                                       |
| Letrero   | `"hola"`           | El contenido sin comillas; las secuencias de escape se interpretan en la máquina virtual |

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

| Estructura         | Implementación              | Contenido                                                     |
| ------------------ | --------------------------- | ------------------------------------------------------------- |
| Pila de operadores | `ArrayDeque<OperatorEntry>` | Operadores pendientes con su línea y columna, y fondos falsos |
| Pila de operandos  | `ArrayDeque<Operand>`       | Operandos pendientes (variables, constantes, temporales)      |
| Pila de tipos      | `ArrayDeque<Type>`          | El tipo de cada operando de la pila de operandos              |
| Fila de cuádruplos | `ArrayList<Quadruple>`      | Los cuádruplos generados, en orden                            |
| Notas              | `ArrayList<Note>`           | Marcas de código que se generará en entregas siguientes       |

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

| Operación                                         | Qué hace                                                                                                |
| ------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| `pushOperand(op)`                                 | Mete el operando y su tipo                                                                              |
| `popOperand()`                                    | Saca el operando y su tipo, y verifica que coincidan                                                    |
| `pushOperator(op, línea, columna)`                | Mete un operador con su posición                                                                        |
| `pushFakeBottom()` / `popFakeBottom()`            | Mete o saca el fondo falso                                                                              |
| `topIs(ops…)`                                     | Indica si el tope de la pila de operadores es alguno de esos operadores (el fondo falso nunca coincide) |
| `generateBinary()`                                | Saca un operador y dos operandos, consulta el cubo, genera el cuádruplo y mete el temporal              |
| `generateUnary(signo)`                            | Signo sobre el operando del tope: `-` genera `UMINUS`; `+` no genera nada                               |
| `generateAssign(destino)`                         | Saca el valor, revisa la asignación con el cubo y genera `=`                                            |
| `emitPrint`, `emitPrintln`, `emitRead`, `emitEnd` | Generan los cuádruplos de los estatutos lineales                                                        |
| `note(texto)`                                     | Deja una nota en la posición actual de la fila                                                          |
| `format(estilo)`                                  | Único método que convierte la fila en texto                                                             |

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

_Pendiente: se completa en los bloques 2, 3 y 5._

## 6. Traducción de expresiones

### 6.1 Dónde viven los puntos neurálgicos

La gramática no se modificó para generar código. ANTLR construye el árbol sintáctico completo
antes del recorrido, y los puntos neurálgicos viven en los métodos del Visitor
(`SemanticAnalyzer`), en el orden de cada regla. Se evaluó escribir acciones de Java dentro del
`.g4` y se descartó por tres razones: mezclan Java con la gramática, obligan a regenerar el parser
con cada cambio de una acción, y se ejecutan mientras se reconoce la entrada, incluso si después
aparece un error de sintaxis. Tampoco hacen falta no terminales vacíos que sirvan como
"marcadores" para ejecutar una acción a mitad de una regla, como en los parsers LR: en un método
del Visitor, una acción se escribe directamente entre la visita de un hijo y la del siguiente.

El árbol conserva cada token con su texto, línea y columna, así que el recorrido tiene todo lo
necesario: el texto de las constantes para convertirlas, los letreros de `print` (que la semántica
no usaba) y la posición de cada operador para los mensajes de error.

### 6.2 Algoritmo

La traducción sigue el algoritmo visto en clase, con las tres pilas:

1. Cada operando (variable, constante, temporal de una llamada) se mete a la pila de operandos, y
   su tipo a la de tipos.
2. Cada operador se mete a la pila de operadores, con su posición.
3. **Después de cada operando se revisa el tope de la pila de operadores** para decidir si ya se
   puede generar el cuádruplo:

| Regla        | Después de…      | Si el tope es…        | Acción  |
| ------------ | ---------------- | --------------------- | ------- |
| `term`       | cada `factor`    | `*` o `/`             | Generar |
| `exp`        | cada `term`      | `+` o `-`             | Generar |
| `expression` | la segunda `exp` | `>`, `<`, `==` o `!=` | Generar |

Generar significa: sacar el operador, sacar los operandos derecho e izquierdo con sus tipos,
consultar el cubo con los tipos que salieron de la pila de tipos, pedir un temporal del tipo
resultante, agregar `(op, izq, der, temporal)` a la fila y meter el temporal a las pilas. El
operando izquierdo siempre queda a la izquierda: `-`, `/`, `<` y `>` no son conmutativos.

La precedencia sale de los niveles de la gramática: los `*` y `/` se resuelven al terminar cada
factor, antes de que su término termine y se revisen los `+` y `-`. La asociatividad izquierda
sale de revisar el tope después de cada operando: en `_A_ - _B_ - _C_`, el primer `-` se genera en
cuanto termina `_B_`, antes de meter el segundo.

```
_X_ = _A_ - _B_ - _C_;          _F_ = _A_ + _B_ * _C_ / 2 - 1.5;
0  -  _A_  _B_  t1              0  *  _B_  _C_  t1
1  -  t1   _C_  t2              1  /  t1   2    t2
2  =  t2   _    _X_             2  +  _A_  t2   t3
                                3  -  t3   1.5  t4
                                4  =  t4   _    _F_
```

El tipo de cada temporal lo decide el cubo: `int` con `int` en `+ - *` da un temporal `int`; con
un `float`, uno `float`; toda división da `float` (`7 / 2` genera un temporal `float`); y los
relacionales dan un temporal `bool`.

### 6.3 Fondo falso

El árbol de ANTLR ya separa cada paréntesis en su propio nodo, pero se mantiene el fondo falso
porque el algoritmo revisa el tope de la pila después de cada operando. Sin él, lo de adentro de un
paréntesis usaría operadores de afuera. En `_A_ * (_B_ + _C_)`, al terminar el factor `_B_` el tope
sería el `*` de afuera, y se generaría `_A_ * _B_`, que es incorrecto.

Al abrir el paréntesis se mete el fondo falso; mientras está en el tope, ninguna revisión coincide;
al cerrar el paréntesis se saca. Recorrido de `_A_ * (_B_ + _C_)`:

| Paso | Acción                                               | Operadores | Operandos | Tipos   | Cuádruplo           |
| ---- | ---------------------------------------------------- | ---------- | --------- | ------- | ------------------- |
| 1    | Factor `_A_`: meter; ¿tope `*` `/`? no               |            | `_A_`     | int     |                     |
| 2    | Meter `*`                                            | `*`        | `_A_`     | int     |                     |
| 3    | `(`: meter fondo falso                               | `*` ⊥      | `_A_`     | int     |                     |
| 4    | Factor `_B_`: meter; ¿tope `*` `/`? no (fondo falso) | `*` ⊥      | `_A_ _B_` | int int |                     |
| 5    | Meter `+`                                            | `*` ⊥ `+`  | `_A_ _B_` | int int |                     |
| 6    | Factor `_C_`; término terminado: ¿tope `+` `-`? sí   | `*` ⊥      | `_A_ t1`  | int int | `(+, _B_, _C_, t1)` |
| 7    | `)`: sacar fondo falso                               | `*`        | `_A_ t1`  | int int |                     |
| 8    | Factor terminado: ¿tope `*` `/`? sí                  |            | `t2`      | int     | `(*, _A_, t1, t2)`  |

Por la misma razón, **cada argumento de una llamada se evalúa entre fondos falsos**: en
`_A_ * _F_(_B_ + 1)`, al terminar el operando `_B_` del argumento el tope sería el `*` de afuera.

### 6.4 Signo y constantes negativas

En BabyDuck las constantes se escriben sin signo, pero el signo puede ir en cualquier factor. El
compilador lo traduce así:

| Caso                               | Ejemplo                         | Traducción                                        |
| ---------------------------------- | ------------------------------- | ------------------------------------------------- |
| Signo `-` pegado a una constante   | `-2`, `-3.5`                    | Constante negativa directamente, sin cuádruplo    |
| Signo `-` en cualquier otro factor | `-_A_`, `-(_A_ + 1)`, `-_F_(3)` | `(UMINUS, x, _, t)`; el temporal conserva el tipo |
| Signo `+`                          | `+_A_`, `+2`                    | Ningún cuádruplo (sí se valida con el cubo)       |

Se compararon tres formas de traducir el signo. Usar siempre `UMINUS` es uniforme, pero genera un
cuádruplo y un temporal por cada número negativo escrito en el programa. Traducir `-x` como
`0 - x` necesita una constante 0 del tipo correcto y es menos claro al leer la fila. Crear la
constante negativa ahorra el cuádruplo y el temporal, y en la memoria virtual la constante `-2`
ocupará su lugar en la tabla de constantes como cualquier otra. Su costo es que el signo tiene dos
caminos; la regla queda precisa porque solo aplica cuando el signo va pegado al número: `-(2)` pasa
por el paréntesis y genera `UMINUS`.

```
_X_ = -2;        0  =       -2   _    _X_
_X_ = -_A_;      1  UMINUS  _A_  _    t1
                 2  =       t1   _    _X_
_X_ = _A_ - -2;  3  -       _A_  -2   t2
                 4  =       t2   _    _X_
```

## 7. Estatutos lineales

### 7.1 Asignación

`_X_ = expresión;` tiene dos puntos. Después del `id`, se valida que sea una variable visible y se
conserva como destino. Después de la expresión, se saca el valor de las pilas, se revisa con el
cubo (destino ← valor) y se genera `(=, valor, _, destino)`.

Esto se aparta del algoritmo visto en clase, donde el `id` y el `=` también entran a las pilas de
operandos y de operadores. Aquí el destino no entra a las pilas porque no es operando de una
expresión, y en BabyDuck el `=` es un estatuto que no se encadena: meterlo a las pilas solo
serviría para sacarlo enseguida. El destino se pasa directamente al generador; la validación y los
mensajes de error son los mismos.

La promoción `int → float` no necesita un cuádruplo de conversión: `_F_ = 2;` genera
`(=, 2, _, _F_)`. La conversión le corresponde a la máquina virtual, que al guardar en un destino
`float` convertirá el valor. Lo mismo ocurre en las operaciones mixtas, en los argumentos y en
`return`.

### 7.2 `print`

Cada elemento genera un `PRINT`, en orden; un letrero es un operando más. Al final de cada `print`
se genera un `PRINTLN`. Si la máquina virtual tuviera que adivinar el salto de línea,
`print("a"); print("b");` no podría distinguirse de `print("a", "b");`. No se agrega espacio
automático entre elementos; esa decisión le corresponde a la máquina virtual.

```
print("a =", _A_, _A_ + 1 > _B_);     0  PRINT    "a ="  _    _
                                      1  PRINT    _A_    _    _
                                      2  +        _A_    1    t1
                                      3  >        t1     _B_  t2
                                      4  PRINT    t2     _    _
                                      5  PRINTLN  _      _    _
```

### 7.3 `read` y fin del programa

`read(_X_);` valida la variable y genera `(READ, _, _, _X_)`. Al terminar `main` se genera
`(END, _, _, _)`.

Al terminar cada estatuto, las tres pilas deben quedar vacías. El compilador lo verifica: si no
fuera así, sería un error interno y se detendría.

## 8. Lo que todavía no genera código

Los saltos, las llamadas, `return` y el inicio y fin de las funciones se generan en las entregas
siguientes. Mientras tanto, el compilador no falla con esos estatutos: genera todo lo lineal que
contienen (la condición de un `if`, los argumentos de una llamada, los cuerpos) y deja una **nota**
en el lugar exacto donde irá el código que falta. Las notas se guardan en una lista aparte de la
fila, así que la fila solo contiene cuádruplos reales y ningún temporal queda sin explicación.
Cuando lleguen las siguientes entregas, las notas simplemente dejarán de agregarse.

| Construcción             | Lo que se genera ahora                     | Nota                                                                                      |
| ------------------------ | ------------------------------------------ | ----------------------------------------------------------------------------------------- |
| Programa con funciones   | Nada extra                                 | Posición 0: "aquí irá Goto a main"; al empezar `main`: "aquí se rellenará el Goto a main" |
| Función                  | Los cuádruplos de su cuerpo                | "inicio de la función" y "aquí irá ENDFUNC"                                               |
| `if` / `else`            | Condición y cuerpos                        | "aquí irá GotoF t", "aquí irá Goto…", "aquí se rellenará…"                                |
| `while`                  | Condición y cuerpo                         | "destino del Goto de regreso", "aquí irá GotoF t", "aquí irá Goto al inicio…"             |
| Llamada                  | Los argumentos (entre fondos falsos)       | "aquí irá ERA", "aquí irá PARAM a (argumento k)", "aquí irá GOSUB"                        |
| Llamada en una expresión | Además, un temporal con el tipo de retorno | "… t recibirá su valor de retorno"                                                        |
| `return`                 | Su expresión                               | "aquí irá RETURN t"                                                                       |

En la consola, las notas aparecen con `·` antes del cuádruplo de su posición:

```
#  op   izq  der  res
   · aquí irá Goto a main
   · inicio de la función _F_
0  *    _P_  2    t1
   · return: aquí irá RETURN t1
   · fin de la función _F_: aquí irá ENDFUNC
   · inicio de main: aquí se rellenará el Goto a main
   · llamada a _F_ (línea 5): aquí irá ERA _F_
1  +    _B_  1    t2
   · aquí irá PARAM t2 (argumento 1)
   · aquí irá GOSUB _F_; t3 recibirá su valor de retorno
2  *    _A_  t3   t4
3  =    t4   _    _X_
4  END  _    _    _
```

## 9. Programas de prueba y fila resultante

_Pendiente: se completa en el bloque 4._

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

### 10.2 Las pilas como única fuente de tipos

**Qué cambió.** En la entrega anterior, cada método de expresión del Visitor devolvía el tipo de
lo que analizaba (`BabyDuckBaseVisitor<Type>`), y por eso no se necesitaba una pila de tipos. Ahora
los métodos de expresión no devuelven nada (`BabyDuckBaseVisitor<Void>`): dejan su resultado
(operando y tipo) en las pilas, y quien usa la expresión (asignación, condición, argumento,
`return`, `print`) lo saca de ahí. El cubo se consulta al sacar los operandos, como en el algoritmo
de clase.

**Por qué.** Esta entrega pide pilas de operadores, operandos y tipos. Conservar el valor de
retorno y además llenar las pilas dejaría dos fuentes del mismo dato que podrían no coincidir.

**Beneficio.** El tipo de cada operación y su cuádruplo salen del mismo lugar y en el mismo
momento. El Visitor se mantiene, aunque ya no devuelva tipos, porque permite acciones a mitad de
una regla: las necesitarán los saltos de `if` y `while` en la siguiente entrega.

### 10.3 Errores en cadena a través de las pilas

**Qué cambió.** En la entrega anterior, un operando con error era un tipo `error` devuelto por el
método de expresión. Ahora es un operando especial de error que viaja por las pilas (sección 4).

**Por qué.** Al pasar los tipos a las pilas, el mecanismo tenía que pasar con ellos.

**Beneficio.** El comportamiento es el mismo: un error se reporta una vez y no produce mensajes
en cadena. Además, las pilas nunca se desbalancean y nunca se genera un cuádruplo con un operando
inválido. Se verificó comparando, carácter por carácter, la lista de errores (mensaje, línea y
columna) de los 22 programas de prueba semánticos y de dos programas con 49 errores mezclados,
antes y después del cambio.

### 10.4 Constantes que no caben en su tipo

**Qué cambió.** Hay un error semántico nuevo: `la constante 2147483648 no cabe en int`. También se
reporta una constante flotante tan grande que resulta infinita.

**Por qué.** Hasta la entrega anterior las constantes se quedaban como texto. Ahora se convierten
a número para guardarlas en su operando, y una constante que no cabe no puede convertirse.

**Beneficio.** El error aparece al compilar, con su línea, en lugar de fallar en la máquina
virtual. Cuando el signo va pegado, la constante se convierte con él, de modo que `-2147483648`
(el menor `int`) es válida.

### 10.5 Puntos neurálgicos que ahora también generan cuádruplos

**Qué cambió.** Los puntos de uso de la expresión, el término, el factor, la asignación y `read`
validaban tipos y nombres. Ahora, en el mismo lugar, también generan su cuádruplo. Conservan su
número; los puntos que solo generan código se numeran desde el 18 (sección 5).

**Por qué.** Con las pilas como única fuente de tipos, la validación ocurre justo al sacar los
operandos, que es donde se genera el cuádruplo.

**Beneficio.** Un solo juego de puntos describe la validación y la traducción, y los números de
la entrega anterior siguen significando lo mismo en todos los documentos.

---

## Navegación

- Anterior: [← Análisis semántico](semantica.md)
- Siguiente: [Testing →](testing.md)

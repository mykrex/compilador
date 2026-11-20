# Código intermedio

## Índice

1. [Overview](01_overview.md)
2. [Herramientas](02_herramientas.md)
3. [Análisis léxico](03_lexico.md)
4. [Análisis sintáctico](04_sintaxis.md)
5. [Análisis semántico](05_semantica.md)
6. **Código intermedio** (este capítulo)
7. [Memoria virtual](07_memoria_virtual.md)
8. [Testing](08_testing.md)
9. [Herramientas de IA](09_herramientas_ia.md)
10. [Guía de uso](10_guia_de_uso.md)

---

## Qué hace

Genera **cuádruplos** para todo el programa: expresiones aritméticas y relacionales, estatutos
lineales (asignación, `print`, `read`), saltos de `if` y `while`, y declaración e invocación de
funciones. Cada operando lleva su dirección virtual ([Memoria virtual](07_memoria_virtual.md)).
La generación ocurre en la
misma pasada que la semántica: `SemanticAnalyzer` llama a `QuadrupleGenerator` desde sus puntos
neurálgicos. Si hay cualquier error, la fila se descarta (`Result.quadruples()` es `null`).

La gramática no cambió: los puntos viven en los métodos del Visitor, no en acciones dentro del
`.g4` (mezclan Java con la gramática y corren aunque haya errores de sintaxis).

| Clase | Paquete | Qué hace |
|---|---|---|
| `QuadrupleGenerator` | `intermediate` | Las pilas de expresiones, la pila de saltos, la fábrica de operandos y la generación |
| `QuadrupleQueue` | `intermediate` | La fila de cuádruplos, el relleno de saltos y el único método de impresión |
| `Quadruple`, `QuadOp` | `intermediate` | Un cuádruplo; los operadores con código numérico fijo |
| `Operand`, `OperandKind`, `TypedOperand` | `intermediate` | Operandos: variable, temporal, constante, letrero, destino de salto, función y número de parámetro |
| `Outcome` | `intermediate` | Resultado de una operación (para reportar errores) |
| `VirtualMemory`, `ConstantTable`, `Segment` | `intermediate` | Direcciones virtuales ([Memoria virtual](07_memoria_virtual.md)) |
| `SemanticAnalyzer` | `babyduck` | Puntos neurálgicos: valida, genera y reporta |

## Cuádruplos

`(operador, izquierdo, derecho, resultado)`; un campo vacío se imprime como `_`. El operador es
un enum y los operandos son objetos; el texto solo aparece al imprimir.

| Código | Operador | Formato |
|---|---|---|
| 1–4 | `+ - * /` | `(op, izq, der, t)`; `/` siempre da `float` |
| 5–8 | `> < == !=` | `(op, izq, der, t)` con `t` de tipo `bool` |
| 9 | `=` | `(=, valor, _, destino)` |
| 10 | `UMINUS` | `(UMINUS, x, _, t)` |
| 11, 12 | `PRINT`, `PRINTLN` | `(PRINT, elemento, _, _)`, `(PRINTLN, _, _, _)` |
| 13, 14 | `READ`, `END` | `(READ, _, _, variable)`, `(END, _, _, _)` |
| 15, 16 | `GOTO`, `GOTOF` | `(GOTO, _, _, destino)`, `(GOTOF, condición, _, destino)` |
| 17 | `ERA` | `(ERA, función, _, _)`: prepara la memoria de la llamada |
| 18 | `PARAM` | `(PARAM, argumento, _, k)`, con `k` desde 1 |
| 19 | `GOSUB` | `(GOSUB, función, _, inicio)` |
| 20 | `RETURN` | `(RETURN, valor, _, dirección de regreso)`; en una función `void`, `(RETURN, _, _, _)` |
| 21 | `ENDFUNC` | `(ENDFUNC, _, _, _)` |

- **`QuadOp` aparte de `Op`:** `Op` es lo que consulta el cubo; `QuadOp`, lo que ejecuta la máquina virtual. Códigos escritos a mano (no `ordinal()`).
- **Operandos:** guardan clase, nombre, tipo, dirección virtual y, si son constantes, su valor convertido. Solo se crean con los métodos de fábrica (constructor no público), que son el único lugar donde reciben su dirección. El destino de un salto, una función y un número de parámetro son operandos sin dirección (`QUAD_INDEX`, `FUNCTION`, `PARAM_INDEX`).
- **Temporales:** `t1, t2…` sin reutilizar; el nombre sigue un solo contador y la dirección se reinicia en cada función y en `main`.

## Estructuras

| Estructura | Implementación | Por qué |
|---|---|---|
| Pila de operadores | `ArrayDeque` | Guarda cada operador con su línea y columna (para el mensaje de error) y el fondo falso |
| Pila de operandos | `ArrayDeque` | Operandos pendientes |
| Pila de tipos | `ArrayDeque` | Solo cambia con `pushOperand`/`popOperand`, que mueven operando y tipo juntos y verifican que coincidan |
| Pila de saltos | `ArrayDeque<Integer>` | Números de cuádruplos pendientes de rellenar y el regreso de cada `while` |
| Fila | `ArrayList` | Acceso por índice para rellenar el destino de los saltos; se numera desde 0 |

`ArrayDeque` en lugar de `Stack` (heredada de `Vector`, sincronizada y con acceso por índice).

Operaciones principales: `pushOperand`, `popOperand`, `pushOperator`, `pushFakeBottom`,
`popFakeBottom`, `topIs`, `generateBinary`, `generateUnary`, `generateAssign`, `emitPrint`,
`emitPrintln`, `emitRead`, `emitEnd`; saltos: `startIf`, `startElse`, `endIf`, `markLoopStart`,
`startLoopBody`, `endLoop`; funciones: `startProgram`, `startMain`, `emitEra`, `emitParam`,
`emitGosub`, `emitCallResult`, `emitReturn`, `emitEndFunc`. `format(Style)` tiene tres estilos:
`FULL` (consola, cada operando como `dirección(nombre)`), `SYMBOLIC` (solo nombres) y `ADDRESS`
(códigos y direcciones, lo que verá la máquina virtual); las pruebas usan los dos últimos.

**Errores.** Toda expresión deja un operando en la pila, aunque sea `Operand.ERROR`; un operando de
error no genera mensajes ni cuádruplos (sin errores en cadena). El generador detecta y regresa un
`Outcome`; `SemanticAnalyzer` arma el mensaje. Un error interno de las pilas lanza
`IllegalStateException`.

## Expresiones

Algoritmo de clase: cada operando entra a las pilas y, **después de cada operando, se revisa el
tope** de la pila de operadores (`* /` tras un factor, `+ -` tras un término, relacional tras el
segundo lado). Si coincide, se sacan operador y operandos, se consulta el cubo con los tipos de
la pila y se genera el cuádruplo con un temporal del tipo resultante.

- **Fondo falso** en cada paréntesis y en cada argumento de una llamada: sin él, en `_A_ * (_B_ + _C_)` el tope al terminar `_B_` sería el `*` de afuera.
- **Signo:** pegado a una constante crea la constante negativa (`-2`, sin cuádruplo); en otro caso, `UMINUS`; el `+` no genera nada.

```
_X_ = _A_ * (_B_ + _C_);        _X_ = _A_ - -2;
0  +  _B_  _C_  t1              0  -  _A_  -2  t1
1  *  _A_  t1   t2              1  =  t1   _   _X_
2  =  t2   _    _X_
```

## Estatutos lineales

- **Asignación:** se valida el destino, que no entra a las pilas (se aparta del algoritmo de clase porque no es operando y `=` no se encadena); después se saca el valor, se revisa con el cubo y se genera `=`. `float ← int` no lleva cuádruplo de conversión.
- **`print`:** un `PRINT` por elemento, en orden, y un `PRINTLN` al final; sin espacio automático.
- **`read`:** `(READ, _, _, variable)`. **Fin:** `END` al terminar `main`.
- Al terminar cada estatuto las pilas de expresiones deben quedar vacías (se verifica).

## Saltos

Algoritmo de clase con una **pila de saltos** y `cont` (número del siguiente cuádruplo):

- **`if`:** después de la condición, `GOTOF` con destino pendiente y su número a la pila. Sin
  `else`, al final del cuerpo se rellena con `cont`. Con `else`: `GOTO` pendiente, se rellena el
  `GOTOF` con el inicio del `else` y se mete el `GOTO`, que se rellena al final.
- **`while`:** antes de la condición se mete `cont` (regreso); después, `GOTOF` pendiente. Al
  final del cuerpo, `GOTO` al regreso y se rellena el `GOTOF` con `cont`.
- La condición debe ser `bool`. Si ya tenía un error, el `GOTOF` se genera igual para que la pila
  de saltos no se desbalancee (con errores la fila se descarta).

```
if (_A_ < _B_) { _A_ = 2; } else { _A_ = 3; };      while (_A_ != 0) do { ...; _A_ = _A_ - 1; };
4  <      _A_  _B_  t2                               9   !=     _A_  0    t3
5  GOTOF  t2   _    8                                10  GOTOF  t3   _    18
6  =      2    _    _A_                              ...
7  GOTO   _    _    9                                16  =      t5   _    _A_
8  =      3    _    _A_                              17  GOTO   _    _    9
```

## Funciones y llamadas

- **Programa:** el cuádruplo 0 siempre es `GOTO` a `main` (se rellena al llegar a `main`), haya o no funciones.
- **Declaración:** antes del cuerpo se guarda el inicio de la función; al final se genera `ENDFUNC`, se guardan sus tamaños y se libera su tabla local ([Memoria virtual](07_memoria_virtual.md)).
- **`return`:** `RETURN valor _ dirección de regreso` (una dirección global por función con valor) y regresa en ese momento; en una función `void`, `return;` genera `RETURN _ _ _`. `ENDFUNC` solo se ejecuta si no hubo `RETURN` antes.
- **Llamada:** primero se evalúan **todos** los argumentos; después `ERA`, un `PARAM` por argumento en orden y `GOSUB` al inicio de la función. Así una llamada anidada como `f(g(1))` termina completa antes del `ERA` de `f`, que prepara una sola memoria a la vez.
- **Valor de regreso:** dentro de una expresión, después del `GOSUB` se copia la dirección de regreso a un temporal propio (`= _F_ _ tN`), así `f(5) + f(3)` no pierde el primer valor. Una llamada usada como estatuto no genera la copia.

```
_A_ = _Fact_(5) + _Fact_(3);
15  ERA    _Fact_  _   _          19  ERA    _Fact_  _   _          23  +  t6  t7  t8
16  PARAM  5       _   1          20  PARAM  3       _   1          24  =  t8  _   _A_
17  GOSUB  _Fact_  _   1          21  GOSUB  _Fact_  _   1
18  =      _Fact_  _   t6         22  =      _Fact_  _   t7
```

## Puntos neurálgicos

Diagramas en [`diagramas/`](diagramas/): un `.drawio` con una página por diagrama, sus imágenes y
el [pseudocódigo de cada punto](diagramas/pseudocodigo_puntos.md). Los puntos se numeran del 1 al
30 en el orden de los diagramas (el de la gramática); una acción que aparece en varios diagramas
conserva su número. En el código, cada punto está marcado con un comentario `punto N`.

| Punto | Diagrama | Acción |
|---|---|---|
| 1–4 | `<Programa>` | Directorio y `GOTO` a main pendiente / nombre / antes de `main`: rellenar el `GOTO` / `END` y tamaños |
| 5 | `<VARS>` (y `<FUNCS>`) | Declarar cada variable con su dirección |
| 6–9 | `<FUNCS>` | Registrar la función y su dirección de regreso / parámetro / inicio / `ENDFUNC`, tamaños y liberar la tabla local |
| 10, 11 | `<ASSIGN>` | Validar el destino / validar y generar `=` |
| 12–14 | `<CONDITION>` | Condición `bool` y `GOTOF` / `GOTO` del `else` y relleno / relleno al final |
| 15, 12, 16 | `<CYCLE>` | Regreso / condición `bool` y `GOTOF` / `GOTO` al regreso y relleno |
| 17–21 | `<F_Call>` | Validar la función / meter y sacar el fondo falso / guardar y revisar cada argumento / `ERA`, `PARAM`, `GOSUB` y copia del regreso |
| 22, 23 | `<Print>` | `PRINT` por elemento / `PRINTLN` |
| 24 | `<Read>` | Validar y generar `READ` |
| 25 | `<Return>` | Reglas de `return` y `RETURN` |
| 26–28 | `<EXPRESIÓN>`, `<EXP>`, `<TÉRMINO>` | Meter el operador / relacional / `+ - * /` |
| 18, 19, 29, 30 | `<FACTOR>` | Fondo falso del paréntesis / meter el operando con su dirección / signo (`UMINUS`) |

## Ejemplo de salida

```
program _E_;
var _A_, _X_ : int;
main { _X_ = _A_ + 2; print("x = ", _X_); }
end
```

```
=== Cuádruplos ===
#  op       izq            der      res
0  GOTO     _              _        1
1  +        1000(_A_)      8000(2)  5000(t1)
2  =        5000(t1)       _        1001(_X_)
3  PRINT    10000("x = ")  _        _
4  PRINT    1001(_X_)      _        _
5  PRINTLN  _              _        _
6  END      _              _        _

=== Tabla de constantes ===
8000   int     2
10000  string  "x = "
```

La fila esperada de cada programa de prueba (Q01–Q10) está dentro de su archivo en
`src/test/resources/quads/valid/`, en nombres y, en los más recientes, también en direcciones
(ver [Testing](08_testing.md)).

## Cambios respecto a la entrega anterior

- Cada operando lleva su dirección virtual; la consola muestra `dirección(nombre)`, hay un estilo de impresión solo con códigos y direcciones, y una tabla de constantes sin repetidos.
- Saltos de `if`, `if-else` y `while` con pila de saltos (`GOTO` 15, `GOTOF` 16).
- Funciones: `GOTO` a main en el cuádruplo 0, `ERA`, `PARAM`, `GOSUB`, `RETURN` con dirección global de regreso y `ENDFUNC` (códigos 17–21); los argumentos se evalúan antes del `ERA` y el valor de regreso se copia a un temporal propio.
- Ya no hay notas en la fila: todo lo que marcaban se genera.
- Numeración nueva de los puntos neurálgicos, secuencial del 1 al 30 y sin letras. Equivalencias con la anterior: 1→1, 2→2, 3→5, 4→6, 5→7, 6→9, 7→3, 8a→29, 8b→30, 9→28, 10→27, 11a→10, 11b→11, 12→12, 13→24, 14→17 y 20, 15→25, 16→6, 18 y 19→26, 20→18, 21→19, 22→22, 23→23, 24→4. La revisión de código inalcanzable (antes 17) queda como regla sin número ([Análisis semántico](05_semantica.md#validaciones)).

---

## Navegación

- Anterior: [← Análisis semántico](05_semantica.md)
- Siguiente: [Memoria virtual →](07_memoria_virtual.md)

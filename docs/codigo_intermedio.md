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

---

## 1. Qué hace

Genera **cuádruplos** para las expresiones aritméticas y relacionales y para los estatutos
lineales (asignación, `print`, `read`), más `END` al final de `main`. La generación ocurre en la
misma pasada que la semántica: `SemanticAnalyzer` llama a `QuadrupleGenerator` desde sus puntos
neurálgicos. Si hay cualquier error, la fila se descarta (`Result.quadruples()` es `null`).

La gramática no cambió: los puntos viven en los métodos del Visitor, no en acciones dentro del
`.g4` (mezclan Java con la gramática y corren aunque haya errores de sintaxis).

| Clase | Paquete | Qué hace |
|---|---|---|
| `QuadrupleGenerator` | `intermediate` | Las tres pilas, la fábrica de operandos y la generación |
| `QuadrupleQueue` | `intermediate` | La fila de cuádruplos, las notas y el único método de impresión |
| `Quadruple`, `QuadOp` | `intermediate` | Un cuádruplo; los operadores con código numérico fijo |
| `Operand`, `OperandKind`, `TypedOperand` | `intermediate` | Operandos (variable, temporal, constante, letrero) |
| `Outcome`, `Note` | `intermediate` | Resultado de una operación (para reportar errores); nota de código pendiente |
| `SemanticAnalyzer` | `babyduck` | Puntos neurálgicos: valida, genera y reporta |

## 2. Cuádruplos

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

- **`QuadOp` aparte de `Op`:** `Op` es lo que consulta el cubo; `QuadOp`, lo que ejecuta la máquina virtual. Códigos escritos a mano (no `ordinal()`); del 15 en adelante, para saltos y funciones.
- **Operandos simbólicos** (`_X_`, `t1`, `-2`, `"hola"`): guardan clase, nombre, tipo y, si son constantes, su valor convertido. Solo se crean con `variable`, `constant`, `string` y `newTemp` (constructor no público), así que en la entrega de memoria solo esos métodos asignarán la dirección.
- **Temporales:** contador simple `t1, t2…`, sin reutilizar.

## 3. Estructuras

| Estructura | Implementación | Por qué |
|---|---|---|
| Pila de operadores | `ArrayDeque` | Guarda cada operador con su línea y columna (para el mensaje de error) y el fondo falso |
| Pila de operandos | `ArrayDeque` | Operandos pendientes |
| Pila de tipos | `ArrayDeque` | Solo cambia con `pushOperand`/`popOperand`, que mueven operando y tipo juntos y verifican que coincidan |
| Fila | `ArrayList` | Los saltos de la siguiente entrega necesitan rellenar cuádruplos por índice; se numera desde 0 |

`ArrayDeque` en lugar de `Stack` (heredada de `Vector`, sincronizada y con acceso por índice).

Operaciones principales: `pushOperand`, `popOperand`, `pushOperator`, `pushFakeBottom`,
`popFakeBottom`, `topIs`, `generateBinary`, `generateUnary`, `generateAssign`, `emitPrint`,
`emitPrintln`, `emitRead`, `emitEnd`, `note` y `format(Style)`. `format` tiene dos estilos:
`FULL` (consola, con notas) y `SYMBOLIC` (solo nombres; lo usan las pruebas y no cambiará con las
direcciones).

**Errores.** Toda expresión deja un operando en la pila, aunque sea `Operand.ERROR`; un operando de
error no genera mensajes ni cuádruplos (sin errores en cadena). El generador detecta y regresa un
`Outcome`; `SemanticAnalyzer` arma el mensaje. Un error interno de las pilas lanza
`IllegalStateException`.

## 4. Expresiones

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

## 5. Estatutos lineales

- **Asignación:** se valida el destino, que no entra a las pilas (se aparta del algoritmo de clase porque no es operando y `=` no se encadena); después se saca el valor, se revisa con el cubo y se genera `=`. `float ← int` no lleva cuádruplo de conversión.
- **`print`:** un `PRINT` por elemento, en orden, y un `PRINTLN` al final; sin espacio automático.
- **`read`:** `(READ, _, _, variable)`. **Fin:** `END` al terminar `main`.
- Al terminar cada estatuto las pilas deben quedar vacías (se verifica).

**Lo que aún no genera código** (saltos, llamadas, `return`, funciones): se genera lo lineal que
contienen y una **nota** marca dónde irá lo demás, por ejemplo `· if (línea 5): aquí irá GotoF t1`.

## 6. Puntos neurálgicos

Diagramas en [`diagramas/`](diagramas/) (un `.drawio` con todas las páginas, PNG y
[pseudocódigo](diagramas/pseudocodigo_puntos.md)). Cada número conserva su significado; una letra
indica que un punto de la entrega 2 ahora ocurre en dos posiciones; los nuevos van desde el 18.

| Punto | Diagrama | Acción |
|---|---|---|
| 8a / 8b | `<FACTOR>` | Validar y meter el operando / aplicar el signo (`UMINUS`) |
| 9 | `<EXP>`, `<TÉRMINO>` | Si el tope es su operador: validar y generar |
| 10 | `<EXPRESIÓN>` | Si el tope es relacional: validar y generar (`bool`) |
| 11a / 11b | `<ASSIGN>` | Validar el destino / sacar el valor, validar y generar `=` |
| 13 | `<Read>` | Validar y generar `READ` |
| 18, 19 | `<EXPRESIÓN>`, `<EXP>`, `<TÉRMINO>` | Meter el operador |
| 20, 21 | `<FACTOR>` | Meter / sacar el fondo falso |
| 22, 23 | `<Print>` | `PRINT` por elemento / `PRINTLN` |
| 24 | `<Programa>` | `END` |

## 7. Ejemplo de salida

```
=== Cuádruplos ===
#  op       izq     der  res
0  +        _A_     _B_  t1
1  UMINUS   _C_     _    t2
2  *        t1      t2   t3
3  =        t3      _    _X_
4  PRINT    "x = "  _    _
5  PRINT    _X_     _    _
6  PRINTLN  _       _    _
7  END      _       _    _
```

La fila esperada de cada programa de prueba (Q01–Q06) está dentro de su archivo en
`src/test/resources/quads/valid/` (ver [Testing](testing.md)).

## 8. Cambios respecto a la entrega anterior

- Tipo interno `string`, solo para letreros (operandos de `PRINT`); fuera del cubo y de las pilas.
- Las pilas son la única fuente de tipos: el Visitor es `BabyDuckBaseVisitor<Void>` y se conserva por las acciones a mitad de regla.
- Los errores en cadena se evitan con `Operand.ERROR` en las pilas; mismos mensajes, verificado antes y después.
- Error nuevo: `la constante 2147483648 no cabe en int` (también un `float` infinito).
- Los puntos 8–11 y 13 también generan cuádruplos; nuevos puntos 18–24.

---

## Navegación

- Anterior: [← Análisis semántico](semantica.md)
- Siguiente: [Testing →](testing.md)

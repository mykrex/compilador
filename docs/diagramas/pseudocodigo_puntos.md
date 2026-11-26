# Puntos neurálgicos: pseudocódigo

Acompaña a `BabyDuck_puntos_neuralgicos.drawio` (una página por diagrama) y a sus imágenes `.png`.

**Numeración.** Los puntos se numeran del 1 al 30 en el orden de los diagramas (el de la
gramática) y, dentro de cada diagrama, de izquierda a derecha. Una acción que aparece en varios
diagramas conserva su número (por ejemplo, el 5 en `<VARS>` y en `<FUNCS>`, el 12 en `<CONDITION>`
y en `<CYCLE>`, el 26 en `<EXPRESIÓN>`, `<EXP>` y `<TÉRMINO>`). En el código, cada punto está
marcado con un comentario `punto N`.

Notación: `PilaO` = pila de operandos, `PTipos` = pila de tipos, `POper` = pila de operadores,
`PSaltos` = pila de saltos, `Fila` = fila de cuádruplos, `cont` = número del siguiente cuádruplo.
`meter(x)` mete el operando y su tipo juntos; `sacar()` saca los dos. `rellenar(n, d)` pone `d`
como destino del salto `n`.

## `<Programa>`

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 1 | Sobre `program` | `directorio ← nuevo directorio (con su tabla de globales)`<br>`Fila.agregar(GOTO, _, _, ?); PSaltos.meter(0)` (el cuádruplo 0 siempre salta a main) |
| 2 | Entre `id` y `;` | `directorio.nombrePrograma ← id` |
| 3 | Antes de `main` | `funciónActual ← ninguna` (main solo ve las globales)<br>`temporales desde la base de su segmento`<br>`rellenar(PSaltos.sacar(), cont)` |
| 4 | Después de `Body` | `Fila.agregar(END, _, _, _)`<br>`directorio guarda el tamaño del segmento global y de los temporales de main` |

## `<VARS>`

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 5 | Entre `TYPE` y `;` | `para cada id de la lista:`<br>`  si el nombre ya existe en el alcance actual o ya lo usa el programa, una función o una global → error`<br>`  si no → dirección ← siguiente dirección (global o local) de su tipo; agregar (id, tipo, dirección) a las globales o a la tabla de funciónActual` |

## `<FUNCS>`

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 6 | Entre `id` y `(` | `si el nombre ya se usa → error; si no → directorio.agregar(nueva función(id, tipo))` (registrada antes del cuerpo: permite recursión)<br>`si tipo ≠ void → dirección de regreso ← siguiente dirección global de su tipo`<br>`si tipo ≠ void y el cuerpo no regresa en todos los caminos → error`<br>`funciónActual ← la función; locales y temporales desde la base de su segmento` |
| 7 | Después del `TYPE` de cada parámetro | `si el nombre se repite o ya se usa → error; si no → dirección ← siguiente local de su tipo; agregar a la lista de parámetros y a la tabla de funciónActual` |
| (5) | Dentro de `VARS` | Las locales usan el punto 5 con la tabla de funciónActual (después de los parámetros) |
| 8 | Antes de `Body` | `funciónActual.inicio ← cont` |
| 9 | Después de `Body` | `Fila.agregar(ENDFUNC, _, _, _)`<br>`funciónActual.tamaño ← cantidad de locales y temporales de cada tipo`<br>`liberar la tabla local (los parámetros quedan en la lista con su dirección)`<br>`funciónActual ← ninguna` |

## `<ASSIGN>`

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 10 | Después de `id` | `destino ← la variable si es visible; si no → reportar; destino ← error` (el destino no entra a las pilas) |
| 11 | Después de `EXPRESIÓN` | `valor ← sacar()`<br>`si destino o valor es error → nada`<br>`si no cabe (cubo: destino ← valor) → reportar "no se puede asignar …"`<br>`si no → Fila.agregar(=, valor, _, destino)` |

## `<CONDITION>`

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 12 | Después de `)` | `c ← sacar(); si c no es bool → reportar "la condición … debe ser una comparación"`<br>`Fila.agregar(GOTOF, c, _, ?); PSaltos.meter(cont - 1)` |
| 13 | Después de `else` | `Fila.agregar(GOTO, _, _, ?)`<br>`falso ← PSaltos.sacar(); PSaltos.meter(cont - 1); rellenar(falso, cont)` |
| 14 | Antes de `;` | `rellenar(PSaltos.sacar(), cont)` |

## `<CYCLE>`

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 15 | Después de `while` | `PSaltos.meter(cont)` (regreso: inicio de la condición) |
| 12 | Después de `)` | Igual que en `<CONDITION>`: condición bool y `GOTOF` |
| 16 | Después de `Body` | `fin ← PSaltos.sacar(); regreso ← PSaltos.sacar()`<br>`Fila.agregar(GOTO, _, _, regreso); rellenar(fin, cont)` |

## `<F_Call>`

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 17 | Después de `id` | `si no es una función → reportar qué es en realidad`<br>`si es void y está dentro de una expresión → reportar`<br>`si el número de argumentos no coincide → reportar` |
| 18 | Antes de cada `EXPRESIÓN` | `POper.meter(fondo falso)` |
| 19 | Después de cada `EXPRESIÓN` | `POper.sacar()` (el fondo falso) |
| 20 | Después de cada `EXPRESIÓN` (tras el 19) | `arg ← sacar(); si no cabe (cubo: parámetro k ← arg) → reportar; guardar arg en la lista de argumentos` |
| 21 | Después de `)` | `Fila.agregar(ERA, función(inicio), _, _)`<br>`para k = 1..n: Fila.agregar(PARAM, argumento k, _, k(dirección del parámetro k))`<br>`Fila.agregar(GOSUB, función(inicio), _, función.inicio)`<br>`si está en una expresión: t ← temporal(tipo de la función); Fila.agregar(=, dirección de regreso, _, t); meter(t)` |

Los argumentos se evalúan todos antes del `ERA`: una llamada anidada, como `f(g(1))`, termina
completa antes de preparar la memoria de `f`.

## `<Print>`

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 22 | Después de cada `EXPRESIÓN` | `v ← sacar(); si no es error → Fila.agregar(PRINT, v, _, _)` |
| 22 | Después de cada `cte.string` | `Fila.agregar(PRINT, letrero, _, _)` (el letrero tiene una dirección en la tabla de constantes) |
| 23 | Después de `)` | `Fila.agregar(PRINTLN, _, _, _)` |

## `<Read>`

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 24 | Después de `id` | `si es una variable visible → Fila.agregar(READ, _, _, variable); si no → reportar` |

## `<Return>`

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 25 | Antes de `;` | `si no hay funciónActual → error`<br>`si es void: con valor → error; sin valor → Fila.agregar(RETURN, _, _, _)`<br>`si tiene tipo: sin valor → error; v ← sacar(); si no cabe → reportar; si no → Fila.agregar(RETURN, v, _, dirección de regreso)` |

## `<EXPRESIÓN>` (versión modificada: incluye `==`)

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 26 | Después del operador relacional | `POper.meter(operador, línea, columna)` |
| 27 | Después de la segunda `EXP` | `si POper.tope ∈ {>, <, ==, !=}:`<br>`  op ← POper.sacar(); der ← sacar(); izq ← sacar()`<br>`  tipo ← cubo(izq.tipo, op, der.tipo)`<br>`  si izq o der es error → meter(error)` (ya se reportó)<br>`  si tipo = error → reportar "no se puede aplicar op entre …"; meter(error)`<br>`  si no → t ← temporal(bool); Fila.agregar(op, izq, der, t); meter(t)` |

## `<EXP>` y `<TÉRMINO>`

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 26 | Después de `+`/`-` (EXP) o `*`/`/` (TÉRMINO) | `POper.meter(operador, línea, columna)` |
| 28 | Después de cada `TÉRMINO` (EXP) o `FACTOR` (TÉRMINO) | `si POper.tope ∈ {+, -}` (EXP) `o {*, /}` (TÉRMINO): igual que el punto 27, con `t ← temporal(tipo que da el cubo)` |

## `<FACTOR>` (versión modificada: signo en cualquier factor)

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 18 | Después de `(` | `POper.meter(fondo falso)` |
| 19 | Después de `)` | `POper.sacar()` (debe ser el fondo falso; lo de adentro ya es un solo operando) |
| 29 | Después de `id`, `CTE` o `F_Call` | `id:` `si es una variable visible → meter(variable con su dirección); si no → reportar qué es en realidad; meter(error)`<br>`CTE:` `valor ← convertir(texto, con su signo si va pegado)`; `si no cabe → reportar; meter(error); si no → meter(constante con la dirección de su valor en la tabla de constantes)`<br>`F_Call:` `la llamada ya dejó en las pilas el temporal con su valor (punto 21)` |
| 30 | Al final del factor, si hubo signo y el valor no era constante | `x ← sacar()`<br>`si x es error → meter(error)`<br>`tipo ← cubo.signo(signo, x.tipo); si error → reportar "no se puede aplicar el signo …"; meter(error)`<br>`si signo = + → meter(x)`<br>`si signo = - → t ← temporal(tipo); Fila.agregar(UMINUS, x, _, t); meter(t)` |

## Regla sin número: código inalcanzable

En `<Body>` y en el bloque `[ ]`, después de cada estatuto: si un estatuto anterior de la misma
lista siempre regresa, se reporta "código inalcanzable: hay un return antes de este estatuto" (una
vez por lista, solo dentro de funciones). Se describe en [Análisis semántico](../05_semantica.md).

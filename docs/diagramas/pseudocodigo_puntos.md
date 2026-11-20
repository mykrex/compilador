# Puntos neurálgicos: pseudocódigo

Acompaña a `BabyDuck_puntos_neuralgicos.drawio` (una página por diagrama) y a los `PN_*.png`.
Los diagramas se generan con `python generar_diagramas.py <carpeta> --png`.

**Numeración.** Cada número conserva su significado en todo el proyecto. Los puntos 1 a 17 vienen
de la entrega 2; los que solo generan código se numeran desde el 18. Una letra (8a/8b, 11a/11b)
indica que la acción de un punto de la entrega 2 ahora ocurre en dos posiciones del diagrama. Los
números 12, 14, 15, 16 y 17 no aparecen en estos diagramas.

Notación: `PilaO` = pila de operandos, `PTipos` = pila de tipos, `POper` = pila de operadores,
`Fila` = fila de cuádruplos. `meter(x)` mete el operando y su tipo juntos; `sacar()` saca los dos.

## `<Programa>`

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 1 | Sobre `program` | `directorio ← nuevo directorio (con su tabla de globales)` |
| 2 | Entre `id` y `;` | `directorio.nombrePrograma ← id` |
| 7 | Antes de `main` | `funciónActual ← ninguna` (main solo ve las globales) |
| 24 | Después de `Body` | `Fila.agregar(END, _, _, _)` |

## `<VARS>`

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 3 | Entre `TYPE` y `;` | `para cada id de la lista:`<br>`  si el nombre ya existe en el alcance actual o ya lo usa el programa, una función o una global → error`<br>`  si no → agregar (id, tipo) a las globales o a la tabla de funciónActual` |

## `<FUNCS>`

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 4 | Entre `id` y `(` | `si el nombre ya se usa → error; si no → directorio.agregar(nueva función(id, tipo))`<br>`funciónActual ← la función` (registrada antes del cuerpo: permite recursión) |
| 5 | Después del `TYPE` de cada parámetro | `si el nombre se repite o ya se usa → error; si no → agregar a la lista de parámetros y a la tabla de funciónActual` |
| (3) | Dentro de `VARS` | Las locales usan el punto 3 con la tabla de funciónActual |
| 6 | Después del `;` final | `funciónActual ← ninguna` (la tabla se conserva) |

## `<EXPRESIÓN>` (versión modificada: incluye `==`)

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 18 | Después del operador relacional | `POper.meter(operador, línea, columna)` |
| 10 | Después de la segunda `EXP` | `si POper.tope ∈ {>, <, ==, !=}:`<br>`  op ← POper.sacar(); der ← sacar(); izq ← sacar()`<br>`  tipo ← cubo(izq.tipo, op, der.tipo)`<br>`  si izq o der es error → meter(error)` (ya se reportó)<br>`  si tipo = error → reportar "no se puede aplicar op entre …"; meter(error)`<br>`  si no → t ← temporal(bool); Fila.agregar(op, izq, der, t); meter(t)` |

## `<EXP>` y `<TÉRMINO>`

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 19 | Después de `+`/`-` (EXP) o `*`/`/` (TÉRMINO) | `POper.meter(operador, línea, columna)` |
| 9 | Después de cada `TÉRMINO` (EXP) o `FACTOR` (TÉRMINO) | `si POper.tope ∈ {+, -}` (EXP) `o {*, /}` (TÉRMINO): igual que el punto 10, con `t ← temporal(tipo que da el cubo)` |

## `<FACTOR>` (versión modificada: signo en cualquier factor)

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 20 | Después de `(` | `POper.meter(fondo falso)` |
| 21 | Después de `)` | `POper.sacar()` (debe ser el fondo falso; lo de adentro ya es un solo operando) |
| 8a | Después de `id`, `CTE` o `F_Call` | `id:` `si es una variable visible → meter(variable); si no → reportar qué es en realidad; meter(error)`<br>`CTE:` `valor ← convertir(texto, con su signo si va pegado)`; `si no cabe → reportar; meter(error); si no → meter(constante)`<br>`F_Call:` `revisar la llamada (argumentos entre fondos falsos); meter(temporal del tipo de retorno)` |
| 8b | Al final del factor, si hubo signo y el valor no era constante | `x ← sacar()`<br>`si x es error → meter(error)`<br>`tipo ← cubo.signo(signo, x.tipo); si error → reportar "no se puede aplicar el signo …"; meter(error)`<br>`si signo = + → meter(x)`<br>`si signo = - → t ← temporal(tipo); Fila.agregar(UMINUS, x, _, t); meter(t)` |

Dentro de `F_Call`, cada argumento se evalúa entre un fondo falso y su retiro (las mismas acciones
de los puntos 20 y 21), para que no use los operadores de afuera.

## `<ASSIGN>`

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 11a | Después de `id` | `destino ← la variable si es visible; si no → reportar; destino ← error` (el destino no entra a las pilas) |
| 11b | Después de `EXPRESIÓN` | `valor ← sacar()`<br>`si destino o valor es error → nada`<br>`si no cabe (cubo: destino ← valor) → reportar "no se puede asignar …"`<br>`si no → Fila.agregar(=, valor, _, destino)` |

## `<Print>`

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 22 | Después de cada `EXPRESIÓN` | `v ← sacar(); si no es error → Fila.agregar(PRINT, v, _, _)` |
| 22 | Después de cada `cte.string` | `Fila.agregar(PRINT, letrero, _, _)` |
| 23 | Después de `)` | `Fila.agregar(PRINTLN, _, _, _)` |

## `<Read>`

| Punto | Posición | Pseudocódigo |
|---|---|---|
| 13 | Después de `id` | `si es una variable visible → Fila.agregar(READ, _, _, variable); si no → reportar` |

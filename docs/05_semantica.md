# Análisis semántico

## Índice

1. [Overview](01_overview.md)
2. [Herramientas](02_herramientas.md)
3. [Análisis léxico](03_lexico.md)
4. [Análisis sintáctico](04_sintaxis.md)
5. **Análisis semántico** (este capítulo)
6. [Código intermedio](06_codigo_intermedio.md)
7. [Memoria virtual](07_memoria_virtual.md)
8. [Testing](08_testing.md)
9. [Herramientas de IA](09_herramientas_ia.md)
10. [Guía de uso](10_guia_de_uso.md)

---

## Qué hace

Revisa que un programa correcto en léxico y sintaxis tenga sentido: cada nombre se declara una
vez y se usa según lo que es, y los tipos de cada operación, asignación, llamada, condición y
`return` son compatibles. Corre solo si no hubo errores léxicos ni sintácticos (un árbol reparado
por ANTLR daría errores falsos). Los mismos puntos neurálgicos asignan las direcciones virtuales
([Memoria virtual](07_memoria_virtual.md)) y generan los cuádruplos
([Código intermedio](06_codigo_intermedio.md)).

Criterio de diseño: la regla más simple de implementar y explicar, **siempre que no deje pasar
errores detectables ni obligue al programador a rodeos**.

| Clase | Qué hace |
|---|---|
| `Type`, `Op` | Tipos internos y operadores del lenguaje |
| `SemanticCube` | Tipo resultante de cada operación, o `ERROR` |
| `VariableInfo`, `VariableTable` | Una variable (nombre, tipo y dirección); la tabla de un alcance |
| `FunctionInfo`, `FunctionDirectory` | Una función; el directorio (programa, globales y funciones) |
| `MemorySize` | Cuántas direcciones de cada tipo usa un segmento (tamaños de cada función) |
| `SemanticAnalyzer` | Recorrido del árbol (Visitor): llena las estructuras, valida y genera cuádruplos |
| `ErrorCollector` | Junta los errores léxicos, sintácticos y semánticos con línea y columna |

El paquete `semantic` no depende de ANTLR; `SemanticAnalyzer` vive en `com.babyduck` porque usa
las clases generadas.

## Tipos

| Tipo | ¿Declarable? | Uso |
|---|---|---|
| `int`, `float` | Sí | Variables, parámetros, funciones y constantes |
| `bool` | No | Resultado de `> < == !=`; tipo que exigen las condiciones |
| `void` | Solo en funciones | Funciones sin valor; nunca válido en una expresión |
| `string` | No | Solo letreros de `print` (operandos de `PRINT`); fuera del cubo y de las pilas |
| `error` | No | Resultado de una combinación inválida |

`bool` propio en lugar de enteros 0/1: así `if (_X_)` y `(_A_ < _B_) + 1` son errores y la regla
de las condiciones es una sola.

## Cubo semántico

| Izquierdo | Derecho | `+ - *` | `/` | `> < == !=` | `=` (destino ← valor) |
|---|---|---|---|---|---|
| int | int | int | float | bool | válido |
| int | float | float | float | bool | error |
| float | int | float | float | bool | válido |
| float | float | float | float | bool | válido |
| bool / void / string | cualquiera | error | error | error | error |

- `/` siempre da `float` (`7 / 2` es 3.5); no hay división entera.
- `int ← float` es error: un aviso claro es mejor que truncar en silencio. La misma regla (`canAssign`) aplica a argumentos y `return`.
- El signo `+ -` conserva `int`/`float`; con otro tipo es error.
- Precedencia por niveles de la gramática; asociatividad izquierda por el recorrido (árbol plano de EBNF). Un solo relacional por expresión.

Estructura: `EnumMap` anidados (izquierdo → operador → derecho → resultado), consulta O(1). Solo
se registran las 35 combinaciones válidas; cualquier otra da `ERROR` por omisión.

```java
cube.result(left, op, right)    // tipo resultante o ERROR
cube.unaryResult(op, operand)   // signo
cube.canAssign(target, value)   // asignación, argumentos y return
```

## Tabla de variables y directorio de funciones

- **`VariableTable`:** `LinkedHashMap` por nombre: búsqueda O(1) y conserva el orden de declaración (salida legible y, en memoria, direcciones en orden). `add` regresa `false` si el nombre ya existe (sin excepciones: quien llama conoce la línea y reporta).
- **`FunctionDirectory`:** nombre del programa, tabla de globales (campo aparte, también la usa `main`), `LinkedHashMap` de funciones y, al terminar, el tamaño del segmento global y de los temporales de `main`.
- **`FunctionInfo`:** nombre, tipo de retorno, parámetros en `ArrayList` (para validar llamadas por posición) y tabla local con parámetros y locales (para buscar por nombre); ambos guardan el mismo objeto. Además: dirección de regreso (si tiene tipo), cuádruplo de inicio y tamaños de locales y temporales.
- **Al terminar una función se libera su tabla local:** lo que necesita una llamada (parámetros con tipo y dirección, inicio, tamaños y dirección de regreso) queda en su `FunctionInfo`. La tabla de globales se conserva, porque la memoria global vive todo el programa.

```java
table.add(v) / get(name) / contains(name) / all() / clear()
dir.addFunction(f) / getFunction(name) / globals() / lookupVariable(name, actual) / globalUse(name) / setSizes(...)
f.addParam(v) / addLocal(v) / params() / paramCount() / locals() / returnType()
f.setReturnAddress(d) / setStartQuad(n) / setSizes(locales, temporales) / releaseLocals()
```

## Alcance y nombres

- Dos alcances: global (incluye `main`) y el local de cada función; basta una referencia a la función actual.
- Sin sombreado: una local o un parámetro no puede llamarse como una global.
- Un solo espacio de nombres para programa, funciones y globales; funciones distintas sí pueden repetir locales.
- Una sola pasada: se declara antes de usar. La recursión directa funciona (la función se registra antes de su cuerpo); la mutua no.

## Recorrido y puntos neurálgicos

Visitor (`BabyDuckBaseVisitor<Void>`) en lugar de Listener, porque permite acciones a mitad de una
regla (por ejemplo, el `GOTOF` entre la condición y el cuerpo de un `if`). Las expresiones dejan
su resultado en las pilas del generador de cuádruplos; el cubo se consulta al sacar los operandos.
Los puntos con validaciones semánticas (numeración completa en
[Código intermedio](06_codigo_intermedio.md#puntos-neurálgicos)):

| Punto | Dónde | Validación |
|---|---|---|
| 2, 5, 6, 7 | `program`, `varDecl`, `funcDecl`, `param` | Nombres no repetidos ni ocupados (cada declaración recibe su dirección) |
| 6 | `funcDecl`, antes del cuerpo | Una función con valor regresa en todos los caminos |
| 10, 24, 29 | `assign`, `read`, `factor` | El nombre es una variable visible |
| 11 | `assign` | `destino ← valor` con el cubo |
| 12 | `condition`, `cycle` | La condición debe ser `bool` |
| 17, 20 | `funcCall` | La función existe, no es `void` dentro de una expresión, número y tipo de argumentos |
| 25 | `returnStmt` | Reglas de `return` |
| 27, 28, 30 | `expression`, `exp`/`term`, `factor` | Operadores y signo con el cubo |
| Sin número | `body`, `[ ]` | Código inalcanzable (ver abajo) |

## Validaciones

Formato: `[Error semántico] línea 3:4 -> mensaje`.

| Situación | Mensaje |
|---|---|
| Doble declaración | `la variable _A_ ya fue declarada` / `_X_ ya fue declarado en la función _F_` / `la función _F_ ya fue declarada` |
| Nombre ocupado | `_X_ ya se usa como variable global` (o `nombre del programa`, `función`) |
| Uso de nombres | `la variable _X_ no está declarada` / `_F_ es una función, no una variable` / `_X_ es una variable, no una función` / `_P_ es el nombre del programa` / `la función _G_ no está declarada` |
| Llamadas | `_Suma_ espera 2 argumentos, recibió 1` / `argumento 2 de _Suma_: no se puede pasar float a int` / `la función _F_ es void y no devuelve un valor` |
| Tipos | `no se puede asignar float a _X_, que es int` / `no se puede aplicar + entre bool e int` / `no se puede aplicar el signo - a bool` / `la condición del if debe ser una comparación (bool), se obtuvo int` |
| Constantes | `la constante 2147483648 no cabe en int` |
| `return` | `return solo puede usarse dentro de una función` / `la función _F_ es void, no puede regresar un valor` / `la función _F_ debe regresar un valor int` / `la función _F_ regresa int, no puede regresar float` |
| Caminos | `la función _F_ no regresa un valor en todos los caminos` / `código inalcanzable: hay un return antes de este estatuto` |
| Memoria | `se excedió el límite de 1000 direcciones para variables globales de tipo int` (también locales, temporales y constantes) |

- En una llamada se revisa primero el número de argumentos; si no coincide, solo se reporta eso.
- Todos los caminos (como Java): `if` con `else` regresa si ambos cuerpos regresan; `if` sin `else` y `while` no cuentan. Se revisa al registrar la función, antes de su cuerpo: solo inspecciona el árbol y así el error sale en orden de línea.
- **Código inalcanzable** (regla sin número de punto): al recorrer los estatutos de un `Body` o de un bloque `[ ]`, si un estatuto anterior de la misma lista siempre regresa, se reporta `código inalcanzable: hay un return antes de este estatuto` en el primero que sigue (una vez por lista y solo dentro de funciones; en `main`, `return` ya es error).
- Se reportan todos los errores, en orden de línea, **sin errores en cadena**: una parte con error viaja como `Operand.ERROR` y no genera más mensajes.

```
if (_X_ > 0) { return 1; };                      // error: falta el caso contrario
if (_X_ > 0) { return 1; } else { return 0; };   // válido
```

## Pruebas

`SemanticCubeTest`, `VariableTableTest`, `FunctionDirectoryTest` (incluye la liberación de la tabla
local) y `SemanticTest` (22 programas
en `src/test/resources/semantic/`: SEM01 y SEM07 válidos; los demás, un error cada uno, cuyo
mensaje debe contener su línea `// esperado:`). Detalle en [Testing](08_testing.md).

Fuera del alcance: variables usadas sin valor y división entre cero (máquina virtual).

## Cambios respecto a la entrega anterior

- Cada variable, parámetro y dirección de regreso recibe su dirección virtual al declararse.
- Al terminar una función se libera su tabla local; se conservan sus parámetros (con dirección), su inicio, sus tamaños y su dirección de regreso.
- Nuevo error cuando un segmento de memoria se queda sin direcciones.
- Numeración nueva de los puntos neurálgicos (equivalencias en [Código intermedio](06_codigo_intermedio.md#cambios-respecto-a-la-entrega-anterior)); el código inalcanzable queda como regla sin número.

---

## Navegación

- Anterior: [← Análisis sintáctico](04_sintaxis.md)
- Siguiente: [Código intermedio →](06_codigo_intermedio.md)

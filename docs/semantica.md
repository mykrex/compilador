# Análisis semántico

## Índice

1. [Overview](overview.md)
2. [Herramientas](herramientas.md)
3. [Análisis léxico](lexico.md)
4. [Análisis sintáctico](sintaxis.md)
5. **Análisis semántico** (este capítulo)
6. [Código intermedio](codigo_intermedio.md)
7. [Testing](testing.md)
8. [Herramientas de IA](herramientas_ia.md)
9. [Guía de uso](guia_de_uso.md)

---

## 1. Qué hace

Revisa que un programa correcto en léxico y sintaxis tenga sentido: cada nombre se declara una
vez y se usa según lo que es, y los tipos de cada operación, asignación, llamada, condición y
`return` son compatibles. Corre solo si no hubo errores léxicos ni sintácticos (un árbol reparado
por ANTLR daría errores falsos). Desde la entrega 3, los mismos puntos neurálgicos generan los
cuádruplos ([Código intermedio](codigo_intermedio.md)).

Criterio de diseño: la regla más simple de implementar y explicar, **siempre que no deje pasar
errores detectables ni obligue al programador a rodeos**.

| Clase | Qué hace |
|---|---|
| `Type`, `Op` | Tipos internos y operadores del lenguaje |
| `SemanticCube` | Tipo resultante de cada operación, o `ERROR` |
| `VariableInfo`, `VariableTable` | Una variable; la tabla de un alcance |
| `FunctionInfo`, `FunctionDirectory` | Una función; el directorio (programa, globales y funciones) |
| `SemanticAnalyzer` | Recorrido del árbol (Visitor): llena las estructuras, valida y genera cuádruplos |
| `ErrorCollector` | Junta los errores léxicos, sintácticos y semánticos con línea y columna |

El paquete `semantic` no depende de ANTLR; `SemanticAnalyzer` vive en `com.babyduck` porque usa
las clases generadas.

## 2. Tipos

| Tipo | ¿Declarable? | Uso |
|---|---|---|
| `int`, `float` | Sí | Variables, parámetros, funciones y constantes |
| `bool` | No | Resultado de `> < == !=`; tipo que exigen las condiciones |
| `void` | Solo en funciones | Funciones sin valor; nunca válido en una expresión |
| `string` | No | Solo letreros de `print` (operandos de `PRINT`); fuera del cubo y de las pilas |
| `error` | No | Resultado de una combinación inválida |

`bool` propio en lugar de enteros 0/1: así `if (_X_)` y `(_A_ < _B_) + 1` son errores y la regla
de las condiciones es una sola.

## 3. Cubo semántico

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

## 4. Tabla de variables y directorio de funciones

- **`VariableTable`:** `LinkedHashMap` por nombre: búsqueda O(1) y conserva el orden de declaración (salida legible y, en memoria, direcciones en orden). `add` regresa `false` si el nombre ya existe (sin excepciones: quien llama conoce la línea y reporta).
- **`FunctionDirectory`:** nombre del programa, tabla de globales (campo aparte, también la usa `main`) y `LinkedHashMap` de funciones.
- **`FunctionInfo`:** nombre, tipo de retorno, parámetros en `ArrayList` (para validar llamadas por posición) y tabla local con parámetros y locales (para buscar por nombre); ambos guardan el mismo objeto.
- Las tablas se conservan al terminar una función (evidencia, pruebas y tamaños en memoria).

```java
table.add(v) / get(name) / contains(name) / all()
dir.addFunction(f) / getFunction(name) / globals() / lookupVariable(name, actual) / globalUse(name)
f.addParam(v) / addLocal(v) / params() / paramCount() / locals() / returnType()
```

## 5. Alcance y nombres

- Dos alcances: global (incluye `main`) y el local de cada función; basta una referencia a la función actual.
- Sin sombreado: una local o un parámetro no puede llamarse como una global.
- Un solo espacio de nombres para programa, funciones y globales; funciones distintas sí pueden repetir locales.
- Una sola pasada: se declara antes de usar. La recursión directa funciona (la función se registra antes de su cuerpo); la mutua no.

## 6. Recorrido y puntos neurálgicos

Visitor (`BabyDuckBaseVisitor<Void>`) en lugar de Listener, porque permite acciones a mitad de una
regla (las necesitarán los saltos de `if` y `while`). Las expresiones dejan su resultado en las
pilas del generador de cuádruplos; el cubo se consulta al sacar los operandos.

| Punto | Dónde | Acción |
|---|---|---|
| 1, 2 | `program` | Crear el directorio; registrar el nombre |
| 3 | `varDecl` | Declarar cada id en el alcance actual |
| 4 | `funcDecl` (inicio) | Registrar la función antes de su cuerpo; función actual |
| 5 | `param` | Agregar a la lista y a la tabla |
| 6, 7 | Fin de función / antes de `main` | Función actual = ninguna |
| 8–11, 13 | `factor`, `term`/`exp`, `expression`, `assign`, `read` | Validar y generar cuádruplos (ver [Código intermedio](codigo_intermedio.md)) |
| 12 | `condition`, `cycle` | La condición debe ser `bool` |
| 14 | Llamada como estatuto | Revisar la llamada; el valor se descarta |
| 15 | `returnStmt` | Reglas de `return` |
| 16 | `funcDecl` (antes del cuerpo) | Una función con valor regresa en todos los caminos |
| 17 | `body`, `[ ]` | Código después de un `return` |

## 7. Validaciones

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

- En una llamada se revisa primero el número de argumentos; si no coincide, solo se reporta eso.
- Todos los caminos (como Java): `if` con `else` regresa si ambos cuerpos regresan; `if` sin `else` y `while` no cuentan.
- Se reportan todos los errores, en orden de línea, **sin errores en cadena**: una parte con error viaja como `Operand.ERROR` y no genera más mensajes.

```
if (_X_ > 0) { return 1; };                      // error: falta el caso contrario
if (_X_ > 0) { return 1; } else { return 0; };   // válido
```

## 8. Pruebas

`SemanticCubeTest`, `VariableTableTest`, `FunctionDirectoryTest` y `SemanticTest` (22 programas
en `src/test/resources/semantic/`: SEM01 y SEM07 válidos; los demás, un error cada uno, cuyo
mensaje debe contener su línea `// esperado:`). Detalle en [Testing](testing.md).

Fuera del alcance: variables usadas sin valor y división entre cero (máquina virtual).

## 9. Cambios respecto a la entrega anterior (entrega 3)

- Nuevo tipo interno `string` para letreros.
- El Visitor ya no devuelve tipos (`<Void>`): las pilas son la única fuente; se conserva por las acciones a mitad de regla.
- Los errores en cadena se evitan con `Operand.ERROR` en las pilas (mismos mensajes).
- Nuevo error de constante que no cabe en su tipo.
- Los puntos 8–11 y 13 también generan cuádruplos.

---

## Navegación

- Anterior: [← Análisis sintáctico](sintaxis.md)
- Siguiente: [Código intermedio →](codigo_intermedio.md)

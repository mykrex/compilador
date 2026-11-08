# 4. Análisis Semántico

## Índice

0. [Overview](overview.md)
1. [Herramientas](herramientas.md)
2. [Análisis Léxico](lexico.md)
3. [Análisis Sintáctico](sintaxis.md)
4. [Análisis Semántico](semantica.md)
5. [Código Intermedio](05_codigo_intermedio.md)
6. [Memoria Virtual](06_memoria_virtual.md)
7. [Máquina Virtual](07_maquina_virtual.md)
8. [Testing](08_testing.md)
9. [Herramientas de IA](herramientas_ia.md)
10. [Guía de uso](guia_de_uso.md)

---

El análisis semántico revisa que un programa correcto en léxico y sintaxis **tenga sentido**: que cada nombre se declare una sola vez y se use según lo que es, y que los tipos de cada operación sean compatibles.

En esta etapa se verifica:

- Variables y funciones declaradas una sola vez.
- Variables y funciones declaradas antes de usarse.
- Uso correcto de cada nombre (una variable no se llama como función, ni al revés).
- Compatibilidad de tipos en operaciones, asignaciones, llamadas, condiciones y `return`.
- Número y tipo de los argumentos de cada llamada.
- Manejo de alcances (global y local de cada función).
- Que toda función con valor regrese en todos los caminos.

Para implementar esta etapa se diseñaron:

1. Un **cubo semántico**.
2. Un **directorio de funciones**.
3. Una **tabla de variables** por alcance.
4. Un **recorrido del árbol sintáctico** (Visitor) con los puntos neurálgicos que llenan las estructuras y validan.

La semántica se ejecuta **solo si el análisis léxico y el sintáctico terminaron sin errores**. Cuando hay errores de sintaxis, ANTLR repara el árbol para poder continuar, y revisar ese árbol reparado produciría errores semánticos falsos.

---

## 4.1 Criterio de diseño

BabyDuck es un lenguaje pequeño. Ante dos opciones se prefirió la regla más simple de implementar y de explicar, **siempre que no deje pasar errores que el compilador puede detectar razonablemente ni obligue al programador a rodeos innecesarios**. El objetivo es un compilador simple, pero no permisivo.

Cada restricción que resulta de este criterio se explica, con ejemplos, en la [Guía de uso](10_guia_de_uso.md).

---

## 4.2 Sistema de tipos

### Tipos que maneja el compilador

| Tipo    | ¿Se puede declarar?                   | Dónde aparece                                                               |
| ------- | ------------------------------------- | --------------------------------------------------------------------------- |
| `int`   | Sí: variables, parámetros y funciones | Constantes enteras y operaciones entre enteros                              |
| `float` | Sí: variables, parámetros y funciones | Constantes flotantes, operaciones con un `float` y toda división            |
| `bool`  | No                                    | Resultado de `> < == !=`; tipo que exigen las condiciones de `if` y `while` |
| `void`  | Solo como tipo de una función         | Funciones sin valor de retorno; nunca es válido dentro de una expresión     |
| `error` | No                                    | Resultado de cualquier combinación inválida                                 |

### ¿Por qué un tipo `bool` y no enteros 0/1 como en C?

Con la convención de C, expresiones como `if (_X_)` o `(_A_ < _B_) + 1` serían válidas, aunque casi siempre son un descuido. Con un tipo `bool` propio, el compilador las rechaza, y la validación de las condiciones queda en una sola regla: **la condición debe ser `bool`**.

La desventaja es que el resultado de una comparación no se puede guardar en una variable. Tiene poco impacto, porque BabyDuck no tiene variables booleanas ni operadores lógicos (`and`, `or`). El costo es pequeño: unas casillas más en el cubo y, en la etapa de memoria, un segmento para temporales booleanos.

### ¿Por qué no hay tipo `string`?

La gramática solo permite letreros dentro de `print` (`printItem : expression | CTE_STRING`), así que nunca llegan a una expresión y el cubo no necesita considerarlos.

---

## 4.3 Cubo Semántico

El cubo semántico define qué operaciones son válidas entre tipos y cuál es el tipo resultante. Si una operación no es válida, devuelve `error`.

### Reglas de las operaciones

| Operación             | Regla                                                                            | Motivo                                                              |
| --------------------- | -------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
| `+ - *`               | `int` con `int` da `int`; cualquier combinación con `float` da `float`           | Convención habitual de promoción                                    |
| `/`                   | Siempre da `float`: `7 / 2` da `3.5`                                             | Conserva los decimales. Consecuencia: no hay división entera        |
| `> < == !=`           | Cualquier combinación de `int` y `float` da `bool`; con un `bool` da `error`     | Sin `and`/`or`, comparar resultados de comparaciones no aporta nada |
| `=` (destino ← valor) | `int ← int`, `float ← float` y `float ← int` son válidas; `int ← float` es error | La promoción no pierde información; la conversión inversa sí        |
| Signo `+ -`           | Conserva el tipo: `-2`, `-_X_`, `-(_A_ + 1)`, `-_F_(3)`; con `bool` da `error`   | El signo no cambia la naturaleza del número                         |
| `print`               | Acepta expresiones `bool` y las muestra como `true`/`false`                      | Facilita depurar                                                    |

**Sobre `int ← float`.** Se evaluó truncar los decimales en silencio, como hace C, y se descartó: `_X_ = 2.9;` guardaría `2` sin aviso, y quien empieza a programar esperaría un redondeo. Tampoco se agregó una conversión explícita (_cast_), porque obligaría a cambiar la gramática, el cubo y los cuádruplos sin que ninguna entrega lo requiera. La regla para el programador es sencilla: si un cálculo puede tener decimales, se guarda en un `float`.

**Una sola regla de compatibilidad.** La regla de la asignación se reutiliza al pasar argumentos (parámetro ← argumento) y en `return` (tipo de la función ← valor). Así, llamar con `2` a una función cuyo parámetro es `float` es válido, y `return 2.5;` en una función `int` es error, sin lógica adicional.

### Precedencia y asociatividad

| Nivel     | Operadores     | Regla del `.g4` | Asociatividad                                                                           |
| --------- | -------------- | --------------- | --------------------------------------------------------------------------------------- |
| 1 (mayor) | `( )`          | `factor`        | No aplica                                                                               |
| 2         | `+ -` unarios  | `factor`        | Un solo signo por factor (`--_X_` es inválido)                                          |
| 3         | `* /`          | `term`          | Izquierda                                                                               |
| 4         | `+ -` binarios | `exp`           | Izquierda                                                                               |
| 5         | `> < == !=`    | `expression`    | Un solo relacional por expresión                                                        |
| 6 (menor) | `=`            | `assign`        | No aplica: es un estatuto y no se encadena (si se permitiera, sería derecha, como en C) |

La gramática fija la precedencia por niveles. Como está escrita en EBNF, el árbol de `_A_ - _B_ - _C_` es **plano**: un solo nodo `exp` con tres términos como hijos. Por eso la asociatividad izquierda la garantiza el recorrido, que procesa los operandos de izquierda a derecha: `10 - 4 - 3` se interpreta como `(10 - 4) - 3 = 3`, no como `10 - (4 - 3) = 9`.

En tipos, todos los operadores de expresión son **simétricos** (`int - float` y `float - int` dan `float`); la asignación no lo es. En valores, `- / < >` **no son conmutativos**, así que la generación de código nunca debe intercambiar sus operandos.

### Tabla de consideraciones semánticas

| Izquierdo   | Derecho     | `+ - *` | `/`   | `> < == !=` | `=` (destino ← valor) |
| ----------- | ----------- | ------- | ----- | ----------- | --------------------- |
| int         | int         | int     | float | bool        | válido (int)          |
| int         | float       | float   | float | bool        | error                 |
| float       | int         | float   | float | bool        | válido (float)        |
| float       | float       | float   | float | bool        | válido (float)        |
| bool / void | cualquiera  | error   | error | error       | error                 |
| cualquiera  | bool / void | error   | error | error       | error                 |

| Signo unario | int | float | bool  | void  |
| ------------ | --- | ----- | ----- | ----- |
| `+ -`        | int | float | error | error |

### Estructura utilizada

Se implementó con `EnumMap` anidados: **tipo izquierdo → operador → tipo derecho → tipo resultante**.

```java
private final Map<Type, Map<Op, Map<Type, Type>>> binary = new EnumMap<>(Type.class);
private final Map<Op, Map<Type, Type>> unary = new EnumMap<>(Op.class);

private void put(Type left, Op op, Type right, Type result) {
    binary.computeIfAbsent(left, k -> new EnumMap<>(Op.class))
          .computeIfAbsent(op, k -> new EnumMap<>(Type.class))
          .put(right, result);
}
```

Razones de diseño:

- `EnumMap` guarda sus valores en un arreglo indexado por la posición de cada valor del enum: la consulta es **O(1) sin calcular hash**, y el cubo se consulta en cada operación de cada expresión.
- Solo acepta valores de `Type` y `Op`, así que es imposible consultar con un tipo mal escrito.
- Se lee igual que el concepto: izquierdo → operador → derecho.
- Solo se registran las **35 combinaciones válidas** (8 operadores × 4 combinaciones numéricas + 3 asignaciones). Cualquier otra devuelve `error` por omisión, de modo que `bool` en aritmética o `void` en una expresión quedan rechazados sin escribir reglas para ellos.
- La asignación es un operador más del cubo, para que toda la tabla de consideraciones esté en un solo lugar.

Alternativas descartadas:

| Alternativa                   | Motivo                                                                                       |
| ----------------------------- | -------------------------------------------------------------------------------------------- |
| Arreglo 3D `Type[][][]`       | Igual de rápido, pero ilegible (`cube[0][3][1]`) y se rompe sin aviso si se reordena un enum |
| `HashMap` con llave compuesta | Calcula un hash y crea un objeto llave en cada consulta; pierde la forma de cubo             |
| `switch` / `if`               | No existe una tabla que documentar; las reglas quedan dispersas                              |

### Operaciones

```java
cube.result(left, op, right)    // tipo de una operación binaria, o ERROR
cube.unaryResult(op, operand)   // tipo de un signo unario, o ERROR
cube.canAssign(target, value)   // ¿se puede guardar value en target? (asignación, argumentos y return)
```

El cubo se llena una sola vez, en su constructor, y después no cambia.

---

## 4.4 Arquitectura de módulos semánticos

Las estructuras están en el paquete `com.babyduck.semantic`, que **no depende de ANTLR**, por lo que se prueban por sí solas con JUnit. El recorrido del árbol está en `com.babyduck`, junto al analizador sintáctico, porque usa las clases que genera ANTLR.

| Clase               | Paquete    | Responsabilidad                                                 |
| ------------------- | ---------- | --------------------------------------------------------------- |
| `Type`, `Op`        | `semantic` | Enumeraciones de tipos y operadores                             |
| `SemanticCube`      | `semantic` | Cubo semántico                                                  |
| `VariableInfo`      | `semantic` | Datos de una variable o parámetro                               |
| `VariableTable`     | `semantic` | Tabla de variables de un alcance                                |
| `FunctionInfo`      | `semantic` | Datos de una función                                            |
| `FunctionDirectory` | `semantic` | Directorio de funciones                                         |
| `SemanticAnalyzer`  | `babyduck` | Recorrido del árbol: llena las estructuras y valida             |
| `ErrorCollector`    | `babyduck` | Junta los errores léxicos, sintácticos y semánticos             |
| `BabyDuckAnalyzer`  | `babyduck` | `compile()`: léxico + sintaxis y, si no hubo errores, semántica |

---

## 4.5 Tabla de Variables (`VariableTable`)

Una instancia para las variables globales y una por cada función. Implementada como `LinkedHashMap` indexado por nombre.

Cada entrada es un `VariableInfo` (un `record` inmutable):

| Campo  | Tipo     | Descripción                      |
| ------ | -------- | -------------------------------- |
| `name` | `String` | Nombre de la variable            |
| `type` | `Type`   | Tipo semántico (`INT` o `FLOAT`) |

En la etapa de memoria virtual se agregará la dirección asignada a cada variable.

### ¿Por qué `LinkedHashMap`?

El compilador consulta una tabla cada vez que se declara un nombre (para detectar duplicados) y cada vez que se usa (para conocer su tipo), así que la búsqueda por nombre debe ser rápida.

| Estructura                 | Buscar por nombre | Conserva el orden de declaración |
| -------------------------- | ----------------- | -------------------------------- |
| `HashMap`                  | O(1) promedio     | No: el orden lo decide el hash   |
| **`LinkedHashMap`**        | **O(1) promedio** | **Sí**                           |
| `LinkedList` / `ArrayList` | O(n)              | Sí                               |

`LinkedHashMap` busca igual que un `HashMap` y, además, enlaza sus entradas en orden de inserción. Con `var _Z_, _A_, _M_ : int;` la tabla se recorre como `_Z_, _A_, _M_`. Esto hace legible la impresión de las tablas, vuelve predecible la salida de las pruebas y, en la etapa de memoria, permitirá asignar direcciones en orden de declaración. Su costo son dos referencias extra por entrada (la anterior y la siguiente), despreciable para el tamaño de un programa BabyDuck.

### Operaciones principales

```java
table.add(variable)     // agrega si el nombre no existe; devuelve false si ya existía
table.get(name)         // VariableInfo o null
table.contains(name)    // boolean
table.size()            // cantidad de variables
table.all()             // variables en orden de declaración (solo lectura)
```

**Duplicados sin excepciones.** Cuando el nombre ya existe, `add` devuelve `false` y la tabla no cambia: se conserva la primera declaración. La tabla no conoce líneas ni columnas; quien la llama (el recorrido del árbol) sí, y es quien registra el error con su posición. Se descartó lanzar una excepción porque el compilador reporta **todos** los errores sin detenerse, y cada declaración tendría que atraparla para continuar. Además, una declaración repetida no es una situación excepcional: es uno de los errores que el compilador busca.

---

## 4.6 Directorio de Funciones (`FunctionDirectory`)

El directorio guarda tres cosas:

| Campo         | Tipo                                  | Descripción                                             |
| ------------- | ------------------------------------- | ------------------------------------------------------- |
| `programName` | `String`                              | Nombre del programa (ocupa su nombre, ver §4.7)         |
| `globals`     | `VariableTable`                       | Variables globales; también las usa `main`              |
| `functions`   | `LinkedHashMap<String, FunctionInfo>` | Funciones indexadas por nombre, en orden de declaración |

Cada función es un `FunctionInfo`:

| Campo        | Tipo                      | Descripción                    |
| ------------ | ------------------------- | ------------------------------ |
| `name`       | `String`                  | Nombre de la función           |
| `returnType` | `Type`                    | `INT`, `FLOAT` o `VOID`        |
| `params`     | `ArrayList<VariableInfo>` | Parámetros en orden            |
| `locals`     | `VariableTable`           | Parámetros y variables locales |

```
FunctionDirectory
├── programName : _Calc_
├── globals     : { _Total_:int, _Prom_:float }
└── functions
     ├── _Suma_    → FunctionInfo(int)
     │                 params : [ _A_:int, _B_:float ]
     │                 locals : { _A_:int, _B_:float, _C_:int }
     └── _Imprime_ → FunctionInfo(void)
                       params : [ ]
                       locals : { }
```

### Globales en un campo aparte

Las variables globales no son una entrada especial `"global"` dentro del mapa de funciones. Así, **cada entrada del mapa describe una función real**: todas tienen tipo de retorno, parámetros y tabla local, sin campos vacíos ni casos especiales. Recorrer las funciones (para validarlas, imprimirlas o generar su código) no requiere saltarse ninguna entrada. Encaja con la gramática: `main` no tiene variables propias, así que estar en `main` equivale a ver solo las globales.

### Parámetros en dos lugares (el mismo objeto)

| Lugar            | Pregunta que responde                                            | Ejemplo                      |
| ---------------- | ---------------------------------------------------------------- | ---------------------------- |
| `params` (lista) | ¿Cuántos parámetros hay y de qué tipo es cada uno, por posición? | Validar `_Suma_(2, 3)`       |
| `locals` (tabla) | ¿Existe este nombre dentro de la función?                        | Usar `_A_` dentro del cuerpo |

Para `int _Suma_(_A_ : int, _B_ : float)`, la llamada `_Suma_(2, 3)` se valida comparando primero 2 argumentos contra 2 parámetros, y después cada argumento con su parámetro: `int` en `int` es válido, `int` en `float` es válido por promoción. `_Suma_(2.5, 1)` falla en el argumento 1 y `_Suma_(1)` falla por el número de argumentos.

La tabla sola no bastaría, porque no distingue cuáles de sus entradas son parámetros; la lista sola obligaría a buscar los parámetros por separado cada vez que se usan. Con el parámetro en la tabla, una local que repita su nombre se detecta sin código adicional. Como ambos lugares guardan **el mismo objeto**, no pueden quedar inconsistentes.

### Las tablas se conservan

En el método clásico, la tabla de una función se borra al terminar de compilarla. Aquí se conservan: al terminar la función, la "función actual" pasa a ser ninguna, así que sus variables dejan de ser visibles aunque la tabla siga existiendo. Lo único que se borraría sería la tabla local; la firma de la función siempre se conserva porque las llamadas posteriores la necesitan.

Conservarlas permite imprimir el directorio completo como evidencia, revisarlo en las pruebas y depurar; en la etapa de memoria servirán para calcular cuánto espacio necesita cada función. El único beneficio de borrarlas sería liberar memoria durante la compilación, despreciable en programas de este tamaño.

### Operaciones principales

```java
dir.setProgramName(name)
dir.globals()                          // tabla de globales
dir.addFunction(function)              // false si ya existía
dir.getFunction(name)                  // FunctionInfo o null
dir.containsFunction(name)
dir.functions()                        // en orden de declaración (solo lectura)
dir.lookupVariable(name, current)      // busca en la función actual y luego en las globales
dir.globalUse(name)                    // PROGRAM, GLOBAL_VARIABLE, FUNCTION o null si está libre

function.addParam(variable)            // a la tabla y, si no se repite, al final de la lista
function.addLocal(variable)            // false si repite un parámetro u otra local
function.params() / paramCount()
function.locals()
function.returnType()
```

---

## 4.7 Alcance y nombres

BabyDuck tiene **dos alcances**: el global (que también usa `main`) y el local de cada función. La gramática evita tres situaciones que obligarían a manejar alcances anidados con una pila de tablas:

| ¿Y si…?                           | En BabyDuck                              | Consecuencia                                                    |
| --------------------------------- | ---------------------------------------- | --------------------------------------------------------------- |
| …`main` tuviera variables locales | No: `program : ... MAIN body END`        | `main` usa la tabla global                                      |
| …se anidaran funciones            | No: `funcDecl` solo aparece en `program` | Una función nunca ve las variables de otra                      |
| …se declarara en cualquier lugar  | No: `vars` solo va al inicio             | Las variables de un alcance existen antes de su primer estatuto |

Por eso el recorrido solo necesita **una referencia a la función actual** (`null` = global/`main`) en lugar de una pila de alcances.

**Sin sombreado.** Un parámetro o variable local no puede llamarse igual que una variable global. Si se permitiera, dentro de la función la global quedaría oculta sin aviso, y el programador podría creer que la está modificando.

```
var _G_ : int;
void _F_(_G_ : float) [ { } ];      // error: _G_ ya se usa como variable global
```

**Un solo espacio de nombres.** El programa, las funciones y las variables globales comparten nombres, aunque la gramática distinga una llamada de una variable por el paréntesis. Cada nombre significa una sola cosa en todo el programa. El nombre del programa también ocupa su nombre: nunca se usa dentro del código, pero incluirlo mantiene la regla sin excepciones.

```
var _Calc_ : int;
int _Calc_(_A_ : int) [ { return _A_; } ];    // error: _Calc_ ya se usa como variable global
```

Sí se permite que **funciones distintas reutilicen nombres locales** (por ejemplo, `_I_` en dos funciones), porque cada una tiene su propia tabla.

**Declarar antes de usar (una sola pasada).** El árbol se recorre una vez, de arriba abajo. Una función solo puede llamar a funciones declaradas antes que ella **o a sí misma**: cada función se registra antes de analizar su cuerpo, así que la recursión funciona.

Se evaluó una segunda pasada que registrara primero todos los encabezados, para permitir cualquier orden y la recursión mutua (dos funciones que se llaman entre sí). Se descartó porque, en las etapas siguientes, una llamada a una función declarada más abajo generaría saltos y reservas de memoria cuyo destino y tamaño aún no se conocen, y habría que completarlos después. La recursión mutua es poco común en programas de este tamaño, y el orden se resuelve reordenando las funciones.

---

## 4.8 Recorrido del árbol y puntos neurálgicos

### Visitor en lugar de Listener

ANTLR genera dos formas de recorrer el árbol:

|                               | Listener                                                     | **Visitor (elegido)**                                  |
| ----------------------------- | ------------------------------------------------------------ | ------------------------------------------------------ |
| Recorrido                     | Automático                                                   | Cada método visita a sus hijos                         |
| Dónde van las acciones        | Solo al entrar o salir de una regla                          | En cualquier punto del método, en el orden de la regla |
| Acciones a mitad de una regla | Indirectas: en una regla hija, preguntando quién es su padre | Directas                                               |
| Tipo de una expresión         | Requiere una pila de tipos                                   | El método devuelve el tipo                             |
| Riesgo                        | Ninguno de omitir nodos                                      | Olvidar visitar un hijo deja ese subárbol sin revisar  |

El Visitor permite colocar cada punto neurálgico donde corresponde, incluidos los que quedan a mitad de una regla (los que necesitarán `if`, `else` y `while` al generar código). El riesgo de omitir un hijo se reduce porque las reglas que no se programan se recorren automáticamente (`visitChildren`) y porque las pruebas revisan cada construcción.

`SemanticAnalyzer extends BabyDuckBaseVisitor<Type>`: los métodos de expresiones devuelven su tipo; los de declaraciones y estatutos devuelven `null`.

ANTLR construye el **árbol completo antes del recorrido**, así que cada método tiene a la mano todos los hijos de su regla. En `var _A_, _B_ : int;` el tipo aparece **después** de los nombres, y aun así no hace falta una variable que recuerde el tipo actual: `ctx.type()` y `ctx.ID()` están en el mismo nodo.

### Puntos de declaración (crean y llenan las estructuras)

| Punto | Regla y método                          | Acción                                                                                                               | Validación                                                                   |
| ----- | --------------------------------------- | -------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| 1     | Constructor de `SemanticAnalyzer`       | Crea el directorio (ya incluye la tabla de globales)                                                                 | —                                                                            |
| 2     | `program` (`visitProgram`)              | Registra el nombre del programa                                                                                      | —                                                                            |
| 3     | `varDecl` (`visitVarDecl`)              | Por cada `ID`, crea la variable con el tipo de la declaración y la agrega al alcance actual                          | Doble declaración; nombre ya usado por el programa, una global o una función |
| 4     | `funcDecl`, al inicio (`visitFuncDecl`) | Crea la función y la registra **antes** de sus parámetros y cuerpo (permite recursión); la marca como función actual | Función doblemente declarada; nombre ya usado                                |
| 5     | `param` (`visitParam`)                  | Agrega el parámetro a la lista y a la tabla de la función                                                            | Parámetro repetido; nombre ya usado                                          |
| 6     | `funcDecl`, al final                    | Función actual = ninguna; la tabla se conserva                                                                       | —                                                                            |
| 7     | `program`, antes de `main`              | Función actual = ninguna: `main` solo ve globales                                                                    | —                                                                            |

Las variables locales pasan por el punto 3 con la tabla de la función actual como destino. Si el nombre de una función está repetido, de todos modos se analizan sus parámetros, variables y cuerpo, para reportar todos los errores.

### Puntos de uso (validaciones)

| Punto | Regla y método                             | Acción                                                                                    |
| ----- | ------------------------------------------ | ----------------------------------------------------------------------------------------- |
| 8     | `factor` (`visitFactor`)                   | Tipo de la constante, variable, llamada o paréntesis; después, el signo con `unaryResult` |
| 9     | `term`, `exp` (`visitTerm`, `visitExp`)    | Combina los operandos de izquierda a derecha con `cube.result`                            |
| 10    | `expression` (`visitExpression`)           | Si hay relacional, combina ambos lados (resultado `bool`)                                 |
| 11    | `assign` (`visitAssign`)                   | El destino debe ser una variable visible; `canAssign(destino, valor)`                     |
| 12    | `condition`, `cycle`                       | La condición debe ser `bool`                                                              |
| 13    | `read` (`visitRead`)                       | El nombre debe ser una variable visible                                                   |
| 14    | `funcCall` como estatuto (`visitFuncCall`) | Revisa la llamada; si regresa un valor, se descarta                                       |
| 15    | `returnStmt` (`visitReturnStmt`)           | Reglas de `return` (§4.9)                                                                 |
| 16    | `funcDecl`, antes del cuerpo               | La función con valor regresa en todos los caminos                                         |
| 17    | `body` y bloques `[ ]`                     | Código inalcanzable después de un `return`                                                |

Para `_A_ + 2 * _F_(1)`: `visitFactor` devuelve el tipo de `_A_`, de `2` y de la llamada; `visitTerm` combina `2 * _F_(1)`; `visitExp` combina el `+`. Los operadores se toman por su posición entre los hijos del nodo (`[operando, op, operando, op, …]`, el operador _i_ está en la posición `2i - 1`), sin modificar la gramática.

### Vista general

```
program _Id_ ;                          ← 1, 2  directorio y nombre del programa
    var _A_, _B_ : int ;                ← 3     cada ID a las globales
    tipo _F_ ( params )                 ← 4     registrar _F_ (antes del cuerpo)
        _X_ : int                       ← 5     cada parámetro a lista + tabla
        [ var _L_ : float ;             ← 3     cada local a la tabla de _F_
          { estatutos } ]               ← 8–17  validaciones de uso y return
    ;                                   ← 6     función actual = ninguna
main                                    ← 7     solo globales
    { estatutos }                       ← 8–14  validaciones de uso
end
```

---

## 4.9 Validaciones Semánticas Implementadas

Los mensajes se escriben en español (los léxicos y sintácticos los genera ANTLR, en inglés). El formato es:

```text
[Error semántico] línea 3:4 -> mensaje
```

### Declaraciones

| Situación                            | Mensaje                                                                  |
| ------------------------------------ | ------------------------------------------------------------------------ |
| Variable global doblemente declarada | `la variable _A_ ya fue declarada`                                       |
| Parámetro o local repetido           | `_X_ ya fue declarado en la función _F_`                                 |
| Función doblemente declarada         | `la función _F_ ya fue declarada`                                        |
| Nombre ocupado                       | `_X_ ya se usa como nombre del programa` / `variable global` / `función` |

### Uso de nombres y llamadas

| Situación                                    | Mensaje                                                |
| -------------------------------------------- | ------------------------------------------------------ |
| Variable no declarada                        | `la variable _X_ no está declarada`                    |
| Función usada como variable                  | `_F_ es una función, no una variable`                  |
| Variable usada como función                  | `_X_ es una variable, no una función`                  |
| Nombre del programa en el código             | `_P_ es el nombre del programa`                        |
| Función no declarada (o declarada más abajo) | `la función _G_ no está declarada`                     |
| Número de argumentos                         | `_Suma_ espera 2 argumentos, recibió 1`                |
| Tipo de argumento                            | `argumento 2 de _Suma_: no se puede pasar float a int` |
| Función `void` en una expresión              | `la función _F_ es void y no devuelve un valor`        |

En una llamada se compara primero el número de argumentos; si no coincide, se reporta solo ese error, porque emparejar tipos por posición ya no tiene sentido. Una función con valor **sí puede usarse como estatuto** (`_Fact_(5);`): el valor se descarta, como en C y Java.

### Tipos

| Situación               | Mensaje                                                              |
| ----------------------- | -------------------------------------------------------------------- |
| Asignación incompatible | `no se puede asignar float a _X_, que es int`                        |
| Operación inválida      | `no se puede aplicar + entre bool e int`                             |
| Signo inválido          | `no se puede aplicar el signo - a bool`                              |
| Condición no booleana   | `la condición del if debe ser una comparación (bool), se obtuvo int` |

### `return`

| Situación                  | Regla                                  | Mensaje                                               |
| -------------------------- | -------------------------------------- | ----------------------------------------------------- |
| `return` en `main`         | Error                                  | `return solo puede usarse dentro de una función`      |
| `return valor;` en `void`  | Error                                  | `la función _F_ es void, no puede regresar un valor`  |
| `return;` en `int`/`float` | Error                                  | `la función _F_ debe regresar un valor int`           |
| `return;` en `void`        | Válido: termina la función             | —                                                     |
| Tipo del valor             | `canAssign(tipo de la función, valor)` | `la función _F_ regresa int, no puede regresar float` |

### Todos los caminos deben regresar

Una función `int` o `float` debe ejecutar un `return` sin importar el camino. Se revisa sobre el árbol, sin ejecutar el programa:

| Estatuto                             | ¿Siempre regresa?                              |
| ------------------------------------ | ---------------------------------------------- |
| `return`                             | Sí                                             |
| `if` con `else`                      | Sí, si **ambos** cuerpos siempre regresan      |
| `if` sin `else`                      | No: la condición puede ser falsa               |
| `while`                              | No: puede no ejecutarse nunca                  |
| Bloque `[ ]` o cuerpo `{ }`          | Sí, si alguno de sus estatutos siempre regresa |
| Asignación, llamada, `print`, `read` | No                                             |

```
if (_X_ > 0) { return 1; };                         // error: falta el caso contrario
if (_X_ > 0) { return 1; } else { return 0; };      // válido
if (_X_ > 0) { return 1; };  return 0;              // válido
while (_X_ > 0) do { return 1; };                   // error: el ciclo puede no ejecutarse
```

Mensaje: `la función _F_ no regresa un valor en todos los caminos`.

Se compararon tres opciones: no revisar (como C y C++, donde el resultado queda indefinido), exigir al menos un `return` (un atajo que no usa ningún lenguaje conocido y que deja pasar el caso del `if` sin `else`), y revisar todos los caminos (como Java, C#, Go, Swift, Kotlin y Rust). Se eligió la última porque es la única que no deja pasar el error. Dejarlo a la máquina virtual no bastaría, porque solo lo detectaría si la ejecución pasa por ese camino. En BabyDuck la revisión se reduce a la tabla anterior, ya que no hay `break`, `switch` ni `goto`. Igual que en Java, el análisis no evalúa condiciones: `while (1 == 1)` se considera un ciclo que puede no ejecutarse.

### Código inalcanzable

Un estatuto después de otro que siempre regresa nunca se ejecuta, y casi siempre es un descuido (por ejemplo, un `print` que el programador cree que se mostrará). Java también lo trata como error. Se reporta una vez por bloque y se siguen revisando los demás estatutos. En `main` no se aplica, porque ahí el `return` ya es un error por sí mismo.

```text
código inalcanzable: hay un return antes de este estatuto
```

---

## 4.10 Manejo de errores

1. **La semántica solo corre si no hubo errores léxicos ni sintácticos.** `BabyDuckAnalyzer.compile()` ejecuta `analyze()` y, solo si el resultado es válido, ejecuta `SemanticAnalyzer`. `analyze()` no cambió, así que las pruebas sintácticas siguen probando solo la sintaxis.
2. **Se reportan todos los errores, en orden de línea.** El programador puede corregir varios en una sola compilación, igual que con los errores léxicos y sintácticos. Como el recorrido va de arriba abajo, no hace falta ordenarlos.
3. **Sin errores en cadena.** Si una parte de una expresión ya es `error`, ese error ya se reportó y las operaciones que la usan no reportan otro.

```
_Y_ = _X_ + 1;        // _X_ no declarada
Sin la regla 3:  la variable _X_ no está declarada
                 no se puede asignar error a int          ← consecuencia del primero
Con la regla 3:  la variable _X_ no está declarada
```

Ejemplo con varios errores de declaración:

```
program _Multi_;
var _A_ : int;
    _A_ : float;
void _Multi_(_A_ : int, _B_ : int, _B_ : float) [ var _Multi_ : int; { } ];
main { }
end
```

```text
[Error semántico] línea 3:4 -> la variable _A_ ya fue declarada
[Error semántico] línea 4:5 -> _Multi_ ya se usa como nombre del programa
[Error semántico] línea 4:13 -> _A_ ya se usa como variable global
[Error semántico] línea 4:35 -> _B_ ya fue declarado en la función _Multi_
[Error semántico] línea 4:54 -> _Multi_ ya se usa como nombre del programa
```

Cuando el programa es válido, `Main` imprime el árbol sintáctico y el directorio de funciones:

```text
=== Directorio de funciones ===
Programa: _Declaraciones_
Globales: [_Total_:int, _N_:int, _Prom_:float]
_Fact_ -> int
  params:  [_K_:int]
  locales: [_K_:int, _I_:int]
_Muestra_ -> void
  params:  [_V_:float]
  locales: [_V_:float, _I_:int]
```

---

## 4.11 Pruebas de la etapa

| Clase                   | Qué prueba                                                                                                            |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------- |
| `SemanticCubeTest`      | Cada fila de la tabla de consideraciones, signo unario, simetría de tipos y que solo existan 35 combinaciones válidas |
| `VariableTableTest`     | Agregar, buscar, rechazar duplicados conservando el primero, orden de declaración                                     |
| `FunctionDirectoryTest` | Parámetros en lista y tabla (mismo objeto), duplicados, búsqueda función → globales, espacio de nombres, orden        |
| `SemanticTest`          | Programas `.bd` completos en `src/test/resources/semantic/`                                                           |

En `SemanticTest`, un programa válido no debe producir errores, y uno inválido debe producir **exactamente un** error, de tipo semántico, cuyo mensaje contenga el texto de su línea `// esperado: ...`. Así cada caso falla por la razón correcta y se comprueba que no haya errores en cadena.

| Caso  | Tipo   | Qué prueba                                                                                                                                                                                                                   |
| ----- | ------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| SEM01 | Válido | Globales, parámetros, locales, el mismo nombre local en dos funciones, función recursiva                                                                                                                                     |
| SEM02 | Error  | Variable global doblemente declarada                                                                                                                                                                                         |
| SEM03 | Error  | Variable local con el nombre de un parámetro                                                                                                                                                                                 |
| SEM04 | Error  | Función doblemente declarada                                                                                                                                                                                                 |
| SEM05 | Error  | Parámetro con el nombre de una global (sin sombreado)                                                                                                                                                                        |
| SEM06 | Error  | Función con el nombre de una global (un solo espacio de nombres)                                                                                                                                                             |
| SEM07 | Válido | Promoción `float ← int` en asignación, argumento y `return`; `print` de una comparación; función con valor como estatuto; `return;` en `void`; `if`/`else` que regresa en ambas ramas; signo unario; recursión; bloque `[ ]` |
| SEM08 | Error  | Variable no declarada                                                                                                                                                                                                        |
| SEM09 | Error  | Asignación `int ← float`                                                                                                                                                                                                     |
| SEM10 | Error  | Operación aritmética con `bool`                                                                                                                                                                                              |
| SEM11 | Error  | Condición no booleana                                                                                                                                                                                                        |
| SEM12 | Error  | Número de argumentos                                                                                                                                                                                                         |
| SEM13 | Error  | Función sin `return` en todos los caminos                                                                                                                                                                                    |
| SEM14 | Error  | Tipo de argumento                                                                                                                                                                                                            |
| SEM15 | Error  | Función `void` en una expresión                                                                                                                                                                                              |
| SEM16 | Error  | Llamada a una función declarada más abajo                                                                                                                                                                                    |
| SEM17 | Error  | Función usada como variable                                                                                                                                                                                                  |
| SEM18 | Error  | `return` en `main`                                                                                                                                                                                                           |
| SEM19 | Error  | `return` con valor en función `void`                                                                                                                                                                                         |
| SEM20 | Error  | `return;` en función `int`                                                                                                                                                                                                   |
| SEM21 | Error  | Tipo de `return` incompatible                                                                                                                                                                                                |
| SEM22 | Error  | Código inalcanzable                                                                                                                                                                                                          |

---

## 4.12 Complejidad (promedio)

| Operación                                              | Complejidad                       |
| ------------------------------------------------------ | --------------------------------- |
| Consultar el cubo semántico                            | O(1)                              |
| Insertar o buscar una variable en una tabla            | O(1)                              |
| Buscar una variable desde una función (local → global) | O(1), dos consultas               |
| Revisar si un nombre está ocupado (`globalUse`)        | O(1), tres consultas              |
| Insertar o buscar una función                          | O(1)                              |
| Validar los argumentos de una llamada                  | O(n), n = número de parámetros    |
| Revisar si una función regresa en todos los caminos    | O(m), m = estatutos de la función |

---

## 4.13 Fuera del alcance de esta etapa

- **Variables usadas antes de recibir un valor:** detectarlo en compilación requiere seguir los caminos de ejecución. Se decidirá en la máquina virtual (error en ejecución o valor inicial).
- **División entre cero:** solo se conoce al ejecutar; le corresponde a la máquina virtual.

---

## Navegación

- Anterior: [← Análisis Sintáctico](03_sintaxis.md)
- Siguiente: [Código Intermedio →](05_codigo_intermedio.md)

# 10. Guía de uso de BabyDuck

## Índice

0. [Overview](00_overview.md)
1. [Herramientas](01_herramientas.md)
2. [Análisis Léxico](02_lexico.md)
3. [Análisis Sintáctico](03_sintaxis.md)
4. [Análisis Semántico](04_semantica.md)
5. [Código Intermedio](05_codigo_intermedio.md)
6. [Memoria Virtual](06_memoria_virtual.md)
7. [Máquina Virtual](07_maquina_virtual.md)
8. [Testing](08_testing.md)
9. [Herramientas de IA](herramientas_ia.md)
10. [Guía de uso](guia_de_uso.md)

---

> **Estado de la guía.** Cubre lo que el compilador ya hace: revisar el léxico, la sintaxis y la semántica de un programa. Las secciones de ejecución (entrada, salida y errores en tiempo de ejecución) se completarán cuando estén listas la generación de código y la máquina virtual; están marcadas como _pendiente_.

Esta guía está pensada para alguien que quiere **escribir programas en BabyDuck**. Explica cómo se escribe un programa, qué reglas sigue el compilador, qué errores puede encontrar y cómo corregirlos. El porqué de cada regla está en los capítulos técnicos (por ejemplo, [Análisis Semántico](04_semantica.md)).

---

## 10.1 ¿Qué es BabyDuck?

BabyDuck es un lenguaje de programación pequeño, imperativo y con tipos estáticos, pensado para aprender cómo funciona un compilador. Sus características principales:

- **Dos tipos de datos:** `int` y `float`. Las funciones también pueden ser `void`.
- **Variables globales y locales.** Las globales se declaran al inicio del programa; las locales, al inicio de cada función.
- **Funciones** con parámetros por valor, valor de retorno y recursión.
- **Estatutos:** asignación, `if`/`else`, `while`, `print`, `read`, llamada a función, `return` y bloques `[ ]`.
- **Expresiones** aritméticas (`+ - * /`) y relacionales (`> < == !=`).
- **Revisión estricta de tipos:** el compilador avisa antes de ejecutar si algo no tiene sentido.
- **Comentarios** de una línea con `//`.

El compilador está hecho en Java 21 con ANTLR 4.13.2. Los programas se guardan en archivos con extensión `.bd`.

---

## 10.2 Cómo compilar un programa

### Desde IntelliJ

1. Abre `Main.java` y ejecútalo una vez (triángulo verde junto a `main`). La primera vez mostrará `Uso: Main <archivo.bd> [--tokens]`.
2. En **Run → Edit Configurations…** escribe en **Program arguments** la ruta del programa, por ejemplo `ejemplos/factorial.bd`, y en **Working directory** `$PROJECT_DIR$`.
3. Ejecuta de nuevo.

### Desde la terminal

En la raíz del proyecto (Mac o Linux; en Windows el separador del classpath es `;`):

```bash
mvn -q compile
mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "target/classes:$(cat cp.txt)" com.babyduck.Main ejemplos/factorial.bd
```

### Opciones

| Comando                     | Qué hace                                                             |
| --------------------------- | -------------------------------------------------------------------- |
| `Main programa.bd`          | Revisa el programa (léxico, sintaxis y semántica)                    |
| `Main programa.bd --tokens` | Además, imprime la lista de tokens que encontró el analizador léxico |

### Qué muestra el compilador

**Si el programa es correcto**, imprime `Programa válido`, el árbol sintáctico y el directorio de funciones (las variables y funciones que encontró):

```text
Programa válido: ejemplos/factorial.bd
(program program _Factorial_ ; ...)

=== Directorio de funciones ===
Programa: _Factorial_
Globales: [_N_:int, _R_:int]
_Fact_ -> int
  params:  [_K_:int]
  locales: [_K_:int]
```

**Si tiene errores**, los lista con su fase, línea y columna, y termina con código de salida 1:

```text
Se encontraron 2 error(es) en programa.bd:
  [Error semántico] línea 5:4 -> la variable _Y_ no está declarada
  [Error semántico] línea 9:8 -> no se puede asignar float a _X_, que es int
```

El compilador revisa en tres fases. Si hay errores léxicos o sintácticos, **no** se hace la revisión semántica (primero hay que corregir la forma del programa). Dentro de cada fase se reportan todos los errores, en orden de línea.

---

## 10.3 Estructura de un programa

Todo programa sigue este orden fijo:

```
program _Nombre_;

var                                 // 1. variables globales (opcional)
    _A_, _B_ : int;
    _C_ : float;

int _Funcion_(_P_ : int) [          // 2. funciones (cero o más)
    var _L_ : int;                  //    variables locales (opcional)
    {
        // estatutos
    }
];

main {                              // 3. programa principal (obligatorio)
    // estatutos
}
end
```

El programa válido más pequeño:

```
program _Minimo_;
main { }
end
```

Reglas importantes:

- `program`, `main` y `end` son obligatorios.
- Las variables globales van **antes** de las funciones, y las funciones **antes** de `main`.
- `main` no tiene variables propias: usa las globales.
- Las funciones no se pueden anidar.

---

## 10.4 Elementos léxicos

### Identificadores

Los nombres de variables, funciones y del programa siguen un formato especial: **empiezan y terminan con guion bajo, y el primer carácter después del guion bajo inicial es una letra mayúscula**.

```
_ [A-Z] [letras, dígitos o _]* _
```

| Válidos        | Inválidos  | Por qué                                      |
| -------------- | ---------- | -------------------------------------------- |
| `_X_`          | `_x_`      | Después del primer `_` debe ir una mayúscula |
| `_Contador_`   | `_X`       | Falta el `_` final                           |
| `_A1_b_`       | `_9A_`     | Empieza con un dígito                        |
| `_Suma_Total_` | `Contador` | Faltan los guiones bajos                     |

Gracias a este formato, un identificador nunca puede confundirse con una palabra reservada.

### Palabras reservadas

`program` `main` `end` `var` `int` `float` `void` `if` `else` `while` `do` `print` `read` `return`

### Constantes

| Tipo     | Ejemplos válidos      | Inválidos                                                |
| -------- | --------------------- | -------------------------------------------------------- |
| Entera   | `0`, `42`, `1000`     | —                                                        |
| Flotante | `3.14`, `10.0`, `0.5` | `3.` y `.5` (debe haber dígitos a ambos lados del punto) |

Las constantes no llevan signo: `-2` se escribe igual, pero el compilador lo interpreta como el signo `-` aplicado a `2`.

### Letreros

Texto entre comillas dobles, por ejemplo `"Resultado: "`. Solo pueden usarse dentro de `print`. No pueden ocupar más de una línea. Se aceptan secuencias con diagonal invertida, como `\"` o `\\`.

> _Pendiente:_ el efecto de las secuencias de escape al imprimir se definirá con la máquina virtual.

### Comentarios

`//` hasta el final de la línea. El compilador los ignora.

```
_X_ = 1;   // esto es un comentario
```

---

## 10.5 Tipos de datos y conversiones

| Tipo    | Uso                                          |
| ------- | -------------------------------------------- |
| `int`   | Números enteros                              |
| `float` | Números con decimales                        |
| `void`  | Solo para funciones que no regresan un valor |

Las comparaciones (`_A_ < _B_`) producen un valor **booleano** que solo puede usarse como condición de `if` y `while`, o mostrarse con `print`. No existen variables booleanas.

### Reglas de conversión

| Situación                          | Resultado                          |
| ---------------------------------- | ---------------------------------- |
| `int` con `int` en `+ - *`         | `int`                              |
| Cualquier operación con un `float` | `float`                            |
| Cualquier división `/`             | `float`, siempre: `7 / 2` da `3.5` |
| Guardar un `int` en un `float`     | Permitido: `_F_ = 2;`              |
| Guardar un `float` en un `int`     | **Error**: `_I_ = 2.5;` no compila |

**Consecuencia importante:** como toda división da `float`, **el resultado de una división no se puede guardar en un `int`**. No existe división entera ni conversión explícita de tipos.

```
var _I_ : int;
    _F_ : float;
...
_F_ = 7 / 2;      // correcto: _F_ vale 3.5
_I_ = 7 / 2;      // error: no se puede asignar float a _I_, que es int
```

Las mismas reglas aplican al pasar argumentos a una función y al regresar un valor con `return`.

---

## 10.6 Variables

### Declaración

```
var _A_, _B_ : int;
    _Promedio_ : float;
```

Después de `var` van una o más líneas de la forma `nombres : tipo;`. Varias variables del mismo tipo se separan con comas.

### Alcance

| Dónde se declara                   | Dónde se puede usar                               |
| ---------------------------------- | ------------------------------------------------- |
| Al inicio del programa (globales)  | En todo el programa: `main` y todas las funciones |
| Al inicio de una función (locales) | Solo dentro de esa función                        |
| Como parámetro                     | Solo dentro de esa función, igual que una local   |

### Reglas de nombres

- Un nombre no puede declararse dos veces en el mismo alcance.
- **Una variable local o un parámetro no puede llamarse igual que una variable global.**
- **El programa, las funciones y las variables globales no pueden compartir nombre.** Por ejemplo, no puede haber una variable `_Calc_` y una función `_Calc_`.
- Dos funciones distintas **sí** pueden tener variables locales con el mismo nombre (por ejemplo, `_I_` en ambas).
- Toda variable debe declararse antes de usarse.

---

## 10.7 Expresiones y operadores

### Precedencia

| De mayor a menor | Operadores                |
| ---------------- | ------------------------- |
| 1                | `( )`                     |
| 2                | `+ -` como signo (`-_X_`) |
| 3                | `* /`                     |
| 4                | `+ -`                     |
| 5                | `> < == !=`               |

Los operadores del mismo nivel se evalúan de izquierda a derecha: `10 - 4 - 3` es `(10 - 4) - 3 = 3`.

### Reglas de las expresiones

- **Solo un operador relacional por expresión.** `_A_ < _B_ < _C_` es un error de sintaxis.
- **No existen `>=`, `<=`, `and`, `or` ni `!`.** Para "mayor o igual", usa la comparación contraria y pon tu código en el `else`: `if (_A_ < _B_) { ... } else { /* aquí _A_ >= _B_ */ };`. Para "y", anida dos `if`.
- **Un solo signo por factor.** `--_X_` es inválido; `_A_ - -2` y `-(_A_ + _B_)` son válidos.
- **El resultado de una comparación no se usa en aritmética.** `(_A_ < _B_) + 1` es un error.
- Las llamadas a funciones que regresan un valor pueden usarse dentro de expresiones: `_X_ = _Doble_(3) + 1;`.

---

## 10.8 Estatutos

Todos los estatutos terminan con `;`.

| Estatuto           | Forma                             | Ejemplo                                        |
| ------------------ | --------------------------------- | ---------------------------------------------- |
| Asignación         | `_Var_ = expresión;`              | `_X_ = _A_ * 2 + 1;`                           |
| Condición          | `if (cond) { ... };`              | `if (_X_ > 0) { print("positivo"); };`         |
| Condición con else | `if (cond) { ... } else { ... };` | `if (_X_ > 0) { _S_ = 1; } else { _S_ = 0; };` |
| Ciclo              | `while (cond) do { ... };`        | `while (_I_ < 10) do { _I_ = _I_ + 1; };`      |
| Impresión          | `print(elemento, elemento, ...);` | `print("total: ", _T_);`                       |
| Lectura            | `read(_Var_);`                    | `read(_N_);`                                   |
| Llamada            | `_Funcion_(argumentos);`          | `_Muestra_(_X_);`                              |
| Retorno            | `return expresión;` o `return;`   | `return _A_ + _B_;`                            |
| Bloque             | `[ estatutos ]`                   | `[ _A_ = 1; _B_ = 2; ]`                        |

Detalles:

- `if` y `while` **siempre** llevan llaves `{ }`, aunque tengan un solo estatuto, y **terminan con `;`** después de la última llave.
- `while` lleva la palabra `do` antes de las llaves.
- La condición de `if` y `while` debe ser una **comparación**: `if (_X_)` es un error.
- `print` acepta expresiones y letreros mezclados, separados por comas. Una comparación se muestra como `true` o `false`.
- `read` lee **una** variable; para leer varias, usa varios `read`.
- `=` no se encadena: `_A_ = _B_ = 1;` es un error de sintaxis.

---

## 10.9 Funciones

### Declaración

```
tipo _Nombre_(_Param1_ : tipo, _Param2_ : tipo) [
    var _Local_ : tipo;          // opcional
    {
        // estatutos
    }
];
```

- `tipo` es `int`, `float` o `void`.
- Los parámetros **no** llevan `var`. Si no hay parámetros: `_F_()`.
- El cuerpo va entre `[ ]` y termina con `];`.

### Reglas

| Regla                                                                                             | Ejemplo de error                                      |
| ------------------------------------------------------------------------------------------------- | ----------------------------------------------------- |
| Una función solo puede llamar a funciones declaradas **arriba** de ella, o a sí misma (recursión) | `_A_` llama a `_B_`, pero `_B_` está declarada debajo |
| El número de argumentos debe coincidir con el de parámetros                                       | `_Suma_(1)` cuando `_Suma_` pide dos                  |
| Cada argumento debe poder guardarse en su parámetro                                               | Pasar `2.5` a un parámetro `int`                      |
| Una función `void` no puede usarse dentro de una expresión                                        | `_X_ = _Imprime_() + 1;`                              |
| Una función `int` o `float` debe regresar un valor **en todos los caminos**                       | `if` con `return` pero sin `else`                     |
| Una función `void` no puede regresar un valor                                                     | `return 1;` en una función `void`                     |
| Una función `int` o `float` no puede usar `return;` sin valor                                     | `return;` en una función `int`                        |
| `return` no puede usarse en `main`                                                                | —                                                     |
| No puede haber estatutos después de un `return` en el mismo bloque                                | `return 1; print("fin");`                             |

Una función que regresa un valor **sí** puede llamarse como estatuto (`_Fact_(5);`); el valor simplemente se descarta. Una función `void` puede usar `return;` para terminar antes.

### ¿Cuándo regresa una función "en todos los caminos"?

```
if (_X_ > 0) { return 1; };                         // error: si _X_ <= 0 no regresa nada
if (_X_ > 0) { return 1; } else { return 0; };      // correcto
if (_X_ > 0) { return 1; };  return 0;              // correcto
while (_X_ > 0) do { return 1; };                   // error: el ciclo podría no ejecutarse
```

El compilador no evalúa las condiciones: un `while` siempre se considera un ciclo que podría no ejecutarse. Si una función termina con un ciclo, agrega un `return` después de él.

---

## 10.10 Resumen de restricciones

| Restricción                                             | Alternativa                                                |
| ------------------------------------------------------- | ---------------------------------------------------------- |
| Solo `int` y `float`                                    | —                                                          |
| No hay división entera                                  | Guarda el resultado de `/` en un `float`                   |
| No se puede guardar un `float` en un `int`              | Declara la variable como `float`                           |
| No hay variables booleanas                              | Usa la comparación directamente en `if`, `while` o `print` |
| No existen `>=`, `<=`, `and`, `or`                      | Combina `if`/`else` o invierte la comparación              |
| Un solo relacional por expresión                        | Divide la condición en varios `if` anidados                |
| `if` y `while` siempre con `{ }` y `;` final            | —                                                          |
| `main` no tiene variables propias                       | Declara variables globales                                 |
| Las funciones se llaman solo si están declaradas arriba | Ordena las funciones; no hay recursión mutua               |
| Nombres únicos entre programa, funciones y globales     | Usa nombres descriptivos y distintos                       |
| Las locales no pueden llamarse como una global          | —                                                          |
| `read` lee una sola variable                            | Usa varios `read`                                          |
| Letreros solo en `print`                                | —                                                          |

---

## 10.11 Buenas prácticas y recomendaciones

**Nombres**

- Usa nombres descriptivos: `_Total_`, `_Contador_`, `_Promedio_` en lugar de `_A_`, `_B_`.
- Nombra las funciones con un verbo: `_Calcula_Promedio_`, `_Imprime_Tabla_`, `_Es_Primo_`.
- Nombra las variables con un sustantivo: `_Suma_`, `_Edad_`, `_Resultado_`.
- No reutilices nombres entre funciones y variables; el compilador lo prohíbe, y además dificulta la lectura.
- Para contadores de ciclos, un nombre corto como `_I_` está bien, porque cada función puede tener el suyo.

**Tipos**

- Si un valor puede tener decimales (cualquier división o cualquier cálculo con un `float`), declara la variable como `float`.
- Declara como `int` solo lo que siempre es entero: contadores, índices, cantidades.

**Funciones**

- Declara las funciones auxiliares antes que las funciones que las usan.
- En funciones con valor, asegúrate de que cada rama de un `if` termine con `return`, o pon un `return` al final.
- No escribas estatutos después de un `return`: nunca se ejecutarán.

**Organización**

- Usa comentarios `//` para explicar qué hace cada función.
- Corrige los errores en el orden en que aparecen: un error de sintaxis puede ocultar los semánticos.

---

## 10.12 Errores comunes y cómo corregirlos

Los errores léxicos y sintácticos los genera ANTLR y aparecen en inglés. Los semánticos aparecen en español.

### Errores léxicos (caracteres o palabras que no existen en el lenguaje)

| Mensaje                                     | Causa típica                          | Corrección                                  |
| ------------------------------------------- | ------------------------------------- | ------------------------------------------- |
| `token recognition error at: '_x'`          | Identificador en minúscula (`_x_`)    | Usa mayúscula después del primer `_`: `_X_` |
| `token recognition error at: '.'`           | Flotante incompleto (`3.`)            | Escribe dígitos después del punto: `3.0`    |
| `token recognition error at: '"hola); }\n'` | Letrero sin comilla de cierre         | Cierra el letrero: `"hola"`                 |
| `token recognition error at: '@'`           | Carácter que no pertenece al lenguaje | Elimínalo                                   |

### Errores sintácticos (la forma del programa es incorrecta)

| Mensaje (ejemplo)                              | Causa típica                                          | Corrección                         |
| ---------------------------------------------- | ----------------------------------------------------- | ---------------------------------- |
| `missing ';' at 'print'`                       | Falta `;` al final del estatuto anterior              | Agrega `;`                         |
| `mismatched input '}' expecting {'else', ';'}` | Falta `;` después de la llave final de un `if`        | `if (...) { ... };`                |
| `missing 'do' at '{'`                          | `while` sin `do`                                      | `while (...) do { ... };`          |
| `missing ')' at ';'`                           | Paréntesis sin cerrar                                 | Revisa que cada `(` tenga su `)`   |
| `mismatched input '<' expecting {...}`         | Dos relacionales en una expresión (`_A_ < _B_ < _C_`) | Usa dos condiciones separadas      |
| `extraneous input '-' expecting {...}`         | Dos signos seguidos (`--1`)                           | Usa paréntesis: `-(-1)`            |
| `mismatched input '=' expecting {...}`         | Asignación encadenada (`_A_ = _B_ = 1`)               | Usa dos asignaciones               |
| `extraneous input 'var' expecting {...}`       | Variables globales declaradas después de una función  | Mueve `var` antes de las funciones |
| `missing 'end' at '<EOF>'`                     | Falta `end` al final                                  | Agrega `end` después de `main { }` |

### Errores semánticos (el programa tiene la forma correcta pero no tiene sentido)

| Mensaje                                                              | Causa                                                                                | Corrección                                         |
| -------------------------------------------------------------------- | ------------------------------------------------------------------------------------ | -------------------------------------------------- |
| `la variable _A_ ya fue declarada`                                   | La misma variable global se declaró dos veces                                        | Elimina o renombra una                             |
| `_X_ ya fue declarado en la función _F_`                             | Parámetro o local repetidos                                                          | Renombra uno                                       |
| `_X_ ya se usa como variable global`                                 | Local o parámetro con el nombre de una global, o función con el nombre de una global | Elige otro nombre                                  |
| `_X_ ya se usa como nombre del programa`                             | Variable o función con el nombre del programa                                        | Elige otro nombre                                  |
| `la función _F_ ya fue declarada`                                    | Dos funciones con el mismo nombre                                                    | Renombra una                                       |
| `la variable _X_ no está declarada`                                  | Se usa una variable sin declararla                                                   | Declárala en `var`                                 |
| `la función _G_ no está declarada`                                   | Función inexistente o declarada más abajo                                            | Declárala o muévela arriba                         |
| `_F_ es una función, no una variable`                                | Falta `( )` al usar una función                                                      | `_F_(...)`                                         |
| `_X_ es una variable, no una función`                                | Se puso `( )` a una variable                                                         | Quita los paréntesis                               |
| `_Suma_ espera 2 argumentos, recibió 1`                              | Número incorrecto de argumentos                                                      | Pasa todos los argumentos                          |
| `argumento 1 de _F_: no se puede pasar float a int`                  | Argumento de tipo incompatible                                                       | Cambia el tipo del parámetro o del argumento       |
| `la función _F_ es void y no devuelve un valor`                      | Función `void` dentro de una expresión                                               | Llámala como estatuto: `_F_();`                    |
| `no se puede asignar float a _X_, que es int`                        | `float` guardado en `int` (por ejemplo, una división)                                | Declara `_X_` como `float`                         |
| `no se puede aplicar + entre bool e int`                             | Una comparación usada en aritmética                                                  | Usa la comparación solo en `if`/`while`/`print`    |
| `no se puede aplicar el signo - a bool`                              | Signo aplicado a una comparación                                                     | Quita el signo                                     |
| `la condición del if debe ser una comparación (bool), se obtuvo int` | `if (_X_)`                                                                           | `if (_X_ != 0)`                                    |
| `return solo puede usarse dentro de una función`                     | `return` en `main`                                                                   | Quítalo                                            |
| `la función _F_ es void, no puede regresar un valor`                 | `return valor;` en función `void`                                                    | Usa `return;` o cambia el tipo de la función       |
| `la función _F_ debe regresar un valor int`                          | `return;` en función con valor                                                       | `return valor;`                                    |
| `la función _F_ regresa int, no puede regresar float`                | El valor de `return` no cabe en el tipo de la función                                | Cambia el tipo de la función a `float`             |
| `la función _F_ no regresa un valor en todos los caminos`            | Algún camino termina sin `return`                                                    | Agrega `else` con `return`, o un `return` al final |
| `código inalcanzable: hay un return antes de este estatuto`          | Estatutos después de un `return`                                                     | Muévelos antes del `return` o elimínalos           |

### Errores en tiempo de ejecución

> _Pendiente (máquina virtual):_ división entre cero, uso de una variable sin valor asignado, entrada inválida en `read`.

---

## 10.13 Ejemplos de programas válidos

Todos estos programas pasan las tres fases del compilador.

### Factorial recursivo

```
// Factorial recursivo
program _Factorial_;
var _N_, _R_ : int;

int _Fact_(_K_ : int) [
    {
        if (_K_ < 2) {
            return 1;
        } else {
            return _K_ * _Fact_(_K_ - 1);
        };
    }
];

main {
    read(_N_);
    _R_ = _Fact_(_N_);
    print("factorial de ", _N_, " = ", _R_);
}
end
```

### Fibonacci iterativo

```
// Fibonacci iterativo
program _Fibonacci_;
var _N_, _A_, _B_, _T_, _I_ : int;

main {
    read(_N_);
    _A_ = 0;
    _B_ = 1;
    _I_ = 0;
    while (_I_ < _N_) do {
        _T_ = _A_ + _B_;
        _A_ = _B_;
        _B_ = _T_;
        _I_ = _I_ + 1;
    };
    print("fib(", _N_, ") = ", _A_);
}
end
```

### Promedio con `float`, función `void` y bloque

```
// Promedio con float, función void y comparación en print
program _Promedio_;
var _Suma_, _Cuenta_ : int;
    _Prom_ : float;

void _Reporta_(_Valor_ : float) [
    {
        if (_Valor_ < 0) {
            print("valor negativo");
            return;
        };
        print("promedio: ", _Valor_);
    }
];

float _Divide_(_A_ : int, _B_ : int) [
    {
        return _A_ / _B_;
    }
];

main {
    _Suma_ = 17;
    _Cuenta_ = 4;
    _Prom_ = _Divide_(_Suma_, _Cuenta_);
    _Reporta_(_Prom_);
    print("¿mayor que 4? ", _Prom_ > 4);
    [
        _Prom_ = -_Prom_ * 2;
        _Reporta_(_Prom_);
    ]
}
end
```

Este ejemplo muestra:

- Un `int` pasado a un parámetro `float`.
- Una división guardada en un `float`.
- Un `return;` en una función `void`.
- Una comparación dentro de `print`.
- Un signo unario.
- Un bloque `[ ]`.

---

## 10.14 Ejecución de programas

> _Pendiente (código intermedio, memoria virtual y máquina virtual)._ Esta sección explicará cómo ejecutar un programa compilado, cómo introducir datos con `read`, cómo se muestran los resultados de `print` y cómo ver el código intermedio generado.

---

## 10.15 Referencia rápida

```
program _Nombre_;
var _A_, _B_ : int;   _F_ : float;

int _Fn_(_P_ : int, _Q_ : float) [
    var _L_ : int;
    {
        _L_ = _P_ * 2;
        if (_L_ > 10) { return _L_; } else { return 0; };
    }
];

void _Proc_() [ { print("hola"); } ];

main {
    _A_ = 1;
    _F_ = _A_ / 2;                       // la división siempre es float
    while (_A_ < 5) do { _A_ = _A_ + 1; };
    if (_A_ == 5) { print("listo"); };
    _B_ = _Fn_(_A_, _F_);
    _Proc_();
    read(_B_);
    print("A = ", _A_, ", B = ", _B_);
    [ _A_ = 0; _B_ = 0; ]
}
end
```

| Elemento      | Forma                                    |
| ------------- | ---------------------------------------- |
| Identificador | `_Mayúscula..._`                         |
| Comentario    | `// texto`                               |
| Tipos         | `int`, `float`, `void` (solo funciones)  |
| Aritméticos   | `+ - * /` (`/` siempre da `float`)       |
| Relacionales  | `> < == !=` (uno por expresión)          |
| `if`          | `if (cond) { } else { };`                |
| `while`       | `while (cond) do { };`                   |
| Función       | `tipo _F_(_P_ : tipo) [ var ...; { } ];` |

---

## Navegación

- Anterior: [← Herramientas de IA](09_herramientas_ia.md)

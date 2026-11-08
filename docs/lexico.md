# 2. Análisis Léxico

## Índice

0. [Overview](overview.md)
1. [Herramientas](herramientas.md)
2. [Análisis Léxico](lexico.md)
3. [Análisis Sintáctico](sintaxis.md)
4. [Análisis Semántico](semantica.md)
5. Código Intermedio _(pendiente)_
6. Memoria Virtual _(pendiente)_
7. Máquina Virtual _(pendiente)_
8. [Testing](08_testing.md)
9. [Herramientas de IA](herramientas_ia.md)
10. [Guía de uso](guia_de_uso.md)

---

## 2.1 Expresiones regulares

| Token        | Expresión regular      | Ejemplos válidos                  |
| ------------ | ---------------------- | --------------------------------- |
| `id`         | `_[A-Z][A-Za-z0-9_]*_` | `_X_`, `_Contador_`, `_A1_b_`     |
| `cte_int`    | `[0-9]+`               | `0`, `42`                         |
| `cte_float`  | `[0-9]+\.[0-9]+`       | `3.14`, `10.0`                    |
| `cte_string` | `"([^"\\\n]\|\\.)*"`   | `"hola"`, `""`, `"dice \"hola\""` |
| comentario   | `//[^\n]*`             | `// esto se ignora`               |

### Decisiones de diseño

- **Identificadores:** inician y terminan con `_` (literal) y el primer carácter después del `_` inicial es una letra **mayúscula**. El identificador más corto posible es `_A_`.
  - Ventaja: ningún identificador puede confundirse con una palabra reservada, porque estas van en minúscula y no empiezan con `_`.
  - Las funciones también son identificadores: `_Factorial_(5)`.
- **Flotantes:** requieren dígitos a ambos lados del punto; `3.` y `.5` no son válidos.
- **Letreros:** van entre comillas dobles, no pueden contener saltos de línea y admiten secuencias de escape con `\` (por ejemplo `\"`). Solo se usan dentro de `print`.
- **Comentarios** (extensión): de una línea, con `//`. Se descartan junto con espacios, tabuladores y saltos de línea.

## 2.2 Tokens

### Palabras reservadas

| Token     | Lexema    | Token    | Lexema   |
| --------- | --------- | -------- | -------- |
| `PROGRAM` | `program` | `WHILE`  | `while`  |
| `MAIN`    | `main`    | `DO`     | `do`     |
| `END`     | `end`     | `IF`     | `if`     |
| `VOID`    | `void`    | `ELSE`   | `else`   |
| `VAR`     | `var`     | `READ`   | `read`   |
| `PRINT`   | `print`   | `RETURN` | `return` |
| `INT`     | `int`     |          |          |
| `FLOAT`   | `float`   |          |          |

### Operadores

| Token    | Lexema | Descripción                                                                             |
| -------- | ------ | --------------------------------------------------------------------------------------- |
| `PLUS`   | `+`    | Suma / signo positivo                                                                   |
| `MINUS`  | `-`    | Resta / signo negativo                                                                  |
| `MULT`   | `*`    | Multiplicación                                                                          |
| `DIV`    | `/`    | División                                                                                |
| `ASSIGN` | `=`    | Asignación                                                                              |
| `EQ`     | `==`   | Igual a _(extensión, ver [§3.3](03_sintaxis.md#33-modificaciones-a-la-especificación))_ |
| `NEQ`    | `!=`   | Diferente de                                                                            |
| `GT`     | `>`    | Mayor que                                                                               |
| `LT`     | `<`    | Menor que                                                                               |

### Caracteres especiales

| Token    | Lexema | Token      | Lexema |
| -------- | ------ | ---------- | ------ |
| `SEMI`   | `;`    | `LPAREN`   | `(`    |
| `COMMA`  | `,`    | `RPAREN`   | `)`    |
| `COLON`  | `:`    | `LBRACKET` | `[`    |
| `LBRACE` | `{`    | `RBRACKET` | `]`    |
| `RBRACE` | `}`    |            |        |

### Identificadores y constantes

| Token        | Descripción                                            |
| ------------ | ------------------------------------------------------ |
| `ID`         | Identificadores de variables, funciones y del programa |
| `CTE_INT`    | Constantes enteras                                     |
| `CTE_FLOAT`  | Constantes flotantes                                   |
| `CTE_STRING` | Letreros (constantes de texto)                         |

### Tokens ignorados

| Token     | Descripción                             |
| --------- | --------------------------------------- |
| `WS`      | Espacios, tabuladores y saltos de línea |
| `COMMENT` | Comentarios de una línea `// ...`       |

## 2.3 Implementación en ANTLR4

Las reglas léxicas están en la segunda mitad de `src/main/antlr4/com/babyduck/BabyDuck.g4`. En ANTLR, los nombres de las reglas léxicas **deben empezar con mayúscula**.

```antlr
// --- Palabras reservadas (antes de ID) ---
PROGRAM : 'program' ;
MAIN    : 'main' ;
END     : 'end' ;
VOID    : 'void' ;
VAR     : 'var' ;
PRINT   : 'print' ;
INT     : 'int' ;
FLOAT   : 'float' ;
WHILE   : 'while' ;
DO      : 'do' ;
IF      : 'if' ;
ELSE    : 'else' ;
READ    : 'read' ;
RETURN  : 'return' ;

// --- Operadores ---
PLUS   : '+' ;
MINUS  : '-' ;
MULT   : '*' ;
DIV    : '/' ;
ASSIGN : '=' ;
EQ     : '==' ;
NEQ    : '!=' ;
GT     : '>' ;
LT     : '<' ;

// --- Caracteres especiales ---
SEMI     : ';' ;
COMMA    : ',' ;
COLON    : ':' ;
LBRACE   : '{' ;
RBRACE   : '}' ;
LPAREN   : '(' ;
RPAREN   : ')' ;
LBRACKET : '[' ;
RBRACKET : ']' ;

// --- Identificadores y constantes ---
ID         : '_' [A-Z] [a-zA-Z0-9_]* '_' ;
CTE_FLOAT  : [0-9]+ '.' [0-9]+ ;
CTE_INT    : [0-9]+ ;
CTE_STRING : '"' (~["\\\r\n] | '\\' .)* '"' ;

// --- Ignorados ---
COMMENT : '//' ~[\r\n]* -> skip ;
WS      : [ \t\r\n]+ -> skip ;
```

### Cómo resuelve ANTLR las coincidencias

1. **Coincidencia más larga:** el lexer siempre toma el token más largo posible. Por eso `==` se reconoce como `EQ` y no como dos `ASSIGN`, `!=` como `NEQ`, y `3.14` como un solo `CTE_FLOAT` (no `3`, `.`, `14`). A diferencia del lexer del proyecto de referencia (SLY), **el orden de `EQ` y `ASSIGN` no importa**.
2. **Empate en longitud:** gana la regla definida primero. Por convención, las palabras reservadas se colocan antes de `ID`. Con el formato `_X_` nunca llegan a empatar, pero se conserva el orden como buena práctica.
3. **`-> skip`:** indica que el token se reconoce pero se descarta, así que el parser nunca ve espacios ni comentarios.

### Errores léxicos

Cuando ningún patrón coincide, el lexer reporta `token recognition error at: '...'`. La clase `ErrorCollector` lo registra como **error léxico** con su línea y columna:

```
[Error léxico] línea 1:0 -> token recognition error at: '_x'
```

---

## Navegación

- Anterior: [← Herramientas de generación](01_herramientas.md)
- Siguiente: [Análisis Sintáctico →](03_sintaxis.md)

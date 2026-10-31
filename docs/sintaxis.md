# 3. Análisis Sintáctico

## Índice

1. [Overview](00_overview.md)
2. [Herramientas de generación](01_herramientas.md)
3. [Análisis Léxico](02_lexico.md)
4. [Análisis Sintáctico](03_sintaxis.md)
5. Análisis Semántico *(pendiente)*
6. Código Intermedio *(pendiente)*
7. Memoria Virtual *(pendiente)*
8. Máquina Virtual *(pendiente)*
9. [Testing](08_testing.md)
10. [Herramientas de IA](09_herramientas_ia.md)

---

## 3.1 Formato en que se dieron de alta las reglas

Las reglas de construcción de BabyDuck se dieron de alta en el archivo **`BabyDuck.g4`** (`src/main/antlr4/com/babyduck/`), en la **notación EBNF de ANTLR4**. Es una gramática combinada: el mismo archivo contiene

- las **reglas del parser** (nombres en minúscula), que corresponden a la CFG, y
- las **reglas del lexer** (nombres en MAYÚSCULA), que corresponden a las expresiones regulares ([§2.3](02_lexico.md#23-implementación-en-antlr4)).

La CFG se diseñó originalmente en **BNF** ([§3.2](#32-gramática-original-bnf)) y se transcribió a EBNF sustituyendo las reglas recursivas con ε por los operadores:

| Operador EBNF | Significado | Equivale en BNF a |
|---|---|---|
| `X?` | Opcional (0 o 1 vez) | `<X_opt> → X \| ε` |
| `X*` | Cero o más veces | `<Li_X> → X <Li_X> \| ε` |
| `X+` | Una o más veces | `<Li_X> → X <Li_X> \| X` |
| `( ... )` | Agrupación | Una regla auxiliar para el grupo |

Ejemplo:

```
BNF                                              EBNF (ANTLR)
<Exp>      → <Termino> <Add_term>                exp : term ((PLUS | MINUS) term)* ;
<Add_term> → <Opr_add> <Termino> <Add_term>
<Add_term> → ε
<Opr_add>  → + | -
```

Ambas notaciones describen el mismo lenguaje. Con EBNF el árbol de parseo es más plano (los términos de una suma quedan como hijos directos de `exp`), lo que facilita recorrerlo en las siguientes etapas y conserva la asociatividad izquierda al leerlo de izquierda a derecha.

**Convención de nombres:** los nombres de reglas y tokens están en inglés, igual que las palabras reservadas de BabyDuck, siguiendo la convención de ANTLR (`lowerCamelCase` para reglas y `UPPER_SNAKE_CASE` para tokens).

## 3.2 Gramática original (BNF)

Gramática entregada en la actividad anterior, basada en los diagramas de sintaxis de BabyDuck.

```bnf
/* ─── PROGRAMA ─── */
<Programa>     → program id ; <Vars_opt> <Li_funcs> main <Body> end
<Vars_opt>     → <Vars> | ε
<Li_funcs>     → <Funcs> <Li_funcs> | ε

/* ─── VARIABLES ─── */
<Vars>         → var <Declarar_var> <Decl_mas>
<Declarar_var> → id <Mas_id> : <Type> ;
<Mas_id>       → , id <Mas_id> | ε
<Decl_mas>     → <Declarar_var> <Decl_mas> | ε
<Type>         → int | float

/* ─── FUNCIONES ─── */
<Funcs>        → <TypeFunc> id ( <Params_opt> ) [ <Vars_opt> <Body> ] ;
<TypeFunc>     → void | <Type>
<Params_opt>   → <Param> <Mas_params> | ε
<Param>        → id : <Type>
<Mas_params>   → , <Param> <Mas_params> | ε

/* ─── ESTATUTOS ─── */
<Body>         → { <Li_Statement> }
<Li_Statement> → <Statement> <Li_Statement> | ε
<Statement>    → [ <Li_Statement> ] | <Assign> | <Condition> | <Cycle>
               | <F_call> | <Print> | <Read> | <Return>
<Assign>       → id = <Expresion> ;
<Condition>    → if ( <Expresion> ) <Statement> <Co_else> ;
<Co_else>      → else <Statement> | ε
<Cycle>        → while ( <Expresion> ) do <Statement> ;
<Print>        → print ( <Li_print> ) ;
<Li_print>     → <Expresion> <Li_p> | cte.string <Li_p>
<Li_p>         → , <Li_print> | ε
<Read>         → read ( id ) ;
<Return>       → return <Expresion> ;
<F_call>       → id ( <Args_opt> ) ;
<Args_opt>     → <Expresion> <More_args> | ε
<More_args>    → , <Expresion> <More_args> | ε

/* ─── EXPRESIONES ─── */
<Expresion>    → <Exp> <Rel_Expr>
<Rel_Expr>     → <Opr_rel> <Exp> | ε
<Opr_rel>      → > | < | !=
<Exp>          → <Termino> <Add_term>
<Add_term>     → <Opr_add> <Termino> <Add_term> | ε
<Opr_add>      → + | -
<Termino>      → <Factor> <Add_Fact>
<Add_Fact>     → <Opr_mult> <Factor> <Add_Fact> | ε
<Opr_mult>     → * | /
<Factor>       → ( <Expresion> ) | <F_call> | <Signo_opt> <valor>
<Signo_opt>    → <Opr_add> | ε
<valor>        → id | <CTE>
<CTE>          → cte_int | cte_float
```

## 3.3 Modificaciones a la especificación

Los diagramas de BabyDuck son una propuesta que puede ajustarse con justificación. Se hicieron las siguientes modificaciones:

| # | Regla | Cambio | Tipo |
|---|---|---|---|
| 1 | `<F_call>` | Se quita el `;` y se agrega en `<Statement> → <F_call> ;` | Corrección |
| 2 | `<Condition>`, `<Cycle>` | `<Statement>` → `<Body>` | Mejora de claridad |
| 3 | `<Opr_rel>` | Se agrega `==` | Extensión |
| 4 | `<Factor>` | El signo aplica a cualquier factor | Extensión |
| 5 | `<Return>` | La expresión es opcional | Extensión |
| 6 | Lexer | Comentarios `//` | Extensión léxica |

### M1. `<F_call>` sin `;`

**Problema:** `<Factor> → <F_call>` y `<F_call>` terminaba en `;`, así que una llamada dentro de una expresión exigía un `;` a la mitad: `_X_ = _Doble_(4) + 1;` tendría que escribirse `_X_ = _Doble_(4); + 1;`.

**Cambio:** la llamada se define sin `;` y el `;` se agrega solo cuando la llamada se usa como estatuto.

```
<F_call>    → id ( <Args_opt> )
<Statement> → <F_call> ;
```

Es el mismo criterio que con `<Expresion>`: la expresión no lleva `;`, y es el estatuto que la usa (`<Assign>`) quien lo agrega.

### M2. `<Condition>` y `<Cycle>` usan `<Body>`

**Problema:** en el diagrama original, `if` y `while` reciben un solo `<Statement>`. Como cada estatuto ya termina en `;`, un estatuto simple genera doble punto y coma (`while (c) do _X_ = 1; ;`), y para varios estatutos se necesitan corchetes `[ ]`, distintos de las llaves `{ }` de `main` y de las funciones. El conteo de `;` es propenso a errores, sobre todo en estructuras anidadas.

**Cambio:**

```
<Condition> → if ( <Expresion> ) <Body> <Co_else> ;
<Co_else>   → else <Body> | ε
<Cycle>     → while ( <Expresion> ) do <Body> ;
```

| | `<Statement>` (original) | `<Body>` (modificación) |
|---|---|---|
| Sintaxis | `while (c) do _X_ = 1; ;` o `do [ ... ];` | `while (c) do { ... };` |
| Doble `;` | Sí | No |
| Delimitadores de bloque | `{ }` y `[ ]` | Solo `{ }` |
| Bloques anidados | Difíciles de leer | Explícitos |
| Costo | — | Un solo estatuto también va entre llaves |

Se conserva el `;` final para que el cambio se limite a sustituir `<Statement>` por `<Body>`; ese `;` además evita la ambigüedad del *dangling else*. La generación de código intermedio (saltos `GotoF`/`Goto`) no se ve afectada. El proyecto de referencia del curso toma la misma decisión (`<CONDICION>` y `<CICLO>` usan `<CUERPO>`).

La alternativa `[ <Li_Statement> ]` de `<Statement>` se conserva tal como está en el diagrama.

### M3. Operador `==`

**Problema:** solo existían `>`, `<` y `!=`. Para preguntar si un valor es igual a otro había que invertir la lógica: `if (_X_ != 0) { } else { ... };`.

**Cambio:** `<Opr_rel> → > | < | != | ==`, con el token `EQ : '==' ;`. No hay conflicto con `=` porque el lexer toma el token más largo.

### M4. Signo aplicable a cualquier factor

**Problema:** con `<Factor> → ... | <Signo_opt> <valor>` y `<valor> → id | <CTE>`, solo los identificadores y constantes aceptaban signo. Expresiones como `-(_A_ + _B_)` o `-_Doble_(3)` eran inválidas y debían escribirse `0 - (_A_ + _B_)`.

**Cambio:** los paréntesis y la llamada se mueven a `<valor>`:

```
<Factor> → <Signo_opt> <valor>
<valor>  → ( <Expresion> ) | <F_call> | id | <CTE>
```

Sigue permitiéndose un solo signo (`--_X_` es inválido, `_A_ - -2` es válido).

### M5. `return` con expresión opcional

**Problema:** una función `void` no tenía forma de terminar su ejecución antes de tiempo.

**Cambio:** `<Return> → return <Expresion> ; | return ;`

La sintaxis acepta ambas formas; en el **análisis semántico** se validará que una función `void` no regrese valor y que una función `int`/`float` sí lo haga.

### M6. Comentarios

Se agrega `COMMENT : '//' ~[\r\n]* -> skip ;` al lexer. No modifica ninguna regla gramatical y permite documentar los programas de prueba.

## 3.4 Gramática final (BNF)

Gramática con las modificaciones aplicadas (marcadas con `M#`).

```bnf
/* ─── PROGRAMA ─── */
<Programa>     → program id ; <Vars_opt> <Li_funcs> main <Body> end
<Vars_opt>     → <Vars> | ε
<Li_funcs>     → <Funcs> <Li_funcs> | ε

/* ─── VARIABLES ─── */
<Vars>         → var <Declarar_var> <Decl_mas>
<Declarar_var> → id <Mas_id> : <Type> ;
<Mas_id>       → , id <Mas_id> | ε
<Decl_mas>     → <Declarar_var> <Decl_mas> | ε
<Type>         → int | float

/* ─── FUNCIONES ─── */
<Funcs>        → <TypeFunc> id ( <Params_opt> ) [ <Vars_opt> <Body> ] ;
<TypeFunc>     → void | <Type>
<Params_opt>   → <Param> <Mas_params> | ε
<Param>        → id : <Type>
<Mas_params>   → , <Param> <Mas_params> | ε

/* ─── ESTATUTOS ─── */
<Body>         → { <Li_Statement> }
<Li_Statement> → <Statement> <Li_Statement> | ε
<Statement>    → [ <Li_Statement> ] | <Assign> | <Condition> | <Cycle>
               | <F_call> ;                                          /* M1 */
               | <Print> | <Read> | <Return>
<Assign>       → id = <Expresion> ;
<Condition>    → if ( <Expresion> ) <Body> <Co_else> ;               /* M2 */
<Co_else>      → else <Body> | ε                                     /* M2 */
<Cycle>        → while ( <Expresion> ) do <Body> ;                   /* M2 */
<Print>        → print ( <Li_print> ) ;
<Li_print>     → <Expresion> <Li_p> | cte_string <Li_p>
<Li_p>         → , <Li_print> | ε
<Read>         → read ( id ) ;
<Return>       → return <Ret_val> ;                                  /* M5 */
<Ret_val>      → <Expresion> | ε                                     /* M5 */
<F_call>       → id ( <Args_opt> )                                   /* M1 */
<Args_opt>     → <Expresion> <More_args> | ε
<More_args>    → , <Expresion> <More_args> | ε

/* ─── EXPRESIONES ─── */
<Expresion>    → <Exp> <Rel_Expr>
<Rel_Expr>     → <Opr_rel> <Exp> | ε
<Opr_rel>      → > | < | != | ==                                     /* M3 */
<Exp>          → <Termino> <Add_term>
<Add_term>     → <Opr_add> <Termino> <Add_term> | ε
<Opr_add>      → + | -
<Termino>      → <Factor> <Add_Fact>
<Add_Fact>     → <Opr_mult> <Factor> <Add_Fact> | ε
<Opr_mult>     → * | /
<Factor>       → <Signo_opt> <valor>                                 /* M4 */
<Signo_opt>    → <Opr_add> | ε
<valor>        → ( <Expresion> ) | <F_call> | id | <CTE>             /* M4 */
<CTE>          → cte_int | cte_float
```

## 3.5 Implementación en ANTLR4 (EBNF)

Reglas del parser en `BabyDuck.g4`. La primera regla, `program`, es el símbolo inicial; el `EOF` final obliga a consumir el archivo completo.

```antlr
grammar BabyDuck;

// ---------- Programa ----------
program    : PROGRAM ID SEMI vars? funcDecl* MAIN body END EOF ;

vars       : VAR varDecl+ ;
varDecl    : ID (COMMA ID)* COLON type SEMI ;
type       : INT | FLOAT ;

// ---------- Funciones ----------
funcDecl   : funcType ID LPAREN params? RPAREN LBRACKET vars? body RBRACKET SEMI ;
funcType   : VOID | type ;
params     : param (COMMA param)* ;
param      : ID COLON type ;

// ---------- Estatutos ----------
body       : LBRACE statement* RBRACE ;

statement  : assign
           | condition
           | cycle
           | funcCall SEMI                    // M1
           | print
           | read
           | returnStmt
           | LBRACKET statement* RBRACKET ;

assign     : ID ASSIGN expression SEMI ;
condition  : IF LPAREN expression RPAREN body (ELSE body)? SEMI ;   // M2
cycle      : WHILE LPAREN expression RPAREN DO body SEMI ;          // M2
print      : PRINT LPAREN printItem (COMMA printItem)* RPAREN SEMI ;
printItem  : expression | CTE_STRING ;
read       : READ LPAREN ID RPAREN SEMI ;
returnStmt : RETURN expression? SEMI ;                              // M5

// ---------- Expresiones ----------
expression : exp (relOp exp)? ;
relOp      : GT | LT | NEQ | EQ ;                                   // M3
exp        : term ((PLUS | MINUS) term)* ;
term       : factor ((MULT | DIV) factor)* ;
factor     : (PLUS | MINUS)? value ;                                // M4
value      : LPAREN expression RPAREN
           | funcCall
           | ID
           | cte ;
cte        : CTE_INT | CTE_FLOAT ;
funcCall   : ID LPAREN (expression (COMMA expression)*)? RPAREN ;   // M1
```

## 3.6 Equivalencia BNF → ANTLR

| Gramática BNF | Regla en `BabyDuck.g4` |
|---|---|
| `<Programa>` | `program` |
| `<Vars_opt>`, `<Vars>`, `<Declarar_var>`, `<Mas_id>`, `<Decl_mas>` | `vars?`, `vars`, `varDecl` |
| `<Type>` | `type` |
| `<Li_funcs>`, `<Funcs>`, `<TypeFunc>` | `funcDecl*`, `funcDecl`, `funcType` |
| `<Params_opt>`, `<Param>`, `<Mas_params>` | `params?`, `params`, `param` |
| `<Body>`, `<Li_Statement>` | `body`, `statement*` |
| `<Statement>` | `statement` |
| `<Assign>` | `assign` |
| `<Condition>`, `<Co_else>` | `condition` |
| `<Cycle>` | `cycle` |
| `<Print>`, `<Li_print>`, `<Li_p>` | `print`, `printItem` |
| `<Read>` | `read` |
| `<Return>`, `<Ret_val>` | `returnStmt` |
| `<F_call>`, `<Args_opt>`, `<More_args>` | `funcCall` |
| `<Expresion>`, `<Rel_Expr>` | `expression` |
| `<Opr_rel>` | `relOp` |
| `<Exp>`, `<Add_term>`, `<Opr_add>` | `exp` |
| `<Termino>`, `<Add_Fact>`, `<Opr_mult>` | `term` |
| `<Factor>`, `<Signo_opt>` | `factor` |
| `<valor>` | `value` |
| `<CTE>` | `cte` |

## 3.7 Observaciones sobre la gramática

- **Precedencia de operadores:** se obtiene de la jerarquía de reglas `expression → exp → term → factor`. Los operadores relacionales tienen la menor precedencia, después `+ -` y al final `* /`. Los paréntesis permiten alterarla.
- **Un solo operador relacional por expresión:** `expression : exp (relOp exp)?` hace que `_A_ < _B_ < _C_` sea inválido.
- **Reglas que empiezan con `id`:** `assign`, `funcCall` y el `ID` dentro de `value` comparten el primer token. El algoritmo ALL(\*) de ANTLR revisa los tokens siguientes (`=`, `(` u otro) y elige la alternativa correcta sin indicar *lookahead* manualmente.
- **Orden de las secciones del programa:** `var` → funciones → `main`. Declarar variables globales después de las funciones es un error de sintaxis.
- **Validaciones que no son sintácticas:** que una variable esté declarada, la compatibilidad de tipos y el uso correcto de `return` se verificarán en el análisis semántico.

## 3.8 Manejo de errores sintácticos

ANTLR reporta los errores a los *error listeners* del lexer y del parser. Por defecto solo los imprime y continúa; en este proyecto se reemplazan por `ErrorCollector`, que guarda cada error con su fase, línea y columna:

```java
ErrorCollector errors = new ErrorCollector();
lexer.removeErrorListeners();   lexer.addErrorListener(errors);
parser.removeErrorListeners();  parser.addErrorListener(errors);
ParseTree tree = parser.program();
```

Ejemplo de salida:

```
[Error sintáctico] línea 6:4 -> missing ';' at 'print'
```

Mensajes típicos de ANTLR:

| Mensaje | Significado |
|---|---|
| `missing X at 'Y'` | Faltaba el token X antes de Y |
| `extraneous input 'X' expecting {...}` | Sobra el token X |
| `mismatched input 'X' expecting {...}` | Se encontró X donde se esperaba otra cosa |
| `no viable alternative at input '...'` | Ninguna alternativa de la regla coincide |

Después de un error, ANTLR intenta recuperarse y seguir analizando, por lo que un solo error puede provocar otros en cascada. El primer error reportado es el que indica la causa.

---

## Navegación

- Anterior: [← Análisis Léxico](02_lexico.md)
- Siguiente: [Testing →](08_testing.md)

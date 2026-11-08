# 8. Testing

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

## 8.1 Estrategia del Test-Plan

Las pruebas validan por separado las dos fases implementadas:

| Fase       | Clase de prueba   | Qué verifica                                                                         |
| ---------- | ----------------- | ------------------------------------------------------------------------------------ |
| Léxica     | `LexerTest.java`  | La secuencia exacta de tokens que genera una entrada, o que produzca un error léxico |
| Sintáctica | `ParserTest.java` | Que un programa completo se acepte sin errores, o que produzca un error sintáctico   |

Criterios:

- **Casos válidos:** deben analizarse con **cero errores**.
- **Casos inválidos:** deben producir al menos un error, y **el primero debe ser de la fase que se prueba** (léxico en `LexerTest`, sintáctico en `ParserTest`). Así una prueba de sintaxis no pasa "por accidente" debido a un error del lexer.
- Cada caso inválido contiene **un solo error intencional**, para identificar claramente la causa.
- Los casos de sintaxis son archivos `.bd` en `src/test/resources/syntax/valid` e `invalid`. Cada archivo inicia con un comentario que describe qué valida.

## 8.2 Ejecución

```bash
mvn test
```

Resultado esperado:

```
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

<!-- Agregar aquí la captura de la ventana de pruebas de IntelliJ con los 26 casos en verde -->

## 8.3 Casos de prueba léxicos

### Válidos

| ID  | Entrada                                                                  | Tokens esperados                                                         | Resultado |
| --- | ------------------------------------------------------------------------ | ------------------------------------------------------------------------ | --------- |
| L01 | `program main end void var print int float while do if else read return` | `PROGRAM MAIN END VOID VAR PRINT INT FLOAT WHILE DO IF ELSE READ RETURN` | ✅        |
| L02 | `_X_ _Contador_ _A1_b_ _A__`                                             | `ID ID ID ID`                                                            | ✅        |
| L03 | `0 42 3.14 10.0`                                                         | `CTE_INT CTE_INT CTE_FLOAT CTE_FLOAT`                                    | ✅        |
| L04 | `= == != < > + - * /`                                                    | `ASSIGN EQ NEQ LT GT PLUS MINUS MULT DIV`                                | ✅        |
| L05 | `; , : { } ( ) [ ]`                                                      | `SEMI COMMA COLON LBRACE RBRACE LPAREN RPAREN LBRACKET RBRACKET`         | ✅        |
| L06 | `"hola" "" "dice \"hola\""`                                              | `CTE_STRING CTE_STRING CTE_STRING`                                       | ✅        |
| L07 | `_X_=_Y_+1; // comentario` + salto de línea + `_Y_`                      | `ID ASSIGN ID PLUS CTE_INT SEMI ID`                                      | ✅        |

- **L04** comprueba la regla de coincidencia más larga: `==` y `!=` no se separan.
- **L07** comprueba que no se requieren espacios entre tokens y que los comentarios se descartan.

### Inválidos

| ID  | Entrada | Motivo                                   | Error obtenido                        | Resultado |
| --- | ------- | ---------------------------------------- | ------------------------------------- | --------- |
| L08 | `_x_`   | Minúscula después del `_` inicial        | `token recognition error at: '_x'`    | ✅        |
| L09 | `_X`    | Falta el `_` final                       | `token recognition error at: '_X'`    | ✅        |
| L10 | `_9A_`  | Dígito después del `_` inicial           | `token recognition error at: '_9'`    | ✅        |
| L11 | `3.`    | Flotante sin parte decimal               | `token recognition error at: '.'`     | ✅        |
| L12 | `"hola` | Letrero sin cerrar                       | `token recognition error at: '"hola'` | ✅        |
| L13 | `@ # !` | Caracteres que no pertenecen al lenguaje | `token recognition error at: '@'`     | ✅        |

## 8.4 Casos de prueba sintácticos

### Válidos

| ID  | Archivo                         | Qué valida                                                                                                                                        | Resultado |
| --- | ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- | --------- |
| S01 | `S01_programa_minimo.bd`        | Programa mínimo: sin variables, sin funciones y `main` vacío                                                                                      | ✅        |
| S02 | `S02_variables_y_funciones.bd`  | Variables globales de varios tipos; función `void` sin parámetros; función `int` con parámetros, variables locales y `return` con y sin expresión | ✅        |
| S03 | `S03_expresiones.bd`            | Precedencia, paréntesis, signo unario en id, constante, paréntesis y llamada; llamadas dentro de expresiones                                      | ✅        |
| S04 | `S04_condicion.bd`              | `if` sin `else`, con `else` y anidado; operadores `>`, `<`, `==`, `!=`                                                                            | ✅        |
| S05 | `S05_ciclo_y_entrada_salida.bd` | `while`, `read`, `print` con letreros y expresiones, bloque `[ ]` (incluido uno vacío)                                                            | ✅        |
| S06 | `S06_programa_integral.bd`      | Programa completo (factorial iterativo) que combina todas las construcciones y comentarios                                                        | ✅        |

Programa S06:

```
// S06: Programa integral (factorial iterativo) que combina todo:
//      variables, funciones, ciclo, condición, E/S y comentarios
program _Factorial_ ;
var _Num_, _Res_ : int;

int _Fact_(_N_ : int) [
    var _Acc_ : int;
    {
        _Acc_ = 1;
        while (_N_ > 1) do {
            _Acc_ = _Acc_ * _N_;
            _N_ = _N_ - 1;
        };
        return _Acc_;
    }
];

void _Muestra_(_V_ : int) [
    {
        if (_V_ == 0) {
            print("resultado cero");
        } else {
            print("resultado:", _V_);
        };
    }
];

main {
    read(_Num_);
    _Res_ = _Fact_(_Num_);   // llamada dentro de una asignación
    _Muestra_(_Res_);        // llamada como estatuto
}
end
```

<!-- Agregar aquí la captura del árbol de parseo de S06 (ANTLR Preview) -->

### Inválidos

| ID  | Archivo                             | Entrada relevante                      | Motivo                                                          | Primer error obtenido                | Resultado |
| --- | ----------------------------------- | -------------------------------------- | --------------------------------------------------------------- | ------------------------------------ | --------- |
| S07 | `S07_falta_punto_y_coma.bd`         | `_X_ = 5`                              | Falta `;` en una asignación                                     | línea 6:4 `missing ';' at 'print'`   | ✅        |
| S08 | `S08_ciclo_sin_body.bd`             | `while (_X_ != 0) do _X_ = _X_ - 1; ;` | Sintaxis del diagrama original; el ciclo ahora exige `{ }` (M2) | línea 6:24 `missing '{' at '_X_'`    | ✅        |
| S09 | `S09_condicion_sin_punto_y_coma.bd` | `if (...) { ... } else { ... }`        | Falta el `;` que cierra la condición                            | línea 6:4 `missing ';' at 'print'`   | ✅        |
| S10 | `S10_parentesis_sin_cerrar.bd`      | `_X_ = (_A_ + 2 * 3;`                  | Paréntesis sin cerrar                                           | línea 5:22 `missing ')' at ';'`      | ✅        |
| S11 | `S11_relacionales_encadenados.bd`   | `if (_A_ < _B_ < _C_)`                 | Solo se permite un operador relacional por expresión            | línea 6:18 `mismatched input '<'`    | ✅        |
| S12 | `S12_orden_de_declaraciones.bd`     | `void _F_() ...` antes de `var`        | Las variables globales deben declararse antes de las funciones  | línea 5:0 `extraneous input 'var'`   | ✅        |
| S13 | `S13_falta_end.bd`                  | Programa sin `end`                     | Falta la palabra reservada final                                | línea 6:0 `missing 'end' at '<EOF>'` | ✅        |

- **S08** es la evidencia de la modificación M2 ([§3.3](03_sintaxis.md#33-modificaciones-a-la-especificación)): la sintaxis anterior ya no se acepta.
- **S08, S11 y S12** generan errores adicionales en cascada por la recuperación de errores de ANTLR; se reporta solo el primero, que indica la causa real.

## 8.5 Cobertura

| Construcción                         | Casos válidos | Casos inválidos |
| ------------------------------------ | ------------- | --------------- |
| Estructura del programa              | S01, S06      | S12, S13        |
| Declaración de variables             | S02, S03, S06 | S12             |
| Funciones y parámetros               | S02, S03, S06 | —               |
| Asignación                           | S02–S06       | S07             |
| Condición                            | S04, S06      | S09             |
| Ciclo                                | S05, S06      | S08             |
| `print` / `read`                     | S02, S05, S06 | —               |
| `return` (con y sin valor)           | S02, S03, S06 | —               |
| Llamada como estatuto y en expresión | S02, S03, S06 | —               |
| Expresiones y precedencia            | S03           | S10, S11        |
| Bloque `[ ]`                         | S05           | —               |
| Tokens y expresiones regulares       | L01–L07       | L08–L13         |

---

## Navegación

- Anterior: [← Análisis Sintáctico](03_sintaxis.md)
- Siguiente: [Herramientas de IA →](09_herramientas_ia.md)

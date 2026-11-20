# Herramientas de Generación de Scanners y Parsers

## Índice

1. [Overview](01_overview.md)
2. **Herramientas** (este capítulo)
3. [Análisis léxico](03_lexico.md)
4. [Análisis sintáctico](04_sintaxis.md)
5. [Análisis semántico](05_semantica.md)
6. [Código intermedio](06_codigo_intermedio.md)
7. [Memoria virtual](07_memoria_virtual.md)
8. [Testing](08_testing.md)
9. [Herramientas de IA](09_herramientas_ia.md)
10. [Guía de uso](10_guia_de_uso.md)

---

## Criterios de selección

Se buscó la herramienta que:

1. **Conecte mejor con Java**, el lenguaje de desarrollo del compilador.
2. **Tenga buena documentación** y comunidad activa.
3. Se adapte a la gramática ya diseñada (escrita en estilo LL, sin recursión izquierda).
4. Facilite las siguientes etapas: análisis semántico y generación de código intermedio.

## Herramientas analizadas

| Herramienta      | ¿Genera Java?                                | Tipo de parser       | Archivos de entrada | Documentación                                                                            |
| ---------------- | -------------------------------------------- | -------------------- | ------------------- | ---------------------------------------------------------------------------------------- |
| **Flex & Bison** | Flex no; Bison tiene un modo Java secundario | LALR(1), _bottom-up_ | `.l` + `.y`         | Muy buena, pero orientada a C/C++                                                        |
| **ANTLR4**       | Sí, de forma nativa (escrito en Java)        | ALL(\*), _top-down_  | Un solo `.g4`       | Excelente: libro oficial, documentación en GitHub y repositorio de gramáticas de ejemplo |
| **JFlex + CUP**  | Sí                                           | LALR(1), _bottom-up_ | `.flex` + `.cup`    | JFlex buena; CUP antigua y con poco mantenimiento                                        |
| **JavaCC**       | Sí                                           | LL(k), _top-down_    | Un solo `.jj`       | Regular: dispersa y algo desactualizada                                                  |

### Hallazgos principales

**Flex & Bison**

- Son el estándar clásico para C/C++ y tienen excelente documentación.
- Flex no genera Java, así que para Java habría que combinar Bison (modo Java) con otro scanner como JFlex.
- Al ser LALR, reportan conflictos _shift/reduce_ y _reduce/reduce_ en la tabla de parseo, lo cual ayuda a detectar ambigüedades.

**ANTLR4**

- Está escrito en Java y genera clases Java listas para usar (`Lexer`, `Parser`, `Listener`, `Visitor`).
- Lexer y parser se definen en **un solo archivo** con notación EBNF (`?`, `*`, `+`).
- Su algoritmo ALL(\*) resuelve automáticamente reglas que empiezan con el mismo token, sin indicar el _lookahead_ a mano.
- Genera _listeners_ y _visitors_ con métodos `enter…`/`exit…`/`visit…` por regla, útiles para colocar acciones semánticas sin tocar la gramática.
- El plugin de IntelliJ muestra el árbol de parseo en vivo (ANTLR Preview).
- Desventaja: ante una ambigüedad elige en silencio la primera alternativa en lugar de reportar un conflicto.

**JFlex + CUP**

- Combinación tradicional para Java, equivalente a Flex & Bison.
- Requiere mantener y conectar dos herramientas y dos archivos.
- CUP tiene documentación escasa y poco mantenimiento reciente.

**JavaCC**

- Genera un parser de descenso recursivo en Java, fácil de seguir.
- Cuando varias alternativas empiezan con el mismo token (en BabyDuck: asignación, llamada a función e identificador en un factor empiezan con `id`) obliga a escribir `LOOKAHEAD(k)` manualmente.
- Su documentación es menos completa que la de ANTLR.

### Otras opciones identificadas para Java

| Herramienta | Comentario                                                                 |
| ----------- | -------------------------------------------------------------------------- |
| Coco/R      | LL(1), de origen académico y sencillo; comunidad pequeña                   |
| SableCC     | LALR; genera AST y _visitors_ automáticamente, pero con poco mantenimiento |
| Beaver      | LALR; alternativa más moderna a CUP, con poca comunidad                    |
| CongoCC     | Sucesor moderno de JavaCC                                                  |

## Top-Down (LL) vs Bottom-Up (LR)

|                      | LL (_top-down_): ANTLR, JavaCC                      | LR (_bottom-up_): Bison, CUP                         |
| -------------------- | --------------------------------------------------- | ---------------------------------------------------- |
| Gramáticas aceptadas | Menos (el LL clásico no acepta recursión izquierda) | Más, incluida la recursión izquierda                 |
| Comprensión          | Cada regla equivale a una función                   | Más abstracto: estados, pila y reducciones           |
| Depuración           | Sencilla, regla por regla                           | Requiere interpretar conflictos de la tabla          |
| Acciones semánticas  | En cualquier punto de la regla                      | Solo al reducir; requiere no terminales "marcadores" |

## Herramienta seleccionada: ANTLR4

Se eligió **ANTLR 4.13.2** por las siguientes razones:

1. **Conexión con Java:** es nativa y se integra a Maven con un plugin que genera el código en cada compilación.
2. **Documentación:** la más completa de las herramientas analizadas.
3. **Compatibilidad con la gramática:** la CFG de BabyDuck ya estaba escrita en estilo LL (sin recursión izquierda), por lo que se transcribió directamente.
4. **Facilidad de aprendizaje y depuración:** el parser descendente sigue la estructura de la gramática y el árbol puede visualizarse en el IDE.
5. **Puntos neurálgicos fuera de la gramática:** el _visitor_ generado permite colocar las validaciones semánticas y la generación de cuádruplos sin modificar el `.g4`.

---

## Navegación

- Anterior: [← Overview](01_overview.md)
- Siguiente: [Análisis léxico →](03_lexico.md)

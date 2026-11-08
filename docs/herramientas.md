# 1. Herramientas de Generación de Scanners y Parsers

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

## 1.1 Criterios de selección

Se buscó la herramienta que:

1. **Conecte mejor con Java**, el lenguaje de desarrollo del compilador.
2. **Tenga buena documentación** y comunidad activa.
3. Se adapte a la gramática ya diseñada (escrita en estilo LL, sin recursión izquierda).
4. Facilite las siguientes etapas: análisis semántico y generación de código intermedio.

## 1.2 Herramientas analizadas

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
- Genera _listeners_ y _visitors_ con métodos `enter…`/`exit…`/`visit…` por regla, útiles para colocar acciones semánticas en las siguientes entregas.
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

## 1.3 Top-Down (LL) vs Bottom-Up (LR)

|                      | LL (_top-down_): ANTLR, JavaCC                      | LR (_bottom-up_): Bison, CUP                         |
| -------------------- | --------------------------------------------------- | ---------------------------------------------------- |
| Gramáticas aceptadas | Menos (el LL clásico no acepta recursión izquierda) | Más, incluida la recursión izquierda                 |
| Comprensión          | Cada regla equivale a una función                   | Más abstracto: estados, pila y reducciones           |
| Depuración           | Sencilla, regla por regla                           | Requiere interpretar conflictos de la tabla          |
| Acciones semánticas  | En cualquier punto de la regla                      | Solo al reducir; requiere no terminales "marcadores" |

## 1.4 Herramienta seleccionada: ANTLR4

Se eligió **ANTLR 4.13.2** por las siguientes razones:

1. **Conexión con Java:** es nativa y se integra a Maven con un plugin que genera el código en cada compilación.
2. **Documentación:** la más completa de las herramientas analizadas.
3. **Compatibilidad con la gramática:** la CFG de BabyDuck ya estaba escrita en estilo LL (sin recursión izquierda), por lo que se transcribió directamente.
4. **Facilidad de aprendizaje y depuración:** el parser descendente sigue la estructura de la gramática y el árbol puede visualizarse en el IDE.
5. **Siguientes entregas:** los _listeners_ y _visitors_ generados permiten insertar los puntos neurálgicos semánticos sin modificar la gramática.

---

## Navegación

- Anterior: [← Overview](00_overview.md)
- Siguiente: [Análisis Léxico →](02_lexico.md)

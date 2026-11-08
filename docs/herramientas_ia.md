# 9. Herramientas de IA

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

> **Nota para la autora:** este registro es un borrador. Revísalo y ajústalo para que describa con precisión cómo usaste la herramienta.

## 9.1 Entrega 2: Scanner, Parser y Test-Plan

**Herramienta:** Claude (Anthropic)

| Actividad                   | Uso de la IA                                                                                      | Trabajo propio / verificación                                                  |
| --------------------------- | ------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| Comparación de herramientas | Resumen de características de Flex & Bison, ANTLR4, JFlex + CUP y JavaCC; explicación de LL vs LR | Investigación previa propia; selección final de ANTLR4                         |
| Configuración del proyecto  | Guía para el `pom.xml` y la estructura de carpetas de Maven                                       | Creación del proyecto en IntelliJ y ajuste de la versión de Java               |
| Revisión de la gramática    | Detección del `;` en `<F_call>`, explicación de BNF vs EBNF y propuesta de modificaciones         | Decisión de qué modificaciones aplicar y contraste con los diagramas oficiales |
| Gramática `.g4`             | Transcripción de la CFG a EBNF por bloques                                                        | Prueba de cada bloque en el ANTLR Preview                                      |
| Código de soporte           | Generación de `Main`, `BabyDuckAnalyzer`, `ErrorCollector` y las clases de prueba                 | Ejecución de las pruebas y revisión de resultados                              |
| Test-Plan                   | Propuesta de los casos léxicos y sintácticos                                                      | Selección de los casos y ejecución                                             |
| Documentación               | Redacción del borrador de esta documentación                                                      | Revisión y edición final                                                       |

## 9.2 Semántica de variables: cubo semántico, directorio de funciones y tablas de variables

**Herramienta:** Claude (Anthropic), modelo Opus 5.5

La forma de trabajo fue por bloques que se podían probar antes de seguir. Para cada decisión, la IA presentó alternativas con sus ventajas y desventajas y una recomendación; la decisión final fue propia.

| Actividad                        | Uso de la IA                                                                                                                                                                                                            | Trabajo propio / verificación                                                                      |
| -------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| Reglas semánticas del lenguaje   | Comparación de alternativas: tipo `bool` frente a enteros 0/1, resultado de la división, conversiones entre `int` y `float`                                                                                             | Decisión de cada regla (por ejemplo, que `/` siempre dé `float` y que no se permita `int ← float`) |
| Cubo semántico                   | Comparación de estructuras (`EnumMap`, arreglo 3D, `HashMap` con llave compuesta, `switch`); generación de `Type`, `Op`, `SemanticCube` y `SemanticCubeTest`                                                            | Elección de `EnumMap`; integración en el proyecto y ejecución de las pruebas                       |
| Directorio y tablas de variables | Comparación de `HashMap`, `LinkedHashMap` y `LinkedList`; opciones para las globales, los parámetros y los duplicados; generación de `VariableInfo`, `VariableTable`, `FunctionInfo`, `FunctionDirectory` y sus pruebas | Elección de cada estructura; revisión                                                              |
| Alcance y nombres                | Opciones sobre sombreado, espacio de nombres compartido y una o dos pasadas                                                                                                                                             | Decisión: sin sombreado, un solo espacio de nombres (incluido el programa) y una sola pasada       |
| Puntos neurálgicos               | Comparación entre Listener y Visitor; generación de `SemanticAnalyzer`                                                                                                                                                  | Elección del Visitor; revisión del código                                                          |
| Validaciones de uso              | Reglas de llamadas, `return`, caminos sin `return` y código inalcanzable lenguajes                                                                                                                                      | Decisión de las reglas (ej. revisar todos los caminos)                                             |
| Pruebas                          | Propuesta de los casos `.bd` válidos e inválidos y de la clase `SemanticTest`                                                                                                                                           | Ejecución de las pruebas                                                                           |

---

## Navegación

- Anterior: [← Testing](08_testing.md)
- Siguiente: [Guía de uso →](10_guia_de_uso.md)

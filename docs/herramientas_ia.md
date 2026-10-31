# 9. Herramientas de IA

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

> **Nota para la autora:** este registro es un borrador. Revísalo y ajústalo para que describa con precisión cómo usaste la herramienta.

## 9.1 Entrega 2: Scanner, Parser y Test-Plan

**Herramienta:** Claude (Anthropic)

| Actividad | Uso de la IA | Trabajo propio / verificación |
|---|---|---|
| Comparación de herramientas | Resumen de características de Flex & Bison, ANTLR4, JFlex + CUP y JavaCC; explicación de LL vs LR | Investigación previa propia; selección final de ANTLR4 |
| Configuración del proyecto | Guía para el `pom.xml` y la estructura de carpetas de Maven | Creación del proyecto en IntelliJ y ajuste de la versión de Java |
| Revisión de la gramática | Detección del `;` en `<F_call>`, explicación de BNF vs EBNF y propuesta de modificaciones | Decisión de qué modificaciones aplicar y contraste con los diagramas oficiales |
| Gramática `.g4` | Transcripción de la CFG a EBNF por bloques | Prueba de cada bloque en el ANTLR Preview |
| Código de soporte | Generación de `Main`, `BabyDuckAnalyzer`, `ErrorCollector` y las clases de prueba | Ejecución de las pruebas y revisión de resultados |
| Test-Plan | Propuesta de los casos léxicos y sintácticos | Selección de los casos y ejecución |
| Documentación | Redacción del borrador de esta documentación | Revisión y edición final |

---

## Navegación

- Anterior: [← Testing](08_testing.md)

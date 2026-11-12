# 9. Herramientas de IA

## Índice

0. [Overview](overview.md)
1. [Herramientas](herramientas.md)
2. [Análisis Léxico](lexico.md)
3. [Análisis Sintáctico](sintaxis.md)
4. [Análisis Semántico](semantica.md)
5. [Código Intermedio](codigo_intermedio.md)
6. Memoria Virtual _(pendiente)_
7. Máquina Virtual _(pendiente)_
8. [Testing](08_testing.md)
9. [Herramientas de IA](herramientas_ia.md)
10. [Guía de uso](guia_de_uso.md)

---

> **Nota para la autora:** este registro es un borrador. Revísalo y ajústalo para que describa con precisión cómo usaste la herramienta.

## 9.1 Entrega 1: Scanner, Parser y Test-Plan

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

## 9.2 Entrega 2: Semántica de variables (cubo semántico, directorio de funciones y tablas de variables)

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

## 9.3 Entrega 3: Código de expresiones y estatutos (pilas, fila de cuádruplos y traducción)

**Herramienta:** Claude (Anthropic), modelo Opus 5.5

**Forma de trabajo.** Se compartieron los documentos de contexto de las etapas anteriores, el código actual del compilador, los diagramas oficiales, las instrucciones de la entrega, el PDF de la entrega 2 y el material de referencia del curso, con la petición: _"¿Qué se va a hacer para esta entrega? ¿Cuál será nuestro paso a paso?"_. La IA propuso un plan por bloques y, antes de programar, las decisiones de diseño con sus ventajas, desventajas y una recomendación. Cada decisión se tomó de forma propia y, en varios casos, se aceptó con condiciones adicionales que después se implementaron y probaron.

| Actividad                               | Uso de la IA                                                                                                                                                                  | Trabajo propio / verificación                                                                                                                               |
| --------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Plan de la entrega                      | Lectura de las instrucciones y propuesta de un plan por bloques que se pueden probar                                                                                          | Revisión del plan; política de documentación propia (sección de cambios respecto a la entrega 2, documentación al cerrar cada bloque)                       |
| Arquitectura                            | Opciones: generar en una clase aparte llamada desde el Visitor o en un segundo recorrido; pilas como única fuente de tipos o en paralelo al valor de retorno                  | Elección de la clase aparte y de las pilas como única fuente, con condiciones: mismos mensajes de error, sin errores en cadena, pila de tipos encapsulada   |
| Representación de los cuádruplos        | Opciones para los operandos (nombres, direcciones, dos representaciones) y para los operadores (enum existente, enum nuevo con códigos, enteros)                              | Elección de operandos como objetos con clase y métodos de fábrica, tipo interno para letreros y enum con códigos fijos; preparación para la memoria virtual |
| Algoritmo de expresiones                | Explicación del fondo falso con el Visitor; detección de que también se necesita en los argumentos de una llamada                                                             | Decisión de seguir el algoritmo de clase (revisar el tope después de cada operando) y de los casos de prueba de paréntesis y asociatividad                  |
| Signo y constantes                      | Comparación entre `UMINUS`, `0 - x` y constante negativa                                                                                                                      | Decisión: constante negativa cuando el signo va pegado, `UMINUS` en los demás casos; revisión de constantes que no caben, con signo                         |
| Estatutos y lo que aún no genera código | Propuestas para `print`, `read`, fin del programa y notas en la fila                                                                                                          | Decisión de `PRINTLN` por estatuto sin espacio automático, `END` y la nota de `Goto` a main                                                                 |
| Errores                                 | Propuesta del operando de error y del reparto: el generador detecta y el recorrido reporta                                                                                    | Aceptación con la condición de conservar los mensajes                                                                                                       |
| Código                                  | Generación del paquete `intermediate` (operandos, cuádruplos, fila, generador) y de los cambios a `Type`, `SemanticAnalyzer`, `BabyDuckAnalyzer` y `Main`                     | Integración en el proyecto; ejecución de `mvn test`                                                                                                         |
| Verificación                            | Comparación, carácter por carácter, de la lista de errores (mensaje, línea y columna) antes y después del cambio, sobre las pruebas semánticas y dos programas con 49 errores | Revisión de los resultados                                                                                                                                  |
| Pruebas                                 | `QuadrupleGeneratorTest` y propuesta de los casos de cuádruplos                                                                                                               | Selección de los casos que debía cubrir cada regla                                                                                                          |
| Documentación                           | Borrador del capítulo de código intermedio y actualización de semántica, guía de uso y este registro                                                                          | Revisión y edición final                                                                                                                                    |

---

## Navegación

- Anterior: [← Testing](08_testing.md)
- Siguiente: [Guía de uso →](10_guia_de_uso.md)

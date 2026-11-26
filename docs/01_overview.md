# Compilador BabyDuck

**Autor(a):** Maria Quetzalli Ramirez Martinez · **Matrícula:** A01753959
**Materia:** Desarrollo de aplicaciones avanzadas de ciencias computacionales · **Profesor(a):** [Nombre]

## Índice

1. **Overview** (este capítulo)
2. [Herramientas](02_herramientas.md)
3. [Análisis léxico](03_lexico.md)
4. [Análisis sintáctico](04_sintaxis.md)
5. [Análisis semántico](05_semantica.md)
6. [Código intermedio](06_codigo_intermedio.md)
7. [Memoria virtual](07_memoria_virtual.md)
8. [Máquina virtual](08_maquina_virtual.md)
9. [Testing](09_testing.md)
10. [Herramientas de IA](10_herramientas_ia.md)
11. [Guía de uso](11_guia_de_uso.md)

---

## Descripción

Este proyecto implementa un compilador para **BabyDuck**, un mini-lenguaje imperativo con variables enteras y flotantes, funciones con parámetros, condiciones, ciclos y entrada/salida.

El compilador se desarrolla en **Java** y usa **ANTLR4** para generar automáticamente el analizador léxico (Scanner) y el analizador sintáctico (Parser) a partir de un archivo de gramática. Sobre el árbol de parseo, un solo recorrido valida la semántica, asigna las direcciones virtuales y genera el código intermedio (cuádruplos). Si el programa no tiene errores, la máquina virtual ejecuta esos cuádruplos en la misma corrida.

## Tecnologías

| Elemento                         | Versión / Herramienta           |
| -------------------------------- | ------------------------------- |
| Lenguaje de desarrollo           | Java 25 (LTS)                   |
| Generador de Scanner y Parser    | ANTLR 4.13.2                    |
| Gestor de proyecto y compilación | Maven                           |
| Pruebas automáticas              | JUnit 5                         |
| IDE                              | IntelliJ IDEA + plugin ANTLR v4 |

## Flujo del compilador

```mermaid
flowchart LR
    A[Programa .bd] --> B[BabyDuckLexer<br/>Scanner]
    B -->|tokens| C[BabyDuckParser<br/>Parser]
    C -->|árbol de parseo| D[SemanticAnalyzer<br/>semántica + direcciones + cuádruplos]
    D --> F[Directorio de funciones,<br/>fila de cuádruplos y<br/>tabla de constantes]
    F --> G[VirtualMachine<br/>ejecución]
    B -.->|errores léxicos| E[ErrorCollector]
    C -.->|errores sintácticos| E
    D -.->|errores semánticos| E
```

La semántica solo corre si no hubo errores léxicos ni sintácticos; si encuentra errores, la fila
de cuádruplos se descarta y no se ejecuta nada. La máquina virtual solo recibe códigos de
operación, direcciones, la tabla de constantes y los tamaños e inicios del directorio.

## Estructura del proyecto

```
compilador/
├── pom.xml                                  Configuración de Maven y ANTLR
├── docs/                                    Esta documentación
└── src/
    ├── main/
    │   ├── antlr4/com/babyduck/BabyDuck.g4  Gramática: tokens + reglas
    │   └── java/com/babyduck/
    │       ├── Main.java                    Punto de entrada
    │       ├── BabyDuckAnalyzer.java        Ejecuta lexer + parser y, si no hay errores, la semántica
    │       ├── ErrorCollector.java          Recolecta errores léxicos, sintácticos y semánticos
    │       ├── SemanticAnalyzer.java        Recorrido del árbol: puntos neurálgicos
    │       ├── semantic/                    Tipos, cubo semántico, tablas y directorio de funciones
    │       ├── intermediate/                Cuádruplos, pilas, fila y memoria virtual
    │       └── vm/                          Máquina virtual y memoria de ejecución
    └── test/
        ├── java/com/babyduck/               Pruebas JUnit por fase (y semantic/, intermediate/, vm/)
        └── resources/
            ├── sintaxis/                    Programas .bd de sintaxis (valid/ e invalid/)
            ├── semantic/                    Programas .bd de semántica
            ├── quads/                       Programas .bd con su fila de cuádruplos esperada
            └── vm/                          Programas .bd con su salida o error de ejecución esperado
```

Las clases `BabyDuckLexer`, `BabyDuckParser`, `BabyDuckListener` y `BabyDuckVisitor` **no se escriben a mano**: el plugin de Maven las genera desde `BabyDuck.g4` en cada compilación, dentro de `target/generated-sources/antlr4/`.

## Compilación y ejecución

```bash
mvn compile                 # genera el Scanner/Parser y compila
mvn test                    # ejecuta el Test-Plan (JUnit)
```

Para analizar un programa (en IntelliJ, como _Program arguments_ de `Main`):

```
<archivo.bd> [--tokens]
```

- Sin `--tokens`: si el programa es válido imprime el árbol de parseo, el directorio de funciones, la fila de cuádruplos y la tabla de constantes, y después lo ejecuta (sección `=== Ejecución ===`, donde `read` lee de la consola); si no, lista los errores. Un error de ejecución detiene el programa con su mensaje.
- Con `--tokens`: además imprime la lista de tokens reconocidos.

## Avance por entrega

| Entrega | Contenido                                                         | Estado |
| ------- | ----------------------------------------------------------------- | ------ |
| 0       | Expresiones regulares, tokens y gramática libre de contexto (CFG) | ✓      |
| 1       | Selección de herramienta, Scanner, Parser y Test-Plan             | ✓      |
| 2       | Cubo semántico, directorio de funciones y tablas de variables     | ✓      |
| 3       | Cuádruplos de expresiones y estatutos lineales                    | ✓      |
| 4       | Saltos, funciones y direcciones virtuales                         | ✓      |
| 5       | Máquina virtual y memoria de ejecución                            | ✓      |

---

## Navegación

- Siguiente: [Herramientas →](02_herramientas.md)

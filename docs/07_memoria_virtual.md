# Memoria virtual

## Índice

1. [Overview](01_overview.md)
2. [Herramientas](02_herramientas.md)
3. [Análisis léxico](03_lexico.md)
4. [Análisis sintáctico](04_sintaxis.md)
5. [Análisis semántico](05_semantica.md)
6. [Código intermedio](06_codigo_intermedio.md)
7. **Memoria virtual** (este capítulo)
8. [Testing](08_testing.md)
9. [Herramientas de IA](09_herramientas_ia.md)
10. [Guía de uso](10_guia_de_uso.md)

---

## Qué hace

Cada variable, temporal y constante recibe una **dirección virtual** en el momento en que se
crea, y los cuádruplos guardan esas direcciones (además del nombre, que solo sirve para leer la
salida). La dirección dice en qué segmento y de qué tipo es el valor; la máquina virtual la usará
para encontrarlo en su memoria.

| Clase | Paquete | Qué hace |
|---|---|---|
| `Segment` | `intermediate` | Segmentos: global, local, temporal y constante |
| `VirtualMemory` | `intermediate` | Base y contador por segmento y tipo; da la siguiente dirección libre |
| `ConstantTable` | `intermediate` | Una dirección por cada valor constante distinto (números y letreros) |
| `MemorySize` | `semantic` | Cuántas direcciones de cada tipo usa un segmento (tamaño de una función) |

Las direcciones solo se asignan en dos lugares: al declarar una variable (`SemanticAnalyzer`) y en
la fábrica de operandos de `QuadrupleGenerator` (`variable`, `constant`, `string`, `newTemp`).

## Distribución de direcciones

1000 direcciones por segmento y tipo:

| Segmento | `int` | `float` | `bool` | letrero |
|---|---|---|---|---|
| Global | 1000–1999 | 2000–2999 | — | — |
| Local | 3000–3999 | 4000–4999 | — | — |
| Temporal | 5000–5999 | 6000–6999 | 7000–7999 | — |
| Constante | 8000–8999 | 9000–9999 | — | 10000–10999 |

- Del número salen el segmento y el tipo, y la casilla dentro del segmento es `dirección − base`
  (por ejemplo, 4002 es la tercera local `float`).
- No hay variables `bool` ni variables de letrero: `bool` solo existe como resultado de un
  relacional (temporal) y los letreros solo como constantes.

## Cómo se asigna

| Qué | Segmento | Cuándo |
|---|---|---|
| Variable global | Global | Al declararla |
| Dirección de regreso de una función con valor | Global, de su tipo | Al registrar la función; ahí deja su valor el `RETURN` |
| Parámetro | Local | Al declararlo, en orden y antes que las variables locales |
| Variable local | Local | Al declararla |
| Temporal | Temporal | Al crearlo; no se reutilizan |
| Constante o letrero | Constante | La primera vez que aparece su valor |

- **Locales y temporales se reinician** al empezar cada función y `main`: cada función usa sus
  direcciones desde la base, porque cada llamada tendrá su propia memoria. Globales y constantes
  no se reinician. El nombre de los temporales (`t1`, `t2`…) sigue un solo contador en todo el
  programa, para que la fila se lea sin ambigüedad.
- **Constantes sin repetir:** la tabla guarda `valor → dirección` por tipo, con el valor ya
  convertido; `2.5` y `2.50` comparten dirección, y un letrero repetido también.
- Una declaración con error no gasta direcciones (la dirección se pide solo si es válida).

## Tamaño de cada función

Al terminar de compilar una función se guarda en su `FunctionInfo` cuántas locales (incluye
parámetros) y cuántos temporales de cada tipo usa, su cuádruplo de inicio y su dirección de
regreso; después se libera su tabla local. Al final del programa se guardan también el tamaño del
segmento global y los temporales de `main`. Con esto, la máquina virtual puede crear en cada
llamada solo la memoria que la función necesita. Directorio de `Q08_direcciones.bd`:

```
Programa: _Q08_
Globales: [_G_:int@1000, _H_:float@2000]
Segmento global: int 1, float 1, bool 0
Temporales de main: int 1, float 1, bool 0
_F_ -> void
  inicio:      1
  params:      [_P_:int@3000, _Q_:float@4000]
  locales:     int 2, float 1, bool 0
  temporales:  int 1, float 1, bool 1
```

## Rango lleno

Si un segmento se queda sin direcciones para un tipo, se reporta un error semántico una sola vez
por rango, por ejemplo `se excedió el límite de 1000 direcciones para temporales de tipo int`. No
se lanza excepción: es un error del programa, no del compilador.

## Ejemplo

```
void _F_(_P_ : int, _Q_ : float) [ var _L_ : int; { _L_ = _P_ + 2; ... } ];
main { _G_ = _G_ + 2; _H_ = _H_ / 2.50; ... }
```

```
#   op   izq        der      res
1   +    3000(_P_)  8000(2)  5000(t1)
2   =    5000(t1)   _        3001(_L_)
...
10  +    1000(_G_)  8000(2)  5000(t4)
12  /    2000(_H_)  9000(2.50)  6000(t5)
```

`_P_` es la primera local `int` (3000) y `_L_` la segunda (3001); la constante 2 tiene una sola
dirección (8000) en la función y en `main`; en `main` los temporales vuelven a empezar en 5000 y
6000 aunque su nombre siga (`t4`, `t5`). La fila completa y sus direcciones esperadas están en
`src/test/resources/quads/valid/Q08_direcciones.bd`.

---

## Navegación

- Anterior: [← Código intermedio](06_codigo_intermedio.md)
- Siguiente: [Testing →](08_testing.md)

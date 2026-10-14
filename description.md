# Descripción de los cambios

Este documento describe los tres trabajos hechos sobre el proyecto: la
decoración manual con `#`, el nuevo comportamiento de la flecha del minimapa y
la separación de responsabilidades de `Core.GameLoop`.

---

## 1. Decoración manual con `#`

Antes la decoración se repartía sola según la forma del mapa (esquinas,
callejones, puertas). Eso hacía imposible pedir una pieza en un sitio concreto:
una planta podía acabar donde el jugador no quería. Ahora la decide quien
escribe el nivel.

### Cómo se usa

- En la rejilla de un nivel, el símbolo `#` marca una celda **de suelo** donde
  se quiere una pieza del catálogo.
- El `#` **se pisa**: es suelo transitable, no un obstáculo. No cambia la
  colisión ni el trazado del nivel.
- La pieza concreta y su giro los elige el juego, no el autor del nivel.

```text
map:
  □□□□
  □#□□   <- esta celda lleva una pieza del catálogo
  □□□□
```

> Ojo: por debajo de `map:` un `#` **ya no es un comentario**. Solo se admiten
> comentarios por encima de `map:`.

### Qué pieza sale

- La elección es **determinista**: la misma celda coloca siempre la misma pieza
  y con el mismo giro, partida tras partida y máquina tras máquina. Se calcula
  con un *hash* de las coordenadas `(fila, columna)`, no con un `Random`
  sembrado, para que recargar el nivel no cambie el resultado ni dependa del
  orden en que se recorre el mapa.
- Primero se elige el grupo (plantas, mesas o alfombra) según el reparto de
  `ConfigGlobal.PROP_DECOR_GROUP_ENDS`; después, la pieza dentro del grupo.
- El giro sale también del *hash*, en múltiplos de **90°**, para mantener la
  pieza dentro de su celda en una planta ortogonal.

### Dónde vive

| Fichero | Responsabilidad |
| --- | --- |
| `Render3D/map/MapConfig.java` | Nuevo `SymbolType.DECORATION('#', ...)`; `isWalkable()` lo devuelve `true`. |
| `Render3D/map/PropLayout.java` | Plan de decoración: recorre el mapa, detecta `#` y coloca una pieza por celda. Sin reparto automático. |
| `Render3D/map/LevelLoader.java` | Deja de saltarse las filas con `#`; ahora son símbolo de decoración. |
| `Core/ConfigGlobal.java` | `PROP_DECOR_GROUP_ENDS = { 0.55f, 0.85f, 1.01f }` (el último valor pasa de 1 para cubrir el resto del rango). |

---

## 2. Flecha del minimapa: encaje a la celda y giro a 90°

En el plano, el marcador del jugador ahora **avanza bloque a bloque** y solo
mira en cuatro direcciones. Es puramente visual: la navegación y el movimiento
en 3D no cambian, el personaje sigue girando de forma continua en primera
persona.

- **Posición:** el marcador se dibuja encajado al **centro de la celda** que
  contiene al jugador (`cellIndex`), como un mapa de RPG.
- **Orientación:** el ángulo del jugador se encaja a la dirección cardinal más
  cercana (`snapYaw90`), o sea a múltiplos de 90°.

Todo vive en `Render2D/Minimap.java`, en dos funciones puras y comprobables sin
abrir una ventana: `cellIndex(...)` y `snapYaw90(...)`. Nada fuera del dibujo de
la flecha consume estos valores; la matemática del movimiento no se toca.

---

## 3. Separación de responsabilidades de `GameLoop`

`Core.GameLoop` hacía de todo: bucle, entrada, estado, HUD, menús y dibujo 3D.
Se partió en clases con una responsabilidad cada una, sin cambiar el
comportamiento observable del juego. La entrada de siempre, `GameLoop.main(...)`,
se mantiene igual.

| Clase nueva | Responsabilidad |
| --- | --- |
| `Core/GameState.java` | Enum del estado de la partida (`TITLE`, `PLAYING`, `PAUSED`, `FROZEN`, `END`). |
| `Core/Difficulty.java` | Enum de dificultad (`FACIL`, `NORMAL`, `DIFICIL`). |
| `Core/LevelSession.java` | Niveles: lista, índice, nombre, tiempo, preset, `isFacil()` / `isDificil()` / `isLastLevel()`. |
| `Core/HudRenderer.java` | HUD de partida: llaves, tiempo y avisos (*toast*). |
| `Core/MenuScreens.java` | Pantallas de título y pausa: botones, rótulos, cuadrícula y maquetación. |
| `Core/InputHandler.java` | Lectura de teclado y ratón; despacha cada pulsación a una acción. |
| `Core/SceneRenderer.java` | Dibujo del escenario 3D: cielo, muros, suelos, decoración, items y luces. |

La comunicación es por interfaces de acciones (`InputHandler.Actions`,
`MenuScreens.Actions`), de modo que la entrada no sabe cómo se juega y los menús
no conocen el bucle. `GameLoop` queda como el cableado y el estado de partida, y
baja de unas 1.360 líneas a unas 660.

---

## Comprobaciones

Los tests del repositorio son clases con `main` (no hay JUnit) y se ejecutan de
forma explícita. Se ajustaron `PausaRapidaChecks` y `ItemsMapChecks` para leer
el código en su nueva ubicación.

```bash
./mvnw -o -q test-compile
```

Después se ejecutan las nueve comprobaciones headless:

- `DecorYTechoChecks`
- `PausaRapidaChecks`
- `Render2D.GridLayoutUIChecks`
- `Render2D.MinimapArrowChecks`
- `Render2D.MinimapItemsChecks`
- `Render2D.MinimapPaletteChecks`
- `Render3D.map.DecorGlyphChecks`
- `Render3D.map.ItemsMapChecks`
- `Render3D.map.LucesChecks`

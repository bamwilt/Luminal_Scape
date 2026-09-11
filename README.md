# Luminal Scape

<p align="center">
  <img src="src/main/resources/images/image1.png" alt="Luminal Scape: vista del entorno de la mazmorra" width="100%">
</p>

Luminal Scape es un juego de exploración en 3D escrito en Java sobre LWJGL. El jugador recorre una mazmorra interconectada, recolectando artefactos antes de que se agote el tiempo. Cada artefacto recogido suma segundos extra al reloj, por lo que la estrategia y el control del movimiento son decisivos en los niveles más exigentes.

El proyecto funciona como referencia práctica de programación gráfica moderna: uso de shaders, iluminación dinámica, mapping de texturas, detección de colisiones y diseño modular, todo integrado en un motor ligero y ordenado por responsabilidades.

---

## Características

- Renderizado 3D en tiempo real con OpenGL 3.3 vía LWJGL.
- Escenarios modulares construidos a partir de una cuadrícula de símbolos, editables desde un único archivo de configuración.
- Niveles independientes cargados desde textos con nombre y tiempo propios.
- Detección de colisiones para muros, puertas, ventanas y barandales.
- Iluminación dinámica con shaders: muros con brillo y artefactos con resplandor.
- Puertas transitables al agacharse; las ventanas no pueden atravesarse.
- Artefactos flotantes rotatorios que otorgan tiempo extra al recogerlos.
- Audio ambiental (pasos) y sonido de recolección con OpenAL.
- HUD con temporizador, contador de artefactos y FPS.
- Minimapa configurable en tamaño y visibilidad.
- Menú de pausa con control de FPS, reinicio de partida y opciones del minimapa.
- Selector de nivel y dificultad en la pantalla de título.
- Ventana redimensionable y modo de pantalla completa con antialiasing (MSAA 4x).

---

## Capturas de pantalla

<p align="center">

| Partida en curso (HUD activo) | Interior del entorno |
|:------------------------------:|:--------------------:|
| <img src="src/main/resources/images/image2.png" alt="Partida en curso con temporizador y contador de artefactos" width="100%"> | <img src="src/main/resources/images/image3.png" alt="Interior de la mazmorra renderizado con OpenGL" width="100%"> |

</p>

---

## Tecnologías

- Java 21
- LWJGL 3.3.6 (GLFW, OpenGL, OpenAL, STB)
- JOML (matemática de vectores y matrices)
- Maven

---

## Requisitos previos

- JDK 21 o superior.
- Maven (o el wrapper que se incluye en el repositorio).

---

## Compilación y ejecución

1. Clona el repositorio:

   ```
   git clone <url-del-repositorio>
   cd Luminal_Scape
   ```

2. Compila el proyecto:

   ```
   ./mvnw compile
   ```

3. Ejecuta la clase `main.Main` con Maven:

   ```
   ./mvnw exec:java
   ```

También es posible compilar y ejecutar desde cualquier IDE con soporte para Maven, abriendo el proyecto y lanzando la clase `main.Main`.

---

## Mecánicas del juego

- **Objetivo:** encuentra todos los artefactos de la mazmorra antes de que el temporizador llegue a cero.
- **Tiempo extra:** cada artefacto recogido añade 10 segundos al reloj (7 en dificultad difícil).
- **Dificultades:**
  - Fácil: el minimapa permanece visible durante la partida.
  - Normal: el minimapa está oculto al iniciar.
  - Difícil: los artefactos otorgan menos tiempo extra.
- **Puertas y ventanas:** las puertas se cruzan agachándose; las ventanas solo son visibles, no transitables.
- **Menú:** `M` o `P` abren el menú de pausa, desde el que se puede continuar, reiniciar el tiempo, ajustar el FPS o salir.

## Controles

| Tecla                  | Acción                                    |
|:-----------------------|:------------------------------------------|
| `W`, `A`, `S`, `D`     | Moverse por la mazmorra                   |
| Ratón                  | Rotar la cámara                          |
| `Shift` izquierdo      | Correr                                   |
| `Ctrl` izquierdo       | Agacharse (permite cruzar puertas)       |
| `M`, `P`               | Abrir o cerrar el menú                   |
| `F11`                  | Alternar pantalla completa               |
| `Esc`                  | Salir del juego                          |
| `Enter`                | Iniciar la partida                       |
| `<`, `>`              | Cambiar de nivel en la pantalla de título |
| `8`, `9`, `0`          | Efectos de visión (pruebas de render)     |

---

## Estructura del proyecto

```
src/main/java
├── main/                 Clase principal y configuración del juego
├── Player/               Jugador, cámara, entrada y ratón
├── Render2D/             HUD, minimapa, textos y botones
├── Render3D/             Geometría, colisiones, mapas y artefactos
├── MediaUtil/            Carga y reproducción de audio
└── UtilsRender/          Ventana, shaders, texturas y utilidades de tiempo

src/main/resources
├── levels/               Niveles en texto (editables)
├── images/               Capturas de pantalla
├── fonts/                Tipografías
├── shaders/              Shaders GLSL
├── sound/                Recursos de audio
└── textures/             Texturas del entorno
```

---

## Autor

Bryan Maradiaga

---

## Licencia

Este proyecto se distribuye bajo la licencia MIT. Consulta el archivo `LICENSE` para más detalles.
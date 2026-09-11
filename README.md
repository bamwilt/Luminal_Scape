# Luminal Scape

<p align="center">
  <img src="src/main/resources/images/image1.png" alt="Luminal Scape: vista dentro de uno de los niveles" width="100%">
</p>

Luminal Scape es un juego de exploración 3D en Java con LWJGL. Inspirado en los *backrooms*, propone recorrer niveles compuestos por corredores y salas infinitos, iluminados con fluorescentes y envueltos en un ambiente agobiante, mientras se recolectan artefactos antes de que se agote el tiempo. Cada artefacto recogido añade tiempo extra al reloj, por lo que la planificación de la ruta y el control del movimiento marcan la diferencia entre salir o quedarse ahí para siempre.

El proyecto sirve como referencia práctica de programación gráfica moderna: shaders, iluminación dinámica, mapeo de texturas, detección de colisiones y un motor ordenado por responsabilidades, todo integrado con LWJGL y OpenGL.

---

## Características

- Renderizado 3D en tiempo real con OpenGL, a través de LWJGL.
- Niveles modulares definidos por una cuadrícula de símbolos, editables desde un único archivo de texto.
- Niveles independientes cargados desde archivos con nombre y tiempo propios.
- Detección de colisiones para muros, puertas, ventanas y barandales.
- Iluminación dinámica con shaders: muros con brillo y artefactos con resplandor sobre el suelo mojado.
- Pasillos con raíles, puertas y ventanas ¿explora el espacio con libertad.
- Artefactos flotantes que rotan y otorgan tiempo extra al recogerlos.
- Audio ambiental (pasos) y sonido al recoger artefactos con OpenAL.
- HUD con temporizador, contador de artefactos y FPS.
- Minimapa configurable en tamaño y visibilidad.
- Menú de pausa con control de FPS, reinicio de temporizador y opciones del minimapa.
- Selector de nivel y dificultad en la pantalla de título.
- Ventana redimensionable y modo de pantalla completa con antialiasing (MSAA 4x).

---

## Capturas de pantalla

Solo capturas del juego en plena partida:

| Captura 1 | Captura 2 | Captura 3 |
|:---------:|:---------:|:---------:|
| <img src="src/main/resources/images/image1.png" alt="Primera captura de una partida" width="100%"> | <img src="src/main/resources/images/image2.png" alt="Segunda captura de una partida" width="100%"> | <img src="src/main/resources/images/image3.png" alt="Tercera captura de una partida" width="100%"> |

---

## Tecnologías

- Java 21
- LWJGL 3.3.6 (GLFW, OpenGL, OpenAL, STB)
- JOML (matemáticas de vectores y matrices)
- Maven

---

## Requisitos previos

- JDK 21 o superior.
- Maven, o el wrapper incluido en el repositorio (`./mvnw`).

---

## Compilación y ejecución

1. Clonar el repositorio:

   ```
   git clone <url-del-repositorio>
   cd Luminal_Scape
   ```

2. Compilar el proyecto:

   ```
   ./mvnw compile
   ```

3. Ejecutar la clase `main.Main`:

   ```
   ./mvnw exec:java
   ```

También se puede abrir el proyecto en cualquier IDE con soporte de Maven y lanzar la clase `main.Main`.

---

## Cómo se juega

El objetivo es encontrar todos los artefactos de cada nivel antes de que se agote el tiempo:

- Recolectar un artefacto añade segundos extra al temporizador (más en dificultad fácil, menos en difícil).
- Hay tres niveles y tres dificultades seleccionables desde la pantalla de título.
- El minimapa y el control de tiempo cambian según la dificultad elegida.

### Controles

| Tecla           | Acción                                                          |
|:----------------|:----------------------------------------------------------------|
| `W`, `A`, `S`, `D` | Moverse por el espacio                                       |
| Ratón           | Rotar la cámara                                                 |
| `Shift` izquierdo | Correr                                                        |
| `Ctrl` izquierdo | Agacharse (permite cruzar las puertas)                        |
| `M`, `P`        | Abrir o cerrar el menú                                          |
| `F11`           | Alternar pantalla completa                                      |
| `Esc`           | Cerrar la ventana                                               |
| `Enter`         | Empezar la partida (pantalla de título)                         |
| `←` / `→`       | Cambiar de nivel en la pantalla de título                       |

---

## Estructura del proyecto

```
src/main/java
├── main/            Clase principal y configuración del juego
├── Player/          Jugador, cámara, entrada y ratón
├── Render2D/        HUD, minimapa, textos y botones
├── Render3D/        Renderizado, colisiones, niveles y artefactos
├── MediaUtil/       Carga y reproducción de audio
└── UtilsRender/     Ventana, shaders y utilidades de tiempo

src/main/resources
├── levels/          Niveles en texto (editables)
├── images/          Capturas del juego
├── fonts/           Tipografías
├── shaders/         Shaders GLSL
├── sound/           Recursos de audio
└── textures/        Texturas del entorno
```

---

## Licencia

Este proyecto se distribuye bajo la licencia MIT. Consulta el archivo `LICENSE` para más detalles.

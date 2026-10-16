$ErrorActionPreference = "Stop"
$base = "C:\Users\Bryan Maradiaga\Documents\Luminal_Scape-Main"
$p2 = Join-Path $base "src\main\java\Core\TitleCameraAnimator.java"
$content = @'
package Core;

import Player.Camera;
import Render3D.map.DungeonManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Animador de camara para cinematicas de la pantalla de titulo.
 *
 * <p>Recibe una lista de puntos de interes y un Modo de movimiento. La camara
 * aparece en el primer punto, se acerca al siguiente, y recorre la lista en
 * bucle segun el modo elegido.
 *
 * <ul>
 *   <li>{@link Modo#SMOOTH}: desplazamiento suave y pausado.</li>
 *   <li>{@link Modo#FAST}: traslacion rapida entre puntos.</li>
 *   <li>{@link Modo#LOW}: camara baja y movimiento lento.</li>
 * </ul>
 *
 * <p>Prioridad de puntos: 1) items del nivel, 2) spawn, 3) origen del mapa.
 */
public final class TitleCameraAnimator {

    /** Modo de desplazamiento de la camara entre puntos. */
    public enum Modo {
        SMOOTH, FAST, LOW
    }

    private final Camera camara;
    private final DungeonManager dungeon;
    private final List<Vector3f> puntos = new ArrayList<>();
    private final Modo modo;
    private int indiceActual = 0;
    private float tiempoEnPunto = 0f;

    private final float holdSegundos;
    private final float alturaCamara;
    private final float distanciaInicial;
    private final float distanciaFinal;

    /**
     * @param dungeon  nivel actual (para obtener items y spawn)
     * @param puntos   lista de puntos de interes (si esta vacia se calcula automaticamente con prioridad a items)
     * @param modo     como se mueve la camara entre puntos (ultimo parametro)
     */
    public TitleCameraAnimator(DungeonManager dungeon, List<Vector3f> puntos, Modo modo) {
        this.dungeon = dungeon;
        this.camara = new Camera();
        this.modo = modo;

        switch (modo) {
            case SMOOTH -> {
                holdSegundos = 3f;
                alturaCamara = 8f;
                distanciaInicial = 12f;
                distanciaFinal = 6f;
            }
            case FAST -> {
                holdSegundos = 2f;
                alturaCamara = 10f;
                distanciaInicial = 14f;
                distanciaFinal = 7f;
            }
            case LOW -> {
                holdSegundos = 4f;
                alturaCamara = 4f;
                distanciaInicial = 9f;
                distanciaFinal = 5f;
            }
            default -> {
                holdSegundos = 3f;
                alturaCamara = 8f;
                distanciaInicial = 12f;
                distanciaFinal = 6f;
            }
        }

        if (puntos != null && !puntos.isEmpty()) {
            this.puntos.addAll(puntos);
        } else {
            reconstruirPuntos();
        }

        indiceActual = 0;
        tiempoEnPunto = 0f;
    }

    /** Recalcula la lista de puntos de interes (prioridad: items, luego spawn). */
    public void reconstruirPuntos() {
        puntos.clear();
        List<Vector3f> items = dungeon.getItemCenters();
        if (items != null) {
            puntos.addAll(items);
        }
        Vector3f spawn = dungeon.getSpawnPosition();
        if (spawn != null && !puntos.contains(spawn)) {
            puntos.add(spawn);
        }
        if (puntos.isEmpty()) {
            puntos.add(new Vector3f(dungeon.getOriginX(), 0f, dungeon.getOriginZ()));
        }
    }

    /** Actualiza la camara y devuelve su matriz de vista. */
    public Matrix4f update(float deltaTime, float elapsedTime) {
        if (puntos.isEmpty()) {
            return camara.getViewMatrix();
        }

        tiempoEnPunto += deltaTime;
        if (tiempoEnPunto >= holdSegundos) {
            tiempoEnPunto = 0f;
            indiceActual = (indiceActual + 1) % puntos.size();
        }

        Vector3f punto = puntos.get(indiceActual);
        float t = tiempoEnPunto / holdSegundos; // 0..1 dentro del periodo

        // Distancia: se acerca segun el modo
        float distancia;
        switch (modo) {
            case SMOOTH -> distancia = distanciaInicial - (distanciaInicial - distanciaFinal) * t;
            case FAST -> distancia = distanciaInicial - (distanciaInicial - distanciaFinal) * (t * t);
            case LOW -> distancia = distanciaInicial - (distanciaInicial - distanciaFinal) * (1f - (1f - t) * (1f - t));
            default -> distancia = distanciaInicial - (distanciaInicial - distanciaFinal) * t;
        }

        // Mira hacia el spawn (o hacia el mismo punto si no hay spawn)
        Vector3f spawn = dungeon.getSpawnPosition();
        Vector3f mirar = (spawn != null) ? spawn : punto;

        // Angulo en plano XZ desde el punto hacia donde mirar
        float dx = mirar.x - punto.x;
        float dz = mirar.z - punto.z;
        float anguloYaw = (float) Math.toDegrees(Math.atan2(dx, dz));

        // Camara: situada tras el punto, opuesto a la mirada, elevada segun modo
        float rad = (float) Math.toRadians(anguloYaw);
        float cx = punto.x - (float) (Math.sin(rad) * distancia);
        float cz = punto.z - (float) (Math.cos(rad) * distancia);
        float cy = punto.y + alturaCamara;

        camara.setPosition(new Vector3f(cx, cy, cz));
        camara.setYaw(anguloYaw);
        float pitch = switch (modo) {
            case SMOOTH -> -25f;
            case FAST -> -15f;
            case LOW -> -45f;
            default -> -25f;
        };
        camara.setPitch(pitch);

        return camara.getViewMatrix();
    }

    public Camera getCamera() {
        return camara;
    }

    public Modo getModo() {
        return modo;
    }

    public int getIndiceActual() {
        return indiceActual;
    }

    public int getTotalPuntos() {
        return puntos.size();
    }

    /** Agrega un punto de interes personalizado a la lista. */
    public void addPunto(Vector3f punto) {
        if (punto != null) {
            puntos.add(punto);
        }
    }
}
'@
[System.IO.File]::WriteAllText($p2, $content)
Write-Host "TitleCameraAnimator.java reescrito con Modo enum y lista de puntos"

# Actualizar GameLoop.java para usar el nuevo constructor
$p = Join-Path $base "src\main\java\Core\GameLoop.java"
$c = [System.IO.File]::ReadAllText($p)

$old = '        titleCamera = new TitleCameraAnimator(dungeonManager);'
$new = '        titleCamera = new TitleCameraAnimator(dungeonManager, null, TitleCameraAnimator.Modo.SMOOTH);'

if ($c -match [regex]::Escape($old)) {
    $c = $c -replace [regex]::Escape($old), $new
    [System.IO.File]::WriteAllText($p, $c)
    Write-Host "GameLoop.java: constructor actualizado con Modo.SMOOTH"
} else {
    Write-Host "No se encontro el patron para actualizar el constructor"
}
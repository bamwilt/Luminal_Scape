$ErrorActionPreference = "Stop"
$base = "C:\Users\Bryan Maradiaga\Documents\Luminal_Scape-Main"

# 1. Agregar getItemCenters() a DungeonManager.java
$p = Join-Path $base "src\main\java\Render3D\map\DungeonManager.java"
$c = [System.IO.File]::ReadAllText($p)
$marker = '    public float getSpawnYaw() {' + [char]10 + '        return spawnYaw;' + [char]10 + '    }'
$insert = $marker + [char]10 + [char]10 + '    /** Centros de todos los items del nivel (para la camara aerea del titulo). */' + [char]10 + '    public List<Vector3f> getItemCenters() {' + [char]10 + '        List<Vector3f> centers = new ArrayList<>(items.size());' + [char]10 + '        for (Item item : items) {' + [char]10 + '            centers.add(item.getCenter());' + [char]10 + '        }' + [char]10 + '        return centers;' + [char]10 + '    }'
if ($c -notcontains 'getItemCenters') {
    $c = $c -replace [regex]::Escape($marker), $insert
    [System.IO.File]::WriteAllText($p, $c)
    Write-Host "DungeonManager.java: getItemCenters() agregado"
} else {
    Write-Host "DungeonManager.java: getItemCenters() ya existe"
}

# 2. Crear TitleCameraAnimator.java
$p2 = Join-Path $base "src\main\java\Core\TitleCameraAnimator.java"
$content = @"
package Core;

import Player.Camera;
import Render3D.map.DungeonManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Camara aerea inclinada para la pantalla de titulo.
 *
 * <p>Cada {@link #HOLD_SEGUNDOS} cambia al siguiente punto de interes (un item
 * del nivel, o el spawn si no hay items). La camara se situa en una posicion
 * elevada y trasera respecto al punto, mirando hacia el spawn, y se desliza
 * lentamente acercandose durante el periodo. Al acabar, salta al siguiente
 * punto y repite el ciclo.
 */
public final class TitleCameraAnimator {

    /** Segundos que se mantiene la camara en cada punto antes de cambiar. */
    public static final float HOLD_SEGUNDOS = 3f;

    /** Altura de la camara sobre el punto de interes, en unidades de mundo. */
    private static final float ALTURA_CAMARA = 12f;

    /** Distancia horizontal desde la camara al punto de interes. */
    private static final float DISTANCIA_INICIAL = 18f;

    /** Distancia minima al final del acercamiento. */
    private static final float DISTANCIA_FINAL = 9f;

    /** Angulo de elevacion de la camara (0 = cenital, mas alto = mas rasante). */
    private static final float ANGULO_ELEVACION = 45f;

    private final Camera camara;
    private final DungeonManager dungeon;
    private final List<Vector3f> puntos = new ArrayList<>();
    private int indiceActual = 0;
    private float tiempoEnPunto = 0f;
    private boolean inicializado = false;

    /** Ultima posicion y target interpolados de la camara. */
    private final Vector3f posCamara = new Vector3f();
    private final Vector3f targetCamara = new Vector3f();

    public TitleCameraAnimator(DungeonManager dungeon) {
        this.dungeon = dungeon;
        this.camara = new Camera();
        reconstruirPuntos();
    }

    /** Vuelve a calcular la lista de puntos de interes del nivel actual. */
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
            // Fallback: origen del nivel
            puntos.add(new Vector3f(dungeon.getOriginX(), 0f, dungeon.getOriginZ()));
        }
        indiceActual = 0;
        tiempoEnPunto = 0f;
        inicializado = true;
    }

    /** Actualiza la camara y devuelve su matriz de vista. */
    public Matrix4f update(float deltaTime, float elapsedTime) {
        if (!inicializado || puntos.isEmpty()) {
            return camara.getViewMatrix();
        }

        tiempoEnPunto += deltaTime;
        if (tiempoEnPunto >= HOLD_SEGUNDOS) {
            tiempoEnPunto = 0f;
            indiceActual = (indiceActual + 1) % puntos.size();
        }

        Vector3f punto = puntos.get(indiceActual);
        float t = tiempoEnPunto / HOLD_SEGUNDOS; // 0..1 dentro del periodo

        // Distancia: se acerca gradualmente de DISTANCIA_INICIAL a DISTANCIA_FINAL
        float distancia = DISTANCIA_INICIAL - (DISTANCIA_INICIAL - DISTANCIA_FINAL) * t;

        // Posicion de la camara: elevada y trasera respecto al punto
        float rad = (float) Math.toRadians(ANGULO_ELEVACION);
        float h = (float) (Math.sin(rad) * distancia);
        float d = (float) (Math.cos(rad) * distancia);

        // Yaw del punto: mirar hacia el spawn desde la camara
        Vector3f spawn = dungeon.getSpawnPosition();
        Vector3f mirar = (spawn != null) ? spawn : punto;

        // Direccion desde la camara hacia el punto (en plano XZ)
        float dx = mirar.x - punto.x;
        float dz = mirar.z - punto.z;
        float anguloYaw = (float) Math.toDegrees(Math.atan2(dx, dz));

        // Posicion camara: punto + offset en direccion opuesta a la mirada
        float cx = punto.x - (float) (Math.sin(Math.toRadians(anguloYaw)) * d);
        float cz = punto.z - (float) (Math.cos(Math.toRadians(anguloYaw)) * d);
        float cy = punto.y + ALTURA_CAMARA + h * 0.5f;

        // Interpolacion suave de la posicion actual
        posCamara.lerp(new Vector3f(cx, cy, cz), t * 0.15f + 0.05f);
        targetCamara.lerp(mirar, t * 0.1f + 0.05f);

        camara.setPosition(posCamara);
        camara.setYaw(anguloYaw);
        camara.setPitch(-ANGULO_ELEVACION);

        return camara.getViewMatrix();
    }

    /** Camara de la animacion (para lectura directa si se necesita). */
    public Camera getCamera() {
        return camara;
    }

    /** Indice del punto actual (0-based). */
    public int getIndiceActual() {
        return indiceActual;
    }

    /** Total de puntos de interes. */
    public int getTotalPuntos() {
        return puntos.size();
    }
}
"@
[System.IO.File]::WriteAllText($p2, $content)
Write-Host "TitleCameraAnimator.java creado"

Write-Host "Todos los cambios de estructura aplicados"
$ErrorActionPreference = "Stop"
$base = "C:\Users\Bryan Maradiaga\Documents\Luminal_Scape-Main"

# ============ 1. Reescribir TitleCameraAnimator.java (simple) ============
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
 * Camara simple para el fondo de la pantalla de titulo.
 *
 * <p>Aparece encima de un punto de interes (item con prioridad 1, luego
 * spawn o posiciones del mapa) y se acerca lentamente durante
 * {@link #HOLD_SEGUNDOS}. Al acabar salta al siguiente punto y repite.
 */
public final class TitleCameraAnimator {

    public static final float HOLD_SEGUNDOS = 3f;

    private final Camera camara;
    private final DungeonManager dungeon;
    private final List<Vector3f> puntos = new ArrayList<>();
    private int indiceActual = 0;
    private float tiempoEnPunto = 0f;

    public TitleCameraAnimator(DungeonManager dungeon) {
        this.dungeon = dungeon;
        this.camara = new Camera();
        reconstruirPuntos();
        if (!puntos.isEmpty()) {
            tiempoEnPunto = 0f;
        }
    }

    /** Vuelve a calcular la lista de puntos de interes del nivel actual. */
    public void reconstruirPuntos() {
        puntos.clear();
        // Prioridad 1: items
        List<Vector3f> items = dungeon.getItemCenters();
        if (items != null) {
            puntos.addAll(items);
        }
        // Segundo: spawn
        Vector3f spawn = dungeon.getSpawnPosition();
        if (spawn != null && !puntos.contains(spawn)) {
            puntos.add(spawn);
        }
        // Fallback: origen del mapa
        if (puntos.isEmpty()) {
            puntos.add(new Vector3f(dungeon.getOriginX(), 0f, dungeon.getOriginZ()));
        }
        indiceActual = 0;
        tiempoEnPunto = 0f;
    }

    /** Actualiza la camara y devuelve su matriz de vista. */
    public Matrix4f update(float deltaTime, float elapsedTime) {
        if (puntos.isEmpty()) {
            return camara.getViewMatrix();
        }

        tiempoEnPunto += deltaTime;
        if (tiempoEnPunto >= HOLD_SEGUNDOS) {
            tiempoEnPunto = 0f;
            indiceActual = (indiceActual + 1) % puntos.size();
        }

        Vector3f punto = puntos.get(indiceActual);
        float t = tiempoEnPunto / HOLD_SEGUNDOS; // 0..1 dentro del periodo

        // Acercarse: distancia de 12 a 6, altura de 8 a 4
        float distancia = 12f - 6f * t;
        float altura = 8f - 4f * t;

        // Mira hacia el spawn (o hacia el mismo punto si no hay spawn)
        Vector3f spawn = dungeon.getSpawnPosition();
        Vector3f mirar = (spawn != null) ? spawn : punto;

        // Angulo en plano XZ desde el punto hacia donde mirar
        float dx = mirar.x - punto.x;
        float dz = mirar.z - punto.z;
        float anguloYaw = (float) Math.toDegrees(Math.atan2(dx, dz));

        // Camara: situada tras el punto, opuesto a la mirada, elevada
        float rad = (float) Math.toRadians(anguloYaw);
        float cx = punto.x - (float) (Math.sin(rad) * distancia);
        float cz = punto.z - (float) (Math.cos(rad) * distancia);
        float cy = punto.y + altura;

        camara.setPosition(new Vector3f(cx, cy, cz));
        camara.setYaw(anguloYaw);
        camara.setPitch(-25f); // ligera inclinacion hacia abajo

        return camara.getViewMatrix();
    }

    public Camera getCamera() {
        return camara;
    }

    public int getIndiceActual() {
        return indiceActual;
    }

    public int getTotalPuntos() {
        return puntos.size();
    }
}
"@
[System.IO.File]::WriteAllText($p2, $content)
Write-Host "TitleCameraAnimator.java reescrito (camara simple)"

# ============ 2. Corregir PAUSED en GameLoop (0.0f -> 1.0f) ============
$p = Join-Path $base "src\main\java\Core\GameLoop.java"
$c = [System.IO.File]::ReadAllText($p)

$oldPaused = '            case PAUSED:' + [char]10 + '                // El minimapa NO se dibuja en pausa. El menǧ va sobre un fondo' + [char]10 + '                // plano, y el plano del nivel encima solo estorba (se solapa con' + [char]10 + '                // los botones y con la pista de controles) y confunde: en pausa' + [char]10 + '                // el nivel ya esta en marcha o ya se ha perdido, y el mapa se' + [char]10 + '                // apaga al salir de uno y de otro (ver winGame/loseGame).' + [char]10 + '                loteUi.addQuad(0, 0, w, h, 0f, 0f, 0f, 0.0f);'
$newPaused = '            case PAUSED:' + [char]10 + '                // El minimapa NO se dibuja en pausa. El menǧ va sobre un fondo' + [char]10 + '                // plano, y el plano del nivel encima solo estorba (se solapa con' + [char]10 + '                // los botones y con la pista de controles) y confunde: en pausa' + [char]10 + '                // el nivel ya esta en marcha o ya se ha perdido, y el mapa se' + [char]10 + '                // apaga al salir de uno y de otro (ver winGame/loseGame).' + [char]10 + '                // Primera capa opaca: nunca se ve el fondo 3D detras del menu.' + [char]10 + '                loteUi.addQuad(0, 0, w, h, 0f, 0f, 0f, 1.0f);'

if ($c -match [regex]::Escape($oldPaused)) {
    $c = $c -replace [regex]::Escape($oldPaused), $newPaused
    [System.IO.File]::WriteAllText($p, $c)
    Write-Host "GameLoop.java: PAUSED corregido a 1.0f"
} else {
    Write-Host "No se encontro el patron PAUSED exacto. Revisando lineas..."
    Select-String -Pattern 'case PAUSED' -Path $p | ForEach-Object { Write-Host $_.LineNumber ": " $_.Line.Trim() }
    Select-String -Pattern 'addQuad\(0, 0, w, h, 0f, 0f, 0f, 0\.0f\)' -Path $p | ForEach-Object { Write-Host $_.LineNumber ": " $_.Line.Trim() }
}
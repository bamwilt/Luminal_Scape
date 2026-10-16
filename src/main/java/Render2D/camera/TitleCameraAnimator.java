package Render2D.camera;

import Render3D.map.DungeonManager;
import org.joml.Vector3f;

public class TitleCameraAnimator {
    public enum Modo { SMOOTH, LINEAR }

    private final DungeonManager dungeonManager;
    private final Camera camera;
    private final Modo modo;
    private Vector3f[] puntosInteres;
    private int indiceActual = 0;
    private float t = 0f;
    private float velocidad = 0.15f;

    public TitleCameraAnimator(DungeonManager dungeonManager, Camera camera, Modo modo) {
        this.dungeonManager = dungeonManager;
        this.camera = camera != null ? camera : new Camera();
        this.modo = modo;
        this.puntosInteres = construirPuntos();
        if (puntosInteres.length > 0) this.camera.setPosition(puntosInteres[0]);
    }

    private Vector3f[] construirPuntos() {
        var spawn = dungeonManager.getSpawnPosition();
        Vector3f[] pts = new Vector3f[1];
        pts[0] = spawn != null ? spawn : new Vector3f(0, 2, 0);
        return pts;
    }

    public void reconstruirPuntos() {
        Vector3f posActual = camera.getPosition();
        this.puntosInteres = construirPuntos();
        float minDist = Float.MAX_VALUE; int bestIdx = 0;
        for (int i = 0; i < puntosInteres.length; i++) {
            float d = posActual.distance(puntosInteres[i]);
            if (d < minDist) { minDist = d; bestIdx = i; }
        }
        indiceActual = bestIdx; t = 0f;
    }

    public void update(float deltaTime, float elapsedTime) {
        if (puntosInteres.length < 2) return;
        t += deltaTime * velocidad;
        if (t >= 1f) { t = 0f; indiceActual = (indiceActual + 1) % puntosInteres.length; }
        Vector3f a = puntosInteres[indiceActual];
        Vector3f b = puntosInteres[(indiceActual + 1) % puntosInteres.length];
        float interpT = modo == Modo.SMOOTH ? smoothstep(t) : t;
        camera.setPosition(new Vector3f(
            a.x + (b.x - a.x) * interpT,
            a.y + (b.y - a.y) * interpT,
            a.z + (b.z - a.z) * interpT
        ));
        float dx = b.x - camera.getPosition().x;
        float dz = b.z - camera.getPosition().z;
        camera.setYaw((float) Math.toDegrees(Math.atan2(dx, dz)));
    }

    private float smoothstep(float t) { return t * t * (3f - 2f * t); }
    public Camera getCamera() { return camera; }
}

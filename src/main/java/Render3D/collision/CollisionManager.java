package Render3D.collision;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprueba si el jugador entra en algún volumen sólido del nivel.
 *
 * <p>Los volúmenes son {@link Aabb} en coordenadas de mundo y no dependen de
 * cómo se dibuje la geometría: la malla de la mazmorra se agrupa en un único
 * VAO, pero cada caja que el autotiling dibuja aporta su propio AABB. Así el
 * mismo código sirve para los muros batcheados y para los objetos sueltos
 * (pisos, barandillas, items), que aportan el AABB de su caja.
 *
 * <p>Los muros y los railings se guardan en listas separadas para poder
 * distinguirlos más adelante sin recorrer toda la geometría.
 *
 * <p><b>La posición que recibe son los PIES</b>, no el ojo de la cámara: la caja
 * va de {@code (y, y + altura)} hacia arriba. Si se pasara la posición de la
 * cámara, todo el cuerpo quedaría desplazado por la altura del ojo y el jugador
 * no cabría de pie bajo ningún dintel de puerta.
 */
public class CollisionManager {

    private Vector3f playerPosition;
    private Vector3f playerSize;

    private final List<Aabb> walls = new ArrayList<>();
    private final List<Aabb> railingWalls = new ArrayList<>();

    public void setPlayerBounds(Vector3f position, Vector3f size) {
        this.playerPosition = new Vector3f(position);
        this.playerSize = new Vector3f(size);
    }

    public void addCollision(Aabb box) {
        walls.add(box);
    }

    /** Colisiones de raillings, en una lista aparte de los muros. */
    public void addRailingCollision(Aabb box) {
        railingWalls.add(box);
    }

    /** Libera todas las colisiones registradas (desecha muros ya borrados en GL). */
    public void clear() {
        walls.clear();
        railingWalls.clear();
    }

    public boolean checkCollisions() {
        if (playerPosition == null || playerSize == null) {
            return false;
        }

        float pminX = playerPosition.x - playerSize.x * 0.5f;
        float pminY = playerPosition.y;
        float pminZ = playerPosition.z - playerSize.z * 0.5f;
        float pmaxX = playerPosition.x + playerSize.x * 0.5f;
        float pmaxY = playerPosition.y + playerSize.y;
        float pmaxZ = playerPosition.z + playerSize.z * 0.5f;

        for (Aabb wall : walls) {
            if (wall.intersects(pminX, pminY, pminZ, pmaxX, pmaxY, pmaxZ)) {
                return true;
            }
        }
        for (Aabb wall : railingWalls) {
            if (wall.intersects(pminX, pminY, pminZ, pmaxX, pmaxY, pmaxZ)) {
                return true;
            }
        }
        return false;
    }
}

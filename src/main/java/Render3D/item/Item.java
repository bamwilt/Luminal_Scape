package Render3D.item;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import Render3D.graphics.PropMesh;
import UtilsRender.Shader;

/**
 * Base de todo lo que el jugador recoge.
 *
 * <p>Aqui vive lo que <b>todos</b> los items comparten: una malla, un punto del
 * mundo donde flotan, girar sobre si mismos y desaparecer cuando el jugador se
 * acerca. Lo que hace que una llave sea una llave y no otra cosa va en las
 * subclases ({@link ItemKey}), no aqui.
 *
 * <p>Se separo de {@code DungeonManager} a proposito: cuando se metio la llave,
 * la clase {@code Item} quedo con el nombre del modelo y la escala del modelo
 * grabados dentro. Anyadir un segundo item habia REQUIRED tocar la clase base
 * cada vez, y era facil que un item nuevo saliera con la escala de la llave.
 * Con esta jerarquia, cada tipo declara su recurso y su tamano y la base no se
 * entera.
 *
 * <p>La escala, el giro y el flotado van en el constructor y no en metodos
 * sueltos: asi el comportamiento se declara una vez al crear el item y no se
 * puede desincronizar entre lo que se ve y lo que se recoge.
 */
public abstract class Item {

    /** Malla ya subida a GPU. */
    protected final PropMesh mesh;

    /** Punto del mundo al que flota: el centro de su celda, a la altura de la camara. */
    protected final Vector3f center;

    /** Matriz de mundo del frame. Se reutiliza para no crear una por item y frame. */
    protected final Matrix4f transform = new Matrix4f();

    private final float scale;
    private final float spinSpeed;
    private final float bobAmount;
    private final float bobSpeed;

    /**
     * @param mesh      malla del modelo
     * @param center    centro de la celda donde aparece
     * @param scale     multiplo del tamano real del modelo
     * @param spinSpeed rad/seg alrededor del eje vertical
     * @param bobAmount amplitud del flotado, en unidades de mundo
     * @param bobSpeed  ciclos/seg del flotado
     */
    protected Item(PropMesh mesh, Vector3f center, float scale,
                   float spinSpeed, float bobAmount, float bobSpeed) {
        this.mesh = mesh;
        this.center = center;
        this.scale = scale;
        this.spinSpeed = spinSpeed;
        this.bobAmount = bobAmount;
        this.bobSpeed = bobSpeed;
    }

    /**
     * Gira y flota el item sobre su sitio.
     *
     * <p>El giro no es adorno: un objeto plano desaparece del todo cuando se ve
     * de canto, y sin el el jugador no ve que hay algo que recoger. Flota por la
     * misma razon, para que se distinga de un mueble apoyado en el suelo.
     */
    public void animate(float time) {
        float bob = (float) Math.sin(time * bobSpeed) * bobAmount;
        transform.identity()
                .translate(center.x, center.y + bob, center.z)
                .rotateY(time * spinSpeed)
                .scale(scale);
    }

    /**
     * Si el jugador tiene el item encima.
     *
     * <p>La comparacion va al cuadrado para no sacar raices y el radio lo pasa
     * quien llame, para que la regla de recogida viva en el mapa y no aqui.
     */
    public boolean isWithin(Vector3f playerPos, float radius) {
        float dx = center.x - playerPos.x;
        float dy = center.y - playerPos.y;
        float dz = center.z - playerPos.z;
        return dx * dx + dy * dy + dz * dz < radius * radius;
    }

    /** Dibuja el item con la matriz que dejo {@link #animate(float)}. */
    public void render(Shader shader) {
        mesh.render(shader, transform);
    }

    /** Centro del item en el mundo. */
    public Vector3f getCenter() {
        return center;
    }
}

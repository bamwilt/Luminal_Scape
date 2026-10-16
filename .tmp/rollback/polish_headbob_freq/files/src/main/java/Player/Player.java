package Player;

import Core.ConfigGlobal;
import Render3D.collision.CollisionManager;
import Render2D.camera.Camera;
import org.joml.Vector3f;

public class Player {

    private final Camera camara;
    private final CollisionManager collisionManager;

    private boolean isRunning = false;
    private boolean isCrouching = false;

    /**
     * Velocidades base, en unidades de mundo por segundo.
     *
     * <p>Salen de {@link ConfigGlobal}, que es la fuente de verdad de estos
     * numeros, y no se usan directamente para moverse: lo que mueve al player
     * son {@link #walkSpeed} y {@link #runSpeed}, que son estas mismas
     * multiplicadas por la escala del ajuste de velocidad (ver
     * {@link #setSpeedScale}). Asi el ajuste puede aplicarse tantas veces como
     * se quiera sin que la velocidad se acumule.
     */
    private static final float VELOCIDAD_ANDAR = ConfigGlobal.VELOCIDAD_MOVIMIENTO;
    private static final float VELOCIDAD_CORRER = ConfigGlobal.VELOCIDAD_CARRERA;

    /** Velocidad vigente andando y corriendo, ya escalada por el ajuste. */
    private float walkSpeed = VELOCIDAD_ANDAR;
    private float runSpeed = VELOCIDAD_CORRER;

    private final float crouchOffset = 1.0f;
    private final float playerWidth = 1.0f;
    private final float playerDepth = 1.0f;
    private final float playerStandingHeight = 1.8f;
    private final float crouchSpeed = 5.0f;

    /**
     * Desplazamiento máximo, en unidades de mundo, entre dos comprobaciones de
     * colisión.
     *
     * <p>El movimiento se divide en pasos de este tamaño como máximo. Sin esto,
     * a velocidad alta (el slider llega a 2x, y 40 u/s a 60 fps ya son 0,67 u
     * por frame) un frame podía cruzar entero un muro fino y dejar al jugador
     * dentro: desde ahí todos los movimientos chocan y se queda atascado, o
     * sale por el otro lado. También es lo que hacía que en las esquinas el
     * jugador se detuviese lejos, porque el frame entero se deshacía en cuanto
     * la caja rozaba el segundo muro.
     *
     * <p>0,2 es muy por debajo de la mitad del ancho del jugador (0,5), así que
     * ningún muro del nivel —todos de un tile o más— puede quedar entre dos
     * comprobaciones seguidas.
     */
    private static final float MAX_STEP = 0.2f;

    /**
     * Head bob: balanceo sutil de la camara al caminar.
     *
     * <p>Se aplica sobre la altura ya resuelta y solo cuando hay desplazamiento
     * horizontal, asi no afecta a la caja de colision (que mide desde el suelo,
     * no desde el ojo) ni al agacharse.
     */
    private static final float BOB_FRECUENCIA = 4.0f;
    private static final float BOB_AMPLITUD = 0.018f;
    /**
     * Velocidad de entrada/salida del balanceo, en fraccion por segundo.
     *
     * <p>Es la rampa que hace que el head bob aparezca y desaparezca poco a
     * poco al pulsar o soltar la tecla (igual que el agachado con su
     * crouchSpeed), en vez de saltar de golpe y sentirse como micro temblores.
     */
    private static final float BOB_ENTRADA_VELOCIDAD = 3.0f;
    private float bobPhase = 0f;
    /** Intensidad actual del balanceo (0 apagado .. 1 a tope), con rampa. */
    private float bobIntensidad = 0f;

    /**
     * Tope de substeps por frame.
     *
     * <p>Solo salta con un deltaTime absurdo (depuración, o el equipo parado),
     * y existe para que un frame raro no dispare cientos de comprobaciones de
     * colisión. Con él el jugador se mueve algo menos en ese frame, nunca más.
     */
    private static final int MAX_SUBSTEPS = 64;

    private float baseHeight;
    private Vector3f lastPosition;

    public Player(CollisionManager collisionManager) {
        this.camara = new Camera();
        this.collisionManager = collisionManager;
        this.baseHeight = camara.getPosition().y;
        this.lastPosition = new Vector3f(camara.getPosition());
    }

    public void update(float deltaTime, boolean forward, boolean backward, boolean left, boolean right) {
        Vector3f previousPos = new Vector3f(camara.getPosition());

        float currentSpeed = isRunning ? runSpeed : walkSpeed;
        float movementAmount = currentSpeed * deltaTime;

        float targetHeight = isCrouching ? (playerStandingHeight - crouchOffset) : playerStandingHeight;
        Vector3f size = new Vector3f(playerWidth, targetHeight, playerDepth);

        // El frame se parte en substeps: cada uno avanza como máximo MAX_STEP y
        // comprueba colisión. Si un substep no cabe, se deshace solo ese trozo y
        // se deja de avanzar, así el jugador se acerca todo lo que puede al muro
        // en vez de rebotar al principio del frame.
        int substeps = 1;
        if (movementAmount > MAX_STEP) {
            substeps = Math.min(MAX_SUBSTEPS, (int) Math.ceil(movementAmount / MAX_STEP));
        }
        float paso = movementAmount / substeps;

        for (int i = 0; i < substeps; i++) {
            Vector3f antes = new Vector3f(camara.getPosition());

            if (forward) {
                camara.moveForward(paso);
            }
            if (backward) {
                camara.moveBackward(paso);
            }
            if (left) {
                camara.moveLeft(paso);
            }
            if (right) {
                camara.moveRight(paso);
            }

            // La caja de colisión va de los PIES a la CABEZA, no desde el ojo:
            // la cámara está baseHeight por encima del suelo, así que medir
            // desde su Y desplazaba todo el cuerpo hacia arriba y el player
            // medía 1,8 desde la altura de los ojos (1,6), llegando a 3,4. Con
            // eso no cabía de pie bajo ningún dintel. Aquí los pies están en el
            // suelo (el juego no tiene salto ni gravedad) y la caja es
            // [suelo, suelo + altura del cuerpo].
            Vector3f pos = camara.getPosition();
            collisionManager.setPlayerBounds(new Vector3f(pos.x, 0f, pos.z), size);

            if (collisionManager.checkCollisions()) {
                // Este tramo no cabe: se vuelve a donde empezaba y ya está. El
                // movimiento del frame se da por terminado.
                camara.setPosition(antes);
                break;
            }
        }

        if (isCrouching) {
            Vector3f pos = camara.getPosition();
            pos.y = lerp(pos.y, baseHeight - crouchOffset, crouchSpeed * deltaTime);
            camara.setPosition(pos);
        } else {
            Vector3f pos = camara.getPosition();
            pos.y = baseHeight;
            camara.setPosition(pos);
        }

        Vector3f pos = camara.getPosition();
        collisionManager.setPlayerBounds(new Vector3f(pos.x, 0f, pos.z), size);

        // Si agacharse deja el cuerpo dentro de algo, el frame entero se
        // deshace: es el mismo caso de antes, y sin substeps el jugador podía
        // terminar el frame incrustado en el muro.
        if (collisionManager.checkCollisions()) {
            camara.setPosition(previousPos);
        }

        // Head bob: balanceo sutil de la camara al caminar. Va despues de
        // resolver colisiones y sobre la altura ya fijada, asi la caja de
        // colision (que mide desde el suelo) nunca ve este desplazamiento.
        //
        // La INTENSIDAD entra y sale con una rampa (como el agachado con su
        // crouchSpeed): al pulsar crece de 0 a 1 y al soltar vuelve a 0 poco a
        // poco, asi el balanceo no salta de golpe. La fase solo avanza mientras
        // hay desplazamiento, pero la intensidad se apaga sola, de modo que al
        // parar la camara se reasienta suavemente en vez de cortarse.
        Vector3f posBob = camara.getPosition();
        float bobDx = posBob.x - previousPos.x;
        float bobDz = posBob.z - previousPos.z;
        float bobRecorrido = (float) Math.sqrt(bobDx * bobDx + bobDz * bobDz);
        boolean bobCaminando = bobRecorrido > 1e-4f && !isCrouching;
        if (bobCaminando) {
            bobPhase += bobRecorrido * BOB_FRECUENCIA;
        }
        bobIntensidad = lerp(bobIntensidad, bobCaminando ? 1f : 0f,
                BOB_ENTRADA_VELOCIDAD * deltaTime);
        float bobAmplitud = (isRunning ? BOB_AMPLITUD * 1.4f : BOB_AMPLITUD) * bobIntensidad;
        posBob.y += (float) Math.sin(bobPhase) * bobAmplitud;
        camara.setPosition(posBob);

        lastPosition.set(previousPos);
    }

    private float lerp(float start, float end, float t) {
        if (t > 1.0f) {
            t = 1.0f;
        }
        return start + t * (end - start);
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
    }

    public void setCrouching(boolean crouching) {
        this.isCrouching = crouching;
    }

    /**
     * Escala la velocidad de andar y de correr (1 = velocidad base).
     *
     * <p>Es un multiplicador sobre las velocidades base y no un valor absoluto
     * a proposito: el slider de Configuracion manda su recorrido cada vez que
     * se pulsa + o -, y si el player guardara la velocidad que ha recibido el
     * ajuste se iria acumulando y acabaria volando. Con la escala, el valor
     * siempre se recalcula desde el mismo numero de partida.
     */
    public void setSpeedScale(float escala) {
        float factor = Math.max(0.1f, escala);
        this.walkSpeed = VELOCIDAD_ANDAR * factor;
        this.runSpeed = VELOCIDAD_CORRER * factor;
    }

    /** Velocidad andando vigente, en unidades de mundo por segundo. */
    public float getWalkSpeed() {
        return walkSpeed;
    }

    /** Velocidad corriendo vigente, en unidades de mundo por segundo. */
    public float getRunSpeed() {
        return runSpeed;
    }

    public void setPosition(Vector3f position) {
        camara.setPosition(position);
        this.baseHeight = position.y;
        this.lastPosition.set(position);
    }

    public void setBaseHeight(float height) {
        this.baseHeight = height;
        if (!isCrouching) {
            Vector3f pos = camara.getPosition();
            pos.y = baseHeight;
            camara.setPosition(pos);
        }
    }

    public Camera getCamara() {
        return camara;
    }

    public Vector3f getPosition() {
        return camara.getPosition();
    }

    public Vector3f getVelocity() {
        Vector3f currentPosition = getPosition();
        Vector3f velocity = new Vector3f(currentPosition).sub(lastPosition);
        return velocity;
    }
} 

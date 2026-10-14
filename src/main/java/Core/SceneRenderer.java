package Core;

import Player.Player;
import Render3D.Shader;
import Render3D.graphics.SkyRenderer;
import Render3D.map.Atmosphere;
import Render3D.map.DungeonManager;
import Render3D.map.LevelTypePreset;
import Render3D.map.MapConfig;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

/**
 * Dibuja el escenario 3D del nivel: cielo, muros, suelos, decoración e items.
 *
 * <p>Antes esto vivía dentro de {@link GameLoop} mezclado con el bucle, la
 * entrada y la interfaz. Aquí solo se reúne lo que hay que enseñar: la cámara
 * del jugador, el nivel ya construido y los tres shaders del escenario. El
 * bucle le pasa el jugador, el tiempo transcurrido y la distancia de vista, y
 * esta clase no sabe nada de estados, menús ni colisiones.
 *
 * <p>Los buffers de luces y el color de la luz ambiental son de esta capa: se
 * reutilizan cada frame para no crear arrays nuevos en el camino caliente.
 */
final class SceneRenderer {

    private final GameWindow window;
    private final LevelSession levelSession;
    private final DungeonManager dungeonManager;

    private final SkyRenderer cieloRender;
    private final Shader wallShader;
    private final Shader propShader;

    /** Posición de la luz direccional, fija para todo el nivel. */
    private final Vector3f lightPos =
            new Vector3f(ConfigGlobal.LUZ_X, ConfigGlobal.LUZ_Y, ConfigGlobal.LUZ_Z);

    /**
     * Buffers donde caen las luces puntuales de cada frame, reutilizados para
     * no crear un array por luz y frame. Los rellena
     * {@link DungeonManager#collectLights} y los vuelca
     * {@link Shader#setVec3Array} en los dos shaders del escenario.
     */
    private final float[] lightPosBuffer = new float[MapConfig.MAX_LIGHT_COUNT * 3];
    private final float[] lightColorBuffer = new float[MapConfig.MAX_LIGHT_COUNT * 3];

    SceneRenderer(GameWindow window, LevelSession levelSession, DungeonManager dungeonManager) {
        this.window = window;
        this.levelSession = levelSession;
        this.dungeonManager = dungeonManager;
        this.cieloRender = new SkyRenderer();
        this.wallShader = new Shader("shaders/wall_vertex.glsl", "shaders/wall_fragment.glsl");
        // La decoracion tiene su propio vertex porque lleva matriz por pieza,
        // a diferencia del escenario que va ya en coordenadas de mundo.
        this.propShader = new Shader("shaders/prop_vertex.glsl", "shaders/prop_fragment.glsl");
    }

    /**
     * Dibuja un frame del escenario.
     *
     * @param player      de donde salen la cámara y la posición
     * @param elapsedTime segundos transcurridos, para animar cielo e items
     * @param viewDistance distancia de vista ya resuelta con la dificultad
     */
    void render(Player player, float elapsedTime, float viewDistance) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Matrix4f projection = new Matrix4f().perspective(
                    (float) Math.toRadians(ConfigGlobal.FOV_GRADOS),
                    (float) window.getWidth() / window.getHeight(),
                    ConfigGlobal.PLANO_CERCANO,
                    ConfigGlobal.PLANO_LEJANO);

            Matrix4f view = player.getCamara().getViewMatrix();
            LevelTypePreset preset = levelSession.data().getPreset();
            Vector3f cameraPos = player.getCamara().getPosition();

            // 1. Cielo: se dibuja primero, sin escribir profundidad, y deja el
            //    estado de profundidad listo para la geometria.
            cieloRender.render(projection, view, cameraPos, elapsedTime, ConfigGlobal.PLANO_LEJANO, preset);

            // 2. Escenario: muros, suelos y techos con luz ambiente y niebla.
            wallShader.use();
            wallShader.setMat4("projection", projection);
            wallShader.setMat4("view", view);

            wallShader.setVec3("lightPos", lightPos);
            wallShader.setVec3("lightColor", ConfigGlobal.LUZ_R, ConfigGlobal.LUZ_G, ConfigGlobal.LUZ_B);
            wallShader.setVec3("viewPos", cameraPos);
            wallShader.setFloat("emissionStrength", ConfigGlobal.EMISION);
            wallShader.setFloat("glowIntensity", ConfigGlobal.BRILLO);
            wallShader.setFloat("u_brillo", VideoSettings.brillo);
            wallShader.setFloat("u_contraste", VideoSettings.contraste);
            setAtmosphere(wallShader, preset, viewDistance);
            setLights(wallShader, cameraPos);

            dungeonManager.render(wallShader);

            // 3. Decoracion e items: los dos son modelos GLB, asi que
            // comparten shader. item_fragment.glsl sigue en el repo para
            // cuando vuelva a haber items con textura.
            if (dungeonManager.getTotalProps() > 0 || dungeonManager.hasAnyItem()) {
                propShader.use();
                propShader.setMat4("projection", projection);
                propShader.setMat4("view", view);
                propShader.setVec3("lightPos", lightPos);
                propShader.setVec3("lightColor", ConfigGlobal.LUZ_R, ConfigGlobal.LUZ_G, ConfigGlobal.LUZ_B);
                propShader.setVec3("viewPos", cameraPos);
                propShader.setFloat("u_brillo", VideoSettings.brillo);
                propShader.setFloat("u_contraste", VideoSettings.contraste);
                setAtmosphere(propShader, preset, viewDistance);
                setLights(propShader, cameraPos);
                dungeonManager.renderProps(propShader);
                dungeonManager.renderItems(propShader, elapsedTime);
            }
        }
    }

    /**
     * Vuelca al shader la luz ambiente y la niebla del nivel. El color de
     * niebla es exactamente el del horizonte del cielo, para que el fundido de
     * los muros termine en el mismo color que el fondo.
     */
    private void setAtmosphere(Shader shader, LevelTypePreset preset, float viewDistance) {
        shader.setFloat("u_ambientLight", levelSession.data().getAmbientLight());
        shader.setFloat("u_viewDistance", viewDistance);
        // El margen cercano se deriva de la distancia de vista, asi que el
        // campo limpio se mantiene proporcional en los tres modos.
        shader.setFloat("u_fogNear", Atmosphere.fogNear(viewDistance));
        shader.setVec3("u_fogColor", preset.getSkyHorizonColor());
    }

    /**
     * Manda al shader las luces puntuales que ve el player: las placas '◉' del
     * techo y las llaves, de las que solo llegan las mas cercanas.
     *
     * <p>Se calculan una vez por frame y se vuelcan en los dos shaders del
     * escenario, para que un mueble al pie de una placa reciba la misma luz que
     * el suelo que lo rodea. La caida por distancia la aplica el shader con los
     * dos coeficientes de {@link MapConfig}, que se mandan aqui porque forman
     * parte de la misma ecuacion.
     */
    private void setLights(Shader shader, Vector3f cameraPos) {
        int count = dungeonManager.collectLights(cameraPos, lightPosBuffer, lightColorBuffer,
                MapConfig.MAX_LIGHT_COUNT);
        shader.setInt("u_lightCount", count);
        shader.setFloat("u_attenLinear", MapConfig.LIGHT_ATTEN_LINEAR);
        shader.setFloat("u_attenQuadratic", MapConfig.LIGHT_ATTEN_QUADRATIC);
        shader.setVec3Array("u_lightPos", lightPosBuffer, count);
        shader.setVec3Array("u_lightColor", lightColorBuffer, count);
    }

    void cleanup() {
        cieloRender.cleanup();
        wallShader.cleanup();
        propShader.cleanup();
    }
}

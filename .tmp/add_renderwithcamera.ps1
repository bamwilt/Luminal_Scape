$ErrorActionPreference = "Stop"
$base = "C:\Users\Bryan Maradiaga\Documents\Luminal_Scape-Main"
$p = Join-Path $base "src\main\java\Core\SceneRenderer.java"
$c = [System.IO.File]::ReadAllText($p)

# Agregar import de Camera si no existe
if ($c -notcontains 'import Player.Camera;') {
    $c = $c -replace 'import Player\.Player;', 'import Player.Player;' + [char]10 + 'import Player.Camera;'
}

# Agregar metodo renderWithCamera despues del metodo render()
$marker = '    void cleanup() {'
$insert = @"
    /**
     * Dibuja un frame del escenario usando una camara personalizada (por
     * ejemplo, la camara aerea animada de la pantalla de titulo), en lugar de
     * la camara del jugador.
     */
    void renderWithCamera(Camera cam, float elapsedTime, float viewDistance) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Matrix4f projection = new Matrix4f().perspective(
                    (float) Math.toRadians(ConfigGlobal.FOV_GRADOS),
                    (float) window.getWidth() / window.getHeight(),
                    ConfigGlobal.PLANO_CERCANO,
                    ConfigGlobal.PLANO_LEJANO);

            Matrix4f view = cam.getViewMatrix();
            LevelTypePreset preset = levelSession.data().getPreset();
            Vector3f cameraPos = cam.getPosition();

            // 1. Cielo
            cieloRender.render(projection, view, cameraPos, elapsedTime, ConfigGlobal.PLANO_LEJANO, preset);

            // 2. Escenario
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

            // 3. Decoracion e items
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

    void cleanup() {
"@
if ($c -notcontains 'renderWithCamera') {
    $c = $c -replace [regex]::Escape($marker), $insert
    [System.IO.File]::WriteAllText($p, $c)
    Write-Host "SceneRenderer.java: renderWithCamera() agregado"
} else {
    Write-Host "SceneRenderer.java: renderWithCamera() ya existe"
}
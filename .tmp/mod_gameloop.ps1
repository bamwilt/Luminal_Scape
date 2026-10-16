$ErrorActionPreference = "Stop"
$base = "C:\Users\Bryan Maradiaga\Documents\Luminal_Scape-Main"
$p = Join-Path $base "src\main\java\Core\GameLoop.java"
$c = [System.IO.File]::ReadAllText($p)

# 1. Agregar campo titleCamera despues de scene
if ($c -notmatch 'titleCamera') {
    $marker = '    private static SceneRenderer scene;'
    $insert = $marker + [char]10 + '    /** Camara aerea animada para el fondo de la pantalla de titulo. */' + [char]10 + '    private static TitleCameraAnimator titleCamera;'
    $c = $c -replace [regex]::Escape($marker), $insert
}

# 2. Inicializar titleCamera despues de crear scene (despues de 'scene = new SceneRenderer(...)')
if ($c -notmatch 'titleCamera = new TitleCameraAnimator') {
    $marker = '        scene = new SceneRenderer(window, levelSession, dungeonManager);'
    $insert = $marker + [char]10 + '        titleCamera = new TitleCameraAnimator(dungeonManager);'
    $c = $c -replace [regex]::Escape($marker), $insert
}

# 3. En mainLoop, reemplazar scene.render() para usar camara aerea en TITLE
if ($c -notmatch 'titleCamera\.update') {
    $old = '            scene.render(player, elapsedTime, viewDistance());'
    $new = '            // Pantalla de titulo: fondo 3D animado con camara aerea;' + [char]10 + '            // el resto de estados usa la camara del jugador.' + [char]10 + '            if (state == GameState.TITLE && titleCamera != null) {' + [char]10 + '                titleCamera.update(TimeUtils.getDeltaTime(), elapsedTime);' + [char]10 + '                scene.renderWithCamera(titleCamera.getCamera(), elapsedTime, viewDistance());' + [char]10 + '            } else {' + [char]10 + '                scene.render(player, elapsedTime, viewDistance());' + [char]10 + '            }'
    $c = $c -replace [regex]::Escape($old), $new
}

# 4. Quad de TITLE: transparente (0.0f) para ver el fondo 3D
if ($c -match 'addQuad\(0, 0, w, h, 0f, 0f, 0f, 1\.0f\);') {
    $c = $c -replace 'loteUi\.addQuad\(0, 0, w, h, 0f, 0f, 0f, 1\.0f\);', 'loteUi.addQuad(0, 0, w, h, 0f, 0f, 0f, 0.0f);'
}

[System.IO.File]::WriteAllText($p, $c)
Write-Host "GameLoop.java: animacion de titulo integrada"
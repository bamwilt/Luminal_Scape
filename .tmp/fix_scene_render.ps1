$ErrorActionPreference = "Stop"
$base = "C:\Users\Bryan Maradiaga\Documents\Luminal_Scape-Main"
$p = Join-Path $base "src\main\java\Core\SceneRenderer.java"
$c = [System.IO.File]::ReadAllText($p)

# Dentro de renderWithCamera, cambiar player.getCamara() por cam
$old1 = 'Matrix4f view = player.getCamara().getViewMatrix();'
$new1 = 'Matrix4f view = cam.getViewMatrix();'
$old2 = 'Vector3f cameraPos = player.getCamara().getPosition();'
$new2 = 'Vector3f cameraPos = cam.getPosition();'

$c = $c -replace [regex]::Escape($old1), $new1
$c = $c -replace [regex]::Escape($old2), $new2

[System.IO.File]::WriteAllText($p, $c)
Write-Host "SceneRenderer.java: referencias a player corregidas en renderWithCamera"
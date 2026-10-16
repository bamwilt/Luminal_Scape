$ErrorActionPreference = "Stop"
$base = "C:\Users\Bryan Maradiaga\Documents\Luminal_Scape-Main"
$p = Join-Path $base "src\main\java\Core\SceneRenderer.java"
$c = [System.IO.File]::ReadAllText($p)

# 1. Agregar import Camera
if ($c -notmatch 'import Player\.Camera;') {
    $c = $c -replace 'import Player\.Player;', "import Player.Player;`nimport Player.Camera;"
}

# 2. Reemplazar render() para que llame a renderWithCamera, y agregar renderWithCamera antes de cleanup()
$oldRender = '    void render(Player player, float elapsedTime, float viewDistance) {'
$newRender = '    void render(Player player, float elapsedTime, float viewDistance) {' + [char]10 + '        renderWithCamera(player.getCamara(), elapsedTime, viewDistance);' + [char]10 + '    }' + [char]10 + [char]10 + '    /**' + [char]10 + '     * Dibuja un frame del escenario usando una camara personalizada (por' + [char]10 + '     * ejemplo, la camara aerea animada de la pantalla de titulo), en lugar de' + [char]10 + '     * la camara del jugador.' + [char]10 + '     */' + [char]10 + '    void renderWithCamera(Camera cam, float elapsedTime, float viewDistance) {'

if ($c -notmatch 'renderWithCamera') {
    $c = $c -replace [regex]::Escape($oldRender), $newRender
    [System.IO.File]::WriteAllText($p, $c)
    Write-Host "SceneRenderer.java: renderWithCamera() agregado"
} else {
    Write-Host "SceneRenderer.java: renderWithCamera() ya existe"
}
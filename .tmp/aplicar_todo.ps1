$ErrorActionPreference = "Stop"
$base = "C:\Users\Bryan Maradiaga\Documents\Luminal_Scape-Main"

# ============ 1. Colores distintos por boton en MenuScreens.java ============
$p = Join-Path $base "src\main\java\Core\MenuScreens.java"
$c = [System.IO.File]::ReadAllText($p)

# Botones del TITULO - despues de cada new Button(...), agregar setColorInt
$reemplazosTitulo = @(
    @{ old = '        buttonJugar = new Button("Jugar", text);'; new = '        buttonJugar = new Button("Jugar", text);' + [char]10 + '        buttonJugar.setColorInt(34, 139, 34, 255);' },
    @{ old = '        buttonSalir = new Button("Salir", text);'; new = '        buttonSalir = new Button("Salir", text);' + [char]10 + '        buttonSalir.setColorInt(139, 34, 34, 255);' },
    @{ old = '        buttonLevelPrev = new Button("<", text, 20f, 16f, CELDA_FLECHA_LADO);'; new = '        buttonLevelPrev = new Button("<", text, 20f, 16f, CELDA_FLECHA_LADO);' + [char]10 + '        buttonLevelPrev.setColorInt(218, 165, 32, 255);' },
    @{ old = '        buttonLevelNext = new Button(">", text, 20f, 16f, CELDA_FLECHA_LADO);'; new = '        buttonLevelNext = new Button(">", text, 20f, 16f, CELDA_FLECHA_LADO);' + [char]10 + '        buttonLevelNext.setColorInt(218, 165, 32, 255);' },
    @{ old = '        buttonDiffPrev = new Button("<", text, 20f, 16f, CELDA_FLECHA_LADO);'; new = '        buttonDiffPrev = new Button("<", text, 20f, 16f, CELDA_FLECHA_LADO);' + [char]10 + '        buttonDiffPrev.setColorInt(255, 140, 0, 255);' },
    @{ old = '        buttonDiffNext = new Button(">", text, 20f, 16f, CELDA_FLECHA_LADO);'; new = '        buttonDiffNext = new Button(">", text, 20f, 16f, CELDA_FLECHA_LADO);' + [char]10 + '        buttonDiffNext.setColorInt(255, 140, 0, 255);' },
    @{ old = '        buttonConfigTitulo = new Button("Configuracion", text);'; new = '        buttonConfigTitulo = new Button("Configuracion", text);' + [char]10 + '        buttonConfigTitulo.setColorInt(70, 130, 180, 255);' },
    @{ old = '        buttonCerrarMenu = new Button("Continuar", text);'; new = '        buttonCerrarMenu = new Button("Continuar", text);' + [char]10 + '        buttonCerrarMenu.setColorInt(34, 139, 34, 255);' },
    @{ old = '        buttonRestartTime = new Button("Reiniciar Tiempo", text);'; new = '        buttonRestartTime = new Button("Reiniciar Tiempo", text);' + [char]10 + '        buttonRestartTime.setColorInt(255, 140, 0, 255);' },
    @{ old = '        buttonVolverMenu = new Button("Volver al menu principal", text);'; new = '        buttonVolverMenu = new Button("Volver al menu principal", text);' + [char]10 + '        buttonVolverMenu.setColorInt(70, 130, 180, 255);' },
    @{ old = '        buttonReiniciar = new Button("Reiniciar", text);'; new = '        buttonReiniciar = new Button("Reiniciar", text);' + [char]10 + '        buttonReiniciar.setColorInt(34, 139, 34, 255);' },
    @{ old = '        buttonConfigPausa = new Button("Configuracion", text);'; new = '        buttonConfigPausa = new Button("Configuracion", text);' + [char]10 + '        buttonConfigPausa.setColorInt(70, 130, 180, 255);' }
)

foreach ($r in $reemplazosTitulo) {
    if ($c -match [regex]::Escape($r.old)) {
        $c = $c -replace [regex]::Escape($r.old), $r.new
        Write-Host "MenuScreens: color agregado a boton"
    } else {
        Write-Host "MenuScreens: NO se encontro: $($r.old)"
    }
}

[System.IO.File]::WriteAllText($p, $c)
Write-Host "MenuScreens.java: colores de boton aplicados"

# ============ 2. GameLoop.java: camara sigue el nivel al cambiar con <- -> ============
$p2 = Join-Path $base "src\main\java\Core\GameLoop.java"
$c2 = [System.IO.File]::ReadAllText($p2)

# En stepLevel, despues de refreshSelectorLabels(), agregar reconstruccion de puntos de camara
$oldStepLevel = '    private static void stepLevel(int delta) {' + [char]10 + '        levelSession.stepLevel(delta);' + [char]10 + '        loadCurrentLevel();' + [char]10 + '        refreshSelectorLabels();' + [char]10 + '    }'

$newStepLevel = '    private static void stepLevel(int delta) {' + [char]10 + '        levelSession.stepLevel(delta);' + [char]10 + '        loadCurrentLevel();' + [char]10 + '        // Aplicar el layout del nuevo nivel al dungeonManager para que la' + [char]10 + '        // camara aerea del titulo renderice el nivel correcto.' + [char]10 + '        dungeonManager.applyLayout(levelSession.data().toRows(), wallTexture, floorTexture, ceilingTexture, lightCeilingTexture);' + [char]10 + '        dungeonManager.registerCollisions(collisionManager);' + [char]10 + '        // Reconstruir los puntos de interes de la camara (items del nuevo nivel).' + [char]10 + '        if (titleCamera != null) {' + [char]10 + '            titleCamera.reconstruirPuntos();' + [char]10 + '        }' + [char]10 + '        refreshSelectorLabels();' + [char]10 + '    }'

if ($c2 -match [regex]::Escape($oldStepLevel)) {
    $c2 = $c2 -replace [regex]::Escape($oldStepLevel), $newStepLevel
    [System.IO.File]::WriteAllText($p2, $c2)
    Write-Host "GameLoop.java: stepLevel actualizado para cambiar nivel de camara"
} else {
    Write-Host "GameLoop: NO se encontro el patron stepLevel exacto"
    Select-String -Pattern 'private static void stepLevel' -Path $p2 | ForEach-Object { Write-Host $_.LineNumber ": " $_.Line.Trim() }
}
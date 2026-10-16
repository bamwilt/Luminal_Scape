$ErrorActionPreference = "Stop"
$base = "C:\Users\Bryan Maradiaga\Documents\Luminal_Scape-Main"
$p = Join-Path $base "src\main\java\Core\GameLoop.java"
$c = [System.IO.File]::ReadAllText($p)

# Cambiar solo el quad de TITLE (el que sigue a 'case TITLE:')
$old = '            case TITLE:' + [char]10 + '                // Primera capa opaca: nunca se debe ver el fondo 3D detrás del menú.' + [char]10 + '                loteUi.addQuad(0, 0, w, h, 0f, 0f, 0f, 1.0f);'
$new = '            case TITLE:' + [char]10 + '                // Fondo 3D animado: el quad es transparente para que se vea el nivel.' + [char]10 + '                loteUi.addQuad(0, 0, w, h, 0f, 0f, 0f, 0.0f);'

if ($c -match [regex]::Escape($old)) {
    $c = $c -replace [regex]::Escape($old), $new
    [System.IO.File]::WriteAllText($p, $c)
    Write-Host "GameLoop.java: quad de TITLE pasado a 0.0f (transparente)"
} else {
    Write-Host "No se encontro el patron de TITLE. Buscando..."
    # Mostrar las lineas de los quads para depurar
    Select-String -Pattern 'addQuad\(0, 0, w, h' -Path $p | ForEach-Object { Write-Host $_.LineNumber ": " $_.Line }
}
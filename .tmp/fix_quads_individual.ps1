$ErrorActionPreference = "Stop"
$base = "C:\Users\Bryan Maradiaga\Documents\Luminal_Scape-Main"
$p = Join-Path $base "src\main\java\Core\GameLoop.java"
$c = [System.IO.File]::ReadAllText($p)

# PAUSED: 0.0f -> 1.0f con comentario
$oldPaused = '            case PAUSED:' + [char]10 + '                // El minimapa NO se dibuja en pausa. El menǧ va sobre un fondo' + [char]10 + '                // plano, y el plano del nivel encima solo estorba (se solapa con' + [char]10 + '                // los botones y con la pista de controles) y confunde: en pausa' + [char]10 + '                // el nivel ya esta en marcha o ya se ha perdido, y el mapa se' + [char]10 + '                // apaga al salir de uno y de otro (ver winGame/loseGame).' + [char]10 + '                loteUi.addQuad(0, 0, w, h, 0f, 0f, 0f, 0.0f);'
$newPaused = '            case PAUSED:' + [char]10 + '                // El minimapa NO se dibuja en pausa. El menǧ va sobre un fondo' + [char]10 + '                // plano, y el plano del nivel encima solo estorba (se solapa con' + [char]10 + '                // los botones y con la pista de controles) y confunde: en pausa' + [char]10 + '                // el nivel ya esta en marcha o ya se ha perdido, y el mapa se' + [char]10 + '                // apaga al salir de uno y de otro (ver winGame/loseGame).' + [char]10 + '                // Primera capa opaca: nunca se debe ver el fondo 3D detras del menu.' + [char]10 + '                loteUi.addQuad(0, 0, w, h, 0f, 0f, 0f, 1.0f);'
$c = $c -replace [regex]::Escape($oldPaused), $newPaused

# END: 0.0f -> 1.0f con comentario
$oldEnd = '            case END:' + [char]10 + '                loteUi.addQuad(0, 0, w, h, 0f, 0f, 0f, 0.0f);'
$newEnd = '            case END:' + [char]10 + '                // Primera capa opaca: nunca se debe ver el fondo 3D detras del menu.' + [char]10 + '                loteUi.addQuad(0, 0, w, h, 0f, 0f, 0f, 1.0f);'
$c = $c -replace [regex]::Escape($oldEnd), $newEnd

# CONFIG: 0.0f -> 1.0f con comentario
$oldConfig = '            case CONFIG:' + [char]10 + '                loteUi.addQuad(0, 0, w, h, 0f, 0f, 0f, 0.0f);'
$newConfig = '            case CONFIG:' + [char]10 + '                // Primera capa opaca: nunca se debe ver el fondo 3D detras del menu.' + [char]10 + '                loteUi.addQuad(0, 0, w, h, 0f, 0f, 0f, 1.0f);'
$c = $c -replace [regex]::Escape($oldConfig), $newConfig

[System.IO.File]::WriteAllText($p, $c)
Write-Host "Quads de PAUSED, END, CONFIG devueltos a 1.0f. TITLE queda en 0.0f."
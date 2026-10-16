$ErrorActionPreference = "Stop"
$base = "C:\Users\Bryan Maradiaga\Documents\Luminal_Scape-Main"
$p = Join-Path $base "src\main\java\Core\GameLoop.java"
$lineas = [System.IO.File]::ReadAllLines($p)

# Linea 812 (index 811) es el quad de PAUSED en el primer switch
if ($lineas[811] -match 'addQuad\(0, 0, w, h, 0f, 0f, 0f, 0\.0f\)') {
    $lineas[811] = '                // Primera capa opaca: nunca se ve el fondo 3D detras del menu.' + [char]10 + '                loteUi.addQuad(0, 0, w, h, 0f, 0f, 0f, 1.0f);'
    [System.IO.File]::WriteAllLines($p, $lineas)
    Write-Host "Linea 812 (PAUSED) corregida a 1.0f"
} else {
    Write-Host "La linea 812 no coincide: " + $lineas[811]
    # Mostrar lineas 803-813 para debug
    for ($i = 803; $i -le 813; $i++) {
        Write-Host "$($i+1): $($lineas[$i])"
    }
}
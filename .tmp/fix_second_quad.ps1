$ErrorActionPreference = "Stop"
$base = "C:\Users\Bryan Maradiaga\Documents\Luminal_Scape-Main"
$p = Join-Path $base "src\main\java\Core\GameLoop.java"
$c = [System.IO.File]::ReadAllText($p)

$buscar = 'loteUi.addQuad(0, 0, w, h, 0f, 0f, 0f, 0.0f);'
$reemplazar = 'loteUi.addQuad(0, 0, w, h, 0f, 0f, 0f, 1.0f);'

# Encontrar todas las ocurrencias y reemplazar solo la segunda (PAUSED)
$indices = [System.Collections.ArrayList]::new()
$idx = 0
while (($idx = $c.IndexOf($buscar, $idx)) -ge 0) {
    $indices.Add($idx)
    $idx += $buscar.Length
}

Write-Host "Se encontraron $($indices.Count) ocurrencias de quad 0.0f"
for ($i = 0; $i -lt $indices.Count; $i++) {
    Write-Host "  Ocurrencia $i en indice $($indices[$i])"
}

if ($indices.Count -ge 2) {
    # Reemplazar la segunda ocurrencia (indice 1 en el array)
    $pos = $indices[1]
    $c = $c.Substring(0, $pos) + $reemplazar + $c.Substring($pos + $buscar.Length)
    [System.IO.File]::WriteAllText($p, $c)
    Write-Host "Segunda ocurrencia (PAUSED) reemplazada por 1.0f"
} else {
    Write-Host "No hay suficientes ocurrencias"
}
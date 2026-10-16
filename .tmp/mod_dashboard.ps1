$ErrorActionPreference = "Stop"
$base = "C:\Users\Bryan Maradiaga\Documents\Luminal_Scape-Main"
$p = Join-Path $base "src\main\java\Core\MenuScreens.java"
$c = [System.IO.File]::ReadAllText($p)

# Dashboard: fondo 1.00f -> 0.85f (semi-transparente para ver el fondo 3D)
$old = '.setBackground(0.00f, 0.01f, 0.05f, 1.00f)'
$new = '.setBackground(0.00f, 0.01f, 0.05f, 0.85f)'

if ($c -match [regex]::Escape($old)) {
    $c = $c -replace [regex]::Escape($old), $new
    [System.IO.File]::WriteAllText($p, $c)
    Write-Host "MenuScreens.java: dashboard pasado a alpha 0.85 (semi-transparente)"
} else {
    Write-Host "No se encontro el patron del dashboard"
    Select-String -Pattern 'setBackground\(0\.00f, 0\.01f' -Path $p | ForEach-Object { Write-Host $_.LineNumber ": " $_.Line }
}
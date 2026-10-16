$ErrorActionPreference = "Stop"
$base = "C:\Users\Bryan Maradiaga\Documents\Luminal_Scape-Main"
$p = Join-Path $base "src\main\java\Player\Camera.java"
$c = [System.IO.File]::ReadAllText($p)

$marker = '    public void setYaw(float yaw) {' + [char]10 + '        this.yaw = yaw;' + [char]10 + '        updateDirection();' + [char]10 + '        updateViewMatrix();' + [char]10 + '    }'
$insert = $marker + [char]10 + [char]10 + '    /** Fija el pitch (elevacion) de la camara en grados. */' + [char]10 + '    public void setPitch(float pitch) {' + [char]10 + '        this.pitch = Math.max(-90f, Math.min(90f, pitch));' + [char]10 + '        updateDirection();' + [char]10 + '        updateViewMatrix();' + [char]10 + '    }'

if ($c -notcontains 'public void setPitch') {
    $c = $c -replace [regex]::Escape($marker), $insert
    [System.IO.File]::WriteAllText($p, $c)
    Write-Host "Camera.java: setPitch() agregado"
} else {
    Write-Host "Camera.java: setPitch() ya existe"
}
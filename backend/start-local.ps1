$ErrorActionPreference = 'Stop'
$taskPhpCommand = Get-Command php -ErrorAction SilentlyContinue
$taskPhpPath = if ($taskPhpCommand) { $taskPhpCommand.Source } else { 'C:\xampp\php\php.exe' }
$taskAdbPath = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
if (!(Test-Path -LiteralPath $taskPhpPath)) { throw 'PHP no encontrado. Instala PHP o inicia desde XAMPP.' }
if (!(Test-Path -LiteralPath $taskAdbPath)) { throw 'ADB no encontrado en el SDK de Android.' }
& $taskPhpPath (Join-Path $PSScriptRoot 'check-database.php')
if ($LASTEXITCODE -ne 0) { throw 'No se pudo conectar con MySQL. Comprueba que XAMPP esté iniciado.' }
$taskDevices = & $taskAdbPath devices
foreach ($taskLine in $taskDevices) {
    if ($taskLine -match '^(\S+)\s+device$') {
        & $taskAdbPath -s $Matches[1] reverse tcp:8000 tcp:8000
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo configurar la conexión USB.' }
    }
}
Write-Host 'API local: http://127.0.0.1:8000/health.php. Detener con Ctrl+C.'
& $taskPhpPath -S 127.0.0.1:8000 -t (Join-Path $PSScriptRoot 'public')

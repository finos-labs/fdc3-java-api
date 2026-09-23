# Registers the fdc3-java-app:// protocol handler on Windows.
# Usage:
#   .\register-protocol.ps1 -JarPath "C:\path\to\fdc3-example-app.jar"

param(
    [Parameter(Mandatory = $true)]
    [string]$JarPath,

    [string]$JavaPath = (Get-Command java -ErrorAction SilentlyContinue).Source
)

if (-not $JavaPath) {
    throw "java.exe not found on PATH. Pass -JavaPath explicitly."
}

if (-not (Test-Path $JarPath)) {
    throw "JAR not found: $JarPath"
}

$rootKey = "HKCU:\Software\Classes\fdc3-java-app"
New-Item -Path $rootKey -Force | Out-Null
Set-ItemProperty -Path $rootKey -Name "(Default)" -Value "URL:FDC3 Java App Protocol"
New-ItemProperty -Path $rootKey -Name "URL Protocol" -Value "" -PropertyType String -Force | Out-Null

$iconKey = Join-Path $rootKey "DefaultIcon"
New-Item -Path $iconKey -Force | Out-Null
Set-ItemProperty -Path $iconKey -Name "(Default)" -Value "$JavaPath,0"

$commandKey = Join-Path $rootKey "shell\open\command"
New-Item -Path $commandKey -Force | Out-Null
$command = "`"$JavaPath`" -jar `"$JarPath`" `"%1`""
Set-ItemProperty -Path $commandKey -Name "(Default)" -Value $command

Write-Host "Registered fdc3-java-app:// -> $JarPath"

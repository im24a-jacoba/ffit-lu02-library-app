# Start the complete application from any working directory.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$localJdk = Join-Path $env:USERPROFILE '.jdks/corretto-23.0.2'
if (Test-Path (Join-Path $localJdk 'bin/java.exe')) { $env:JAVA_HOME = $localJdk }
if (-not (Test-Path 'config.properties')) { throw 'config.properties fehlt. Bitte die README-Konfiguration ausfüllen.' }
Write-Host 'Bibliothek startet: http://localhost:7070/'
& "$PSScriptRoot/gradlew.bat" runApi --console=plain
exit $LASTEXITCODE

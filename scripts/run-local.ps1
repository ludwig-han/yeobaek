param([int]$Port = 8080)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if (-not $env:GEMINI_API_KEY) {
    $env:GEMINI_API_KEY = [Environment]::GetEnvironmentVariable('GEMINI_API_KEY', 'User')
}
$javaCommand = Get-Command java -ErrorAction SilentlyContinue
if ($javaCommand) {
    $javaPath = $javaCommand.Source
} elseif ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $javaPath = "$env:JAVA_HOME\bin\java.exe"
} else {
    $javaPath = Get-ChildItem 'C:\Program Files\JetBrains\IntelliJ IDEA*\jbr\bin\java.exe' -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $javaPath) { throw 'Java 17 이상을 설치하거나 JAVA_HOME을 설정해주세요.' }
if (-not (Test-Path 'target/app.jar')) { throw '먼저 Maven verify로 target/app.jar를 빌드해주세요.' }
Write-Host "여백 실행: http://localhost:$Port (종료: Ctrl+C)"
& $javaPath -jar target/app.jar "--server.port=$Port"

# ====================================================================
# SmartATS Standalone WAR Builder (PowerShell)
# ====================================================================

if (-not (Test-Path "target")) {
    New-Item -ItemType Directory -Path "target" | Out-Null
}

Push-Location "src\main\webapp"
& "C:\Program Files\Java\jdk-24\bin\jar.exe" -cvf "..\..\target\smart-ats.war" * | Out-Null
Pop-Location

Write-Host "[SUCCESS] WAR built at target\smart-ats.war" -ForegroundColor Green
Get-Item "target\smart-ats.war" | Select-Object Name, Length, LastWriteTime

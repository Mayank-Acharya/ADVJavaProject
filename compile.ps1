# ====================================================================
# SmartATS Compile Script (PowerShell)
# ====================================================================

if (-not (Test-Path "target\classes")) {
    New-Item -ItemType Directory -Path "target\classes" | Out-Null
}

$javaFiles = Get-ChildItem -Path "src\main\java" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
$classpath = "C:\apache-tomcat-10.1.60\lib\*;src\main\webapp\WEB-INF\lib\*"

Write-Host "Compiling Java classes..." -ForegroundColor Cyan
& "C:\Program Files\Java\jdk-24\bin\javac.exe" -encoding UTF-8 -cp $classpath -d "target\classes" $javaFiles

if ($LASTEXITCODE -eq 0) {
    Copy-Item "src\main\resources\*" "target\classes\" -Force
    Write-Host "[BUILD SUCCESS] All Java classes compiled cleanly!" -ForegroundColor Green
} else {
    Write-Host "[BUILD FAILED] Compilation errors found." -ForegroundColor Red
}

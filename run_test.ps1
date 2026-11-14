# ====================================================================
# SmartATS Automated Verification Test (PowerShell Runner)
# ====================================================================

if (-not (Test-Path "target\test-classes")) {
    New-Item -ItemType Directory -Path "target\test-classes" | Out-Null
}

$classpath = "target\classes;src\main\webapp\WEB-INF\lib\*;C:\apache-tomcat-10.1.60\lib\*"

Write-Host "[1/2] Compiling MainDemoTest.java..." -ForegroundColor Cyan
& "C:\Program Files\Java\jdk-24\bin\javac.exe" -encoding UTF-8 -cp $classpath -d "target\test-classes" "src\test\java\com\smartats\test\MainDemoTest.java"

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Compilation failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[2/2] Running MainDemoTest (Scenario: Company A & Candidates a, b, c)...`n" -ForegroundColor Green
& "C:\Program Files\Java\jdk-24\bin\java.exe" -cp "target\test-classes;target\classes;src\main\webapp\WEB-INF\lib\*;C:\apache-tomcat-10.1.60\lib\*" com.smartats.test.MainDemoTest

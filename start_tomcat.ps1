# ====================================================================
# SmartATS: Start Apache Tomcat (PowerShell)
# ====================================================================

$env:JAVA_HOME = "C:\Program Files\Java\jdk-24"
$env:CATALINA_HOME = "C:\apache-tomcat-10.1.60"

Write-Host "[1/2] Verifying deployment in Tomcat webapps..." -ForegroundColor Cyan
if (-not (Test-Path "C:\apache-tomcat-10.1.60\webapps\smart-ats.war")) {
    Copy-Item "target\smart-ats.war" "C:\apache-tomcat-10.1.60\webapps\smart-ats.war" -Force
}

Write-Host "[2/2] Launching Apache Tomcat 10 from bin directory..." -ForegroundColor Green
Start-Process -FilePath "C:\apache-tomcat-10.1.60\bin\startup.bat" -WorkingDirectory "C:\apache-tomcat-10.1.60\bin"

Write-Host "`n====================================================================" -ForegroundColor Yellow
Write-Host "  Tomcat server process launched in a new window!" -ForegroundColor Yellow
Write-Host "  Wait ~3-5 seconds for webapp deployment, then check your browser." -ForegroundColor Cyan
Write-Host "  URL: http://localhost:8080/smart-ats/" -ForegroundColor Green
Write-Host "====================================================================`n" -ForegroundColor Yellow

Start-Sleep -Seconds 3
Start-Process "http://localhost:8080/smart-ats/"

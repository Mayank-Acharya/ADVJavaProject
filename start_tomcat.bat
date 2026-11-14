@echo off
setlocal

echo ====================================================================
echo   SmartATS: Starting Apache Tomcat Server
echo ====================================================================

set "JAVA_HOME=C:\Program Files\Java\jdk-24"
set "CATALINA_HOME=C:\apache-tomcat-10.1.60"

echo [1/2] Verifying deployment in Tomcat webapps...
if not exist "C:\apache-tomcat-10.1.60\webapps\smart-ats.war" (
    echo Copying WAR package to webapps...
    copy /Y "target\smart-ats.war" "C:\apache-tomcat-10.1.60\webapps\smart-ats.war"
)

echo [2/2] Launching Tomcat from %CATALINA_HOME%\bin...
cd /d "%CATALINA_HOME%\bin"
start "Apache Tomcat 10 (SmartATS)" startup.bat
cd /d "%~dp0"

echo.
echo ====================================================================
echo   Tomcat server process launched!
echo   Wait 3-5 seconds for Tomcat to initialize, then open:
echo   http://localhost:8080/smart-ats/
echo ====================================================================
start http://localhost:8080/smart-ats/

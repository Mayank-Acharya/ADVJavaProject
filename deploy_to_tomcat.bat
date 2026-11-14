@echo off
setlocal enabledelayedexpansion

echo ====================================================================
echo   SmartATS: DEPLOYING TO APACHE TOMCAT 10
echo ====================================================================

if not exist src\main\webapp\WEB-INF\classes mkdir src\main\webapp\WEB-INF\classes

echo [1/3] Copying compiled classes and resources into WEB-INF/classes...
xcopy /E /I /Y target\classes\* src\main\webapp\WEB-INF\classes\ >nul

echo [2/3] Deploying exploded webapp to C:\apache-tomcat-10.1.60\webapps\smart-ats...
if not exist "C:\apache-tomcat-10.1.60\webapps\smart-ats" mkdir "C:\apache-tomcat-10.1.60\webapps\smart-ats"
xcopy /E /I /Y src\main\webapp\* "C:\apache-tomcat-10.1.60\webapps\smart-ats\" >nul

echo [3/3] Generating standalone WAR archive at target\smart-ats.war...
cd src\main\webapp
"C:\Program Files\Java\jdk-24\bin\jar.exe" -cvf "..\..\target\smart-ats.war" * >nul
cd ..\..

echo.
echo ====================================================================
echo   [SUCCESS] Deployment complete!
echo   Webapp Path: C:\apache-tomcat-10.1.60\webapps\smart-ats
echo   WAR Archive: target\smart-ats.war
echo.
echo   To launch Tomcat:
echo     C:\apache-tomcat-10.1.60\bin\startup.bat
echo   Then open in browser:
echo     http://localhost:8080/smart-ats/
echo ====================================================================

@echo off
if not exist target mkdir target
cd src\main\webapp
"C:\Program Files\Java\jdk-24\bin\jar.exe" -cvf ..\..\target\smart-ats.war *
cd ..\..
echo WAR built successfully at target\smart-ats.war!
dir target\smart-ats.war

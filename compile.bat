@echo off
setlocal enabledelayedexpansion

if not exist target\classes mkdir target\classes

dir /s /b src\main\java\*.java > sources.txt

"C:\Program Files\Java\jdk-24\bin\javac.exe" -encoding UTF-8 -cp "C:\apache-tomcat-10.1.60\lib\*" -d target\classes @sources.txt

if %ERRORLEVEL% equ 0 (
    echo [BUILD SUCCESS] All Java classes compiled cleanly!
    copy /Y src\main\resources\* target\classes\
) else (
    echo [BUILD FAILED] Compilation errors found.
)
del sources.txt

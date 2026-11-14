@echo off
setlocal enabledelayedexpansion

if not exist target\test-classes mkdir target\test-classes

set CLASSPATH=target\classes;src\main\webapp\WEB-INF\lib\*;C:\apache-tomcat-10.1.60\lib\*

echo [1/2] Compiling MainDemoTest.java...
"C:\Program Files\Java\jdk-24\bin\javac.exe" -encoding UTF-8 -cp "%CLASSPATH%" -d target\test-classes src\test\java\com\smartats\test\MainDemoTest.java

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Test compilation failed!
    exit /b %ERRORLEVEL%
)

echo [2/2] Running MainDemoTest (Scenario: Company A ^& Candidates a, b, c)...
"C:\Program Files\Java\jdk-24\bin\java.exe" -cp "target\test-classes;target\classes;src\main\webapp\WEB-INF\lib\*;C:\apache-tomcat-10.1.60\lib\*" com.smartats.test.MainDemoTest

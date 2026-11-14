@echo off
if not exist src\main\webapp\WEB-INF\lib mkdir src\main\webapp\WEB-INF\lib

for /R "C:\Users\hp\.m2\repository" %%F in (*.jar) do (
    echo %%~nxF | findstr /I "sources javadoc" >nul
    if errorlevel 1 (
        copy /Y "%%F" "src\main\webapp\WEB-INF\lib\" >nul
    )
)
echo All runtime JARs copied into src\main\webapp\WEB-INF\lib.
dir src\main\webapp\WEB-INF\lib\*.jar

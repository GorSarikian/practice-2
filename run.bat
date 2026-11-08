@echo off
setlocal
cd /d "%~dp0"
call powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1"
if errorlevel 1 exit /b 1
if defined JAVA_HOME (
    "%JAVA_HOME%\bin\java.exe" -Dfile.encoding=UTF-8 -cp build/classes nugetgraph.Main %*
) else (
    java -Dfile.encoding=UTF-8 -cp build/classes nugetgraph.Main %*
)
exit /b %errorlevel%

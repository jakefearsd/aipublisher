@echo off
REM AI Publisher - Windows command line wrapper script
REM Usage: aipublisher [options]

setlocal

REM Get the directory where this script is located
set SCRIPT_DIR=%~dp0

REM Find the JAR file
set JAR_FILE=%SCRIPT_DIR%aipublisher.jar

if not exist "%JAR_FILE%" (
    if exist "%SCRIPT_DIR%..\lib\aipublisher.jar" (
        set JAR_FILE=%SCRIPT_DIR%..\lib\aipublisher.jar
    ) else if exist "%SCRIPT_DIR%target\aipublisher.jar" (
        set JAR_FILE=%SCRIPT_DIR%target\aipublisher.jar
    ) else (
        echo Error: Could not find aipublisher.jar
        echo Expected location: %JAR_FILE%
        exit /b 1
    )
)

REM Check for Java
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 21 or later
    exit /b 1
)

REM Run the application
java -jar "%JAR_FILE%" %*

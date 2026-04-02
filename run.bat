@echo off
setlocal enabledelayedexpansion

:: ============================================================
:: Chuka University Voting System - Windows Build & Run Script
:: ============================================================

set "PROJECT_ROOT=%~dp0"
set "SRC_DIR=%PROJECT_ROOT%src"
set "LIB_DIR=%PROJECT_ROOT%lib"
set "OUT_DIR=%PROJECT_ROOT%out\production"
set "RES_DIR=%PROJECT_ROOT%resources"

:: Check for Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java is not installed or not in PATH. Please install JDK 17+.
    pause
    exit /b 1
)

:: Check for Docker
docker version >nul 2>&1
if %errorlevel% neq 0 (
    echo Warning: Docker not found. Please ensure Docker Desktop is installed and running.
) else (
    echo Starting database containers...
    docker-compose up -d
)

:: Build Classpath
set "CP=."
for %%i in ("%LIB_DIR%\*.jar") do (
    set "CP=!CP!;%%i"
)

if "%1"=="run" goto run_app
if "%1"=="compile" goto compile

:all
call :compile
if %errorlevel% equ 0 call :run_app
goto :eof

:compile
echo Compiling Java sources...
if exist "%OUT_DIR%" rd /s /q "%OUT_DIR%"
mkdir "%OUT_DIR%"

dir /s /b "%SRC_DIR%\*.java" > sources.txt
javac -cp "!CP!" -d "%OUT_DIR%" @sources.txt
del sources.txt

if exist "%RES_DIR%" (
    xcopy /e /i /y "%RES_DIR%" "%OUT_DIR%" >nul
)

echo Compilation successful!
exit /b 0

:run_app
echo Starting Chuka University Voting System...

:: DB Defaults
set "DB_HOST=localhost"
set "DB_PORT=3308"
set "DB_NAME=chuka_voting_db"
set "DB_USER=root"
set "DB_PASSWORD=chuka_root_2024"

java -cp "%OUT_DIR%;!CP!" main.Main
exit /b 0

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

:: check_dependencies
echo 🔍 Checking system dependencies...

:: 1. Check for Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java is missing.
    echo Attempting to install Microsoft OpenJDK 17 via winget...
    winget install Microsoft.OpenJDK.17
    if %errorlevel% neq 0 (
        call :show_manual_java_guide
        pause
        exit /b 1
    )
    echo ✅ Java installed successfully.
)

:: 2. Check for Docker
docker version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker not found or not running.
    call :show_manual_docker_guide
    pause
    exit /b 1
)

:: 3. Port Check (3308)
:check_port
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :3308 ^| findstr LISTENING') do (
    set "PID=%%a"
    echo ⚠️  Port 3308 is occupied by another process (PID: !PID!).
    
    set /p choice="Do you want me to terminate this process automatically? (y/n): "
    if /i "!choice!"=="y" (
        echo Cleaning port 3308...
        taskkill /F /PID !PID!
        echo ✅ Port cleared.
        goto :check_port
    ) else (
        echo Please stop the process manually and restart.
        pause
        exit /b 1
    )
)

echo Starting database containers...
docker-compose up -d

:: Wait for DB (Port 3308 to be ready)
echo Waiting for database to initialize (10s)...
timeout /t 10 /nobreak >nul

echo ✅ Dependencies verified.
goto :after_dependencies

:show_manual_java_guide
    echo.
    echo --------------------------------------------------------
    echo 🛠️  MANUAL WINDOWS JAVA SETUP GUIDE
    echo --------------------------------------------------------
    echo 1. Download OpenJDK 17 MSI from: https://adoptium.net/
    echo 2. Run the .msi installer.
    echo 3. ENSURE YOU CHECK 'Add to PATH' and 'Set JAVA_HOME'
    echo    during the installation steps.
    echo 4. Restart your Command Prompt and run this script.
    echo --------------------------------------------------------
    exit /b 0

:show_manual_docker_guide
    echo.
    echo --------------------------------------------------------
    echo 🛠️  MANUAL WINDOWS DOCKER SETUP GUIDE
    echo --------------------------------------------------------
    echo 1. Download Docker Desktop: https://www.docker.com/
    echo 2. Run the installer and enable 'WSL 2' when prompted.
    echo 3. Log out and log back in to Windows.
    echo 4. Launch Docker Desktop and wait for it to be 'Running'.
    echo --------------------------------------------------------
    exit /b 0

:after_dependencies

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

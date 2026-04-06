#!/usr/bin/env bash
set -euo pipefail

compose_cmd() {
    if command -v docker-compose >/dev/null 2>&1; then
        docker-compose "$@"
    elif docker compose version >/dev/null 2>&1; then
        docker compose "$@"
    else
        echo "❌ Docker Compose not found. Install docker-compose or Docker Compose plugin."
        return 1
    fi
}

# System Requirements Check
check_dependencies() {
    echo "🔍 Checking system dependencies..."

    # 1. Java JDK Check
    if ! command -v java >/dev/null 2>&1 || ! command -v javac >/dev/null 2>&1; then
        echo "❌ Java JDK is missing (java/javac not found)."
        if command -v apt-get >/dev/null 2>&1; then
            echo "Attempting to install OpenJDK 17 via apt..."
            sudo apt-get update && sudo apt-get install -y openjdk-17-jdk
        elif command -v brew >/dev/null 2>&1; then
            echo "Attempting to install OpenJDK 17 via Homebrew..."
            brew install openjdk@17
        else
            show_manual_java_guide
            exit 1
        fi
    fi

    # 2. Docker & Compose Check
    if ! command -v docker >/dev/null 2>&1; then
        echo "❌ Docker not found."
        if command -v apt-get >/dev/null 2>&1; then
            echo "Installing Docker via apt..."
            sudo apt-get update && sudo apt-get install -y docker.io docker-compose
            sudo systemctl start docker && sudo systemctl enable docker
            sudo usermod -aG docker $USER
            echo "✅ Docker installed. Please restart your terminal session to apply group changes."
        else
            show_manual_docker_guide
            exit 1
        fi
    fi

    if ! command -v docker-compose >/dev/null 2>&1 && ! docker compose version >/dev/null 2>&1; then
        echo "❌ Docker Compose not found (neither docker-compose nor docker compose plugin available)."
        show_manual_docker_guide
        exit 1
    fi

    # 3. Port Check (3308)
    solve_port_conflict

    echo "✅ Dependencies verified."
}

show_manual_java_guide() {
    echo "--------------------------------------------------------"
    echo "🛠️  MANUAL JAVA SETUP GUIDE"
    echo "--------------------------------------------------------"
    echo "1. Go to https://adoptium.net/ and download OpenJDK 17 (LTS)."
    echo "2. Extract the file to a folder (e.g., /opt/java/ or C:\Java\)."
    echo "3. Add the 'bin' folder to your system PATH."
    echo "4. Set the JAVA_HOME environment variable to the root folder."
    echo "5. Restart your terminal and run this script again."
    echo "--------------------------------------------------------"
}

show_manual_docker_guide() {
    echo "--------------------------------------------------------"
    echo "🛠️  MANUAL DOCKER SETUP GUIDE"
    echo "--------------------------------------------------------"
    echo "1. Visit https://www.docker.com/products/docker-desktop/"
    echo "2. Download and run the installer for your OS."
    echo "3. Follow the wizard, and ensure 'Use WSL 2' is checked if on Windows."
    echo "4. Launch Docker Desktop and wait for the green 'Running' icon."
    echo "--------------------------------------------------------"
}

solve_port_conflict() {
    local PORT=3308
    if ! command -v lsof >/dev/null 2>&1; then
        echo "⚠️  'lsof' not found; skipping automatic port $PORT conflict detection."
        return
    fi

    local PID=$(lsof -Pi :$PORT -sTCP:LISTEN -t)
    
    if [ -n "$PID" ]; then
        if ! docker ps | grep -q "$PORT"; then
            echo "⚠️  Port $PORT is occupied by another process (PID: $PID)."
            ps -p $PID -f | sed '1d'
            
            read -p "Do you want me to terminate this process automatically? (y/n): " choice
            if [[ "$choice" == "y" || "$choice" == "Y" ]]; then
                echo "Cleaning port $PORT..."
                kill -9 $PID
                echo "✅ Port cleared."
            else
                echo "Please stop the process manually and restart."
                exit 1
            fi
        fi
    fi
}

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$PROJECT_ROOT/src"
LIB_DIR="$PROJECT_ROOT/lib"
OUT_DIR="$PROJECT_ROOT/out/production"
RES_DIR="$PROJECT_ROOT/resources"

# Setup classpath
CLASSPATH=""
for jar in "$LIB_DIR"/*.jar; do
    [ -e "$jar" ] || continue
    if [ -z "$CLASSPATH" ]; then
        CLASSPATH="$jar"
    else
        CLASSPATH="$CLASSPATH:$jar"
    fi
done

compile() {
    echo "Compiling Java sources..."
    rm -rf "$OUT_DIR"
    mkdir -p "$OUT_DIR"

    # Find all .java files and store in a local file for transparency
    find "$SRC_DIR" -name "*.java" > sources.txt

    # Compile with all library JARs on classpath
    if javac -cp "$CLASSPATH" -d "$OUT_DIR" @sources.txt; then
        echo "✅ Compilation successful!"
        rm sources.txt

        # Copy resources
        if [ -d "$RES_DIR" ]; then
            cp -r "$RES_DIR"/* "$OUT_DIR/" 2>/dev/null || true
        fi
    else
        echo "❌ Compilation failed!"
        rm sources.txt
        exit 1
    fi
}

wait_for_database() {
    local host="${DB_HOST:-localhost}"
    local port="${DB_PORT:-3308}"
    local max_attempts=30

    echo "Waiting for database to be reachable at $host:$port..."
    for ((i=1; i<=max_attempts; i++)); do
        if (echo >"/dev/tcp/$host/$port") >/dev/null 2>&1; then
            echo "✅ Database is reachable."
            return 0
        fi
        sleep 2
    done

    echo "❌ Database is not reachable after $((max_attempts * 2)) seconds."
    return 1
}

run_app() {
    echo "Starting Chuka University Voting System..."

    if [ ! -f "$OUT_DIR/main/Main.class" ]; then
        echo "No compiled classes found. Running compile first..."
        compile
    fi
    
    # Set DB connection via environment variables (defaults work with docker-compose)
    export DB_HOST="${DB_HOST:-localhost}"
    export DB_PORT="${DB_PORT:-3308}"
    export DB_NAME="${DB_NAME:-chuka_voting_db}"
    export DB_USER="${DB_USER:-root}"
    export DB_PASSWORD="${DB_PASSWORD:-chuka_root_2024}"

    java -cp "$OUT_DIR:$CLASSPATH" main.Main
}

case "${1:-all}" in
    compile)
        compile
        ;;
    run)
        run_app
        ;;
    setup)
        check_dependencies
        compose_cmd up -d
        wait_for_database
        echo "Environment is ready."
        ;;
    all|*)
        check_dependencies
        compose_cmd up -d
        wait_for_database
        
        compile
        run_app
        ;;
esac

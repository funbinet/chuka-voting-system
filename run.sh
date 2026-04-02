set -e

# System Requirements Check
check_dependencies() {
    echo "🔍 Checking system dependencies..."

    # 1. Java JDK Check
    if ! command -v java >/dev/null 2>&1; then
        echo "❌ Java is missing."
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

    # Find all .java files
    find "$SRC_DIR" -name "*.java" > /tmp/chuka_sources.txt

    # Compile with all library JARs on classpath
    javac -cp "$CLASSPATH" -d "$OUT_DIR" @/tmp/chuka_sources.txt

    # Copy resources
    if [ -d "$RES_DIR" ]; then
        cp -r "$RES_DIR"/* "$OUT_DIR/" 2>/dev/null || true
    fi

    echo "Compilation successful!"
}

run_app() {
    echo "Starting Chuka University Voting System..."
    
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
        docker-compose up -d
        echo "Environment is ready."
        ;;
    all|*)
        check_dependencies
        # Ensure DB is up
        if command -v docker-compose >/dev/null 2>&1; then
            docker-compose up -d
        elif docker compose version >/dev/null 2>&1; then
            docker compose up -d
        fi
        
        echo "Waiting for database to initialize (10s)..."
        sleep 10
        
        compile
        run_app
        ;;
esac

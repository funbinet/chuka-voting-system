set -e

# System Requirements Check
check_dependencies() {
    if ! command -v java >/dev/null 2>&1; then
        echo "Error: Java is not installed. Please install OpenJDK 17 or higher."
        exit 1
    fi

    if ! command -v docker >/dev/null 2>&1; then
        echo "Docker not found. Attempting to install Docker..."
        if command -v apt-get >/dev/null 2>&1; then
            sudo apt-get update
            sudo apt-get install -y docker.io docker-compose
            sudo systemctl start docker
            sudo systemctl enable docker
            sudo usermod -aG docker $USER
            echo "Docker installed successfully. You might need to restart your terminal or logout/login for group changes."
        else
            echo "Automated Docker installation only supported on Debian/Ubuntu. Please install Docker manually."
            exit 1
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
        compile
        run_app
        ;;
esac

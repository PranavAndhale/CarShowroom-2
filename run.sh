#!/bin/bash
# AutoElite Car Showroom Build & Run Script

echo "======================================"
echo "  AutoElite Car Showroom System"
echo "======================================"

SRC="src"
BIN="bin"
LIB="lib"
JFX="lib/javafx-sdk/lib"

mkdir -p "$BIN"

# Check for Java
if ! command -v javac &> /dev/null; then
    echo "[ERROR] Java not found. Please install JDK 11 or higher."
    exit 1
fi

echo "[INFO] Java version: $(java -version 2>&1 | head -1)"

# Build classpath
CP="$BIN"
if [ -d "$LIB" ]; then
    for jar in "$LIB"/*.jar; do
        CP="$CP:$jar"
    done
fi

# JavaFX arguments
JFX_ARGS=""
if [ -d "$JFX" ]; then
    echo "[INFO] JavaFX SDK found at $JFX."
    JFX_ARGS="--module-path $JFX --add-modules javafx.controls,javafx.swing,javafx.web"
else
    echo "[WARNING] JavaFX SDK not found at $JFX. 3D Configurator may not compile."
fi

echo "[BUILD] Compiling sources..."
find "$SRC" -name "*.java" | xargs javac -d "$BIN" -cp "$CP" $JFX_ARGS -encoding UTF-8 2>&1

if [ $? -ne 0 ]; then
    echo "[ERROR] Compilation failed!"
    exit 1
fi

echo "[BUILD] Compilation successful!"
echo "[RUN] Starting AutoElite..."
echo ""
echo "Database: Attempts MySQL (localhost:3306/car_showroom)"
echo "Fallback: H2 in-memory database if MySQL unavailable"
echo ""

java -cp "$CP" $JFX_ARGS -Dfile.encoding=UTF-8 ui.CarShowroomApp

#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "Project root: $SCRIPT_DIR"

echo ""
echo "Checking for Java..."

USE_FALLBACK=false

# Check if java exists AND is real (not macOS stub)
if command -v java >/dev/null 2>&1; then
    if java -version >/dev/null 2>&1; then
        echo "System Java is valid."
    else
        echo "System Java is NOT valid (macOS stub)."
        USE_FALLBACK=true
    fi
else
    echo "Java executable not found."
    USE_FALLBACK=true
fi

# Fallback JDK
FALLBACK_JDK="/Users/$USER/java/jdk/jdk-25.0.1+8/Contents/Home"

if [ "$USE_FALLBACK" = true ]; then
    echo "Trying fallback JDK..."
    if [ -d "$FALLBACK_JDK" ]; then
        export JAVA_HOME="$FALLBACK_JDK"
        export PATH="$JAVA_HOME/bin:$PATH"
        echo "Using fallback JDK at $JAVA_HOME"
    else
        echo "ERROR: No Java found, and fallback JDK not installed at:"
        echo "  $FALLBACK_JDK"
        exit 1
    fi
fi

echo ""
echo "Java version:"
java -version || true

# ----------------------
# Run Node tasks
# ----------------------
echo ""
cd "$SCRIPT_DIR/src/amtrak-api"
echo "Running: npm run update"
npm run update

echo "Running: npm run site"
npm run site

# ----------------------
# Compile Java
# ----------------------
echo ""
cd "$SCRIPT_DIR"
echo "Compiling Java..."

javac -cp lib/gson-2.10.1.jar src/main/java/app/*.java

# ----------------------
# Run the App
# ----------------------
echo ""
echo "Running Java program..."
java -cp "lib/gson-2.10.1.jar:src/main/java" app.Startup

echo ""
echo "Done with test! Running Main.java..."
java -cp "lib/gson-2.10.1.jar:src/main/java" app.Main

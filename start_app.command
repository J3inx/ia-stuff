#!/bin/bash
set -e

# ----------------------------
# Project root
# ----------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "Project root: $SCRIPT_DIR"

# ----------------------------
# Bundled JDK + JavaFX inside project
# ----------------------------
BUNDLED_JDK="$SCRIPT_DIR/lib/jdk/jdk-17.0.16.jdk/Contents/Home"
BUNDLED_FX="$SCRIPT_DIR/lib/javafx/javafx-sdk-17.0.17/lib"

# ----------------------------
# Node tasks
# ----------------------------
cd "$SCRIPT_DIR/src/amtrak-api"
npm run update
npm run site

# ----------------------------
# Helper: compile & run with a given JDK + FX
# ----------------------------
try_java() {
    local JDK_HOME="$1"
    local FX_LIB="$2"

    export JAVA_HOME="$JDK_HOME"
    export PATH="$JAVA_HOME/bin:$PATH"

    echo ""
    echo "Using Java: $JAVA_HOME"
    java -version || true

    echo ""
    echo "Compiling Java..."
    cd "$SCRIPT_DIR" || exit 1
    find src/main/java/app -name "*.java" > sources.txt

    if ! javac \
        --module-path "$FX_LIB" \
        --add-modules javafx.controls,javafx.fxml,javafx.web,javafx.swing \
        -cp lib/gson-2.10.1.jar \
        @sources.txt
    then
        return 1
    fi

    echo "Running Startup test..."
    if ! java \
        --module-path "$FX_LIB" \
        --add-modules javafx.controls,javafx.fxml,javafx.web,javafx.swing \
        -cp "lib/gson-2.10.1.jar:src/main/java" \
        app.Startup
    then
        return 1
    fi

    echo "Running Main GUI..."
    java \
        --module-path "$FX_LIB" \
        --add-modules javafx.controls,javafx.fxml,javafx.web,javafx.swing \
        -cp "lib/gson-2.10.1.jar:src/main/java" \
        app.Main

    return 0
}

# ----------------------------
# Try system Java first (macOS-friendly)
# ----------------------------
if command -v java >/dev/null 2>&1 && java -version >/dev/null 2>&1; then
    SYSTEM_JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null || echo "")
    if [ -n "$SYSTEM_JAVA_HOME" ]; then
        JAVAFX_LIB="$BUNDLED_FX"   # bundled FX path
        if try_java "$SYSTEM_JAVA_HOME" "$JAVAFX_LIB"; then
            echo "System Java succeeded."
            # Only try system Java for non-UI tasks
        if try_java "$SYSTEM_JAVA_HOME" "$BUNDLED_FX"; then
            echo "System Java succeeded for CLI tasks."
        else
            echo "System Java failed for CLI tasks."
        fi

        # Then run GUI explicitly with bundled JDK + FX
        echo "Running GUI with bundled JDK + JavaFX..."
        try_java "$BUNDLED_JDK" "$BUNDLED_FX"

            exit 0
        else
            echo "System Java failed, falling back to bundled JDK..."
        fi
    else
        echo "System Java detected but JAVA_HOME could not be resolved, using bundled JDK..."
    fi
fi


# ----------------------------
# Use bundled JDK + FX
# ----------------------------
if [ ! -d "$BUNDLED_JDK" ]; then
    echo "ERROR: Bundled JDK not found at $BUNDLED_JDK"
    exit 1
fi
if [ ! -d "$BUNDLED_FX" ]; then
    echo "ERROR: Bundled JavaFX not found at $BUNDLED_FX"
    exit 1
fi

if try_java "$BUNDLED_JDK" "$BUNDLED_FX"; then
    echo "Bundled JDK + JavaFX succeeded."
else
    echo "ERROR: Even bundled JDK + JavaFX failed."
    exit 1
fi

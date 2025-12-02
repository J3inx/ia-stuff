#!/bin/bash
set -e

# Determine the directory of this script (project root)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "Project root: $SCRIPT_DIR"

# ----------------------
# Navigate to amtrak-api folder
# ----------------------
echo ""
cd "$SCRIPT_DIR/src/amtrak-api"

echo "Running: npm run update"
npm run update

echo "Running: npm run site"
npm run site

echo "Opening generated site..."
#open "docs/index.html" 2>/dev/null || open "src/site/build/index.html" 2>/dev/null || echo "Could not find index.html"

# ----------------------
# Compile Java program
# ----------------------
echo ""
cd "$SCRIPT_DIR"
javac -cp lib/gson-2.10.1.jar src/main/java/app/*.java

# ----------------------
# Run Java program
# ----------------------
echo ""
java -cp lib/gson-2.10.1.jar:src/main/java app.Startup

echo ""
echo "Done!"

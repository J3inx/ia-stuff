#!/bin/bash
set -e

echo "Setting JAVA_HOME..."
export JAVA_HOME="/Users/bw26168/java/jdk/jdk-25.0.1+8/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

echo "JAVA_HOME set to: $JAVA_HOME"

echo ""
echo "Navigating to amtrak-api folder..."
cd "/Users/bw26168/Desktop/ia-work/src/amtrak-api"

echo ""
echo "Running: npm run update"
npm run update

echo ""
echo "Running: npm run site"
npm run site

echo ""
echo "Opening generated site..."
#open "docs/index.html" 2>/dev/null || open "src/site/build/index.html" 2>/dev/null || echo "Could not find index.html"

echo ""
echo "Compiling Java program..."
cd "/Users/bw26168/Desktop/ia-work"
javac -cp lib/gson-2.10.1.jar src/main/java/app/*.java

echo ""
echo "Running Java program..."
java -cp lib/gson-2.10.1.jar:src/main/java app.Startup

echo ""
echo "Done!"

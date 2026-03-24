#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
#  Mini OS Simulator — Build Script  (no Maven required)
#  Requirements: JDK 17+
# ─────────────────────────────────────────────────────────────────────────────
set -e

SRC="src/main/java"
OUT="out/production"
JAR="mini-os-simulator.jar"

echo "━━━ Mini OS Simulator — Build ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if command -v javac &>/dev/null; then
    JAVAC=javac; JAVA=java
elif [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVAC="$JAVA_HOME/bin/javac"; JAVA="$JAVA_HOME/bin/java"
else
    echo "[ERROR] javac not found. Install JDK 17+ or set JAVA_HOME."
    exit 1
fi

echo "  JDK : $($JAVAC -version 2>&1)"
echo "  Src : $SRC"
echo "  Out : $OUT"
echo ""

mkdir -p "$OUT"

echo "[1/3] Compiling sources..."
find "$SRC" -name "*.java" | xargs "$JAVAC" --release 21 -d "$OUT"
echo "      ✓ $(find $OUT -name '*.class' | wc -l) class files generated."

echo "[2/3] Creating JAR..."
mkdir -p out/meta
echo "Main-Class: Main" > out/meta/MANIFEST.MF
(cd "$OUT" && jar cfm "../../$JAR" ../meta/MANIFEST.MF .)
echo "      ✓ $JAR created ($(du -k $JAR | cut -f1) KB)."

echo "[3/3] Build complete."
echo ""
echo "  Launch GUI         : java -jar $JAR"
echo "  Launch CLI shell   : java -jar $JAR --cli"
echo "  Run a script       : echo 'run scripts/demo_full.ossim' | java -jar $JAR --cli"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

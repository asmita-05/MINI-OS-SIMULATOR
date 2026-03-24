#!/usr/bin/env bash
# Launch the GUI (default) or pass --cli for the text shell
JAR="mini-os-simulator.jar"
if [ ! -f "$JAR" ]; then
    echo "[INFO] Building first..."
    ./build.sh
fi
if command -v java &>/dev/null; then JAVA=java
elif [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then JAVA="$JAVA_HOME/bin/java"
else echo "[ERROR] java not found."; exit 1; fi
"$JAVA" -jar "$JAR" "$@"

#!/usr/bin/env bash
set -e
echo "============================================================================"
echo "  Building StructureRadar Fabric .jar files (1.16.5 to 26.2)"
echo "============================================================================"

if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    ./gradlew chiseledBuild --no-daemon
else
    gradle chiseledBuild --no-daemon
fi

mkdir -p OUTPUT_JARS
find versions -path "*/build/libs/*.jar" ! -name "*-sources.jar" -exec cp {} OUTPUT_JARS/ \; 2>/dev/null || true
find build/libs -name "*.jar" ! -name "*-sources.jar" -exec cp {} OUTPUT_JARS/ \; 2>/dev/null || true

echo ""
echo "[SUCCESS] All compiled .jar mods are ready in ./OUTPUT_JARS/"
echo "Drop the .jar for your Minecraft version into your .minecraft/mods/ folder."

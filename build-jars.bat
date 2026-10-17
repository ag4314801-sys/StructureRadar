@echo off
title StructureRadar Multi-Version .JAR Builder (1.16.5 - 26.2)
echo ============================================================================
echo   Building StructureRadar Fabric .jar files (1.16.5 to 26.2)
echo ============================================================================
echo.

if exist gradlew.bat (
    call gradlew.bat chiseledBuild --no-daemon
) else (
    gradle chiseledBuild --no-daemon
)

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [!] Build failed. Make sure JDK 21+ (or JDK 25 for 26.2) is installed.
    pause
    exit /b %ERRORLEVEL%
)

mkdir OUTPUT_JARS 2>nul
for /d %%V in (versions\*) do (
    if exist "%%V\build\libs\*.jar" (
        copy /Y "%%V\build\libs\*.jar" "OUTPUT_JARS\" >nul
    )
)
if exist "build\libs\*.jar" (
    copy /Y "build\libs\*.jar" "OUTPUT_JARS\" >nul
)

echo.
echo [SUCCESS] All compiled .jar mods have been copied to the OUTPUT_JARS\ folder!
echo Copy the .jar matching your Minecraft version into your .minecraft\mods\ folder.
pause

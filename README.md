# StructureRadar Fabric Mod (1.16.5 – 26.2)

Xaero-style 2D World Map & Render-Distance Structure Scanner for Minecraft Java Edition (Fabric).

## Why You Need to Compile `.java` into `.jar`
Minecraft mods are written in `.java` source code and must be compiled and remapped against Minecraft's proprietary game binaries (`com.mojang:minecraft`) and Fabric API (`net.fabricmc.fabric-api`) using **Fabric Loom + Gradle** to produce the final `.jar` file for `.minecraft/mods/`.

You can turn this project into ready-to-play `.jar` files in **two ways**:

### Method 1: Build Automatically in the Cloud with GitHub (No Java/Gradle Installation Needed)
1. Create a free repository on GitHub and upload the contents of this ZIP file.
2. Click the **Actions** tab in your GitHub repository.
3. The included workflow (`.github/workflows/build.yml`) will automatically download Gradle 9.5.1, Java 25, Minecraft mappings, and Fabric API, and compile all `.jar` versions (`1.16.5` through `26.2`).
4. Download `StructureRadar-Fabric-JARs-All-Versions` from the workflow summary page, unzip it, and place the `.jar` for your version into `.minecraft/mods/`.

### Method 2: One-Click Local Build on Your PC
1. Install [Adoptium Temurin JDK](https://adoptium.net/) and [Gradle](https://gradle.org/install/) (or open this folder in **IntelliJ IDEA** with the Minecraft Development plugin).
2. Double-click **`build-jars.bat`** (Windows) or run **`./build-jars.sh`** (macOS/Linux).
3. Open the generated **`OUTPUT_JARS/`** folder and copy `structureradar-2.4.0+<mc_version>.jar` into your `.minecraft/mods/` folder alongside **Fabric API**.

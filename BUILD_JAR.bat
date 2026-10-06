@echo off
setlocal enabledelayedexpansion
title Building ViewpointOptimizationPatch.jar

set "JAVAC_EXE=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\javac.exe"
set "JAR_EXE=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\jar.exe"

if not exist "!JAVAC_EXE!" (
    where javac >nul 2>nul
    if %errorlevel% equ 0 (
        set "JAVAC_EXE=javac"
        set "JAR_EXE=jar"
    ) else (
        echo [ERROR] javac.exe not found!
        pause
        exit /b 1
    )
)

set "PZ_DIR=J:\SteamLibrary\steamapps\common\ProjectZomboid"
set "WORKSHOP_DIR=J:\SteamLibrary\steamapps\workshop\content\108600"
set "CP=!PZ_DIR!\ZombieBuddy.jar;!PZ_DIR!\projectzomboid.jar;!WORKSHOP_DIR!\3809306528\mods\Viewpoint\42\media\java\client\Viewpoint.jar"

if not exist "build" mkdir "build"

echo Compiling Java source files...
powershell -NoProfile -Command "$files = (Get-ChildItem -Path src -Filter *.java -Recurse).FullName; & '!JAVAC_EXE!' -cp '!CP!' -d build $files"
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b 1
)

echo Packaging ViewpointOptimizationPatch.jar...
"!JAR_EXE!" cvf "Contents\mods\ViewpointOptimizationPatch\42\media\java\client\ViewpointOptimizationPatch.jar" -C build .
if %errorlevel% neq 0 (
    echo [ERROR] Packaging failed!
    pause
    exit /b 1
)

echo [SUCCESS] ViewpointOptimizationPatch.jar built successfully!

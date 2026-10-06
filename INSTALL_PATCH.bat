@echo off
setlocal enabledelayedexpansion
title Viewpoint Unified Performance & Engine Patch Installer v1.1.0

echo =====================================================================
echo       VIEWPOINT UNIFIED PERFORMANCE & ENGINE PATCH v1.1.0
echo =====================================================================
echo.
echo Bundled optimizations included in this patch:
echo   [1] Eliminates glFinish() stalls and GPU freezes on first-person switch (O key).
echo   [2] Proactively drains OpenGL fence queues to prevent 100%% iGPU lockups.
echo   [3] Auto-tunes settings on Intel HD/UHD/Iris Xe to guarantee smooth 60+ FPS.
echo   [4] Auto-replaces heavy Lanczos floor baking with fast Trilinear filtering.
echo   [5] 100%% compatible with Viewpoint Turbo (preserves original Viewpoint.jar).
echo.

set "PZ_MODS=%USERPROFILE%\Zomboid\mods"
set "TARGET_DIR=%PZ_MODS%\ViewpointOptimizationPatch"
set "DEFAULT_TXT=%PZ_MODS%\default.txt"

if not exist "%PZ_MODS%" (
    echo [ERROR] Could not find Zomboid directory at: %PZ_MODS%
    echo Please make sure Project Zomboid has been run at least once.
    pause
    exit /b 1
)

echo [Step 1/3] Installing ViewpointOptimizationPatch mod...
if exist "%TARGET_DIR%" rmdir /s /q "%TARGET_DIR%"
mkdir "%TARGET_DIR%"
xcopy /s /e /y "Contents\mods\ViewpointOptimizationPatch\*" "%TARGET_DIR%\" >nul
if %errorlevel% neq 0 (
    echo [ERROR] Failed to copy mod files. Make sure game is closed.
    pause
    exit /b 1
)
echo [OK] Mod files installed to %TARGET_DIR%
echo.

echo [Step 2/3] Enabling mod in default.txt...
if exist "%DEFAULT_TXT%" (
    findstr /c:"mod = ViewpointOptimizationPatch," "%DEFAULT_TXT%" >nul
    if %errorlevel% neq 0 (
        powershell -NoProfile -Command "$content = Get-Content '%DEFAULT_TXT%'; $new = @(); foreach ($l in $content) { $new += $l; if ($l -like '*mod = ViewpointTurbo,*' -or $l -like '*mod = Viewpoint,*') { $new += '    mod = ViewpointOptimizationPatch,' } }; $new | Set-Content '%DEFAULT_TXT%'"
        echo [OK] Added ViewpointOptimizationPatch to default.txt
    ) else (
        echo [OK] ViewpointOptimizationPatch is already enabled in default.txt
    )
) else (
    echo [NOTE] default.txt not found. You can enable 'Viewpoint Performance Patch' in the in-game Mods menu.
)
echo.

echo [Step 3/3] Choose your hardware profile for optimal settings:
echo   1. Intel Integrated GPU / Low-End PC (Intel HD/UHD/Iris Xe, AMD APU)
echo   2. Dedicated GPU (RX 9060 XT, RTX 3060/4070, RX 6000/7000, etc.)
echo   3. Keep my current settings (engine auto-tuning handles it automatically)
echo.
set /p PROFILE_CHOICE="Enter selection [1, 2, or 3]: "

if "%PROFILE_CHOICE%"=="1" (
    echo Applying Intel iGPU / Potato Performance Preset...
    if exist "%USERPROFILE%\Zomboid\viewpoint-live.properties" (
        copy /y "%USERPROFILE%\Zomboid\viewpoint-live.properties" "%USERPROFILE%\Zomboid\viewpoint-live.properties.bak" >nul
    )
    copy /y "presets\iGPU_POTATO\viewpoint-live.properties" "%USERPROFILE%\Zomboid\viewpoint-live.properties" >nul
    echo [OK] Intel iGPU Profile applied successfully!
)

if "%PROFILE_CHOICE%"=="2" (
    echo Applying Dedicated GPU High-Performance Preset...
    if exist "%USERPROFILE%\Zomboid\viewpoint-live.properties" (
        copy /y "%USERPROFILE%\Zomboid\viewpoint-live.properties" "%USERPROFILE%\Zomboid\viewpoint-live.properties.bak" >nul
    )
    copy /y "presets\DEDICATED_GPU\viewpoint-live.properties" "%USERPROFILE%\Zomboid\viewpoint-live.properties" >nul
    echo [OK] Dedicated GPU Profile applied successfully!
)

if "%PROFILE_CHOICE%"=="3" (
    echo [OK] Keeping current settings. Engine patch will operate on existing configuration.
)

echo.
echo =====================================================================
echo                     INSTALLATION COMPLETE!
echo =====================================================================
echo.
echo You can now launch Project Zomboid!
echo In the Mods menu, ensure "ZombieBuddy", "Viewpoint", "Viewpoint Turbo",
echo and "Viewpoint Performance Patch" are enabled.
echo.
pause

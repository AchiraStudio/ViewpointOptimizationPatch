@echo off
title Viewpoint Unified Performance Patch Uninstaller

echo =====================================================================
echo                RESTORE / UNINSTALL PATCH
echo =====================================================================
echo.

set "TARGET_DIR=%USERPROFILE%\Zomboid\mods\ViewpointOptimizationPatch"
set "DEFAULT_TXT=%USERPROFILE%\Zomboid\mods\default.txt"

if exist "%TARGET_DIR%" (
    echo Removing ViewpointOptimizationPatch mod directory...
    rmdir /s /q "%TARGET_DIR%"
    echo [OK] Removed mod directory.
)

if exist "%DEFAULT_TXT%" (
    echo Removing ViewpointOptimizationPatch from default.txt...
    powershell -NoProfile -Command "$content = Get-Content '%DEFAULT_TXT%' | Where-Object { $_ -notlike '*ViewpointOptimizationPatch*' }; $content | Set-Content '%DEFAULT_TXT%'"
    echo [OK] Removed from default.txt.
)

if exist "%USERPROFILE%\Zomboid\viewpoint-live.properties.bak" (
    echo Restoring original viewpoint-live.properties...
    copy /y "%USERPROFILE%\Zomboid\viewpoint-live.properties.bak" "%USERPROFILE%\Zomboid\viewpoint-live.properties" >nul
    echo [OK] Restored backup settings.
)

echo.
echo =====================================================================
echo                     UNINSTALLATION COMPLETE
echo =====================================================================
pause

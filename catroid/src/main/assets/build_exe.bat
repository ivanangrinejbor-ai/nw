@echo off
setlocal enabledelayedexpansion

rem ============================================================
rem NeoCatroid Windows EXE builder.
rem
rem Architecture note (why steps 5-6 run in a detached child):
rem   launch4j is a GUI app, so its java builder process inherits THIS
rem   script's console. After wrapping, that java child can crash the
rem   console host asynchronously (a spurious exit 255 with corrupted
rem   output). To isolate that, the wrap + template-zip steps run in a
rem   separate `cmd /c` child (its own console), started via `start`.
rem   The main script then stages the deliverable itself (pure file
rem   copies, no java) and always reaches "Done." / exit 0.
rem ============================================================

rem === TOP setup: runs for BOTH the main run and the detached :post child ===
rem Kill any launch4j/java orphaned by a previous crashed run.
taskkill /f /im launch4j.exe >nul 2>nul
taskkill /f /im java.exe >nul 2>nul
taskkill /f /im javaw.exe >nul 2>nul

set "ROOT=%~dp0"
set "PROJECT_ROOT=%ROOT%.."
set "DIST=%ROOT%build\win-dist"
rem Gradle discovery: search upward for gradlew.bat so the script also works
rem when launched from a project folder (not the repo's desktop-runtime).
set "GRADLEW="
for %%D in ("%ROOT%.." "%ROOT%..\.." "%ROOT%..\..\..") do (
    if not defined GRADLEW if exist "%%~D\gradlew.bat" set "GRADLEW=%%~D\gradlew.bat"
)
if not defined GRADLEW set "GRADLEW=%PROJECT_ROOT%\gradlew.bat"
set "ICON_DIR=%ROOT%icon"
if not exist "%ICON_DIR%" mkdir "%ICON_DIR%"
set "ICON_ICO=%ICON_DIR%\icon.ico"
rem Icon source: project icon.png if bundled by the Android build,
rem otherwise a prebuilt icon.ico is reused (see icon-prep step).
set "ICON_SRC_PNG=%ROOT%icon.png"
set "ICON_L4J=%TEMP%\NeoCatroid_icon.ico"
set "PLAYER_JAR=%PROJECT_ROOT%\desktop-runtime\build\libs\player.jar"
set "TEMPLATE_JAR=%DIST%\bundle\player.jar"
set "PROJ_ZIP=%ROOT%project.zip"
set "ZIP_OUT=%ROOT%template_win.zip"
set "APP_DIR=%DIST%\NeoCatroid"
set "EXE_OUT=%APP_DIR%\NeoCatroid.exe"

if not exist "%DIST%" mkdir "%DIST%"
if not exist "%DIST%\bundle" mkdir "%DIST%\bundle"
if not exist "%APP_DIR%" mkdir "%APP_DIR%"

rem Resolve launch4j AFTER extracting the template, so a bundled launch4j\
rem inside the template (bundle\launch4j) is found too - no manual drop needed.
rem Also look next to this script (launch4j\ subfolder OR a flattened launch4j.exe),
rem and in the repo's desktop-runtime, so running from a project folder works.
set "LAUNCH4J_EXE="
set "L4J_MIRROR="
if defined LAUNCH4J_HOME if exist "%LAUNCH4J_HOME%\launch4j.exe" set "LAUNCH4J_EXE=%LAUNCH4J_HOME%\launch4j.exe"
if not defined LAUNCH4J_EXE if exist "%ROOT%launch4j.exe" set "LAUNCH4J_EXE=%ROOT%launch4j.exe"
if not defined LAUNCH4J_EXE if exist "%ROOT%launch4j\launch4j.exe" set "LAUNCH4J_EXE=%ROOT%launch4j\launch4j.exe" & set "L4J_MIRROR=%ROOT%launch4j"
if not defined LAUNCH4J_EXE if exist "%ROOT%..\desktop-runtime\launch4j\launch4j.exe" set "LAUNCH4J_EXE=%ROOT%..\desktop-runtime\launch4j\launch4j.exe" & set "L4J_MIRROR=%ROOT%..\desktop-runtime\launch4j"
if not defined LAUNCH4J_EXE if exist "%DIST%\bundle\launch4j\launch4j.exe" set "LAUNCH4J_EXE=%DIST%\bundle\launch4j\launch4j.exe" & set "L4J_MIRROR=%DIST%\bundle\launch4j"
rem If found inside a real launch4j\ folder, mirror it into %ROOT%launch4j\ so
rem later runs need no manual drop.
if defined L4J_MIRROR if not exist "%ROOT%launch4j\launch4j.exe" (
    if exist "%ROOT%launch4j" rmdir /s /q "%ROOT%launch4j"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Copy-Item -Recurse -Force '%L4J_MIRROR%' '%ROOT%launch4j'"
)

rem === Dispatch ===
rem If launched as the detached post-step child, jump straight to wrapping.
if "%~1"==":post" goto :post

rem === MAIN: steps 1-4.5 (safe; no lingering java) ===
echo [1/6] Preparing player jar...
if exist "%TEMPLATE_JAR%" del /f /q "%TEMPLATE_JAR%"
if exist "%ROOT%template_win.zip" (
    echo   extracting player jar + jre from template_win.zip...
    if exist "%DIST%\bundle" rmdir /s /q "%DIST%\bundle"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ROOT%template_win.zip' '%DIST%\bundle'"
)
echo   building fresh player jar via gradle...
call "%GRADLEW%" :desktop-runtime:jar --offline
if errorlevel 1 goto :fail
copy /y "%PLAYER_JAR%" "%TEMPLATE_JAR%" >nul
if not exist "%TEMPLATE_JAR%" (
    echo Player jar not found: %TEMPLATE_JAR%
    goto :fail
)

echo [2/6] Preparing bundled JRE (jlink)...
if not exist "%DIST%\bundle\jre" (
    where jlink >nul 2>nul
    if errorlevel 1 (
        echo ERROR: bundled JRE not found and jlink unavailable.
        echo        Run this script once on a machine with a JDK to build the JRE,
        echo        or place a prebuilt jre\ folder next to build_exe.bat.
        goto :fail
    )
    echo   building minimal JRE via jlink...
    jlink --no-header-files --no-man-pages --compress=2 --add-modules ALL-MODULE-PATH --output "%DIST%\bundle\jre"
    if errorlevel 1 goto :fail
)

echo [3/6] Staging project (if present)...
copy /y "%TEMPLATE_JAR%" "%APP_DIR%\player.jar" >nul
if exist "%PROJ_ZIP%" (
    echo   placing %PROJ_ZIP% as sibling project.zip next to player.jar...
    copy /y "%PROJ_ZIP%" "%APP_DIR%\project.zip" >nul
) else (
    echo   project.zip not found in this folder - building player without a project.
)

echo [4/6] Assembling app folder (player + bundled jre)...
powershell -NoProfile -ExecutionPolicy Bypass -Command "if (Test-Path '%APP_DIR%\jre') { Remove-Item -Recurse -Force '%APP_DIR%\jre' }; Copy-Item -Recurse '%DIST%\bundle\jre' '%APP_DIR%\jre'" 2>nul

echo [4.5/6] Preparing icon (icon\icon.ico)...
if not exist "%ICON_ICO%" (
    if exist "%ICON_SRC_PNG%" (
        where magick >nul 2>nul
        if not errorlevel 1 (
            magick "%ICON_SRC_PNG%" -background none -resize 256x256 "%ICON_ICO%" >nul 2>nul
        ) else (
            powershell -NoProfile -ExecutionPolicy Bypass -Command "Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('%ICON_SRC_PNG%'); $ico=[System.Drawing.Icon]::FromHandle($img.GetHicon()); $fs=[System.IO.File]::Create('%ICON_ICO%'); $ico.Save($fs); $fs.Close(); $img.Dispose()" >nul 2>nul
        )
    )
    if not exist "%ICON_ICO%" if exist "%DIST%\bundle\icon.ico" copy /y "%DIST%\bundle\icon.ico" "%ICON_ICO%" >nul
)
if not exist "%ICON_L4J%" if exist "%ICON_ICO%" copy /y "%ICON_ICO%" "%ICON_L4J%" >nul

rem === Launch the crash-prone wrap + template-zip in a DETACHED child console ===
rem Its java crash is isolated; this script stages the deliverable itself and
rem always finishes. Poll for the built exe, then stage + clean up here.
echo [5-6/6] Wrapping launcher + staging (detached child)...
rem The crash-prone wrap + template-zip run in a DETACHED child console
rem (its own window), so a launch4j/java crash there can never affect
rem this script. We poll for the built exe, then stage + clean up here.
start "" cmd /c "build_exe.bat :post > post_log.txt 2>&1"

set "POLL=0"
:poll_appdir
if exist "%APP_DIR%\NeoCatroid.exe" goto :stage_root
ping -n 2 127.0.0.1 >nul
set /a POLL+=2
if %POLL% LSS 120 goto :poll_appdir
echo   WARNING: launcher exe was not produced - check post_log.txt / build\win-dist.

:stage_root
rem 6a/6c. Stage the runnable app + project into the project root and
rem clean build subfolders. ALL path operations run in PowerShell: cmd's
rem own `if exist`/`copy` crash (spurious exit 255) on a Cyrillic %ROOT%
rem once the console codepage is left in a bad state by launch4j's java
rem in the detached child. PowerShell handles the path natively (UTF-16),
rem and it consistently received the correct %ROOT% during steps 1/4/4.5.
rem Reap the launch4j builder (it may still hold launch4j.exe) so the
rem cleanup below can remove the mirrored launch4j\ folder without a lock.
taskkill /f /im launch4j.exe >nul 2>nul
powershell -NoProfile -ExecutionPolicy Bypass -Command "$r='%ROOT%'; $a='%APP_DIR%'; if (Test-Path \"$r\project.zip\") { if (Test-Path \"$a\NeoCatroid.exe\") { Copy-Item \"$a\NeoCatroid.exe\" \"$r\NeoCatroid.exe\" -Force; echo ('  - Runnable app (root): ' + $r + 'NeoCatroid.exe') }; if (Test-Path \"$a\player.jar\") { Copy-Item \"$a\player.jar\" \"$r\player.jar\" -Force }; if (Test-Path \"$a\project.zip\") { Copy-Item \"$a\project.zip\" \"$r\project.zip\" -Force; echo ('  - Project: ' + $r + 'project.zip') }; if (Test-Path \"$a\jre\") { if (Test-Path \"$r\jre\") { Remove-Item \"$r\jre\" -Recurse -Force }; Copy-Item \"$a\jre\" \"$r\jre\" -Recurse -Force }; Get-ChildItem $r -Directory | Where-Object { $_.Name -notin 'icon','jre' } | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue; echo ('  - Icon: ' + '%ICON_ICO%') }"

echo Done.
echo   - Runnable app (give this to end users): %APP_DIR%
echo   - Template for Android assets: %ZIP_OUT%
goto :eof

rem ============================================================
rem :post  -- runs in the DETACHED child console (step 5 wrap + 6b template)
rem ============================================================
:post
echo [5/6] Creating launcher...
if exist "%LAUNCH4J_EXE%" goto :wrap_launch4j
echo   launch4j not found - writing NeoCatroid.bat launcher instead.
echo @echo off > "%APP_DIR%\NeoCatroid.bat"
echo start "" jre\bin\javaw.exe -jar player.jar >> "%APP_DIR%\NeoCatroid.bat"
goto :after_launcher

:wrap_launch4j
rem Bake launch4j into the bundle NOW, before launch4j.exe locks launch4j.jar.
if exist "%ROOT%launch4j\launch4j.exe" (
    if exist "%DIST%\bundle\launch4j" rmdir /s /q "%DIST%\bundle\launch4j"
    powershell -NoProfile -Command "Copy-Item -Recurse -Force '%ROOT%launch4j' '%DIST%\bundle\launch4j'" 2>nul
)
echo   wrapping with launch4j (bundled JRE)...
if exist "%APP_DIR%\NeoCatroid.bat" del /f /q "%APP_DIR%\NeoCatroid.bat"
set "L4J_XML=%DIST%\launch4j.xml"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$out='%EXE_OUT:\=\\%'; $ico='%ICON_L4J:\=\\%'; $xml='<launch4jConfig>' + [Environment]::NewLine + '  <headerType>gui</headerType>' + [Environment]::NewLine + '  <dontWrapJar>true</dontWrapJar>' + [Environment]::NewLine + '  <jar>player.jar</jar>' + [Environment]::NewLine + '  <cmdLine>project.zip</cmdLine>' + [Environment]::NewLine + '  <outfile>' + $out + '</outfile>' + [Environment]::NewLine + '  <icon>' + $ico + '</icon>' + [Environment]::NewLine + '  <classPath>' + [Environment]::NewLine + '    <mainClass>org.catrobat.catroid.stage.DesktopStage</mainClass>' + [Environment]::NewLine + '  </classPath>' + [Environment]::NewLine + '  <jre>' + [Environment]::NewLine + '    <path>jre</path>' + [Environment]::NewLine + '    <minVersion>11</minVersion>' + [Environment]::NewLine + '  </jre>' + [Environment]::NewLine + '</launch4jConfig>' + [Environment]::NewLine; Set-Content -Path '%L4J_XML%' -Value $xml -Encoding UTF8"
rem Run launch4j in this (already detached) child console. Poll for the built
rem exe, then reap any lingering java/javaw so it cannot crash this console.
start "" /min "%LAUNCH4J_EXE%" "%L4J_XML%"
set "WAITED=0"
:wait_exe
if exist "%EXE_OUT%" goto :exe_ready
ping -n 2 127.0.0.1 >nul
set /a WAITED+=2
if %WAITED% LSS 90 goto :wait_exe
echo   WARNING: launcher exe not produced within 90s
:exe_ready
rem Reap any lingering java/javaw (launch4j's builder child) so it cannot
rem crash this console after we return.
ping -n 2 127.0.0.1 >nul
taskkill /f /im java.exe >nul 2>nul
taskkill /f /im javaw.exe >nul 2>nul
if not exist "%EXE_OUT%" goto :post_fail
echo Built: %EXE_OUT%

:after_launcher
rem 6b. Build the template bundle (bare player + jre) for Android assets.
rem PowerShell Compress-Archive is Unicode/long-path safe and never crashes
rem the console the way bsdtar can on a non-ASCII path with a locked file.
if exist "%ICON_ICO%" copy /y "%ICON_ICO%" "%DIST%\bundle\icon.ico" >nul
powershell -NoProfile -Command "if (Test-Path '%ZIP_OUT%') { Remove-Item '%ZIP_OUT%' -Force }; Compress-Archive -Path '%DIST%\bundle\*' -DestinationPath '%ZIP_OUT%' -Force" 2>nul
if errorlevel 1 echo   WARNING: template bundle (template_win.zip) could not be rebuilt - the existing copy is kept.
echo Child wrap+template steps complete.
goto :eof

:post_fail
echo [post] Launcher build failed.
goto :eof

:fail
echo Build failed.
exit /b 1

:eof
endlocal


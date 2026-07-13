@echo off
setlocal enabledelayedexpansion

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
set "ICON_SRC=%PROJECT_ROOT%\catroid\src\main\res\mipmap-xxxhdpi\ic_launcher.png"
set "ICON_ICO=%DIST%\icon.ico"
set "PLAYER_JAR=%PROJECT_ROOT%\desktop-runtime\build\libs\player.jar"
set "TEMPLATE_JAR=%DIST%\bundle\player.jar"
set "PROJ_ZIP=%ROOT%project.zip"
set "ZIP_OUT=%ROOT%template_win.zip"
set "APP_DIR=%DIST%\NeoCatroid"
set "EXE_OUT=%APP_DIR%\NeoCatroid.exe"

if not exist "%DIST%" mkdir "%DIST%"
if not exist "%DIST%\bundle" mkdir "%DIST%\bundle"
if not exist "%APP_DIR%" mkdir "%APP_DIR%"

echo [1/6] Preparing player jar...
if exist "%TEMPLATE_JAR%" del /f /q "%TEMPLATE_JAR%"
if exist "%ROOT%template_win.zip" (
    echo   extracting player jar + jre from template_win.zip...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ROOT%template_win.zip' '%DIST%\bundle'"
)
if not exist "%TEMPLATE_JAR%" (
    echo   building player jar via gradle...
    call "%GRADLEW%" :desktop-runtime:jar --offline
    if errorlevel 1 goto :fail
    copy /y "%PLAYER_JAR%" "%TEMPLATE_JAR%" >nul
)
if not exist "%TEMPLATE_JAR%" (
    echo Player jar not found: %TEMPLATE_JAR%
    goto :fail
)

rem Resolve launch4j AFTER extracting the template, so a bundled launch4j\
rem inside the template (bundle\launch4j) is found too — no manual drop needed.
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
rem step 6 bakes it into the regenerated template (later runs need no manual drop).
if defined L4J_MIRROR if not exist "%ROOT%launch4j\launch4j.exe" (
    if exist "%ROOT%launch4j" rmdir /s /q "%ROOT%launch4j"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Copy-Item -Recurse -Force '%L4J_MIRROR%' '%ROOT%launch4j'"
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

echo [3/6] Embedding project (if present)...
copy /y "%TEMPLATE_JAR%" "%APP_DIR%\player.jar" >nul
if exist "%PROJ_ZIP%" (
    echo   embedding %PROJ_ZIP% as NEOCAT01 payload into player.jar...
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "$jar='%APP_DIR%\player.jar'; $proj=[System.IO.File]::ReadAllBytes('%PROJ_ZIP%');" ^
        "$size=[System.BitConverter]::GetBytes([uint64]$proj.Length);" ^
        "$magic=[System.Text.Encoding]::ASCII.GetBytes('NEOCAT01');" ^
        "$fs=[System.IO.File]::Open($jar,'Append');" ^
        "$fs.Write($proj,0,$proj.Length);" ^
        "$fs.Write($size,0,$size.Length);" ^
        "$fs.Write($magic,0,$magic.Length);" ^
        "$fs.Close();"
) else (
    echo   project.zip not found in this folder - building player without embedded project.
)

echo [4/6] Assembling app folder (player + bundled jre)...
powershell -NoProfile -ExecutionPolicy Bypass -Command "if (Test-Path '%APP_DIR%\jre') { Remove-Item -Recurse -Force '%APP_DIR%\jre' }; Copy-Item -Recurse '%DIST%\bundle\jre' '%APP_DIR%\jre'" 2>nul

echo [5/6] Creating launcher...
if exist "%LAUNCH4J_EXE%" goto :wrap_launch4j
echo   launch4j not found - writing NeoCatroid.bat launcher instead.
echo @echo off > "%APP_DIR%\NeoCatroid.bat"
echo start "" jre\bin\javaw.exe -jar player.jar >> "%APP_DIR%\NeoCatroid.bat"
goto :after_launcher

:wrap_launch4j
echo   wrapping with launch4j (bundled JRE)...
if exist "%APP_DIR%\NeoCatroid.bat" del /f /q "%APP_DIR%\NeoCatroid.bat"
set "L4J_XML=%DIST%\launch4j.xml"
> "%L4J_XML%" (
    echo ^<launch4jConfig^>
    echo   ^<headerType^>gui^</headerType^>
    echo   ^<dontWrapJar^>true^</dontWrapJar^>
    echo   ^<jar^>player.jar^</jar^>
    echo   ^<outfile^>%EXE_OUT:\=\\%^</outfile^>
    echo   ^<icon^>%ICON_ICO:\=\\%^</icon^>
    echo   ^<classPath^>
    echo     ^<mainClass^>org.catrobat.catroid.stage.DesktopStage^</mainClass^>
    echo   ^</classPath^>
    echo   ^<jre^>
    echo     ^<path^>jre^</path^>
    echo     ^<minVersion^>11^</minVersion^>
    echo   ^</jre^>
    echo ^</launch4jConfig^>
)
"%LAUNCH4J_EXE%" "%L4J_XML%"
if errorlevel 1 goto :fail
echo Built: %EXE_OUT%

:after_launcher

echo [6/6] Building template bundle for Android assets (bare player + jre)...
rem Bake launch4j into the template so end users need no manual drop.
if exist "%ROOT%launch4j\launch4j.exe" (
    if exist "%DIST%\bundle\launch4j" rmdir /s /q "%DIST%\bundle\launch4j"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Copy-Item -Recurse -Force '%ROOT%launch4j' '%DIST%\bundle\launch4j'" 2>nul
)
if exist "%ICON_ICO%" copy /y "%ICON_ICO%" "%DIST%\bundle\icon.ico" >nul
if exist "%ZIP_OUT%" del /f /q "%ZIP_OUT%"
tar -a -c -f "%ZIP_OUT%" -C "%DIST%\bundle" .
if errorlevel 1 goto :fail

echo Done.
echo   - Template for Android assets: %ZIP_OUT%
echo   - Runnable app (give this to end users): %APP_DIR%
goto :eof

:fail
echo Build failed.
exit /b 1

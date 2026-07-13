@echo off
setlocal enabledelayedexpansion

set "ROOT=%~dp0"
set "PROJECT_ROOT=%ROOT%.."
set "DIST=%ROOT%build\win-dist"
set "ICON_SRC=%PROJECT_ROOT%\catroid\src\main\res\mipmap-xxxhdpi\ic_launcher.png"
set "ICON_ICO=%DIST%\icon.ico"
set "PLAYER_JAR=%PROJECT_ROOT%\desktop-runtime\build\libs\player.jar"
set "ZIP_OUT=%ROOT%template_win.zip"
set "EXE_OUT=%DIST%\NeoCatroid.exe"

if not exist "%DIST%" mkdir "%DIST%"

echo [1/5] Building player jar...
call "%PROJECT_ROOT%\gradlew.bat" :desktop-runtime:jar
if errorlevel 1 goto :fail

if not exist "%PLAYER_JAR%" (
    echo Player jar not found: %PLAYER_JAR%
    goto :fail
)

echo [2/5] Creating icon...
if exist "%ICON_SRC%" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "$pngBytes = [System.IO.File]::ReadAllBytes('%ICON_SRC%');" ^
        "$stream = New-Object System.IO.MemoryStream;" ^
        "$writer = New-Object System.IO.BinaryWriter($stream);" ^
        "$writer.Write([byte]0); $writer.Write([byte]0);" ^
        "$writer.Write([UInt16]1); $writer.Write([UInt16]1);" ^
        "$writer.Write([byte]1); $writer.Write([byte]0);" ^
        "$writer.Write([byte]32); $writer.Write([byte]0);" ^
        "$writer.Write([byte]0); $writer.Write([byte]0);" ^
        "$writer.Write([byte]1); $writer.Write([byte]0);" ^
        "$writer.Write([Int32]$pngBytes.Length);" ^
        "$writer.Write([Int32]22);" ^
        "$writer.Write($pngBytes);" ^
        "$writer.Flush();" ^
        "[System.IO.File]::WriteAllBytes('%ICON_ICO%', $stream.ToArray());"
) else (
    echo Icon source not found, skipping ICO generation.
)

echo [3/5] Preparing bundle...
if exist "%DIST%\bundle" rmdir /s /q "%DIST%\bundle"
mkdir "%DIST%\bundle"
copy /y "%PLAYER_JAR%" "%DIST%\bundle\player.jar" >nul
if exist "%ICON_ICO%" copy /y "%ICON_ICO%" "%DIST%\bundle\icon.ico" >nul

echo [4/5] Creating template zip...
if exist "%ZIP_OUT%" del /f /q "%ZIP_OUT%"
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "Compress-Archive -Path '%DIST%\bundle\*' -DestinationPath '%ZIP_OUT%' -Force"
if errorlevel 1 goto :fail

echo [5/5] Building exe if Launch4j is available...
set "LAUNCH4J_EXE=%LAUNCH4J_HOME%\launch4j.exe"
if not exist "%LAUNCH4J_EXE%" set "LAUNCH4J_EXE=%ROOT%launch4j\launch4j.exe"
if exist "%LAUNCH4J_EXE%" (
    set "L4J_XML=%DIST%\launch4j.xml"
    > "%L4J_XML%" (
        echo ^<launch4j^>
        echo   ^<headerType^>gui^</headerType^>
        echo   ^<jar^>%PLAYER_JAR:\=\\%^</jar^>
        echo   ^<outfile^>%EXE_OUT:\=\\%^</outfile^>
        echo   ^<icon^>%ICON_ICO:\=\\%^</icon^>
        echo   ^<mainClass^>org.catrobat.catroid.stage.DesktopStage^</mainClass^>
        echo   ^<jre^>
        echo     ^<minVersion^>11^</minVersion^>
        echo   ^</jre^>
        echo ^</launch4j^>
    )
    "%LAUNCH4J_EXE%" "%L4J_XML%"
    if errorlevel 1 goto :fail
) else (
    echo Launch4j not found, skipping exe generation.
)

echo Done.
exit /b 0

:fail
echo Build failed.
exit /b 1

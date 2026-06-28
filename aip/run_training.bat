@echo off
REM run_training.bat — Train the AI Assistant
REM
REM 1) Put your .code.xml or code.xml files into datasets/ folder
REM 2) Run this script
REM 3) The result model\patterns.json is ready for Android
REM
REM To write directly into app: run_training.bat --deploy

set DEPLOY=%1

cd /d "%~dp0"

echo ========================================
echo  AI Project Assistant — Training
echo ========================================
echo.
echo  Place .code.xml files in: datasets\
echo.

if "%DEPLOY%"=="--deploy" (
    echo  Writing directly to Android assets...
    python train.py --out-dir ..\catroid\src\main\assets
) else (
    python train.py
)

echo.
if exist model\patterns.json (
    echo  ✅ Training complete! model\patterns.json is ready.
    echo.
    echo  To deploy to app: copy /Y model\patterns.json ..\catroid\src\main\assets\patterns.json
    echo  Or run: run_training.bat --deploy
) else (
    echo  ❌ No projects found in datasets/
    echo     Put .code.xml files in datasets/ and try again.
)
pause

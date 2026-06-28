@echo off
echo Deploy model + patterns to Android assets...
cd /d "%~dp0"
set ASSETS=..\catroid\src\main\assets
if not exist "%ASSETS%" mkdir "%ASSETS%"
if exist "model\model.tflite"      copy /Y "model\model.tflite"      "%ASSETS%\" && echo   model.tflite ^> assets/
if exist "model\vocab.json"        copy /Y "model\vocab.json"        "%ASSETS%\" && echo   vocab.json ^> assets/
if exist "model\model_metadata.json" copy /Y "model\model_metadata.json" "%ASSETS%\" && echo   model_metadata.json ^> assets/
if exist "model\patterns.json"     copy /Y "model\patterns.json"     "%ASSETS%\" && echo   patterns.json ^> assets/
echo Done!
ping -n 3 127.0.0.1 >nul

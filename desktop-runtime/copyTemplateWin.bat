@echo off
setlocal

call "%~dp0build_exe.bat"
if errorlevel 1 exit /b 1

if exist "%~dp0template_win.zip" (
    echo Template bundle is ready at %~dp0template_win.zip
) else (
    echo Expected template_win.zip was not created.
    exit /b 1
)

@echo off
title Instalar Inicio Automatico - Hendrix Desktop
cd /d "%~dp0"

echo ===================================================
echo     CONFIGURAR INICIO AUTOMATICO CON WINDOWS
echo ===================================================
echo.

set "TARGET_DIR=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup"
set "SHORTCUT=%TARGET_DIR%\HendrixDesktop.vbs"

set "PYTHON_PATH="
if exist "C:\Python314\python.exe" set "PYTHON_PATH=C:\Python314\python.exe"
if not defined PYTHON_PATH (
    for /f "tokens=*" %%I in ('where python.exe 2^>nul') do (
        if not defined PYTHON_PATH set "PYTHON_PATH=%%I"
    )
)
if not defined PYTHON_PATH set "PYTHON_PATH=python.exe"

echo Creando lanzador silencioso en la carpeta de Inicio de Windows...
(
    echo Set WshShell = CreateObject("WScript.Shell"^)
    echo WshShell.CurrentDirectory = "%~dp0"
    echo WshShell.Run "cmd.exe /c """"%PYTHON_PATH%"""" main.py --minimized", 0, False
) > "%SHORTCUT%"

echo.
echo [EXITO] Hendrix Desktop se iniciara automaticamente en segundo plano
echo cada vez que enciendas tu computadora (minimizado en la bandeja del reloj).
echo Ruta del ejecutable configurado: %PYTHON_PATH%
echo.
pause

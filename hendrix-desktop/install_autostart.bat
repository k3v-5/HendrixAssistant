@echo off
title Instalar Inicio Automatico - Hendrix Desktop
cd /d "%~dp0"

echo ===================================================
echo     CONFIGURAR INICIO AUTOMATICO CON WINDOWS
echo ===================================================
echo.

set "TARGET_DIR=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup"
set "SHORTCUT=%TARGET_DIR%\HendrixDesktop.vbs"

for /f "delims=" %%I in ('where pythonw.exe 2^>nul') do set "PYTHONW_PATH=%%I"
if not defined PYTHONW_PATH set "PYTHONW_PATH=pythonw.exe"

echo Creando lanzador silencioso en la carpeta de Inicio de Windows...
(
    echo Set WshShell = CreateObject("WScript.Shell"^)
    echo WshShell.CurrentDirectory = "%~dp0"
    echo WshShell.Run """%PYTHONW_PATH%"" main.py", 0, False
) > "%SHORTCUT%"

echo.
echo [EXITO] Hendrix Desktop se iniciara automaticamente en segundo plano
echo cada vez que enciendas tu computadora (minimizado en la bandeja del reloj).
echo.
pause

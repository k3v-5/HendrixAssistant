@echo off
title Desinstalar Inicio Automatico - Hendrix Desktop

set "SHORTCUT=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup\HendrixDesktop.vbs"

if exist "%SHORTCUT%" (
    del /f /q "%SHORTCUT%"
    echo [EXITO] Se ha removido Hendrix Desktop del inicio automatico de Windows.
) else (
    echo [INFO] Hendrix Desktop no estaba configurado para iniciar con Windows.
)
echo.
pause

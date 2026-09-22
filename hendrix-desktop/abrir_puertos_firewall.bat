@echo off
title Abrir Puertos Firewall para Hendrix Desktop
cd /d "%~dp0"

:: Auto-elevate to Administrator
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo Solicitando permisos de Administrador...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process cmd -ArgumentList '/c \"\"%~f0\"\"' -Verb RunAs"
    exit /b
)

echo ===================================================
echo     CONFIGURANDO REGLAS DE FIREWALL DE WINDOWS
echo ===================================================
echo.

netsh advfirewall firewall delete rule name="Hendrix Desktop TCP" >nul 2>&1
netsh advfirewall firewall delete rule name="Hendrix Desktop UDP" >nul 2>&1
netsh advfirewall firewall delete rule name="Hendrix Desktop Python" >nul 2>&1
netsh advfirewall firewall delete rule name="Hendrix Desktop Pythonw" >nul 2>&1

echo [1/3] Habilitando puertos TCP 8899 (WebSocket) y 8900 (AirSync)...
netsh advfirewall firewall add rule name="Hendrix Desktop TCP" dir=in action=allow protocol=TCP localport=8899,8900 profile=any >nul

echo [2/3] Habilitando puerto UDP 8764 (Descubrimiento Zero-Config)...
netsh advfirewall firewall add rule name="Hendrix Desktop UDP" dir=in action=allow protocol=UDP localport=8764 profile=any >nul

echo [3/3] Permitiendo Python en la red local...
if exist "C:\Python314\python.exe" (
    netsh advfirewall firewall add rule name="Hendrix Desktop Python" dir=in action=allow program="C:\Python314\python.exe" profile=any >nul
)
if exist "C:\Python314\pythonw.exe" (
    netsh advfirewall firewall add rule name="Hendrix Desktop Pythonw" dir=in action=allow program="C:\Python314\pythonw.exe" profile=any >nul
)

echo.
echo ===================================================
echo [EXITO] Puertos de Hendrix Desktop habilitados!
echo Tu celular ahora podra descubrir y comunicarse con la PC.
echo ===================================================
echo.
pause

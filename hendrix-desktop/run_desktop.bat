@echo off
title Hendrix Desktop Companion
cd /d "%~dp0"

echo ===================================================
echo        HENDRIX DESKTOP COMPANION - WINDOWS
echo ===================================================
echo.

python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Python no fue encontrado en el sistema.
    echo Por favor instala Python 3.10 o superior y agregalo al PATH.
    pause
    exit /b 1
)

echo [1/2] Verificando dependencias...
pip install -r requirements.txt --quiet --disable-pip-version-check

set "PYTHON_EXE=python"
if exist "C:\Users\sasuk\AppData\Local\python\pythoncore-3.14-64\python.exe" set "PYTHON_EXE=C:\Users\sasuk\AppData\Local\python\pythoncore-3.14-64\python.exe"

echo [2/2] Iniciando Hendrix Desktop Companion...
echo.
"%PYTHON_EXE%" main.py

if %errorlevel% neq 0 (
    echo.
    echo [AVISO] La aplicacion se cerro con codigo de error %errorlevel%.
    pause
)

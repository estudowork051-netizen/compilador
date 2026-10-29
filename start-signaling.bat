@echo off
setlocal EnableExtensions
chcp 65001 >nul 2>&1

rem Resolve a raiz do repositorio de forma independente do diretorio de chamada
set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"

set "SIGNALING_SCRIPT=%PROJECT_ROOT%\scripts\ngrok\start-signaling.ps1"

echo ======================================================================
echo   REMOTEDESK - INICIANDO SERVIDOR DE SINALIZACAO (.NET 8)
echo   Raiz do Projeto:       "%PROJECT_ROOT%"
echo   Script de Inicializacao: "%SIGNALING_SCRIPT%"
echo ======================================================================
echo.

if not exist "%SIGNALING_SCRIPT%" (
    echo [ERRO CRITICO] O script de inicializacao do Servidor de Sinalizacao nao foi encontrado!
    echo               Caminho procurado: "%SIGNALING_SCRIPT%"
    echo.
    echo Verifique se o repositorio foi clonado corretamente.
    echo.
    if /i not "%CI%"=="true" pause
    exit /b 1
)

where powershell >nul 2>&1
if errorlevel 1 (
    echo [ERRO] PowerShell nao foi encontrado no sistema.
    echo Certifique-se de que o Windows PowerShell esta instalado e acessivel no PATH.
    if /i not "%CI%"=="true" pause
    exit /b 1
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SIGNALING_SCRIPT%"
set "EXIT_CODE=%errorlevel%"

if not "%EXIT_CODE%"=="0" (
    echo.
    echo [ERRO] O Servidor de Sinalizacao encerrou com codigo de saida: %EXIT_CODE%
    if /i not "%CI%"=="true" pause
)
exit /b %EXIT_CODE%

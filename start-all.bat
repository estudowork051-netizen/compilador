@echo off
setlocal EnableExtensions
chcp 65001 >nul 2>&1

rem Define a raiz do repositorio de forma explicita e imutavel a partir de %~dp0
set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"
cd /d "%PROJECT_ROOT%"

echo ======================================================================
echo   REMOTEDESK - INICIANDO AMBIENTE DE ACESSO REMOTO NGROK
echo   Raiz do Projeto: "%PROJECT_ROOT%"
echo ======================================================================
echo.

rem Validacao dos caminhos reais dos dois projetos .csproj antes de prosseguir
set "AGENT_PROJ=%PROJECT_ROOT%\windows-agent\src\RemoteDesk.Agent\RemoteDesk.Agent.csproj"
set "SERVER_PROJ=%PROJECT_ROOT%\signaling-server\src\RemoteDesk.SignalingServer\RemoteDesk.SignalingServer.csproj"

if not exist "%AGENT_PROJ%" (
    echo [ERRO CRITICO] Arquivo de projeto do Windows Agent nao encontrado!
    echo               Caminho procurado: "%AGENT_PROJ%"
    echo.
    echo Verifique a estrutura do repositorio antes de continuar.
    if /i not "%CI%"=="true" pause
    exit /b 1
)

if not exist "%SERVER_PROJ%" (
    echo [ERRO CRITICO] Arquivo de projeto do Servidor de Sinalizacao nao encontrado!
    echo               Caminho procurado: "%SERVER_PROJ%"
    echo.
    echo Verifique a estrutura do repositorio antes de continuar.
    if /i not "%CI%"=="true" pause
    exit /b 1
)

set "NGROK_BAT=%PROJECT_ROOT%\scripts\ngrok\start-all.bat"
if exist "%NGROK_BAT%" (
    call "%NGROK_BAT%" %*
    exit /b %errorlevel%
) else (
    echo [ERRO] O script start-all.bat nao foi encontrado em:
    echo        "%NGROK_BAT%"
    if /i not "%CI%"=="true" pause
    exit /b 1
)

@echo off
setlocal EnableExtensions
chcp 65001 >nul 2>&1

rem Resolve a raiz do repositorio de forma independente do diretorio de chamada
set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"

set "AGENT_DIR=%PROJECT_ROOT%\windows-agent\src\RemoteDesk.Agent"
set "AGENT_PROJ=%AGENT_DIR%\RemoteDesk.Agent.csproj"

echo ======================================================================
echo   REMOTEDESK - INICIANDO WINDOWS AGENT (.NET 8)
echo   Raiz do Projeto:       "%PROJECT_ROOT%"
echo   Diretorio de Trabalho: "%AGENT_DIR%"
echo   Arquivo de Projeto:    "%AGENT_PROJ%"
echo ======================================================================
echo.

if not exist "%AGENT_PROJ%" (
    echo [ERRO CRITICO] O arquivo de projeto do Windows Agent nao foi encontrado!
    echo               Caminho procurado: "%AGENT_PROJ%"
    echo.
    echo Verifique se os arquivos foram clonados corretamente na pasta:
    echo "%AGENT_DIR%"
    echo.
    if /i not "%CI%"=="true" pause
    exit /b 1
)

cd /d "%AGENT_DIR%"
dotnet run --project "%AGENT_PROJ%" %*
set "EXIT_CODE=%errorlevel%"

if not "%EXIT_CODE%"=="0" (
    echo.
    echo [ERRO] O Agente Windows encerrou com codigo de saida: %EXIT_CODE%
    if /i not "%CI%"=="true" pause
)
exit /b %EXIT_CODE%

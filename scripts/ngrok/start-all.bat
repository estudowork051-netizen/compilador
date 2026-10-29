@echo off
setlocal EnableExtensions
chcp 65001 >nul 2>&1

rem Define a raiz do repositorio de forma explicita e imutavel
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%~dp0..\.."
for %%I in ("%PROJECT_ROOT%") do set "PROJECT_ROOT=%%~fI"

rem Garante que o diretorio de trabalho inicial seja a raiz do projeto
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
    set "EXIT_CODE=1"
    goto :fail_pause
)

if not exist "%SERVER_PROJ%" (
    echo [ERRO CRITICO] Arquivo de projeto do Servidor de Sinalizacao nao encontrado!
    echo               Caminho procurado: "%SERVER_PROJ%"
    echo.
    set "EXIT_CODE=1"
    goto :fail_pause
)

where powershell >nul 2>&1
if errorlevel 1 (
    echo [ERRO] PowerShell nao foi encontrado no sistema.
    echo Certifique-se de que o Windows PowerShell esta instalado e acessivel no PATH.
    set "EXIT_CODE=1"
    goto :fail_pause
)

set "PS_SCRIPT=%SCRIPT_DIR%start-all.ps1"
if not exist "%PS_SCRIPT%" (
    echo [ERRO] Arquivo start-all.ps1 nao encontrado em:
    echo        "%PS_SCRIPT%"
    set "EXIT_CODE=1"
    goto :fail_pause
)

set "RUN_FROM_BAT=1"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%PS_SCRIPT%" %*
set "EXIT_CODE=%errorlevel%"

if not "%EXIT_CODE%"=="0" goto :on_error

echo.
echo ======================================================================
echo   [OK] Sessao do RemoteDesk iniciada com sucesso.
echo ======================================================================
goto :end

:on_error
echo.
echo ======================================================================
echo   [ERRO] Ocorreu uma falha durante a execucao [Codigo de Saida: %EXIT_CODE%].
echo   Consulte as mensagens de erro detalhadas exibidas acima.
echo   Os arquivos de log completos estao disponiveis em:
echo   "%~dp0..\..\logs"
echo ======================================================================
echo.

:fail_pause
if /i not "%CI%"=="true" (
    echo Pressione qualquer tecla para fechar esta janela...
    pause >nul
)
exit /b %EXIT_CODE%

:end
if /i not "%CI%"=="true" (
    echo Pressione qualquer tecla para continuar...
    pause >nul
)
exit /b 0

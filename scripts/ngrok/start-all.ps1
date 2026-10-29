# ==============================================================================
# RemoteDesk - Orquestrador Geral de Conexão Externa (start-all.ps1)
# Inicializa Servidor de Sinalização + Túnel ngrok + Windows Agent
# ==============================================================================
[CmdletBinding()]
param(
    [string]$Domain = "",
    [switch]$SkipAgent,
    [switch]$SkipVerify
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = [System.IO.Path]::GetFullPath((Resolve-Path "$ScriptDir\..\..").Path)

# Caminhos absolutos garantidos para os dois projetos e seus diretorios
$AgentDir = [System.IO.Path]::GetFullPath((Join-Path $ProjectRoot "windows-agent\src\RemoteDesk.Agent"))
$AgentProj = [System.IO.Path]::GetFullPath((Join-Path $AgentDir "RemoteDesk.Agent.csproj"))
$AgentBuildOutputDir = [System.IO.Path]::GetFullPath((Join-Path $AgentDir "bin\Debug\net8.0"))
$AgentDll = [System.IO.Path]::GetFullPath((Join-Path $AgentBuildOutputDir "RemoteDesk.Agent.dll"))
$AgentExe = [System.IO.Path]::GetFullPath((Join-Path $AgentBuildOutputDir "RemoteDesk.Agent.exe"))

$ServerDir = [System.IO.Path]::GetFullPath((Join-Path $ProjectRoot "signaling-server\src\RemoteDesk.SignalingServer"))
$ServerProj = [System.IO.Path]::GetFullPath((Join-Path $ServerDir "RemoteDesk.SignalingServer.csproj"))

$UrlFile = Join-Path $ScriptDir ".ngrok-url.txt"
$AgentPidFile = Join-Path $ScriptDir ".agent.pid"
$AgentSourceUrlFile = [System.IO.Path]::GetFullPath((Join-Path $AgentDir ".ngrok-url.txt"))
$AgentOutputUrlFile = [System.IO.Path]::GetFullPath((Join-Path $AgentBuildOutputDir ".ngrok-url.txt"))
$LogsDir = Join-Path $ProjectRoot "logs"
$WindowsAgentBuildLog = Join-Path $LogsDir "windows_agent_build.log"
$AgentRuntimeLog = Join-Path $LogsDir "windows_agent_runtime.log"
$AgentRuntimeErrorLog = Join-Path $LogsDir "windows_agent_runtime_error.log"

. (Join-Path $ScriptDir "agent-process.ps1")

# Garante que a sessao atual utilize a raiz do projeto como base
Set-Location "$ProjectRoot"

if (-not (Test-Path $LogsDir)) {
    New-Item -ItemType Directory -Path $LogsDir -Force | Out-Null
}
$StartAllLog = Join-Path $LogsDir "start_all.log"

$startTime = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
Add-Content -Path $StartAllLog -Value "[$startTime] INÍCIO DO ORQUESTRADOR: Domain='$Domain', SkipAgent=$SkipAgent, SkipVerify=$SkipVerify" -Encoding UTF8
Add-Content -Path $StartAllLog -Value "[$startTime] Raiz do Projeto: '$ProjectRoot'" -Encoding UTF8
Add-Content -Path $StartAllLog -Value "[$startTime] Projeto do Servidor: '$ServerProj'" -Encoding UTF8
Add-Content -Path $StartAllLog -Value "[$startTime] Projeto do Agent: '$AgentProj'" -Encoding UTF8

function Log-And-Fail {
    param(
        [string]$StepTitle,
        [string]$ErrorMessage,
        [int]$ExitCode = 1
    )
    $timestamp = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    $logMsg = "[$timestamp] [ERRO] $StepTitle - $ErrorMessage (Código: $ExitCode)"
    Add-Content -Path $StartAllLog -Value $logMsg -Encoding UTF8

    Write-Host ""
    Write-Host "======================================================================" -ForegroundColor Red
    Write-Host "  [ERRO NA INICIALIZAÇÃO] Falha em: $StepTitle" -ForegroundColor Red
    Write-Host "  Código de Saída: $ExitCode" -ForegroundColor Red
    Write-Host "  Detalhe: $ErrorMessage" -ForegroundColor Yellow
    Write-Host "  Consulte os logs detalhados salvos em:" -ForegroundColor White
    Write-Host "  $LogsDir" -ForegroundColor Cyan
    Write-Host "======================================================================" -ForegroundColor Red
    Write-Host ""

    # Se executado diretamente pelo PowerShell (fora do BAT), aguarda tecla do usuário
    if ((-not $env:RUN_FROM_BAT) -and [Environment]::UserInteractive -and (-not $env:CI)) {
        Write-Host "Pressione qualquer tecla para sair..." -ForegroundColor Gray
        try { [Console]::ReadKey($true) | Out-Null } catch { }
    }
    exit $ExitCode
}

# Validacao previa da existencia dos dois arquivos .csproj reais
if (-not (Test-Path "$ServerProj")) {
    Log-And-Fail -StepTitle "Validação de Caminhos" -ErrorMessage "Arquivo do Servidor de Sinalização não encontrado em: '$ServerProj'" -ExitCode 1
}
if (-not (Test-Path "$AgentProj")) {
    Log-And-Fail -StepTitle "Validação de Caminhos" -ErrorMessage "Arquivo do Windows Agent não encontrado em: '$AgentProj'" -ExitCode 1
}

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "  REMOTEDESK - ORQUESTRADOR DE ACESSO REMOTO EXTERNO VIA NGROK         " -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Validação de Pré-requisitos
Write-Host "[PASSO 1/5] Verificando pré-requisitos do sistema..." -ForegroundColor Yellow
& "$ScriptDir\check-prerequisites.ps1"
if (-not $? -or ($LASTEXITCODE -ne $null -and $LASTEXITCODE -ne 0)) {
    Log-And-Fail -StepTitle "Passo 1/5 - Pré-requisitos" -ErrorMessage "Um ou mais pré-requisitos (.NET 8 SDK ou ngrok) não foram atendidos." -ExitCode 1
}
Write-Host ""

# 2. Inicialização do Servidor de Sinalização Local (Porta 5000)
Write-Host "[PASSO 2/5] Inicializando Servidor de Sinalização local..." -ForegroundColor Yellow
& "$ScriptDir\start-signaling.ps1"
if (-not $? -or ($LASTEXITCODE -ne $null -and $LASTEXITCODE -ne 0)) {
    Log-And-Fail -StepTitle "Passo 2/5 - Servidor de Sinalização" -ErrorMessage "Falha na compilação ou inicialização do Servidor de Sinalização. Verifique signaling_server_build.log." -ExitCode 1
}
Write-Host ""

# 3. Abertura do Túnel ngrok (Porta 5000)
Write-Host "[PASSO 3/5] Abrindo túnel público ngrok..." -ForegroundColor Yellow
if ($Domain -ne "") {
    & "$ScriptDir\start-tunnel.ps1" -Domain $Domain
} else {
    & "$ScriptDir\start-tunnel.ps1"
}
if (-not $? -or ($LASTEXITCODE -ne $null -and $LASTEXITCODE -ne 0)) {
    Log-And-Fail -StepTitle "Passo 3/5 - Túnel ngrok" -ErrorMessage "Falha ao abrir túnel ngrok na porta 5000." -ExitCode 1
}

$publicUrl = ""
if (Test-Path $UrlFile) {
    $publicUrl = (Get-Content $UrlFile -Raw).Trim()
}

if (-not $publicUrl) {
    Log-And-Fail -StepTitle "Passo 3/5 - Túnel ngrok" -ErrorMessage "Não foi possível obter a URL pública gerada pelo ngrok." -ExitCode 1
}
Write-Host ""

# 4. Verificação Externa da Conectividade do Túnel
if (-not $SkipVerify) {
    Write-Host "[PASSO 4/5] Testando túnel público externamente..." -ForegroundColor Yellow
    & "$ScriptDir\verify-tunnel.ps1" -Url $publicUrl
    if (-not $? -or ($LASTEXITCODE -ne $null -and $LASTEXITCODE -ne 0)) {
        Log-And-Fail -StepTitle "Passo 4/5 - Verificação do túnel ngrok" -ErrorMessage "O túnel público não respondeu corretamente nos testes de conectividade." -ExitCode 1
    }
    Write-Host ""
} else {
    Write-Host "[PASSO 4/5] Verificação de túnel pulada (-SkipVerify)." -ForegroundColor Gray
}

# 5. Inicialização do Windows Host Agent
Write-Host "[PASSO 5/5] Preparando Windows Host Agent..." -ForegroundColor Yellow

if (-not (Test-Path "$AgentProj")) {
    Log-And-Fail -StepTitle "Passo 5/5 - Windows Agent" -ErrorMessage "Arquivo do Windows Agent não encontrado em: '$AgentProj'" -ExitCode 1
}

# Evita que uma sessao anterior continue bloqueando as DLLs compiladas.
$stoppedAgentProcesses = Stop-RemoteDeskAgentProcesses -ProjectRoot $ProjectRoot
if ($stoppedAgentProcesses -gt 0) {
    Write-Host "[AVISO] $stoppedAgentProcesses processo(s) residual(is) do Windows Agent foram encerrados." -ForegroundColor Yellow
}
Remove-Item $AgentPidFile -Force -ErrorAction SilentlyContinue

$env:DOTNET_CLI_DO_NOT_USE_MSBUILD_SERVER = "1"
& dotnet build-server shutdown 2>&1 | Out-Null
Start-Sleep -Milliseconds 500

# Remove somente os binarios compilados para nunca aceitar um artefato antigo como novo.
foreach ($oldArtifact in @($AgentDll, $AgentExe)) {
    if (Test-Path $oldArtifact) {
        try {
            Remove-Item $oldArtifact -Force -ErrorAction Stop
        } catch {
            $removalMessage = "Nao foi possivel remover o artefato anterior '$oldArtifact': $($_.Exception.Message)"
            Set-Content -Path $WindowsAgentBuildLog -Value $removalMessage -Encoding UTF8
            Log-And-Fail -StepTitle "Passo 5/5 - Preparacao do Windows Agent" -ErrorMessage "$removalMessage Verifique windows_agent_build.log." -ExitCode 1
        }
    }
}

# Compila o .csproj diretamente e registra a saida completa, inclusive em caso de falha.
Write-Host "Compilando Windows Agent em '$AgentProj'..." -ForegroundColor Gray
$buildStartedAt = Get-Date
$buildArgs = @(
    "build",
    $AgentProj,
    "-c",
    "Debug",
    "--nologo",
    "-v",
    "normal",
    "--no-incremental",
    "/nodeReuse:false",
    "/m:1",
    "-p:UseSharedCompilation=false",
    "-p:DisableRarCache=true"
)

Push-Location "$AgentDir"
try {
    $agentBuildOut = & dotnet @buildArgs 2>&1
    $agentBuildExit = $LASTEXITCODE
}
finally {
    Pop-Location
}

$agentBuildText = ($agentBuildOut | Out-String)
$agentDllReady = (Test-Path $AgentDll) -and
    ((Get-Item $AgentDll).LastWriteTimeUtc -ge $buildStartedAt.ToUniversalTime().AddSeconds(-2))
$agentExeReady = (Test-Path $AgentExe) -and
    ((Get-Item $AgentExe).LastWriteTimeUtc -ge $buildStartedAt.ToUniversalTime().AddSeconds(-2))
$buildStatus = if ($agentBuildExit -eq 0 -and $agentDllReady) { "SUCESSO" } else { "FALHA" }

$agentBuildLogContent = @"
================================================================================
REMOTEDESK BUILD LOG - WINDOWS AGENT (.NET 8)
================================================================================
Data e Horario:       $buildStartedAt
Projeto:              $AgentProj
Comando:              dotnet $($buildArgs -join " ")
Codigo de Saida:      $agentBuildExit
Status:               $buildStatus
Artefato DLL:         $AgentDll (Pronto: $agentDllReady)
Artefato EXE:         $AgentExe (Pronto: $agentExeReady)
================================================================================
SAIDA COMPLETA DO PROCESSO:
================================================================================
$agentBuildText
================================================================================
"@
Set-Content -Path $WindowsAgentBuildLog -Value $agentBuildLogContent -Encoding UTF8

if ($agentBuildExit -ne 0) {
    Log-And-Fail -StepTitle "Passo 5/5 - Compilação do Windows Agent" -ErrorMessage "Falha na compilacao do Windows Agent (codigo $agentBuildExit). Verifique windows_agent_build.log." -ExitCode $agentBuildExit
}
if (-not $agentDllReady) {
    Log-And-Fail -StepTitle "Passo 5/5 - Validacao do artefato" -ErrorMessage "A DLL nova do Windows Agent nao foi gerada em '$AgentDll'. Verifique windows_agent_build.log." -ExitCode 1
}

Write-Host "[OK] Windows Agent compilado: $AgentDll" -ForegroundColor Green

# O Agent le a URL ngrok a partir de AppContext.BaseDirectory.
Set-Content -Path $AgentSourceUrlFile -Value $publicUrl -Encoding UTF8
Set-Content -Path $AgentOutputUrlFile -Value $publicUrl -Encoding UTF8

# Consulta o PIN diretamente na DLL recem-compilada, sem criar um processo 'dotnet run' residual.
$pinOutput = & dotnet $AgentDll --pin 2>&1
$pinExit = $LASTEXITCODE
$pinText = ($pinOutput | Out-String)
if ($pinExit -ne 0) {
    Add-Content -Path $WindowsAgentBuildLog -Value "`nConsulta de PIN (codigo $pinExit):`n$pinText" -Encoding UTF8
    Log-And-Fail -StepTitle "Passo 5/5 - Leitura do PIN do Agent" -ErrorMessage "Falha ao consultar o PIN do Windows Agent (codigo $pinExit). Verifique windows_agent_build.log." -ExitCode $pinExit
}

$pinMatch = [regex]::Match($pinText, "PIN DE ACESSO / PAREAMENTO ATUAL:\s*([^\s\r\n]+)")
$agentPin = if ($pinMatch.Success) { $pinMatch.Groups[1].Value } else { "consulte a saida do Agent" }

if (-not $SkipAgent) {
    Write-Host "Iniciando Windows Host Agent a partir do artefato recem-compilado..." -ForegroundColor White
    if ($agentExeReady) {
        $agentProcessFile = $AgentExe
        $agentProcessArgs = @()
    } else {
        $agentProcessFile = "dotnet"
        $agentProcessArgs = @("`"$AgentDll`"")
    }

    $agentStartParams = @{
        FilePath = $agentProcessFile
        WorkingDirectory = $AgentBuildOutputDir
        WindowStyle = "Hidden"
        RedirectStandardOutput = $AgentRuntimeLog
        RedirectStandardError = $AgentRuntimeErrorLog
        PassThru = $true
        ErrorAction = "Stop"
    }
    if ($agentProcessArgs.Count -gt 0) {
        $agentStartParams.ArgumentList = $agentProcessArgs
    }

    try {
        $agentProc = Start-Process @agentStartParams
    } catch {
        Log-And-Fail -StepTitle "Passo 5/5 - Execucao do Windows Agent" -ErrorMessage "Nao foi possivel iniciar o Windows Agent: $($_.Exception.Message)" -ExitCode 1
    }

    Start-Sleep -Seconds 2
    $agentProc.Refresh()
    if ($agentProc.HasExited) {
        $agentReportedExitCode = $agentProc.ExitCode
        $agentExitCode = if ($agentReportedExitCode -ne 0) { [int]$agentReportedExitCode } else { 1 }
        Remove-Item $AgentPidFile -Force -ErrorAction SilentlyContinue
        Log-And-Fail -StepTitle "Passo 5/5 - Execucao do Windows Agent" -ErrorMessage "O Windows Agent encerrou imediatamente (codigo $agentReportedExitCode). Verifique windows_agent_runtime_error.log." -ExitCode $agentExitCode
    }

    Set-Content -Path $AgentPidFile -Value $agentProc.Id
    Write-Host "[OK] Windows Host Agent iniciado (PID real: $($agentProc.Id))." -ForegroundColor Green
    Write-Host "PIN de pareamento: $agentPin" -ForegroundColor Yellow
    Write-Host "Logs do Agent: '$AgentRuntimeLog' e '$AgentRuntimeErrorLog'" -ForegroundColor Gray
} else {
    Write-Host "Inicializacao do Agent pulada (-SkipAgent). Para executar manualmente:" -ForegroundColor Gray
    Write-Host "  dotnet `"$AgentDll`"" -ForegroundColor Cyan
    Write-Host "PIN de pareamento: $agentPin" -ForegroundColor Yellow
}

$endTime = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
Add-Content -Path $StartAllLog -Value "[$endTime] SUCESSO: Todos os serviços inicializados. URL: $publicUrl" -Encoding UTF8

# 6. Painel de Instruções Finais para o Usuário
Write-Host ""
Write-Host "======================================================================" -ForegroundColor Green
Write-Host "  TÚNEL REMOTEDESK PRONTO E OPERACIONAL PARA ACESSO EXTERNO!         " -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  -> URL DO SERVIDOR DE SINALIZAÇÃO PARA COLOCAR NO APP ANDROID:" -ForegroundColor White
Write-Host "     $publicUrl" -ForegroundColor Cyan
Write-Host ""
Write-Host "  -> INSTRUÇÕES PARA CONEXÃO 4G/5G PELO CELULAR:" -ForegroundColor White
Write-Host "     1. Abra o aplicativo RemoteDesk (DoHsktoP) no seu celular Android."
Write-Host "     2. No campo 'Servidor de Sinalização', cole exatamente a URL acima:"
Write-Host "        $publicUrl"
Write-Host "     3. No campo 'PIN de Pareamento / Acesso', digite o PIN do Agent:"
Write-Host "        (Use o PIN exibido acima ou execute: dotnet `"$AgentDll`" --pin)"
Write-Host "     4. Toque no botão 'CONECTAR AO HOST'."
Write-Host "     5. O celular negociará WebSockets + WebRTC através do ngrok e se conectará"
Write-Host "        diretamente ao seu PC sem exigir que ninguém aceite em casa!"
Write-Host ""
Write-Host "  -> PARA FINALIZAR A SESSÃO:" -ForegroundColor White
Write-Host "     Execute: .\stop-session.ps1" -ForegroundColor Yellow
Write-Host "======================================================================" -ForegroundColor Green

if ((-not $env:RUN_FROM_BAT) -and [Environment]::UserInteractive -and (-not $env:CI)) {
    Write-Host ""
    Write-Host "Pressione qualquer tecla para continuar..." -ForegroundColor Gray
    try { [Console]::ReadKey($true) | Out-Null } catch { }
}

# ==============================================================================
# RemoteDesk - Inicialização do Servidor de Sinalização Local (Porta 5000)
# ==============================================================================
[CmdletBinding()]
param()

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = [System.IO.Path]::GetFullPath((Resolve-Path "$ScriptDir\..\..").Path)
$ServerDir = [System.IO.Path]::GetFullPath((Join-Path $ProjectRoot "signaling-server\src\RemoteDesk.SignalingServer"))
$ServerProj = [System.IO.Path]::GetFullPath((Join-Path $ServerDir "RemoteDesk.SignalingServer.csproj"))
$PidFile = Join-Path $ScriptDir ".signaling.pid"
$BuildOutputDir = [System.IO.Path]::GetFullPath((Join-Path $ProjectRoot ".build\signaling\bin\Debug\net8.0"))
$TargetDll = [System.IO.Path]::GetFullPath((Join-Path $BuildOutputDir "RemoteDesk.SignalingServer.dll"))
$TargetExe = [System.IO.Path]::GetFullPath((Join-Path $BuildOutputDir "RemoteDesk.SignalingServer.exe"))
$LogsDir = [System.IO.Path]::GetFullPath((Join-Path $ProjectRoot "logs"))
$HealthUrl = "http://127.0.0.1:5000/health"
$ServerUrl = "http://0.0.0.0:5000"
$RuntimeLogFile = Join-Path $LogsDir "signaling_server_runtime.log"
$RuntimeErrorLogFile = Join-Path $LogsDir "signaling_server_runtime_error.log"

if (-not (Test-Path $LogsDir)) {
    New-Item -ItemType Directory -Path $LogsDir -Force | Out-Null
}

$BuildLogFile = Join-Path $LogsDir "signaling_server_build.log"
$StartupLogFile = Join-Path $LogsDir "signaling_server_startup.log"

function Write-BuildLog {
    param(
        [string]$Stage,
        [int]$ExitCode,
        [string]$CommandExecuted,
        [string]$OutputText,
        [string]$Diagnosis = ""
    )
    $timestamp = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    $logContent = @"
================================================================================
REMOTEDESK BUILD & STARTUP LOG - SERVIDOR DE SINALIZAÇÃO (.NET 8)
================================================================================
1. Data e Horário:       $timestamp
2. Componente:           Servidor de Sinalização (RemoteDesk.SignalingServer)
   Diretório do Servidor:$ServerDir
   Arquivo de Projeto:   $ServerProj
   Comando Executado:    $CommandExecuted
   Etapa:                $Stage
3. Resultado e Código:   $($ExitCode) $(if ($ExitCode -eq 0) { "(SUCESSO)" } else { "(FALHA)" })
   Artefato DLL:         $TargetDll
   Artefato Executável:  $TargetExe
===============================================================================
4. DIAGNÓSTICO E LOCALIZAÇÃO DE ERROS:
===============================================================================
$Diagnosis
===============================================================================
5. SAÍDA COMPLETA DO PROCESSO:
===============================================================================
$OutputText
===============================================================================
Fim do log. Arquivo gerado em: $BuildLogFile
===============================================================================
"@
    Set-Content -Path $BuildLogFile -Value $logContent -Encoding UTF8
    Add-Content -Path $StartupLogFile -Value "`n$logContent" -Encoding UTF8
}

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "  REMOTEDESK - INICIALIZANDO SERVIDOR DE SINALIZAÇÃO (.NET 8)         " -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

if (-not (Test-Path "$ServerProj")) {
    $msg = "Arquivo de projeto do Servidor de Sinalização não encontrado em: '$ServerProj'"
    Write-BuildLog -Stage "Validação do Projeto" -ExitCode 1 -CommandExecuted "Test-Path '$ServerProj'" -OutputText $msg -Diagnosis $msg
    Write-Error $msg
    exit 1
}

# 1. Verifica se o servidor já está ativo e saudável
try {
    $standaloneHealth = Invoke-RestMethod -Uri $HealthUrl -TimeoutSec 2 -ErrorAction SilentlyContinue
    if ($standaloneHealth -and $standaloneHealth.status -eq "Healthy") {
        Write-Host "[INFO] O Servidor de Sinalização já está em execução e saudável no endpoint $HealthUrl." -ForegroundColor Green
        exit 0
    }
} catch { }

# 2. Encerra processos residuais ou órfãos na porta 5000 para liberar arquivos travados
if (Test-Path $PidFile) {
    $oldPid = Get-Content $PidFile -ErrorAction SilentlyContinue
    if ($oldPid) {
        $p = Get-Process -Id $oldPid -ErrorAction SilentlyContinue
        if ($p) {
            Write-Host "[AVISO] Encerrando processo registrado anterior (PID $oldPid)..." -ForegroundColor Yellow
            Stop-Process -Id $oldPid -Force -ErrorAction SilentlyContinue
            try { $p.WaitForExit(1000) } catch { }
        }
    }
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
}

try {
    $conns = Get-NetTCPConnection -LocalPort 5000 -State Listen -ErrorAction SilentlyContinue
    foreach ($c in $conns) {
        $p = Get-Process -Id $c.OwningProcess -ErrorAction SilentlyContinue
        if ($p -and ($p.ProcessName -match "(?i)(dotnet|RemoteDesk)")) {
            Write-Host "[AVISO] Liberando porta 5000 ocupada por processo residual '$($p.ProcessName)' (PID: $($p.Id))..." -ForegroundColor Yellow
            Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue
            try { $p.WaitForExit(1000) } catch { }
        }
    }
} catch { }

# 3. Desativa servidor em segundo plano do MSBuild para evitar nós persistentes com arquivos bloqueados
$env:DOTNET_CLI_DO_NOT_USE_MSBUILD_SERVER = "1"
& dotnet build-server shutdown 2>&1 | Out-Null
Start-Sleep -Milliseconds 500

# 4. Compilação com isolamento estrito de diretório e flags anti-bloqueio (/nodeReuse:false /m:1)
Write-Host "[1/4] Compilando Servidor de Sinalização (.NET 8)..." -ForegroundColor White
Push-Location "$ServerDir"
try {
    $buildArgs = @("build", "$ServerProj", "-c", "Debug", "--nologo", "-v", "normal", "/nodeReuse:false", "/m:1")
    $firstCmd = "dotnet " + ($buildArgs -join " ")
    $firstBuildOut = & dotnet @buildArgs 2>&1
    $firstExit = $LASTEXITCODE

    if ($firstExit -eq 0) {
        Write-BuildLog -Stage "Compilação Inicial" -ExitCode 0 -CommandExecuted $firstCmd -OutputText ($firstBuildOut | Out-String) -Diagnosis "Compilação concluída com sucesso na primeira tentativa."
        Write-Host "[OK] Servidor de Sinalização compilado com sucesso." -ForegroundColor Green
    } else {
        Write-Host "[AVISO] Falha na compilação inicial (Código: $firstExit). Iniciando recuperação limpa..." -ForegroundColor Yellow
        
        & dotnet build-server shutdown 2>&1 | Out-Null
        Start-Sleep -Milliseconds 500
        
        $cleanCmd = "dotnet clean `"$ServerProj`" -c Debug --nologo /nodeReuse:false"
        $cleanOut = & dotnet clean "$ServerProj" -c Debug --nologo /nodeReuse:false 2>&1
        
        $restoreCmd = "dotnet restore `"$ServerProj`" --nologo /nodeReuse:false"
        $restoreOut = & dotnet restore "$ServerProj" --nologo /nodeReuse:false 2>&1
        
        $cleanBuildCmd = "dotnet " + ($buildArgs -join " ")
        $cleanBuildOut = & dotnet @buildArgs 2>&1
        $cleanExit = $LASTEXITCODE
        
        $fullCleanOutput = "--- TENTATIVA INICIAL (Código: $firstExit) ---`n" + ($firstBuildOut | Out-String) + `
                           "`n--- LIMPEZA (dotnet clean) ---`n" + ($cleanOut | Out-String) + `
                           "`n--- RESTAURAÇÃO (dotnet restore) ---`n" + ($restoreOut | Out-String) + `
                           "`n--- RECOMPILAÇÃO LIMPA (Código: $cleanExit) ---`n" + ($cleanBuildOut | Out-String)
        
        if ($cleanExit -eq 0) {
            Write-BuildLog -Stage "Recompilação Limpa" -ExitCode 0 -CommandExecuted "$cleanCmd && $restoreCmd && $cleanBuildCmd" -OutputText $fullCleanOutput -Diagnosis "Recompilação limpa concluída com sucesso após encerramento do build-server e restauração."
            Write-Host "[OK] Servidor de Sinalização recompilado com sucesso após recuperação." -ForegroundColor Green
        } else {
            $errorLines = @()
            foreach ($line in ($cleanBuildOut | Out-String).Split("`n")) {
                $trimmed = $line.Trim()
                if ($trimmed -match "(?i)(error|MSB\d+|CS\d+|fatal)") {
                    $errorLines += $trimmed
                }
            }
            $diagnosisText = "Falha crítica na compilação do projeto $ServerProj.`n" + ($errorLines -join "`n")
            Write-BuildLog -Stage "Falha na Recompilação" -ExitCode $cleanExit -CommandExecuted $cleanBuildCmd -OutputText $fullCleanOutput -Diagnosis $diagnosisText
            
            Write-Host ""
            Write-Host "======================================================================" -ForegroundColor Red
            Write-Host "  [ERRO CRÍTICO] Falha na compilação do Servidor de Sinalização (.NET 8)" -ForegroundColor Red
            Write-Host "  Código de Saída: $cleanExit" -ForegroundColor Red
            Write-Host "  Log detalhado salvo em: $BuildLogFile" -ForegroundColor Yellow
            Write-Host "======================================================================" -ForegroundColor Red
            Write-Host "Erros detectados:" -ForegroundColor White
            foreach ($el in ($errorLines | Select-Object -First 10)) {
                Write-Host "  -> $el" -ForegroundColor Red
            }
            Write-Host "======================================================================" -ForegroundColor Red
            Write-Host ""
            exit 1
        }
    }
}
finally {
    Pop-Location
}

# 5. Inicia o processo do servidor a partir do diretório de saída curto do MSBuild
Write-Host "[2/4] Iniciando processo do servidor na porta local 5000..." -ForegroundColor White

if (Test-Path $TargetDll) {
    $processFile = "dotnet"
    $processArgs = @("`"$TargetDll`"", "--urls", $ServerUrl)
} elseif (Test-Path $TargetExe) {
    $processFile = $TargetExe
    $processArgs = @("--urls", $ServerUrl)
} else {
    $msg = "Nenhum artefato do Servidor de Sinalização foi gerado em: '$BuildOutputDir'."
    Write-BuildLog -Stage "Validação do Artefato" -ExitCode 1 -CommandExecuted "dotnet build `"$ServerProj`"" -OutputText $msg -Diagnosis $msg
    Write-Error $msg
    exit 1
}

$proc = Start-Process -FilePath $processFile `
    -ArgumentList $processArgs `
    -WorkingDirectory $BuildOutputDir `
    -WindowStyle Hidden `
    -RedirectStandardOutput $RuntimeLogFile `
    -RedirectStandardError $RuntimeErrorLogFile `
    -PassThru
if ($proc) {
    Set-Content -Path $PidFile -Value $proc.Id
    Write-Host "[3/4] Processo iniciado com PID: $($proc.Id). Aguardando inicialização..." -ForegroundColor White
} else {
    $msg = "Falha ao iniciar processo do servidor de sinalização."
    Write-BuildLog -Stage "Inicialização do Processo" -ExitCode 1 -CommandExecuted "$processFile $($processArgs -join ' ')" -OutputText $msg -Diagnosis $msg
    Write-Error $msg
    exit 1
}

# 6. Aguarda confirmação ativa do endpoint de saúde (/health)
$maxAttempts = 20
$attempt = 0
$healthy = $false
while ($attempt -lt $maxAttempts -and -not $healthy) {
    Start-Sleep -Seconds 1
    $attempt++
    
    if ($proc.HasExited) {
        Write-Host ""
        Write-Host "[ERRO] O processo do Servidor de Sinalização (PID $($proc.Id)) encerrou prematuramente com código: $($proc.ExitCode)." -ForegroundColor Red
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
        $msg = "Processo encerrou prematuramente com código $($proc.ExitCode)."
        Write-BuildLog -Stage "Execução do Servidor" -ExitCode $proc.ExitCode -CommandExecuted "$processFile $($processArgs -join ' ')" -OutputText $msg -Diagnosis $msg
        exit 1
    }
    
    try {
        $res = Invoke-RestMethod -Uri $HealthUrl -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($res -and $res.status -eq "Healthy") {
            $healthy = $true
        }
    } catch { }
}

Write-Host ""
if ($healthy) {
    Write-Host "[4/4] Servidor de Sinalização OPERACIONAL em $ServerUrl" -ForegroundColor Green
    Write-Host "  -> Health Endpoint: $HealthUrl" -ForegroundColor Gray
    Write-Host "  -> WebSocket Rota:  ws://127.0.0.1:5000/ws/signaling" -ForegroundColor Gray
    Write-Host "======================================================================" -ForegroundColor Cyan
    exit 0
} else {
    Write-Host "[ERRO] O servidor foi iniciado (PID $($proc.Id)), mas o endpoint de saúde não respondeu em tempo hábil (20s)." -ForegroundColor Red
    try {
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    } catch { }
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    $msg = "Timeout no health check após 20 tentativas."
    Write-BuildLog -Stage "Health Check" -ExitCode 1 -CommandExecuted "Invoke-RestMethod $HealthUrl" -OutputText $msg -Diagnosis $msg
    Write-Error $msg
    exit 1
}

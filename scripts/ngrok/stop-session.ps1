# ==============================================================================
# RemoteDesk - Encerramento Seguro da Sessão ngrok e Serviços Locais
# ==============================================================================
[CmdletBinding()]
param()

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = [System.IO.Path]::GetFullPath((Resolve-Path "$ScriptDir\..\..").Path)
$SignalingPidFile = Join-Path $ScriptDir ".signaling.pid"
$NgrokPidFile = Join-Path $ScriptDir ".ngrok.pid"
$UrlFile = Join-Path $ScriptDir ".ngrok-url.txt"
$AgentUrlFile = [System.IO.Path]::GetFullPath((Join-Path $ProjectRoot "windows-agent\src\RemoteDesk.Agent\.ngrok-url.txt"))
$AgentOutputUrlFile = [System.IO.Path]::GetFullPath((Join-Path $ProjectRoot "windows-agent\src\RemoteDesk.Agent\bin\Debug\net8.0\.ngrok-url.txt"))

. (Join-Path $ScriptDir "agent-process.ps1")

$stopErrors = 0

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "  REMOTEDESK - ENCERRANDO SESSÃO NGROK E SERVIÇOS (ENCERRAMENTO LIMPO)" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

# 1. Encerra o túnel ngrok associado à sessão
if (Test-Path $NgrokPidFile) {
    $ngrokPid = Get-Content $NgrokPidFile -ErrorAction SilentlyContinue
    if ($ngrokPid) {
        $p = Get-Process -Id $ngrokPid -ErrorAction SilentlyContinue
        if ($p) {
            if ($p.ProcessName -notmatch "(?i)^ngrok") {
                Write-Host "[1/3] PID do ngrok ($ngrokPid) foi reutilizado por outro processo; ele nao foi encerrado." -ForegroundColor Yellow
                $stopErrors++
            } else {
                Write-Host -NoNewline "[1/3] Encerrando processo do túnel ngrok (PID $ngrokPid)... " -ForegroundColor White
                try {
                    Stop-Process -Id $ngrokPid -Force -ErrorAction Stop
                    Write-Host "OK" -ForegroundColor Green
                } catch {
                    Write-Host "ERRO ($_)" -ForegroundColor Yellow
                    $stopErrors++
                }
            }
        } else {
            Write-Host "[1/3] Processo do ngrok (PID $ngrokPid) já não estava em execução." -ForegroundColor Gray
        }
    }
    Remove-Item $NgrokPidFile -Force -ErrorAction SilentlyContinue
} else {
    Write-Host "[1/3] Nenhum arquivo de PID do ngrok encontrado." -ForegroundColor Gray
}

# 2. Encerra o servidor de sinalização associado à sessão
if (Test-Path $SignalingPidFile) {
    $sigPid = Get-Content $SignalingPidFile -ErrorAction SilentlyContinue
    if ($sigPid) {
        $p = Get-Process -Id $sigPid -ErrorAction SilentlyContinue
        if ($p) {
            $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $sigPid" -ErrorAction SilentlyContinue
            $isSignalingProcess = $processInfo -and
                (([string]$processInfo.CommandLine).IndexOf("RemoteDesk.SignalingServer", [System.StringComparison]::OrdinalIgnoreCase) -ge 0)
            if (-not $isSignalingProcess) {
                Write-Host "[2/3] PID do servidor ($sigPid) foi reutilizado por outro processo; ele nao foi encerrado." -ForegroundColor Yellow
                $stopErrors++
            } else {
                Write-Host -NoNewline "[2/3] Encerrando Servidor de Sinalização (PID $sigPid)... " -ForegroundColor White
                try {
                    Stop-Process -Id $sigPid -Force -ErrorAction Stop
                    Write-Host "OK" -ForegroundColor Green
                } catch {
                    Write-Host "ERRO ($_)" -ForegroundColor Yellow
                    $stopErrors++
                }
            }
        } else {
            Write-Host "[2/3] Processo do Servidor (PID $sigPid) já não estava em execução." -ForegroundColor Gray
        }
    }
    Remove-Item $SignalingPidFile -Force -ErrorAction SilentlyContinue
} else {
    Write-Host "[2/3] Nenhum arquivo de PID do Servidor de Sinalização encontrado." -ForegroundColor Gray
}

# 3. Encerra o Windows Agent associado à sessão e limpa instancias orfas.
$AgentPidFile = Join-Path $ScriptDir ".agent.pid"
if (Test-Path $AgentPidFile) {
    $agentPid = Get-Content $AgentPidFile -ErrorAction SilentlyContinue
    if ($agentPid) {
        $registeredAgent = @(Get-RemoteDeskAgentProcesses -ProjectRoot $ProjectRoot | Where-Object { $_.ProcessId -eq [int]$agentPid })
        if ($registeredAgent.Count -gt 0) {
            Write-Host -NoNewline "[3/3] Encerrando Windows Host Agent (PID $agentPid)... " -ForegroundColor White
            Stop-RemoteDeskProcessTree -ProcessId ([int]$agentPid)
            Write-Host "OK" -ForegroundColor Green
        } elseif (Get-Process -Id $agentPid -ErrorAction SilentlyContinue) {
            Write-Host "[3/3] PID do Agent ($agentPid) foi reutilizado por outro processo; ele nao foi encerrado." -ForegroundColor Yellow
            $stopErrors++
        } else {
            Write-Host "[3/3] Processo do Agent (PID $agentPid) já não estava em execução." -ForegroundColor Gray
        }
    }
    Remove-Item $AgentPidFile -Force -ErrorAction SilentlyContinue
} else {
    Write-Host "[3/3] Nenhum arquivo de PID do Agent encontrado; verificando processos residuais..." -ForegroundColor Gray
}

$orphanAgentProcesses = @(Get-RemoteDeskAgentProcesses -ProjectRoot $ProjectRoot)
if ($orphanAgentProcesses.Count -gt 0) {
    Write-Host -NoNewline "[3/3] Encerrando $($orphanAgentProcesses.Count) processo(s) residual(is) do Agent... " -ForegroundColor White
    Stop-RemoteDeskAgentProcesses -ProjectRoot $ProjectRoot | Out-Null
    $remainingAgentProcesses = @(Get-RemoteDeskAgentProcesses -ProjectRoot $ProjectRoot)
    if ($remainingAgentProcesses.Count -eq 0) {
        Write-Host "OK" -ForegroundColor Green
    } else {
        Write-Host "FALHA" -ForegroundColor Red
        $stopErrors++
    }
}

# 4. Limpeza dos arquivos temporários de URL
Remove-Item $UrlFile -Force -ErrorAction SilentlyContinue
Remove-Item $AgentUrlFile -Force -ErrorAction SilentlyContinue
Remove-Item $AgentOutputUrlFile -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "======================================================================" -ForegroundColor Cyan
if ($stopErrors -eq 0) {
    Write-Host "  SESSÃO ENCERRADA COM SUCESSO! Recursos liberados com segurança.     " -ForegroundColor Green
    Write-Host "======================================================================" -ForegroundColor Cyan
    exit 0
}

Write-Host "  SESSÃO ENCERRADA COM AVISOS. Verifique os processos acima.            " -ForegroundColor Yellow
Write-Host "======================================================================" -ForegroundColor Cyan
exit 1

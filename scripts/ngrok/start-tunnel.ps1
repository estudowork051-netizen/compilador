# ==============================================================================
# RemoteDesk - Abertura do Túnel ngrok para a Porta 5000
# ==============================================================================
[CmdletBinding()]
param(
    [string]$Domain = ""
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = [System.IO.Path]::GetFullPath((Resolve-Path "$ScriptDir\..\..").Path)
$PidFile = Join-Path $ScriptDir ".ngrok.pid"
$UrlFile = Join-Path $ScriptDir ".ngrok-url.txt"
$AgentUrlFile = [System.IO.Path]::GetFullPath((Join-Path $ProjectRoot "windows-agent\src\RemoteDesk.Agent\.ngrok-url.txt"))
$AgentOutputUrlFile = [System.IO.Path]::GetFullPath((Join-Path $ProjectRoot "windows-agent\src\RemoteDesk.Agent\bin\Debug\net8.0\.ngrok-url.txt"))
$LogsDir = Join-Path $ProjectRoot "logs"
$NgrokRuntimeLog = Join-Path $LogsDir "ngrok_runtime.log"
$NgrokRuntimeErrorLog = Join-Path $LogsDir "ngrok_runtime_error.log"

function Get-NgrokPublicUrl {
    try {
        $tunnelsJson = Invoke-RestMethod -Uri "http://127.0.0.1:4040/api/tunnels" -TimeoutSec 2 -ErrorAction Stop
        if (-not $tunnelsJson -or -not $tunnelsJson.tunnels) {
            return $null
        }

        $tunnels = @($tunnelsJson.tunnels)
        $matchingTunnels = @($tunnels | Where-Object {
            $_.config -and ([string]$_.config.addr) -match "(?i)(localhost|127\.0\.0\.1):5000$"
        })
        if ($matchingTunnels.Count -eq 0 -and $tunnels.Count -eq 1) {
            $matchingTunnels = $tunnels
        }
        if ($matchingTunnels.Count -eq 0) {
            return $null
        }

        $httpsTunnel = $matchingTunnels | Where-Object { $_.proto -eq "https" } | Select-Object -First 1
        if ($httpsTunnel) {
            return [string]$httpsTunnel.public_url
        }
        return [string]$matchingTunnels[0].public_url
    } catch {
        return $null
    }
}

function Save-NgrokUrl {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PublicUrl
    )

    Set-Content -Path $UrlFile -Value $PublicUrl
    Set-Content -Path $AgentUrlFile -Value $PublicUrl
    if (Test-Path (Split-Path -Parent $AgentOutputUrlFile)) {
        Set-Content -Path $AgentOutputUrlFile -Value $PublicUrl
    }
}

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "  REMOTEDESK - ABRINDO TÚNEL PÚBLICO NGROK (PORTA 5000)               " -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

# Reaproveita um tunel ja registrado somente se a API local confirmar a URL publica.
if (Test-Path $PidFile) {
    $existingPid = Get-Content $PidFile -ErrorAction SilentlyContinue
    $existingProcess = if ($existingPid) { Get-Process -Id $existingPid -ErrorAction SilentlyContinue } else { $null }
    if ($existingProcess -and $existingProcess.ProcessName -notmatch "(?i)^ngrok") {
        Write-Host "[AVISO] PID do ngrok ($existingPid) foi reutilizado por outro processo; ele nao sera encerrado." -ForegroundColor Yellow
        $existingProcess = $null
    }
    if ($existingProcess) {
        $existingUrl = Get-NgrokPublicUrl
        if ($existingUrl) {
            Save-NgrokUrl -PublicUrl $existingUrl
            Write-Host "[INFO] Tunel ngrok ja ativo e reutilizado (PID: $existingPid, URL: $existingUrl)." -ForegroundColor Green
            exit 0
        }

        Write-Host "[AVISO] O ngrok registrado (PID: $existingPid) nao possui uma URL valida; ele sera reiniciado." -ForegroundColor Yellow
        Stop-Process -Id $existingProcess.Id -Force -ErrorAction SilentlyContinue
        Start-Sleep -Milliseconds 500
    }
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
}

# Inicia o ngrok para a porta 5000 (onde o Signaling Server está ouvindo)
$ngrokArgs = @("http", "5000", "--log=stdout")
if ($Domain -ne "") {
    $ngrokArgs += "--domain=$Domain"
}

if (-not (Test-Path $LogsDir)) {
    New-Item -ItemType Directory -Path $LogsDir -Force | Out-Null
}

Write-Host "[1/3] Iniciando ngrok para encaminhar porta local 5000..." -ForegroundColor White
$ngrokCommand = Get-Command ngrok -ErrorAction SilentlyContinue
if (-not $ngrokCommand) {
    Write-Error "Falha ao iniciar o processo do ngrok: executavel nao encontrado no PATH."
    exit 1
}

$proc = Start-Process `
    -FilePath $ngrokCommand.Source `
    -ArgumentList $ngrokArgs `
    -WorkingDirectory $ProjectRoot `
    -WindowStyle Hidden `
    -RedirectStandardOutput $NgrokRuntimeLog `
    -RedirectStandardError $NgrokRuntimeErrorLog `
    -PassThru
if ($proc) {
    Set-Content -Path $PidFile -Value $proc.Id
    Write-Host "[2/3] ngrok iniciado com PID: $($proc.Id). Consultando API local para obter a URL pública..." -ForegroundColor White
} else {
    Write-Error "Falha ao iniciar o processo do ngrok."
    exit 1
}

# Consulta a API local do ngrok (http://127.0.0.1:4040/api/tunnels) para obter a URL atribuída
$publicUrl = $null
$maxAttempts = 12
$attempt = 0
while ($attempt -lt $maxAttempts -and -not $publicUrl) {
    Start-Sleep -Seconds 1
    $attempt++
    if ($proc.HasExited) {
        Write-Host "[ERRO] O processo do ngrok encerrou com codigo $($proc.ExitCode)." -ForegroundColor Red
        break
    }
    $publicUrl = Get-NgrokPublicUrl
}

Write-Host ""
if ($publicUrl) {
    # Salva nos arquivos de sessão para uso do Windows Agent e Android
    Save-NgrokUrl -PublicUrl $publicUrl

    Write-Host "[3/3] TÚNEL PÚBLICO NGROK ESTABELECIDO COM SUCESSO!" -ForegroundColor Green
    Write-Host ""
    Write-Host "======================================================================" -ForegroundColor Green
    Write-Host "  URL PÚBLICA PARA O ANDROID (4G/5G):                                " -ForegroundColor White
    Write-Host "  $publicUrl" -ForegroundColor Yellow
    Write-Host "======================================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "COMO CONECTAR O CELULAR FORA DE CASA (4G/5G):" -ForegroundColor Cyan
    Write-Host "  1. Abra o aplicativo DoHsktoP / RemoteDesk no celular." -ForegroundColor White
    Write-Host "  2. Toque em 'Configurações Avançadas de Servidor e Rede'." -ForegroundColor White
    Write-Host "  3. No campo 'Servidor de Sinalização', cole a URL:" -ForegroundColor White
    Write-Host "     $publicUrl" -ForegroundColor Yellow
    Write-Host "  4. Digite o PIN de 6 dígitos exibido no Windows Agent." -ForegroundColor White
    Write-Host "  5. Toque em 'Conectar ao Computador'." -ForegroundColor White
    Write-Host ""
    Write-Host "Nota sobre Plano Gratuito: O aplicativo Android já inclui o cabeçalho" -ForegroundColor DarkGray
    Write-Host "'ngrok-skip-browser-warning: true' para evitar a tela de aviso do ngrok." -ForegroundColor DarkGray
    Write-Host "======================================================================" -ForegroundColor Cyan
    exit 0
} else {
    if ($proc -and -not $proc.HasExited) {
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    }
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    Write-Host "[ERRO] Não foi possível obter a URL pública automaticamente pela API local do ngrok (http://127.0.0.1:4040/api/tunnels)." -ForegroundColor Red
    Write-Host "Verifique se o ngrok está rodando e se o authtoken está configurado corretamente." -ForegroundColor Yellow
    exit 1
}

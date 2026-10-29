# ==============================================================================
# RemoteDesk - Verificação de Conectividade Externa via Túnel ngrok
# ==============================================================================
[CmdletBinding()]
param(
    [string]$Url = ""
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$UrlFile = Join-Path $ScriptDir ".ngrok-url.txt"

if ($Url -eq "") {
    if (Test-Path $UrlFile) {
        $Url = (Get-Content $UrlFile -Raw).Trim()
    }
}

if ($Url -eq "") {
    Write-Error "Nenhuma URL fornecida e nenhum túnel ativo encontrado em $UrlFile. Execute .\start-tunnel.ps1 primeiro."
    exit 1
}

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "  REMOTEDESK - TESTE DE CONECTIVIDADE EXTERNA (NGROK)                 " -ForegroundColor Cyan
Write-Host "  Testando URL Pública: $Url" -ForegroundColor Yellow
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host ""

$headers = @{
    "ngrok-skip-browser-warning" = "true"
    "User-Agent" = "RemoteDesk-Diagnostic/1.0"
}

$failures = 0

# 1. Teste de Endpoint Raiz (/)
Write-Host -NoNewline "[1/3] Testando Endpoint Raiz (/)... "
try {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $rootRes = Invoke-RestMethod -Uri "$Url/" -Headers $headers -TimeoutSec 10 -ErrorAction Stop
    $sw.Stop()
    if ($rootRes -and $rootRes.service) {
        Write-Host "OK ($($sw.ElapsedMilliseconds)ms - $($rootRes.service))" -ForegroundColor Green
    } else {
        Write-Host "RESPOSTA INESPERADA" -ForegroundColor Yellow
        $failures++
    }
} catch {
    Write-Host "FALHA ($_)" -ForegroundColor Red
    $failures++
}

# 2. Teste de Health Check (/health)
Write-Host -NoNewline "[2/3] Testando Endpoint de Saúde (/health)... "
try {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $healthRes = Invoke-RestMethod -Uri "$Url/health" -Headers $headers -TimeoutSec 10 -ErrorAction Stop
    $sw.Stop()
    if ($healthRes -and $healthRes.status -eq "Healthy") {
        Write-Host "OK ($($sw.ElapsedMilliseconds)ms - Status: Healthy, Hosts Ativos: $($healthRes.activeHosts))" -ForegroundColor Green
    } else {
        Write-Host "STATUS INCORRETO" -ForegroundColor Yellow
        $failures++
    }
} catch {
    Write-Host "FALHA ($_)" -ForegroundColor Red
    $failures++
}

# 3. Teste de Configuração ICE / STUN (/api/signaling/ice-servers)
Write-Host -NoNewline "[3/3] Testando Descoberta ICE (/api/signaling/ice-servers)... "
try {
    $iceRes = Invoke-RestMethod -Uri "$Url/api/signaling/ice-servers" -Headers $headers -TimeoutSec 10 -ErrorAction Stop
    if ($iceRes -and $iceRes.success) {
        Write-Host "OK ($($iceRes.count) servidores ICE retornados)" -ForegroundColor Green
    } else {
        Write-Host "FALHA" -ForegroundColor Red
        $failures++
    }
} catch {
    Write-Host "FALHA ($_)" -ForegroundColor Red
    $failures++
}

Write-Host ""
Write-Host "======================================================================" -ForegroundColor Cyan
if ($failures -eq 0) {
    Write-Host "  DIAGNÓSTICO CONCLUÍDO! O túnel está pronto para receber o Android." -ForegroundColor Green
    Write-Host "======================================================================" -ForegroundColor Cyan
    exit 0
}

Write-Host "  DIAGNÓSTICO FALHOU: $failures verificacao(oes) nao passaram." -ForegroundColor Red
Write-Host "======================================================================" -ForegroundColor Cyan
exit 1

# ==============================================================================
# RemoteDesk - Verificação de Pré-Requisitos para ngrok no Windows
# ==============================================================================
[CmdletBinding()]
param()

$ErrorActionPreference = "Continue"

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "  REMOTEDESK - VERIFICAÇÃO DE PRÉ-REQUISITOS (NGROK / WINDOWS)       " -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host ""

$allPassed = $true

# 1. Verifica instalação do .NET 8 SDK
Write-Host -NoNewline "[1/4] Verificando .NET 8 SDK (dotnet).................... "
try {
    $dotnetVer = & dotnet --version 2>$null
    if ($dotnetVer -and $dotnetVer.StartsWith("8.")) {
        Write-Host "OK ($dotnetVer)" -ForegroundColor Green
    } elseif ($dotnetVer) {
        Write-Host "AVISO ($dotnetVer detectado - versão recomendada: 8.0.x)" -ForegroundColor Yellow
    } else {
        Write-Host "FALHA" -ForegroundColor Red
        Write-Host "  -> O .NET 8 SDK não foi encontrado no PATH." -ForegroundColor Yellow
        Write-Host "  -> Instale via: winget install Microsoft.DotNet.SDK.8" -ForegroundColor Yellow
        $allPassed = $false
    }
} catch {
    Write-Host "FALHA ($_)" -ForegroundColor Red
    $allPassed = $false
}

# 2. Verifica instalação do ngrok
Write-Host -NoNewline "[2/4] Verificando executável do ngrok................... "
$ngrokPath = $null
if (Get-Command ngrok -ErrorAction SilentlyContinue) {
    $ngrokPath = (Get-Command ngrok).Source
} else {
    # Procura em locais comuns de instalação no Windows
    $commonPaths = @(
        "$env:LOCALAPPDATA\ngrok\ngrok.exe",
        "$env:USERPROFILE\bin\ngrok.exe",
        "C:\ngrok\ngrok.exe",
        "C:\ProgramData\chocolatey\bin\ngrok.exe"
    )
    foreach ($p in $commonPaths) {
        if (Test-Path $p) {
            $ngrokPath = $p
            break
        }
    }
}

if ($ngrokPath) {
    $ngrokVer = & $ngrokPath version 2>$null
    Write-Host "OK ($ngrokVer)" -ForegroundColor Green
} else {
    Write-Host "NÃO ENCONTRADO" -ForegroundColor Red
    Write-Host "  -> O ngrok não está instalado ou não está no PATH." -ForegroundColor Yellow
    Write-Host "  -> Como instalar no Windows:" -ForegroundColor Cyan
    Write-Host "       Opção A (winget):  winget install ngrok" -ForegroundColor White
    Write-Host "       Opção B (choco):   choco install ngrok" -ForegroundColor White
    Write-Host "       Opção C (manual):  Baixe em https://ngrok.com/download e extraia em C:\ngrok" -ForegroundColor White
    $allPassed = $false
}

# 3. Verifica autenticação do ngrok (sem exibir nem solicitar o token)
Write-Host -NoNewline "[3/4] Verificando configuração de autenticação do ngrok.. "
$authed = $false
$detectedConfigPath = $null
$searchedPaths = @()

if ($ngrokPath) {
    try {
        # 1. Consulta 'ngrok config check' para extrair o caminho oficial e validar integridade
        $cfgOutput = & $ngrokPath config check 2>&1
        $cfgText = ($cfgOutput | Out-String).Trim()
        
        # Padrões conhecidos do ngrok CLI: "Valid configuration file at C:\..." ou "configuration file: C:\..."
        if ($cfgText -match '(?i)(?:Valid configuration file at|configuration file:?)\s*["'']?(?<cfg>(?:[a-zA-Z]:[/\\]|/)[^\r\n"'']+)["'']?') {
            $parsedPath = $Matches['cfg'].Trim().Trim('"').Trim("'").Replace('/', '\')
            if (Test-Path $parsedPath) {
                $searchedPaths += $parsedPath
            }
        }

        # 2. Caminhos padrão do ngrok v2 e v3 no Windows (%APPDATA%, %LOCALAPPDATA%, etc.)
        $candidatePaths = @(
            "$env:APPDATA\ngrok\ngrok.yml",
            "$env:LOCALAPPDATA\ngrok\ngrok.yml",
            "$env:USERPROFILE\.ngrok2\ngrok.yml",
            "$env:USERPROFILE\.config\ngrok\ngrok.yml",
            "$env:PROGRAMDATA\ngrok\ngrok.yml"
        )
        foreach ($c in $candidatePaths) {
            if ($c -and ($searchedPaths -notcontains $c)) {
                $searchedPaths += $c
            }
        }

        # 3. Varre os caminhos encontrados procurando pela diretiva authtoken com valor válido
        foreach ($p in $searchedPaths) {
            if (Test-Path $p) {
                try {
                    $content = Get-Content -Path $p -Raw -ErrorAction SilentlyContinue
                    # Valida se há diretiva authtoken preenchida (sem extrair nem exibir o valor)
                    if ($content -match 'authtoken\s*:\s*[^\s#]+') {
                        $authed = $true
                        $detectedConfigPath = $p
                        break
                    }
                } catch { }
            }
        }

        # 4. Caso o 'ngrok config check' oficial tenha validado com sucesso mesmo com leitura restrita
        if (-not $authed -and $LASTEXITCODE -eq 0 -and $cfgText -match '(?i)Valid configuration file') {
            $authed = $true
            $detectedConfigPath = "ngrok CLI config check"
        }
    } catch { }
}

if ($authed) {
    if ($detectedConfigPath -and $detectedConfigPath -ne "ngrok CLI config check") {
        Write-Host "OK (Configurado em: $detectedConfigPath)" -ForegroundColor Green
    } else {
        Write-Host "OK (Autenticado localmente)" -ForegroundColor Green
    }
} else {
    Write-Host "PENDENTE" -ForegroundColor Yellow
    Write-Host "  -> O ngrok precisa ser autenticado com seu authtoken pessoal." -ForegroundColor Yellow
    Write-Host "  -> Locais verificados no Windows:" -ForegroundColor Gray
    foreach ($sp in $searchedPaths) {
        $status = if (Test-Path $sp) { "Arquivo presente, mas sem authtoken configurado" } else { "Não encontrado" }
        Write-Host "       * $sp ($status)" -ForegroundColor Gray
    }
    Write-Host "  -> Passo 1: Acesse https://dashboard.ngrok.com/get-started/your-authtoken" -ForegroundColor White
    Write-Host "  -> Passo 2: Copie seu authtoken no painel oficial do ngrok." -ForegroundColor White
    Write-Host "  -> Passo 3: No terminal PowerShell do seu PC, execute:" -ForegroundColor White
    Write-Host "       ngrok config add-authtoken <SEU_TOKEN_AQUI>" -ForegroundColor Cyan
    Write-Host "  (Nota: O token será gravado em %APPDATA%\ngrok\ngrok.yml ou %LOCALAPPDATA%\ngrok\ngrok.yml)." -ForegroundColor DarkGray
    $allPassed = $false
}

# 4. Verifica disponibilidade da porta 5000 (Signaling Server)
Write-Host -NoNewline "[4/4] Verificando disponibilidade da porta local 5000... "
$portInUse = $false
try {
    $conns = Get-NetTCPConnection -LocalPort 5000 -ErrorAction SilentlyContinue
    if ($conns) { $portInUse = $true }
} catch { }

if (-not $portInUse) {
    Write-Host "OK (Porta 5000 livre)" -ForegroundColor Green
} else {
    Write-Host "OCUPADA" -ForegroundColor Yellow
    Write-Host "  -> A porta 5000 já está em uso por outro processo (pode ser o próprio Signaling Server anterior)." -ForegroundColor Yellow
    Write-Host "  -> Execute .\stop-session.ps1 para liberar processos anteriores." -ForegroundColor White
}

Write-Host ""
Write-Host "======================================================================" -ForegroundColor Cyan
if ($allPassed) {
    Write-Host "  PRÉ-REQUISITOS APROVADOS! Você pode iniciar os serviços com:       " -ForegroundColor Green
    Write-Host "  .\start-all.ps1                                                     " -ForegroundColor White
    Write-Host "======================================================================" -ForegroundColor Cyan
    exit 0
} else {
    Write-Host "  Ajuste os itens pendentes acima para continuar.                     " -ForegroundColor Yellow
    Write-Host "======================================================================" -ForegroundColor Cyan
    exit 1
}

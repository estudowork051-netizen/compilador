# ==============================================================================
# RemoteDesk - Utilitarios para identificar e encerrar o Windows Agent
# ==============================================================================

function Get-RemoteDeskAgentProcesses {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$ProjectRoot
    )

    $agentDir = [System.IO.Path]::GetFullPath((Join-Path $ProjectRoot "windows-agent\src\RemoteDesk.Agent"))
    $agentProject = [System.IO.Path]::GetFullPath((Join-Path $agentDir "RemoteDesk.Agent.csproj"))
    $agentOutputDir = [System.IO.Path]::GetFullPath((Join-Path $agentDir "bin\Debug\net8.0"))
    $agentDll = [System.IO.Path]::GetFullPath((Join-Path $agentOutputDir "RemoteDesk.Agent.dll"))

    $processes = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue)
    foreach ($process in $processes) {
        $commandLine = [string]$process.CommandLine
        $executablePath = [string]$process.ExecutablePath
        $isAgentExecutable = $false

        if ($executablePath) {
            try {
                $fullExecutablePath = [System.IO.Path]::GetFullPath($executablePath)
                $isAgentExecutable =
                    $fullExecutablePath.StartsWith(
                        ($agentOutputDir + [System.IO.Path]::DirectorySeparatorChar),
                        [System.StringComparison]::OrdinalIgnoreCase
                    ) -and
                    [System.IO.Path]::GetFileNameWithoutExtension($fullExecutablePath) -ieq "RemoteDesk.Agent"
            } catch {
                $isAgentExecutable = $false
            }
        }

        $mentionsProject = $commandLine.IndexOf(
            $agentProject,
            [System.StringComparison]::OrdinalIgnoreCase
        ) -ge 0
        $mentionsDll = $commandLine.IndexOf(
            $agentDll,
            [System.StringComparison]::OrdinalIgnoreCase
        ) -ge 0

        $isDotnetAgent = [string]$process.Name -ieq "dotnet.exe" -and
            ($mentionsProject -or $mentionsDll)
        $isLegacyRunner = [string]$process.Name -match "^(powershell|pwsh)(\.exe)?$" -and
            $mentionsProject -and
            $commandLine -match "(?i)dotnet\s+run"

        if ($isAgentExecutable -or $isDotnetAgent -or $isLegacyRunner) {
            [PSCustomObject]@{
                ProcessId = [int]$process.ProcessId
                ParentProcessId = [int]$process.ParentProcessId
                Name = [string]$process.Name
                CommandLine = $commandLine
            }
        }
    }
}

function Stop-RemoteDeskProcessTree {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [int]$ProcessId
    )

    $children = @(Get-CimInstance Win32_Process -Filter "ParentProcessId = $ProcessId" -ErrorAction SilentlyContinue)
    foreach ($child in $children) {
        Stop-RemoteDeskProcessTree -ProcessId ([int]$child.ProcessId)
    }

    Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
}

function Stop-RemoteDeskAgentProcesses {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$ProjectRoot
    )

    $targets = @(Get-RemoteDeskAgentProcesses -ProjectRoot $ProjectRoot)
    if ($targets.Count -eq 0) {
        return 0
    }

    foreach ($target in $targets) {
        Stop-RemoteDeskProcessTree -ProcessId $target.ProcessId
    }

    Start-Sleep -Milliseconds 300
    $remaining = @(Get-RemoteDeskAgentProcesses -ProjectRoot $ProjectRoot)
    foreach ($target in $remaining) {
        Stop-RemoteDeskProcessTree -ProcessId $target.ProcessId
    }

    return $targets.Count
}

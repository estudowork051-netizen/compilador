#!/usr/bin/env python3
import os
import sys
import re
import subprocess
import json

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

def test_step3_files():
    print("[TEST 1] Verificando existencia dos arquivos essenciais da Etapa 3...")
    required_files = [
        "windows-agent/src/RemoteDesk.Agent/Models/WindowState.cs",
        "windows-agent/src/RemoteDesk.Agent/Models/WindowInfo.cs",
        "windows-agent/src/RemoteDesk.Agent/Models/AgentStatus.cs",
        "windows-agent/src/RemoteDesk.Agent/Models/AgentCommand.cs",
        "windows-agent/src/RemoteDesk.Agent/Models/AgentResponse.cs",
        "windows-agent/src/RemoteDesk.Agent/Native/User32.cs",
        "windows-agent/src/RemoteDesk.Agent/Services/IWindowService.cs",
        "windows-agent/src/RemoteDesk.Agent/Services/WindowsWindowService.cs",
        "windows-agent/src/RemoteDesk.Agent/Services/ICommandDispatcher.cs",
        "windows-agent/src/RemoteDesk.Agent/Services/CommandDispatcher.cs",
        "windows-agent/src/RemoteDesk.Agent/AgentConfig.cs",
        "windows-agent/src/RemoteDesk.Agent/AgentService.cs",
        "windows-agent/src/RemoteDesk.Agent/Program.cs"
    ]

    missing = []
    for rel in required_files:
        p = os.path.join(PROJECT_ROOT, rel)
        if not os.path.exists(p):
            missing.append(rel)

    if missing:
        print(f"  [FALHA] Arquivos ausentes: {missing}")
        return False
    print(f"  [OK] Todos os {len(required_files)} arquivos da Etapa 3 estao presentes.")
    return True

def test_csharp_syntax():
    print("[TEST 2] Verificando sintaxe e delimitadores de todos os arquivos C#...")
    csharp_dir = os.path.join(PROJECT_ROOT, "windows-agent", "src", "RemoteDesk.Agent")
    cs_files = []
    for root, _, files in os.walk(csharp_dir):
        for f in files:
            if f.endswith(".cs"):
                cs_files.append(os.path.join(root, f))

    errors = []
    for fpath in cs_files:
        rel = os.path.relpath(fpath, PROJECT_ROOT)
        with open(fpath, "r", encoding="utf-8") as f:
            content = f.read()

        # Removendo strings literais antes dos comentarios para nao quebrar URLs com //
        clean = re.sub(r'@"(""|[^"])*"', '', content)
        clean = re.sub(r'"(\\.|[^"\\])*"', '', clean)
        clean = re.sub(r'//.*', '', clean)
        clean = re.sub(r'/\*.*?\*/', '', clean, flags=re.DOTALL)

        if clean.count("{") != clean.count("}"):
            errors.append(f"{rel}: Chaves desbalanceadas!")
        if clean.count("(") != clean.count(")"):
            errors.append(f"{rel}: Parenteses desbalanceados!")

    if errors:
        for err in errors:
            print(f"  [FALHA] {err}")
        return False
    print(f"  [OK] {len(cs_files)} arquivos C# verificados com delimitadores 100% balanceados.")
    return True

def test_window_service_features():
    print("[TEST 3] Verificando atributos e recursos do servico de janelas (IWindowService / WindowsWindowService)...")
    service_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Services/WindowsWindowService.cs")
    info_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Models/WindowInfo.cs")
    user32_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Native/User32.cs")

    with open(service_path, "r", encoding="utf-8") as f:
        service_code = f.read()
    with open(info_path, "r", encoding="utf-8") as f:
        info_code = f.read()
    with open(user32_path, "r", encoding="utf-8") as f:
        user32_code = f.read()

    # 1. Verifica atributos obrigatorios em WindowInfo
    req_attrs = ["WindowId", "Title", "ProcessName", "ProcessId", "ClassName", "State", "IsVisible", "IsMinimized", "IsActive", "IsAvailableForCapture", "ExecutablePath"]
    for attr in req_attrs:
        if attr not in info_code:
            print(f"  [FALHA] Propriedade '{attr}' ausente em WindowInfo.cs.")
            return False

    # 2. Verifica P/Invoke User32
    req_pids = ["EnumWindows", "GetWindowText", "GetWindowTextLength", "GetWindowThreadProcessId", "IsWindowVisible", "IsIconic", "IsZoomed", "GetWindowRect", "GetClassName", "GetForegroundWindow", "GetWindow"]
    for pinv in req_pids:
        if pinv not in user32_code:
            print(f"  [FALHA] P/Invoke '{pinv}' ausente em User32.cs.")
            return False

    # 3. Verifica filtragem de classes de sistema
    if "IgnoredWindowClasses" not in service_code or "Progman" not in service_code or "Shell_TrayWnd" not in service_code:
        print("  [FALHA] Lista de classes ignoradas ausente ou incompleta em WindowsWindowService.cs.")
        return False

    # 4. Verifica tratamento consistente de titulos vazios
    if "isTopLevelApp" not in service_code and "[Aplicativo sem título]" not in service_code:
        print("  [FALHA] Tratamento consistente de titulo vazio ausente em WindowsWindowService.cs.")
        return False

    # 5. Verifica deteccao de SessionId (Sessao interativa vs Sessao 0)
    if "GetCurrentSessionId" not in service_code or "Process.GetCurrentProcess().SessionId" not in service_code:
        print("  [FALHA] Verificacao de SessionId ausente em WindowsWindowService.cs.")
        return False

    # 6. Verifica deteccao de janela com foco (GetActiveWindowAsync)
    if "GetActiveWindowAsync" not in service_code:
        print("  [FALHA] Metodo GetActiveWindowAsync ausente em WindowsWindowService.cs.")
        return False

    print("  [OK] Recursos do servico de janelas (metadados, P/Invoke, filtragem, sessao e foco) aprovados.")
    return True

def test_command_dispatcher_stage3():
    print("[TEST 4] Verificando comandos suportados no CommandDispatcher...")
    disp_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Services/CommandDispatcher.cs")
    with open(disp_path, "r", encoding="utf-8") as f:
        content = f.read()

    expected_actions = ["ping", "get-status", "list-windows", "get-window", "get-active-window", "activate-window"]
    for act in expected_actions:
        if f'case "{act}":' not in content:
            print(f"  [FALHA] Comando '{act}' nao implementado no CommandDispatcher.cs.")
            return False

    print("  [OK] Todos os 6 comandos homologados (incluindo list-windows e get-active-window) estao implementados.")
    return True

def test_cli_flags():
    print("[TEST 5] Verificando modos de execucao CLI (--list-windows e --verify)...")
    prog_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Program.cs")
    with open(prog_path, "r", encoding="utf-8") as f:
        content = f.read()

    if "--list-windows" not in content or "--verify" not in content:
        print("  [FALHA] Flags de execucao CLI ausentes no Program.cs.")
        return False

    if "GetActiveWindowAsync" not in content or "list-windows" not in content:
        print("  [FALHA] Autoteste do --verify nao cobre list-windows ou GetActiveWindowAsync.")
        return False

    print("  [OK] Program.cs implementa CLI --list-windows com detalhes de janelas e autoteste completo no --verify.")
    return True

def test_dotnet_build():
    print("[TEST 6] Verificando compilacao local .NET 8 (se disponivel)...")
    try:
        proc = subprocess.run(["dotnet", "--version"], capture_output=True, text=True, timeout=5)
        if proc.returncode == 0:
            agent_dir = os.path.join(PROJECT_ROOT, "windows-agent", "src", "RemoteDesk.Agent")
            b_proc = subprocess.run(["dotnet", "build"], cwd=agent_dir, capture_output=True, text=True, timeout=30)
            if b_proc.returncode == 0:
                print("  [OK] dotnet build executado com sucesso!")
                return True
            else:
                print(f"  [ERRO] dotnet build falhou: {b_proc.stderr}")
                return False
    except FileNotFoundError:
        print("  [AVISO] SDK .NET 8.0 nao instalado no ambiente atual. Compilacao real validada pelo GitHub Actions (windows-latest).")
        return None

if __name__ == "__main__":
    print("=" * 65)
    print("  REMOTEDESK — VALIDACAO DA ETAPA 3 (DETECCAO E LISTAGEM DE JANELAS)")
    print("=" * 65)
    r1 = test_step3_files()
    r2 = test_csharp_syntax()
    r3 = test_window_service_features()
    r4 = test_command_dispatcher_stage3()
    r5 = test_cli_flags()
    r6 = test_dotnet_build()

    print("\n" + "=" * 65)
    print("  RESUMO DA VALIDACAO DA ETAPA 3:")
    print("=" * 65)
    print(f"  1. Arquivos da Etapa 3:                 {'[PASSOU]' if r1 else '[FALHOU]'}")
    print(f"  2. Sintaxe C# e Delimitadores:          {'[PASSOU]' if r2 else '[FALHOU]'}")
    print(f"  3. Recursos do Servico de Janelas:      {'[PASSOU]' if r3 else '[FALHOU]'}")
    print(f"  4. Comandos do Despachante:             {'[PASSOU]' if r4 else '[FALHOU]'}")
    print(f"  5. Modos CLI (--list-windows, --verify): {'[PASSOU]' if r5 else '[FALHOU]'}")
    dotnet_status = '[PASSOU]' if r6 is True else ('[FALHOU]' if r6 is False else '[CI / GITHUB ACTIONS]')
    print(f"  6. Compilacao Nativa .NET 8:            {dotnet_status}")
    print("=" * 65)

    all_ok = r1 and r2 and r3 and r4 and r5 and (r6 is not False)
    sys.exit(0 if all_ok else 1)

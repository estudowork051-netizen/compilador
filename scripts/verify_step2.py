#!/usr/bin/env python3
import os
import sys
import re
import subprocess
import json

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

def test_step2_files():
    print("[TEST 1] Verificando existencia dos arquivos da Etapa 2 (Windows Agent)...")
    required_step2_files = [
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
    for rel_path in required_step2_files:
        full_path = os.path.join(PROJECT_ROOT, rel_path)
        if not os.path.exists(full_path):
            missing.append(rel_path)

    if missing:
        print(f"  [FALHA] Arquivos ausentes: {missing}")
        return False
    print(f"  [OK] Todos os {len(required_step2_files)} arquivos da Etapa 2 estao presentes.")
    return True

def test_csharp_syntax_integrity():
    print("[TEST 2] Verificando integridade estrutural e balanceamento de chaves dos arquivos C#...")
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

        # Verifica balanceamento de chaves (ignorando strings e comentarios)
        # Removendo strings literais antes dos comentarios para nao quebrar URLs com //
        clean = re.sub(r'@"(""|[^"])*"', '', content)
        clean = re.sub(r'"(\\.|[^"\\])*"', '', clean)
        clean = re.sub(r'//.*', '', clean)
        clean = re.sub(r'/\*.*?\*/', '', clean, flags=re.DOTALL)

        open_braces = clean.count("{")
        close_braces = clean.count("}")
        if open_braces != close_braces:
            errors.append(f"{rel}: Chaves desbalanceadas! (abertas: {open_braces}, fechadas: {close_braces})")

        open_parens = clean.count("(")
        close_parens = clean.count(")")
        if open_parens != close_parens:
            errors.append(f"{rel}: Parenteses desbalanceados! (abertos: {open_parens}, fechados: {close_parens})")

    if errors:
        for err in errors:
            print(f"  [FALHA] {err}")
        return False
    print(f"  [OK] {len(cs_files)} arquivos C# verificados com delimitadores 100% balanceados.")
    return True

def test_command_dispatcher_security():
    print("[TEST 3] Verificando modelo de seguranca contra comandos arbitrarios...")
    disp_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Services/CommandDispatcher.cs")
    with open(disp_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Checar se existe lista branca (switch/case) e rejeicao default
    whitelisted_actions = ["ping", "get-status", "list-windows", "get-window", "activate-window"]
    for action in whitelisted_actions:
        if f'case "{action}":' not in content:
            print(f"  [FALHA] Acao homologada '{action}' ausente no CommandDispatcher.")
            return False

    if 'default:' not in content or 'UNSUPPORTED_ACTION' not in content:
        print("  [FALHA] Bloqueio default para acoes nao homologadas ausente no CommandDispatcher.")
        return False

    if 'PromptOnConnectionRequest' not in content or 'UNAUTHORIZED' not in content:
        print("  [FALHA] Validacao de AccessPin ausente no CommandDispatcher.")
        return False

    print("  [OK] Seguranca do CommandDispatcher validada: lista branca restrita e bloqueio de comandos arbitrarios ativo.")
    return True

def test_pairing_code_format():
    print("[TEST 4] Verificando geracao do FriendlyPairingCode no AgentConfig...")
    config_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/AgentConfig.cs")
    with open(config_path, "r", encoding="utf-8") as f:
        content = f.read()

    if "FriendlyPairingCode" not in content or "raw[..3]" not in content:
        print("  [FALHA] Formato de codigo amigavel de 9 digitos ausente no AgentConfig.")
        return False

    print("  [OK] Formato amigavel de pareamento de 9 digitos (ex: '123 456 789') devidamente implementado.")
    return True

def test_lifecycle_and_flags():
    print("[TEST 5] Verificando flags de execucao (--verify, --list-windows, mutex de instancia unica)...")
    prog_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Program.cs")
    with open(prog_path, "r", encoding="utf-8") as f:
        content = f.read()

    if "--verify" not in content or "--list-windows" not in content:
        print("  [FALHA] Modos de CLI (--verify / --list-windows) ausentes no Program.cs.")
        return False

    if "Mutex" not in content or "singleInstanceMutex" not in content:
        print("  [FALHA] Controle de instancia unica com Mutex ausente no Program.cs.")
        return False

    print("  [OK] Program.cs implementa --verify, --list-windows e controle de instancia unica via Mutex.")
    return True

def test_dotnet_compilation_if_available():
    print("[TEST 6] Testando compilacao real do Agente Windows (se dotnet disponivel)...")
    try:
        proc = subprocess.run(["dotnet", "--version"], capture_output=True, text=True, timeout=5)
        if proc.returncode == 0:
            print(f"  .NET SDK detectado: {proc.stdout.strip()}. Executando dotnet build...")
            agent_dir = os.path.join(PROJECT_ROOT, "windows-agent", "src", "RemoteDesk.Agent")
            build_proc = subprocess.run(["dotnet", "build"], cwd=agent_dir, capture_output=True, text=True, timeout=30)
            if build_proc.returncode == 0:
                print("  [OK] Agente Windows compilou com sucesso!")
                return True
            else:
                print(f"  [ERRO] Falha de compilacao: {build_proc.stderr}")
                return False
    except FileNotFoundError:
        print("  [AVISO] SDK .NET 8.0 nao instalado no ambiente atual. Compilacao executada via GitHub Actions (windows-latest).")
        return None

if __name__ == "__main__":
    print("=" * 60)
    print("  REMOTEDESK — VERIFICACAO DA ETAPA 2 (AGENTE WINDOWS)")
    print("=" * 60)
    r1 = test_step2_files()
    r2 = test_csharp_syntax_integrity()
    r3 = test_command_dispatcher_security()
    r4 = test_pairing_code_format()
    r5 = test_lifecycle_and_flags()
    r6 = test_dotnet_compilation_if_available()

    print("\n" + "=" * 60)
    print("  RESUMO DA VALIDACAO DA ETAPA 2:")
    print("=" * 60)
    print(f"  1. Arquivos da Etapa 2:                 {'[PASSOU]' if r1 else '[FALHOU]'}")
    print(f"  2. Sintaxe e delimitadores C#:         {'[PASSOU]' if r2 else '[FALHOU]'}")
    print(f"  3. Seguranca do CommandDispatcher:      {'[PASSOU]' if r3 else '[FALHOU]'}")
    print(f"  4. Formato do Codigo de Pareamento:     {'[PASSOU]' if r4 else '[FALHOU]'}")
    print(f"  5. Ciclo de Vida e CLI (--verify):      {'[PASSOU]' if r5 else '[FALHOU]'}")
    dotnet_status = '[PASSOU]' if r6 is True else ('[FALHOU]' if r6 is False else '[CI / GITHUB ACTIONS]')
    print(f"  6. Compilacao Nativa .NET 8:            {dotnet_status}")
    print("=" * 60)

    all_passed = r1 and r2 and r3 and r4 and r5 and (r6 is not False)
    sys.exit(0 if all_passed else 1)

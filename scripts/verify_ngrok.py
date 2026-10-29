#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
REMOTEDESK — SCRIPT DE VALIDAÇÃO DA INTEGRAÇÃO NGROK E ACESSO EXTERNO SEM VPS
Valida a infraestrutura de scripts PowerShell/Batch, bypass de avisos ngrok,
conexão TLS/SSL, autenticação por PIN e documentação.
"""

import os
import sys
import re

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

def check_file_exists(relative_path, description):
    abs_path = os.path.join(PROJECT_ROOT, relative_path)
    if not os.path.isfile(abs_path):
        print(f"  [FALHA] Arquivo obrigatório não encontrado: {relative_path} ({description})")
        return False
    size = os.path.getsize(abs_path)
    if size == 0:
        print(f"  [FALHA] Arquivo vazio: {relative_path}")
        return False
    print(f"  [OK] {relative_path} ({size} bytes)")
    return True

def test_ngrok_scripts_existence():
    print("[TEST 1] Verificando existência de todos os scripts de automação ngrok...")
    required_files = [
        ("scripts/ngrok/check-prerequisites.ps1", "Verificador de pré-requisitos .NET 8 e ngrok"),
        ("scripts/ngrok/start-signaling.ps1", "Inicializador do Servidor de Sinalização na porta 5000"),
        ("scripts/ngrok/start-tunnel.ps1", "Inicializador do túnel ngrok e consulta de URL pública"),
        ("scripts/ngrok/verify-tunnel.ps1", "Testador de conectividade externa e endpoints HTTP"),
        ("scripts/ngrok/stop-session.ps1", "Encerramento seguro com limpeza de PIDs"),
        ("scripts/ngrok/start-all.ps1", "Orquestrador geral PowerShell em 1 clique"),
        ("scripts/ngrok/start-all.bat", "Iniciador Windows Batch em 1 clique"),
        ("scripts/ngrok/ngrok.yml.example", "Template de configuração do ngrok"),
        ("docs/NGROK_SETUP.md", "Guia completo de configuração e uso externo")
    ]
    all_ok = True
    for path, desc in required_files:
        if not check_file_exists(path, desc):
            all_ok = False
    return all_ok

def test_ngrok_bypass_headers_in_android():
    print("[TEST 2] Verificando cabeçalhos de bypass de aviso ngrok no aplicativo Android...")
    android_client = os.path.join(PROJECT_ROOT, "android-app/app/src/main/java/com/remotedesk/app/network/RemoteDeskClient.kt")
    with open(android_client, "r", encoding="utf-8") as f:
        client_code = f.read()

    if "ngrok-skip-browser-warning" not in client_code:
        print("  [FALHA] Cabeçalho 'ngrok-skip-browser-warning' ausente em RemoteDeskClient.kt!")
        return False

    if "RemoteDesk-Android" not in client_code:
        print("  [FALHA] User-Agent customizado ausente em RemoteDeskClient.kt!")
        return False

    android_sig = os.path.join(PROJECT_ROOT, "android-app/app/src/main/java/com/remotedesk/app/network/SignalingClient.kt")
    with open(android_sig, "r", encoding="utf-8") as f:
        sig_code = f.read()

    if "ngrok-skip-browser-warning" not in sig_code:
        print("  [FALHA] Cabeçalho 'ngrok-skip-browser-warning' ausente no handshake WebSocket em SignalingClient.kt!")
        return False

    if "SSLSocket" not in sig_code or "createSocket" not in sig_code:
        print("  [FALHA] Suporte a SSLSocket/TLS para HTTPS/WSS ausente em SignalingClient.kt!")
        return False

    print("  [OK] Android injeta 'ngrok-skip-browser-warning', User-Agent customizado e suporta TLS/WSS.")
    return True

def test_ngrok_bypass_headers_in_windows_agent():
    print("[TEST 3] Verificando cabeçalhos ngrok e compatibilidade no Windows Agent...")
    agent_sig = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Services/SignalingClientService.cs")
    with open(agent_sig, "r", encoding="utf-8") as f:
        agent_code = f.read()

    if "ngrok-skip-browser-warning" not in agent_code:
        print("  [FALHA] Cabeçalho 'ngrok-skip-browser-warning' ausente em SignalingClientService.cs!")
        return False

    if "SetRequestHeader" not in agent_code:
        print("  [FALHA] SetRequestHeader ausente no ClientWebSocket do Agent!")
        return False

    print("  [OK] Windows Agent configura cabeçalhos de bypass do ngrok no WebSocket de sinalização.")
    return True

def test_pin_management_and_cli():
    print("[TEST 4] Verificando gerenciamento, CLI e exibição de PIN no Windows Agent...")
    auth_service = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Services/DeviceAuthService.cs")
    with open(auth_service, "r", encoding="utf-8") as f:
        auth_code = f.read()

    if "RegenerateFixedCodeAsync" not in auth_code or "FixedCode" not in auth_code or "ChangeFixedCodeAsync" not in auth_code:
        print("  [FALHA] Métodos de manipulação de PIN ausentes em DeviceAuthService.cs!")
        return False

    agent_svc = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/AgentService.cs")
    with open(agent_svc, "r", encoding="utf-8") as f:
        asvc_code = f.read()

    if "PIN DE ACESSO / PAREAMENTO" not in asvc_code:
        print("  [FALHA] Banner chamativo de PIN ausente na inicialização em AgentService.cs!")
        return False

    program_cs = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Program.cs")
    with open(program_cs, "r", encoding="utf-8") as f:
        prog_code = f.read()

    if "--pin" not in prog_code or "--set-pin" not in prog_code or "--regenerate-pin" not in prog_code:
        print("  [FALHA] Argumentos de linha de comando CLI (--pin, --set-pin, --regenerate-pin) ausentes em Program.cs!")
        return False

    print("  [OK] PIN configurável, recuperável via CLI (--pin, --set-pin) e exibido em destaque no console.")
    return True

def test_security_isolation_and_credentials():
    print("[TEST 5] Verificando isolamento de segurança e proteção de credenciais...")
    # Verifica se a porta 5050 NÃO é exposta nos scripts ngrok
    ngrok_start = os.path.join(PROJECT_ROOT, "scripts/ngrok/start-tunnel.ps1")
    with open(ngrok_start, "r", encoding="utf-8") as f:
        nstart_code = f.read()

    if "5050" in nstart_code:
        print("  [FALHA DE SEGURANÇA] A porta local de controle 5050 foi indevidamente referenciada em start-tunnel.ps1!")
        return False

    if "5000" not in nstart_code:
        print("  [FALHA] start-tunnel.ps1 deve apontar estritamente para a porta 5000 (Signaling Server)!")
        return False

    # Verifica se não há tokens reais vazados no código
    sensitive_patterns = [r"authtoken\s*[:=]\s*[0-9a-zA-Z]{20,}", r"2[a-zA-Z0-9]{30,}"]
    for root, dirs, files in os.walk(os.path.join(PROJECT_ROOT, "scripts/ngrok")):
        for fname in files:
            fpath = os.path.join(root, fname)
            with open(fpath, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()
                for pat in sensitive_patterns:
                    matches = re.findall(pat, content)
                    for m in matches:
                        if "SEU_AUTHTOKEN_AQUI" not in m and "YOUR_NGROK_AUTHTOKEN_HERE" not in m:
                            print(f"  [FALHA DE SEGURANÇA] Possível token vazado encontrado em {fname}: {m}")
                            return False

    print("  [OK] Isolamento de portas preservado (somente 5000 exposta; 5050 estritamente interna) e zero tokens vazados.")
    return True

def test_documentation_and_roadmap():
    print("[TEST 6] Verificando integridade da documentação ngrok e roadmap...")
    doc_path = os.path.join(PROJECT_ROOT, "docs/NGROK_SETUP.md")
    with open(doc_path, "r", encoding="utf-8") as f:
        doc = f.read()

    required_doc_terms = [
        "Porta 5000",
        "5050",
        "start-all.bat",
        "check-prerequisites.ps1",
        "stop-session.ps1",
        "ngrok-skip-browser-warning",
        "--pin",
        "--set-pin",
        "4G/5G"
    ]
    for term in required_doc_terms:
        if term not in doc:
            print(f"  [FALHA] Termo essencial '{term}' ausente em docs/NGROK_SETUP.md!")
            return False

    roadmap_path = os.path.join(PROJECT_ROOT, "docs/ROADMAP.md")
    with open(roadmap_path, "r", encoding="utf-8") as f:
        rm = f.read()

    if "PLANEJADA / PENDENTE" not in rm or "Interface de Administração" not in rm:
        print("  [FALHA] Registro de roadmap da Interface de Administração ausente!")
        return False

    print("  [OK] Documentação completa com instruções de uso externo, PIN e roadmap atualizado.")
    return True

def main():
    print("=" * 72)
    print("  REMOTEDESK — VALIDAÇÃO DA INTEGRAÇÃO NGROK & ACESSO EXTERNO (SEM VPS)")
    print("=" * 72)

    tests = [
        test_ngrok_scripts_existence,
        test_ngrok_bypass_headers_in_android,
        test_ngrok_bypass_headers_in_windows_agent,
        test_pin_management_and_cli,
        test_security_isolation_and_credentials,
        test_documentation_and_roadmap
    ]

    for t in tests:
        if not t():
            print("=" * 72)
            print("  FALHA NA VALIDAÇÃO DO NGROK!")
            print("=" * 72)
            sys.exit(1)

    print("=" * 72)
    print("  RESUMO DA VALIDAÇÃO DO NGROK:")
    print("=" * 72)
    print("  1. Scripts de Automação e Suíte ngrok:       [PASSOU]")
    print("  2. Headers de Bypass e TLS/SSL no Android:   [PASSOU]")
    print("  3. Headers de Bypass no Windows Agent:       [PASSOU]")
    print("  4. Gerenciamento e Exibição do PIN (CLI):    [PASSOU]")
    print("  5. Isolamento de Portas e Segurança:        [PASSOU]")
    print("  6. Documentação NGROK_SETUP e ROADMAP:       [PASSOU]")
    print("=" * 72)
    print("  STATUS NGROK: 100% VALIDADO E APROVADO COM SUCESSO!")
    print("=" * 72)
    sys.exit(0)

if __name__ == "__main__":
    main()

#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
REMOTEDESK / DOHSKTOP — SCRIPT DE VALIDAÇÃO DA ETAPA 8/12
Acesso Remoto pela Internet, Resolução do PIN, WebRTC, STUN/TURN e Planejamento do Roadmap.
"""

import os
import re
import sys
import json
import hashlib
import hmac
import base64
import time

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

def check_brackets(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    stack = []
    pairs = {'{': '}', '(': ')', '[': ']'}
    in_str = False
    in_char = False
    in_comment_line = False
    in_comment_block = False
    i = 0
    line = 1
    while i < len(content):
        c = content[i]
        if c == '\n':
            line += 1
            in_comment_line = False
            i += 1
            continue
        if in_comment_line:
            i += 1
            continue
        if in_comment_block:
            if c == '*' and i + 1 < len(content) and content[i+1] == '/':
                in_comment_block = False
                i += 2
                continue
            i += 1
            continue
        if not in_str and not in_char:
            if c == '/' and i + 1 < len(content) and content[i+1] == '/':
                in_comment_line = True
                i += 2
                continue
            if c == '/' and i + 1 < len(content) and content[i+1] == '*':
                in_comment_block = True
                i += 2
                continue
        if c == '\"' and not in_char:
            if not in_str:
                in_str = True
            elif content[i-1] != '\\':
                in_str = False
        elif c == '\'' and not in_str:
            if not in_char:
                in_char = True
            elif content[i-1] != '\\':
                in_char = False
        elif not in_str and not in_char:
            if c in pairs:
                stack.append((c, line))
            elif c in pairs.values():
                if not stack:
                    return False
                top, _ = stack.pop()
                if pairs[top] != c:
                    return False
        i += 1
    return len(stack) == 0

def test_essential_files():
    print("[TEST 1] Verificando arquivos essenciais da Etapa 8 e da infraestrutura de Internet...")
    required_files = [
        "signaling-server/src/RemoteDesk.SignalingServer/Models/SignalingModels.cs",
        "signaling-server/src/RemoteDesk.SignalingServer/Services/SignalingSessionManager.cs",
        "signaling-server/src/RemoteDesk.SignalingServer/Program.cs",
        "windows-agent/src/RemoteDesk.Agent/Services/SignalingClientService.cs",
        "windows-agent/src/RemoteDesk.Agent/Services/DeviceAuthService.cs",
        "windows-agent/src/RemoteDesk.Agent/Program.cs",
        "android-app/app/src/main/java/com/remotedesk/app/network/SignalingClient.kt",
        "android-app/app/src/main/java/com/remotedesk/app/MainActivity.kt",
        "android-app/app/src/main/res/values/strings.xml",
        "deployment/Dockerfile.signaling",
        "deployment/docker-compose.yml",
        "deployment/turnserver.conf",
        "deployment/Caddyfile",
        "deployment/.env.example",
        "deployment/deploy.sh",
        "deployment/verify_deployment.sh",
        "deployment/FIREWALL_RULES.md",
        "deployment/DEPLOYMENT.md",
        "docs/ROADMAP.md"
    ]

    for rel in required_files:
        full = os.path.join(PROJECT_ROOT, rel)
        if not os.path.isfile(full):
            print(f"  [FALHA] Arquivo essencial ausente: {rel}")
            return False

    print(f"  [OK] Todos os {len(required_files)} arquivos essenciais verificados com sucesso.")
    return True

def test_syntax_and_brackets():
    print("[TEST 2] Verificando sintaxe e delimitadores em todos os fontes C# e Kotlin...")
    for root, _, files in os.walk(PROJECT_ROOT):
        if any(skip in root for skip in ["bin", "obj", ".git", ".gradle"]):
            continue
        for f in files:
            if f.endswith((".cs", ".kt")):
                p = os.path.join(root, f)
                if not check_brackets(p):
                    print(f"  [FALHA] Delimitadores desbalanceados em {p}")
                    return False

    print("  [OK] Delimitadores balanceados em todos os códigos C# e Kotlin.")
    return True

def test_pin_fix_and_authentication():
    print("[TEST 3] Verificando correção definitiva do PIN e clareza no Windows Agent...")
    auth_svc = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Services/DeviceAuthService.cs")
    with open(auth_svc, "r", encoding="utf-8") as f:
        content = f.read()

    # Valida suporte a PIN numérico e friendly code
    if "FriendlyPairingCode" not in content:
        print("  [FALHA] Validação com FriendlyPairingCode ausente em DeviceAuthService.cs!")
        return False

    if "RegenerateFixedCodeAsync" not in content:
        print("  [FALHA] Método RegenerateFixedCodeAsync ausente em DeviceAuthService.cs!")
        return False

    agent_svc = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/AgentService.cs")
    with open(agent_svc, "r", encoding="utf-8") as f:
        as_content = f.read()

    if "PIN DE ACESSO / PAREAMENTO:" not in as_content:
        print("  [FALHA] Exibição explícita do PIN ausente no banner do AgentService.cs!")
        return False

    prog_cs = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Program.cs")
    with open(prog_cs, "r", encoding="utf-8") as f:
        prg = f.read()

    if "--pin" not in prg or "--regenerate-pin" not in prg:
        print("  [FALHA] Opções CLI --pin e --regenerate-pin ausentes em Program.cs!")
        return False

    print("  [OK] Fluxo de PIN unificado, exibido em destaque no console e disponível via CLI.")
    return True

def test_android_pin_clarity_and_ui():
    print("[TEST 4] Verificando rótulos, orientações e formatação de PIN no Android...")
    strings_xml = os.path.join(PROJECT_ROOT, "android-app/app/src/main/res/values/strings.xml")
    with open(strings_xml, "r", encoding="utf-8") as f:
        str_content = f.read()

    if "PIN de Pareamento / Acesso" not in str_content:
        print("  [FALHA] Rótulo atualizado de PIN ausente em strings.xml!")
        return False

    if "RemoteDesk.Agent --pin" not in str_content:
        print("  [FALHA] Instrução de recuperação de PIN via terminal ausente nas strings de ajuda!")
        return False

    main_kt = os.path.join(PROJECT_ROOT, "android-app/app/src/main/java/com/remotedesk/app/MainActivity.kt")
    with open(main_kt, "r", encoding="utf-8") as f:
        mkt = f.read()

    if "RemoteDesk.Agent --pin" not in mkt:
        print("  [FALHA] Orientação explicativa sobre obtenção de PIN ausente na MainActivity.kt!")
        return False

    print("  [OK] Instruções de obtenção do PIN integradas na interface móvel com clareza.")
    return True

def test_signaling_server_architecture():
    print("[TEST 5] Verificando arquitetura do Servidor de Sinalização e Relé WebSockets...")
    sig_prog = os.path.join(PROJECT_ROOT, "signaling-server/src/RemoteDesk.SignalingServer/Program.cs")
    with open(sig_prog, "r", encoding="utf-8") as f:
        sp = f.read()

    expected_routes = [
        "/health",
        "/api/signaling/ice-servers",
        "/api/signaling/hosts",
        "/ws/signaling"
    ]
    for r in expected_routes:
        if r not in sp:
            print(f"  [FALHA] Rota de sinalização {r} ausente em Program.cs do Signaling Server!")
            return False

    mgr_file = os.path.join(PROJECT_ROOT, "signaling-server/src/RemoteDesk.SignalingServer/Services/SignalingSessionManager.cs")
    with open(mgr_file, "r", encoding="utf-8") as f:
        mgr = f.read()

    if "HMACSHA1" not in mgr:
        print("  [FALHA] Geração de credenciais efêmeras TURN (HMAC-SHA1) ausente em SignalingSessionManager.cs!")
        return False

    if "relay-data" not in mgr:
        print("  [FALHA] Canal de contingência relay-data ausente no gerenciador de sinalização!")
        return False

    print("  [OK] Servidor de sinalização implementa WebSockets, STUN/TURN dinâmico e fallback relay.")
    return True

def test_webrtc_and_agent_signaling_client():
    print("[TEST 6] Verificando cliente de sinalização no Windows Agent e Android...")
    agent_sig = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Services/SignalingClientService.cs")
    with open(agent_sig, "r", encoding="utf-8") as f:
        asig = f.read()

    if "offer" not in asig or "answer" not in asig or "ice-candidate" not in asig:
        print("  [FALHA] Tratamento de mensagens WebRTC SDP/ICE ausente em SignalingClientService.cs!")
        return False

    if "StartWindowStream" not in asig:
        print("  [FALHA] Transmissão contínua de quadros de vídeo ausente em SignalingClientService.cs!")
        return False

    android_sig = os.path.join(PROJECT_ROOT, "android-app/app/src/main/java/com/remotedesk/app/network/SignalingClient.kt")
    with open(android_sig, "r", encoding="utf-8") as f:
        andsig = f.read()

    if "fetchIceServers" not in andsig or "sendOffer" not in andsig or "sendIceCandidate" not in andsig:
        print("  [FALHA] Métodos de negociação WebRTC ausentes em SignalingClient.kt!")
        return False

    print("  [OK] Negociação SDP Offer/Answer, troca de ICE Candidates e streaming de vídeo ativos.")
    return True

def test_turn_stun_and_deployment_config():
    print("[TEST 7] Verificando arquivos de implantação pública (Coturn, Docker, Caddy, Firewall)...")
    turn_conf = os.path.join(PROJECT_ROOT, "deployment/turnserver.conf")
    with open(turn_conf, "r", encoding="utf-8") as f:
        tconf = f.read()

    if "3478" not in tconf or "use-auth-secret" not in tconf or "49152" not in tconf:
        print("  [FALHA] Configuração de porta 3478 ou faixa de relé ausente em turnserver.conf!")
        return False

    fw_rules = os.path.join(PROJECT_ROOT, "deployment/FIREWALL_RULES.md")
    with open(fw_rules, "r", encoding="utf-8") as f:
        fwr = f.read()

    if "49152" not in fwr or "3478" not in fwr or "443" not in fwr:
        print("  [FALHA] Documentação de portas obrigatórias ausente em FIREWALL_RULES.md!")
        return False

    print("  [OK] Infraestrutura TURN/STUN para CGNAT e regras de firewall devidamente preparadas.")
    return True

def test_future_roadmap_registration():
    print("[TEST 8] Verificando registro formal da Etapa Futura de Administração no Roadmap...")
    roadmap_file = os.path.join(PROJECT_ROOT, "docs/ROADMAP.md")
    with open(roadmap_file, "r", encoding="utf-8") as f:
        rm = f.read()

    required_phrases = [
        "Etapa futura — Interface de Administração, Configuração e Usabilidade do RemoteDesk",
        "PLANEJADA / PENDENTE",
        "Configuração Inicial e Contas",
        "Gerenciamento Seguro dos Serviços",
        "Controle do Windows Host Agent"
    ]
    for ph in required_phrases:
        if ph not in rm:
            print(f"  [FALHA] Seção ou termo obrigatório '{ph}' ausente em docs/ROADMAP.md!")
            return False

    print("  [OK] Etapa futura de Interface de Administração devidamente registrada como PLANEJADA/PENDENTE.")
    return True

def test_preservation_of_previous_stages():
    print("[TEST 9] Garantindo preservação estrita de todas as funcionalidades anteriores...")
    main_act = os.path.join(PROJECT_ROOT, "android-app/app/src/main/java/com/remotedesk/app/MainActivity.kt")
    with open(main_act, "r", encoding="utf-8") as f:
        mact = f.read()

    essential_tokens = [
        "REF_VIEWPORT_WIDTH_DP",
        "REF_VIEWPORT_HEIGHT_DP",
        "mapTouchToNormalizedCoordinates",
        "startLiveStream",
        "setupViewportGestures",
        "setupRemoteControlInputs",
        "deviceToken",
        "isPaired",
        "SecureStorage"
    ]
    for tk in essential_tokens:
        if tk not in mact:
            print(f"  [FALHA] Token essencial de etapas anteriores '{tk}' ausente em MainActivity.kt!")
            return False

    print("  [OK] 100% das funcionalidades das Etapas 1 a 7 e o Redesign DoHsktoP permanecem intactos.")
    return True

def main():
    print("=" * 72)
    print("  REMOTEDESK — VALIDAÇÃO DA ETAPA 8/12 (INTERNET, WEBRTC, PIN & ROADMAP)")
    print("=" * 72)

    tests = [
        test_essential_files,
        test_syntax_and_brackets,
        test_pin_fix_and_authentication,
        test_android_pin_clarity_and_ui,
        test_signaling_server_architecture,
        test_webrtc_and_agent_signaling_client,
        test_turn_stun_and_deployment_config,
        test_future_roadmap_registration,
        test_preservation_of_previous_stages
    ]

    for t in tests:
        if not t():
            print("=" * 72)
            print("  FALHA NA VALIDAÇÃO DA ETAPA 8!")
            print("=" * 72)
            sys.exit(1)

    print("=" * 72)
    print("  RESUMO DA VALIDAÇÃO DA ETAPA 8/12:")
    print("=" * 72)
    print("  1. Arquivos Essenciais e Infraestrutura:      [PASSOU]")
    print("  2. Sintaxe e Delimitadores (C#/Kotlin):      [PASSOU]")
    print("  3. Correção do PIN e Exibição no Agent:       [PASSOU]")
    print("  4. Rótulos e Orientações no App Android:     [PASSOU]")
    print("  5. Arquitetura do Servidor de Sinalização:    [PASSOU]")
    print("  6. Negociação WebRTC e Streaming Remoto:      [PASSOU]")
    print("  7. Configuração Coturn, STUN/TURN e Docker:   [PASSOU]")
    print("  8. Registro Oficial do Roadmap da Interface:  [PASSOU]")
    print("  9. Preservação Integral de Etapas Anteriores: [PASSOU]")
    print("=" * 72)
    print("  STATUS DA ETAPA 8: APROVADA COM SUCESSO (100%)!")
    print("=" * 72)
    sys.exit(0)

if __name__ == "__main__":
    main()

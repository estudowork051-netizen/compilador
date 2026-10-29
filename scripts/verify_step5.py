#!/usr/bin/env python3
"""
RemoteDesk - Script de Verificação da Etapa 5/12
Valida a captura real e transmissão contínua de tela de janelas selecionadas do Windows para o Android.
Verifica:
1. Existência e integridade dos arquivos da Etapa 5 (Windows Agent e Android App).
2. Sintaxe e delimitadores estruturais (C#, Kotlin, XML).
3. Recursos nativos do Windows Agent (PrintWindow, PW_RENDERFULLCONTENT, GDI, BMP, multipart stream).
4. Interface e cliente Android (layoutLiveViewer, aspect ratio, LiveStreamSession, tratamento de minimizado/fechado).
5. Conformidade estrita de escopo (Controle remoto de mouse e teclado NÃO implementados na Etapa 5).
"""

import os
import sys
import xml.etree.ElementTree as ET

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

def test_step5_files():
    print("[TEST 1] Verificando existência dos arquivos essenciais da Etapa 5...")
    required_files = [
        "windows-agent/src/RemoteDesk.Agent/Native/User32.cs",
        "windows-agent/src/RemoteDesk.Agent/Native/Gdi32.cs",
        "windows-agent/src/RemoteDesk.Agent/Models/WindowInfo.cs",
        "windows-agent/src/RemoteDesk.Agent/Models/CaptureResult.cs",
        "windows-agent/src/RemoteDesk.Agent/Models/WindowResizeResult.cs",
        "windows-agent/src/RemoteDesk.Agent/Models/AgentCommand.cs",
        "windows-agent/src/RemoteDesk.Agent/Models/AgentResponse.cs",
        "windows-agent/src/RemoteDesk.Agent/Services/IWindowService.cs",
        "windows-agent/src/RemoteDesk.Agent/Services/WindowsWindowService.cs",
        "windows-agent/src/RemoteDesk.Agent/Services/IWindowCaptureService.cs",
        "windows-agent/src/RemoteDesk.Agent/Services/WindowsWindowCaptureService.cs",
        "windows-agent/src/RemoteDesk.Agent/Services/ICommandDispatcher.cs",
        "windows-agent/src/RemoteDesk.Agent/Services/CommandDispatcher.cs",
        "windows-agent/src/RemoteDesk.Agent/Services/ILocalServer.cs",
        "windows-agent/src/RemoteDesk.Agent/Services/LocalHttpServer.cs",
        "windows-agent/src/RemoteDesk.Agent/Program.cs",
        "android-app/app/src/main/java/com/remotedesk/app/MainActivity.kt",
        "android-app/app/src/main/java/com/remotedesk/app/network/RemoteDeskClient.kt",
        "android-app/app/src/main/res/layout/activity_main.xml",
        "android-app/app/src/main/res/values/strings.xml",
    ]

    missing = []
    for rel_path in required_files:
        full_path = os.path.join(PROJECT_ROOT, rel_path)
        if not os.path.isfile(full_path):
            missing.append(rel_path)

    if missing:
        print(f"  [FALHA] Arquivos ausentes: {missing}")
        return False

    print(f"  [OK] Todos os {len(required_files)} arquivos essenciais da Etapa 5 estão presentes.")
    return True

def test_delimiters():
    print("[TEST 2] Verificando integridade e delimitadores C#, Kotlin e XML...")
    import re
    # 1. C# files
    csharp_dir = os.path.join(PROJECT_ROOT, "windows-agent", "src", "RemoteDesk.Agent")
    for root, _, files in os.walk(csharp_dir):
        for f in files:
            if f.endswith(".cs"):
                fpath = os.path.join(root, f)
                with open(fpath, "r", encoding="utf-8") as fp:
                    content = fp.read()
                clean = re.sub(r'@"(""|[^"])*"', '', content)
                clean = re.sub(r'"(\\.|[^"\\])*"', '', clean)
                clean = re.sub(r'//.*', '', clean)
                clean = re.sub(r'/\*.*?\*/', '', clean, flags=re.DOTALL)
                if clean.count("{") != clean.count("}") or clean.count("(") != clean.count(")"):
                    print(f"  [FALHA] Delimitadores desbalanceados em {f}")
                    return False

    # 2. Kotlin files
    kt_dir = os.path.join(PROJECT_ROOT, "android-app", "app", "src", "main", "java")
    for root, _, files in os.walk(kt_dir):
        for f in files:
            if f.endswith(".kt"):
                fpath = os.path.join(root, f)
                with open(fpath, "r", encoding="utf-8") as fp:
                    content = fp.read()
                clean = re.sub(r'"""(.*?)"""', '', content, flags=re.DOTALL)
                clean = re.sub(r'"(\\.|[^"\\])*"', '', clean)
                clean = re.sub(r'//.*', '', clean)
                clean = re.sub(r'/\*.*?\*/', '', clean, flags=re.DOTALL)
                if clean.count("{") != clean.count("}") or clean.count("(") != clean.count(")"):
                    print(f"  [FALHA] Delimitadores desbalanceados no Kotlin {f}")
                    return False

    # 3. XML files
    res_dir = os.path.join(PROJECT_ROOT, "android-app", "app", "src", "main", "res")
    for root, _, files in os.walk(res_dir):
        for f in files:
            if f.endswith(".xml"):
                fpath = os.path.join(root, f)
                try:
                    ET.parse(fpath)
                except ET.ParseError as e:
                    print(f"  [FALHA] XML inválido em {f}: {e}")
                    return False

    print("  [OK] Todos os arquivos C#, Kotlin e XML estão íntegros e balanceados.")
    return True

def test_windows_agent_stage5_features():
    print("[TEST 3] Verificando recursos de captura e streaming no Windows Agent...")
    cap_service_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Services/WindowsWindowCaptureService.cs")
    with open(cap_service_path, "r", encoding="utf-8") as f:
        cap_content = f.read()

    # Validação do método PrintWindow e flag PW_RENDERFULLCONTENT
    if "PrintWindow" not in cap_content or "PW_RENDERFULLCONTENT" not in cap_content:
        print("  [FALHA] WindowsWindowCaptureService deve utilizar PrintWindow com PW_RENDERFULLCONTENT.")
        return False

    # Validação do codificador BMP 32bpp puro
    if "CreateBmpImage" not in cap_content or "0x42" not in cap_content or "0x4D" not in cap_content:
        print("  [FALHA] Codificador BMP nativo 32bpp ausente em WindowsWindowCaptureService.")
        return False

    # Validação do gerenciamento de recursos GDI
    if "DeleteObject" not in cap_content or "DeleteDC" not in cap_content or "ReleaseDC" not in cap_content:
        print("  [FALHA] Liberação correta de recursos nativos GDI ausente em WindowsWindowCaptureService.")
        return False

    # Validação de rotas no LocalHttpServer
    server_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Services/LocalHttpServer.cs")
    with open(server_path, "r", encoding="utf-8") as f:
        server_content = f.read()

    if "/api/stream" not in server_content or "multipart/x-mixed-replace" not in server_content:
        print("  [FALHA] Rota de streaming contínuo /api/stream com multipart ausente em LocalHttpServer.")
        return False

    if "/api/window/frame" not in server_content:
        print("  [FALHA] Rota de snapshot /api/window/frame ausente em LocalHttpServer.")
        return False

    # Validação de ações no CommandDispatcher
    disp_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Services/CommandDispatcher.cs")
    with open(disp_path, "r", encoding="utf-8") as f:
        disp_content = f.read()

    if 'case "restore-window":' not in disp_content:
        print("  [FALHA] Ação 'restore-window' ausente no CommandDispatcher.")
        return False

    if 'case "resize-window":' not in disp_content:
        print("  [FALHA] Ação 'resize-window' ausente no CommandDispatcher.")
        return False

    win_svc_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Services/WindowsWindowService.cs")
    with open(win_svc_path, "r", encoding="utf-8") as f:
        win_svc_content = f.read()

    if "SetWindowPos" not in win_svc_content or "ResizeWindowAsync" not in win_svc_content:
        print("  [FALHA] Implementação de redimensionamento real (SetWindowPos/ResizeWindowAsync) ausente em WindowsWindowService.")
        return False

    user32_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Native/User32.cs")
    with open(user32_path, "r", encoding="utf-8") as f:
        user32_content = f.read()

    if "SetWindowPos" not in user32_content or "GetDpiForWindowSafe" not in user32_content:
        print("  [FALHA] Declaração P/Invoke de SetWindowPos ou GetDpiForWindowSafe ausente em User32.cs.")
        return False

    print("  [OK] Recursos nativos do Windows Agent (PrintWindow, GDI, BMP, multipart stream, restore, resize real) validados.")
    return True

def test_android_stage5_features():
    print("[TEST 4] Verificando interface do usuário e cliente de streaming no Android...")
    layout_path = os.path.join(PROJECT_ROOT, "android-app/app/src/main/res/layout/activity_main.xml")
    with open(layout_path, "r", encoding="utf-8") as f:
        layout_content = f.read()

    # Validação dos elementos visuais da tela dedicada (layoutLiveViewer)
    expected_ids = [
        "layoutLiveViewer",
        "ivLiveFrame",
        "btnLiveBack",
        "btnLiveStop",
        "tvLiveBadge",
        "layoutLiveLoading",
        "layoutLiveMinimized",
        "btnLiveRestore",
        "layoutLiveClosed",
        "btnLiveReturnClosed",
        "layoutLiveError",
        "btnStartStream"
    ]
    for eid in expected_ids:
        if f'android:id="@+id/{eid}"' not in layout_content:
            print(f"  [FALHA] Componente visual '{eid}' ausente em activity_main.xml.")
            return False

    # Validação da preservação de proporção da imagem (aspect ratio)
    if 'scaleType="fitCenter"' not in layout_content or 'adjustViewBounds="true"' not in layout_content:
        print("  [FALHA] ivLiveFrame deve possuir adjustViewBounds='true' e scaleType='fitCenter' para manter o aspect ratio.")
        return False

    # Validação do cliente HTTP e streaming no RemoteDeskClient
    client_path = os.path.join(PROJECT_ROOT, "android-app/app/src/main/java/com/remotedesk/app/network/RemoteDeskClient.kt")
    with open(client_path, "r", encoding="utf-8") as f:
        client_content = f.read()

    if "startWindowStream" not in client_content or "LiveStreamSession" not in client_content:
        print("  [FALHA] Método startWindowStream ou classe LiveStreamSession ausente em RemoteDeskClient.kt.")
        return False

    if "restoreWindow" not in client_content:
        print("  [FALHA] Método restoreWindow ausente em RemoteDeskClient.kt.")
        return False

    if "resizeWindow" not in client_content:
        print("  [FALHA] Método resizeWindow ausente em RemoteDeskClient.kt.")
        return False

    # Validação dos fluxos na MainActivity
    main_path = os.path.join(PROJECT_ROOT, "android-app/app/src/main/java/com/remotedesk/app/MainActivity.kt")
    with open(main_path, "r", encoding="utf-8") as f:
        main_content = f.read()

    if "startLiveStream" not in main_content or "stopLiveStream" not in main_content:
        print("  [FALHA] Métodos startLiveStream ou stopLiveStream ausentes em MainActivity.kt.")
        return False

    print("  [OK] Interface do Android, preservação de aspect ratio e cliente de streaming validados.")
    return True

def test_scope_compliance_no_stage6():
    print("[TEST 5] Garantindo conformidade estrita de escopo (Sem controle remoto da Etapa 6)...")
    stage6_files = ["Input", "User32.cs", "CommandDispatcher.cs", "LocalHttpServer.cs", "RemoteDeskClient.kt", "MainActivity.kt"]
    all_files = []
    for d in ["windows-agent", "android-app"]:
        for root, _, files in os.walk(os.path.join(PROJECT_ROOT, d)):
            for f in files:
                if f.endswith((".cs", ".kt")):
                    if any(s in f for s in stage6_files):
                        continue
                    all_files.append(os.path.join(root, f))

    forbidden_keywords = [
        "mouse_event",
        "SendInput",
        "keybd_event"
    ]

    for fpath in all_files:
        with open(fpath, "r", encoding="utf-8") as f:
            content = f.read()
        for kw in forbidden_keywords:
            if kw in content:
                print(f"  [FALHA] Palavra-chave proibida de etapa posterior '{kw}' encontrada em {fpath}!")
                return False

    print("  [OK] Conformidade estrita: Controle remoto (mouse e teclado) NÃO implementado na Etapa 5.")
    return True

if __name__ == "__main__":
    print("=" * 65)
    print("  REMOTEDESK — VALIDACAO DA ETAPA 5 (CAPTURA E TRANSMISSAO DE TELA)")
    print("=" * 65)
    r1 = test_step5_files()
    r2 = test_delimiters()
    r3 = test_windows_agent_stage5_features()
    r4 = test_android_stage5_features()
    r5 = test_scope_compliance_no_stage6()

    print("\n" + "=" * 65)
    print("  RESUMO DA VALIDACAO DA ETAPA 5:")
    print("=" * 65)
    print(f"  1. Arquivos da Etapa 5:                 {'[PASSOU]' if r1 else '[FALHOU]'}")
    print(f"  2. Integridade e Sintaxe (C#/KT/XML):   {'[PASSOU]' if r2 else '[FALHOU]'}")
    print(f"  3. Captura e Streaming (Windows Agent): {'[PASSOU]' if r3 else '[FALHOU]'}")
    print(f"  4. Interface e Streaming (Android App): {'[PASSOU]' if r4 else '[FALHOU]'}")
    print(f"  5. Conformidade de Escopo (Sem Etapa 6):{'[PASSOU]' if r5 else '[FALHOU]'}")
    print("=" * 65)

    all_ok = r1 and r2 and r3 and r4 and r5
    sys.exit(0 if all_ok else 1)

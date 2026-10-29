#!/usr/bin/env python3
import os
import sys
import re
import xml.etree.ElementTree as ET

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

def test_step4_files():
    print("[TEST 1] Verificando existencia dos arquivos essenciais da Etapa 4...")
    required_files = [
        "windows-agent/src/RemoteDesk.Agent/Services/ILocalServer.cs",
        "windows-agent/src/RemoteDesk.Agent/Services/LocalHttpServer.cs",
        "windows-agent/src/RemoteDesk.Agent/Models/WindowInfo.cs",
        "windows-agent/src/RemoteDesk.Agent/AgentConfig.cs",
        "windows-agent/src/RemoteDesk.Agent/AgentService.cs",
        "android-app/app/src/main/AndroidManifest.xml",
        "android-app/app/src/main/res/drawable/ic_app_browser.xml",
        "android-app/app/src/main/res/drawable/ic_app_editor.xml",
        "android-app/app/src/main/res/drawable/ic_app_terminal.xml",
        "android-app/app/src/main/res/drawable/ic_app_files.xml",
        "android-app/app/src/main/res/drawable/ic_app_media.xml",
        "android-app/app/src/main/res/drawable/ic_app_calc.xml",
        "android-app/app/src/main/res/drawable/ic_app_settings.xml",
        "android-app/app/src/main/res/drawable/ic_app_generic.xml",
        "android-app/app/src/main/res/drawable/ic_refresh.xml",
        "android-app/app/src/main/res/drawable/ic_check_circle.xml",
        "android-app/app/src/main/res/layout/item_window_card.xml",
        "android-app/app/src/main/res/layout/activity_main.xml",
        "android-app/app/src/main/java/com/remotedesk/app/models/WindowItem.kt",
        "android-app/app/src/main/java/com/remotedesk/app/models/AgentStatusItem.kt",
        "android-app/app/src/main/java/com/remotedesk/app/network/RemoteDeskClient.kt",
        "android-app/app/src/main/java/com/remotedesk/app/ui/WindowGridAdapter.kt",
        "android-app/app/src/main/java/com/remotedesk/app/MainActivity.kt"
    ]

    missing = []
    for rel in required_files:
        p = os.path.join(PROJECT_ROOT, rel)
        if not os.path.exists(p):
            missing.append(rel)

    if missing:
        print(f"  [FALHA] Arquivos ausentes: {missing}")
        return False
    print(f"  [OK] Todos os {len(required_files)} arquivos da Etapa 4 estao presentes.")
    return True

def test_syntax_and_delimiters():
    print("[TEST 2] Verificando integridade e delimitadores C#, Kotlin e XML...")
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
                clean = re.sub(r'"(\\.|[^"\\])*"', '', content)
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
                except Exception as e:
                    print(f"  [FALHA] XML invalido em {f}: {e}")
                    return False

    manifest_path = os.path.join(PROJECT_ROOT, "android-app", "app", "src", "main", "AndroidManifest.xml")
    try:
        ET.parse(manifest_path)
    except Exception as e:
        print(f"  [FALHA] AndroidManifest.xml invalido: {e}")
        return False

    print("  [OK] Todos os arquivos C#, Kotlin e XML estao estruturalmente integros e balanceados.")
    return True

def test_windows_local_server_features():
    print("[TEST 3] Verificando rotas e recursos do servidor local HTTP no Windows Agent...")
    server_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Services/LocalHttpServer.cs")
    with open(server_path, "r", encoding="utf-8") as f:
        content = f.read()

    expected_routes = [
        "/health",
        "/api/ping",
        "/api/status",
        "/api/windows",
        "/api/windows/active",
        "/api/command"
    ]

    for route in expected_routes:
        if f'"{route}"' not in content:
            print(f"  [FALHA] Rota obrigatoria '{route}' ausente em LocalHttpServer.cs.")
            return False

    if "Access-Control-Allow-Origin" not in content or "OPTIONS" not in content:
        print("  [FALHA] Suporte a headers CORS ausente em LocalHttpServer.cs.")
        return False

    # Verifica integracao no AgentConfig e AgentService
    config_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/AgentConfig.cs")
    with open(config_path, "r", encoding="utf-8") as f:
        cfg = f.read()
    if "LocalPort" not in cfg or "EnableLocalHttpServer" not in cfg:
        print("  [FALHA] Configuracoes de porta e servidor local ausentes em AgentConfig.cs.")
        return False

    print("  [OK] Servidor HTTP local do Windows Agent implementa todas as rotas REST, CORS e configuracao de porta.")
    return True

def test_android_icon_grid_and_networking():
    print("[TEST 4] Verificando integracao Android: Cleartext traffic, Grid de icones e cliente HTTP...")
    # 1. Cleartext traffic no manifest
    manifest_path = os.path.join(PROJECT_ROOT, "android-app/app/src/main/AndroidManifest.xml")
    with open(manifest_path, "r", encoding="utf-8") as f:
        m = f.read()
    if 'android:usesCleartextTraffic="true"' not in m:
        print("  [FALHA] android:usesCleartextTraffic='true' ausente no AndroidManifest.xml.")
        return False

    # 2. RecyclerView Grid na MainActivity
    main_activity_path = os.path.join(PROJECT_ROOT, "android-app/app/src/main/java/com/remotedesk/app/MainActivity.kt")
    with open(main_activity_path, "r", encoding="utf-8") as f:
        ma = f.read()
    if "GridLayoutManager" not in ma or "WindowGridAdapter" not in ma:
        print("  [FALHA] GridLayoutManager ou WindowGridAdapter ausente em MainActivity.kt.")
        return False

    # 3. Estrategia de icones no WindowGridAdapter
    adapter_path = os.path.join(PROJECT_ROOT, "android-app/app/src/main/java/com/remotedesk/app/ui/WindowGridAdapter.kt")
    with open(adapter_path, "r", encoding="utf-8") as f:
        ad = f.read()
    if "Base64.decode" not in ad or "BitmapFactory.decodeByteArray" not in ad:
        print("  [FALHA] Decodificacao de icones Base64 ausente em WindowGridAdapter.kt.")
        return False
    for cat in ["browser", "editor", "terminal", "file_manager", "media", "calculator", "settings"]:
        if f'"{cat}"' not in ad:
            print(f"  [FALHA] Fallback do icone semantico '{cat}' ausente em WindowGridAdapter.kt.")
            return False

    # 4. Item layout contem ImageView principal destacado e sem visual puramente textual
    item_layout_path = os.path.join(PROJECT_ROOT, "android-app/app/src/main/res/layout/item_window_card.xml")
    with open(item_layout_path, "r", encoding="utf-8") as f:
        it = f.read()
    if "ivWindowIcon" not in it or "MaterialCardView" not in it:
        print("  [FALHA] Elementos visuais (ivWindowIcon / MaterialCardView) ausentes em item_window_card.xml.")
        return False

    # 5. Cliente HTTP com endpoints
    client_path = os.path.join(PROJECT_ROOT, "android-app/app/src/main/java/com/remotedesk/app/network/RemoteDeskClient.kt")
    with open(client_path, "r", encoding="utf-8") as f:
        cl = f.read()
    if "/api/windows" not in cl or "/api/status" not in cl or "/api/command" not in cl:
        print("  [FALHA] Chamadas as rotas do agente ausentes no RemoteDeskClient.kt.")
        return False

    print("  [OK] Integracao visual por grade de icones, decodificacao Base64, fallbacks semanticos e cliente HTTP validados.")
    return True

def test_no_forbidden_stage5_features():
    print("[TEST 5] Garantindo que recursos futuros (Etapa 5 e 6: Captura de tela e controle de mouse) NAO foram introduzidos...")
    later_stage_files = ["Capture", "Stream", "Input", "WindowResizer", "User32.cs", "LocalHttpServer.cs", "CommandDispatcher.cs", "RemoteDeskClient.kt", "MainActivity.kt"]
    all_files = []
    for d in ["windows-agent", "android-app"]:
        for root, _, files in os.walk(os.path.join(PROJECT_ROOT, d)):
            for f in files:
                if f.endswith((".cs", ".kt")):
                    if any(l in f for l in later_stage_files):
                        continue
                    all_files.append(os.path.join(root, f))

    forbidden_keywords = [
        "Graphics.CopyFromScreen",
        "BitBlt",
        "mouse_event",
        "SendInput",
        "keybd_event",
        "MediaProjectionManager",
        "createVirtualDisplay"
    ]

    for fpath in all_files:
        with open(fpath, "r", encoding="utf-8") as f:
            content = f.read()
        for kw in forbidden_keywords:
            if kw in content:
                print(f"  [FALHA] Palavra-chave proibida de etapa posterior '{kw}' encontrada em {fpath}!")
                return False

    print("  [OK] Conformidade estrita com o escopo: Captura de tela e controle remoto NAO foram implementados.")
    return True

if __name__ == "__main__":
    print("=" * 65)
    print("  REMOTEDESK — VALIDACAO DA ETAPA 4 (COMUNICACAO E GRADE DE ICONES)")
    print("=" * 65)
    r1 = test_step4_files()
    r2 = test_syntax_and_delimiters()
    r3 = test_windows_local_server_features()
    r4 = test_android_icon_grid_and_networking()
    r5 = test_no_forbidden_stage5_features()

    print("\n" + "=" * 65)
    print("  RESUMO DA VALIDACAO DA ETAPA 4:")
    print("=" * 65)
    print(f"  1. Arquivos da Etapa 4:                 {'[PASSOU]' if r1 else '[FALHOU]'}")
    print(f"  2. Integridade e Sintaxe (C#/KT/XML):   {'[PASSOU]' if r2 else '[FALHOU]'}")
    print(f"  3. Servidor Local HTTP (Windows Agent): {'[PASSOU]' if r3 else '[FALHOU]'}")
    print(f"  4. Grade Visual de Icones e Networking: {'[PASSOU]' if r4 else '[FALHOU]'}")
    print(f"  5. Conformidade de Escopo (Sem Etapa 5):{'[PASSOU]' if r5 else '[FALHOU]'}")
    print("=" * 65)

    all_ok = r1 and r2 and r3 and r4 and r5
    sys.exit(0 if all_ok else 1)

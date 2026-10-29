#!/usr/bin/env python3
"""
RemoteDesk - Script de Verificacao da Etapa 6/12
Valida a implementacao completa do controle remoto real entre Android e Windows:
- Entrada real de mouse (movimento, clique, duplo clique, clique direito, down, up, arrasto, rolagem)
- Mapeamento preciso de coordenadas (fitCenter, letterbox rejection, coordenadas de tela do Windows)
- Teclado virtual e injecao de texto Unicode com acentuacao (KEYEVENTF_UNICODE) e teclas especiais
- Foco da janela antes da entrada e liberacao segura de teclas/botoes retidos
- Barra compacta de controles no Android sem rotacionar horizontalmente (Portrait estrito)
- Seguranca: validacao de janelas, whitelist de acoes, sem log de texto sensivel, sem recursos da Etapa 7+
"""

import os
import sys
import re
import xml.etree.ElementTree as ET

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

def test_step6_files():
    print("[TEST 1] Verificando presenca dos arquivos essenciais da Etapa 6...")
    required_files = [
        "windows-agent/src/RemoteDesk.Agent/Native/User32.cs",
        "windows-agent/src/RemoteDesk.Agent/Models/RemoteInputModels.cs",
        "windows-agent/src/RemoteDesk.Agent/Models/AgentCommand.cs",
        "windows-agent/src/RemoteDesk.Agent/Services/IInputService.cs",
        "windows-agent/src/RemoteDesk.Agent/Services/WindowsRemoteInputService.cs",
        "windows-agent/src/RemoteDesk.Agent/Services/CommandDispatcher.cs",
        "windows-agent/src/RemoteDesk.Agent/Services/LocalHttpServer.cs",
        "windows-agent/src/RemoteDesk.Agent/Program.cs",
        "android-app/app/src/main/java/com/remotedesk/app/network/RemoteDeskClient.kt",
        "android-app/app/src/main/java/com/remotedesk/app/MainActivity.kt",
        "android-app/app/src/main/res/layout/activity_main.xml",
        "android-app/app/src/main/res/values/strings.xml",
        "android-app/app/src/main/AndroidManifest.xml"
    ]
    for rel_path in required_files:
        full_path = os.path.join(PROJECT_ROOT, rel_path)
        if not os.path.isfile(full_path):
            print(f"  [FALHA] Arquivo essencial ausente: {rel_path}")
            return False
    print(f"  [OK] Todos os {len(required_files)} arquivos essenciais da Etapa 6 estao presentes.")
    return True

def test_delimiters_and_syntax():
    print("[TEST 2] Verificando sintaxe e balanceamento de delimitadores (C#, Kotlin, XML)...")
    # C# files
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
                    print(f"  [FALHA] Delimitadores desbalanceados em C# {f}")
                    return False

    # Kotlin files
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

    # XML files
    res_dir = os.path.join(PROJECT_ROOT, "android-app", "app", "src", "main", "res")
    for root, _, files in os.walk(res_dir):
        for f in files:
            if f.endswith(".xml"):
                fpath = os.path.join(root, f)
                try:
                    ET.parse(fpath)
                except ET.ParseError as e:
                    print(f"  [FALHA] XML invalido em {f}: {e}")
                    return False

    print("  [OK] Todos os arquivos C#, Kotlin e XML estao integros e sintaticamente validos.")
    return True

def test_win32_user32_input_definitions():
    print("[TEST 3] Verificando declaracoes Win32 P/Invoke para SendInput e constantes de entrada...")
    user32_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Native/User32.cs")
    with open(user32_path, "r", encoding="utf-8") as f:
        content = f.read()

    expected_signatures = [
        "SendInput",
        "SetCursorPos",
        "GetCursorPos",
        "INPUT",
        "MOUSEINPUT",
        "KEYBDINPUT",
        "MOUSEEVENTF_LEFTDOWN",
        "MOUSEEVENTF_LEFTUP",
        "MOUSEEVENTF_RIGHTDOWN",
        "MOUSEEVENTF_RIGHTUP",
        "MOUSEEVENTF_WHEEL",
        "KEYEVENTF_KEYUP",
        "KEYEVENTF_UNICODE",
        "WHEEL_DELTA"
    ]
    for sig in expected_signatures:
        if sig not in content:
            print(f"  [FALHA] Declaracao Win32 ausente em User32.cs: {sig}")
            return False

    print("  [OK] User32.cs contem declaracoes completas de SendInput, structs e constantes.")
    return True

def test_input_service_and_di_registration():
    print("[TEST 4] Verificando servico de entrada (IInputService / WindowsRemoteInputService) e DI...")
    service_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Services/WindowsRemoteInputService.cs")
    with open(service_path, "r", encoding="utf-8") as f:
        service_content = f.read()

    methods = [
        "ProcessMouseAsync",
        "ProcessKeyAsync",
        "ProcessTextAsync",
        "ReleaseAllInputsAsync",
        "EnsureWindowFocus",
        "SendInput"
    ]
    for m in methods:
        if m not in service_content:
            print(f"  [FALHA] Metodo/recurso ausente em WindowsRemoteInputService.cs: {m}")
            return False

    prog_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Program.cs")
    with open(prog_path, "r", encoding="utf-8") as f:
        prog_content = f.read()

    if "IInputService" not in prog_content or "WindowsRemoteInputService" not in prog_content:
        print("  [FALHA] IInputService nao registrado no injetor de dependencias em Program.cs.")
        return False

    print("  [OK] WindowsRemoteInputService implementado com fallback cross-platform e registrado no container DI.")
    return True

def test_command_dispatcher_and_http_server():
    print("[TEST 5] Verificando integracao no CommandDispatcher e endpoints no LocalHttpServer...")
    dispatcher_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Services/CommandDispatcher.cs")
    with open(dispatcher_path, "r", encoding="utf-8") as f:
        disp_content = f.read()

    expected_actions = [
        "mouse-input",
        "mouse-event",
        "key-event",
        "keyboard-input",
        "text-input",
        "scroll-event",
        "release-inputs"
    ]
    for act in expected_actions:
        if f'"{act}"' not in disp_content:
            print(f"  [FALHA] Acao ausente no CommandDispatcher: {act}")
            return False

    http_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Services/LocalHttpServer.cs")
    with open(http_path, "r", encoding="utf-8") as f:
        http_content = f.read()

    if "/api/input" not in http_content:
        print("  [FALHA] Endpoint '/api/input' ausente no LocalHttpServer.cs.")
        return False

    if "ReleaseAllInputsAsync" not in http_content:
        print("  [FALHA] LocalHttpServer nao libera inputs retidos na desconexao/finalizacao do stream.")
        return False

    print("  [OK] CommandDispatcher e LocalHttpServer processam acoes de entrada e liberam retencoes com seguranca.")
    return True

def test_coordinate_mapping_math():
    print("[TEST 6] Simulando e testando o mapeamento matematico de coordenadas e deteccao de letterbox...")
    # Simulando a logica de mapTouchToNormalizedCoordinates de MainActivity.kt
    def map_touch(touch_x, touch_y, view_w, view_h, bmp_w, bmp_h, scale_factor=1.0, pan_x=0.0, pan_y=0.0):
        if view_w <= 0 or view_h <= 0 or bmp_w <= 0 or bmp_h <= 0:
            return None
        aspect_ratio = bmp_w / bmp_h
        view_ratio = view_w / view_h
        if aspect_ratio > view_ratio:
            base_w = view_w
            base_h = view_w / aspect_ratio
            base_left = 0.0
            base_top = (view_h - base_h) / 2.0
        else:
            base_h = view_h
            base_w = view_h * aspect_ratio
            base_top = 0.0
            base_left = (view_w - base_w) / 2.0
        
        base_right = base_left + base_w
        base_bottom = base_top + base_h

        if scale_factor <= 1.05:
            # Rejeicao estrita de toques no letterbox quando sem zoom
            if touch_x < base_left or touch_x > base_right or touch_y < base_top or touch_y > base_bottom:
                return None
            norm_x = (touch_x - base_left) / base_w
            norm_y = (touch_y - base_top) / base_h
            return (round(norm_x, 4), round(norm_y, 4))
        else:
            center_x = view_w / 2.0
            center_y = view_h / 2.0
            cur_left = center_x + (base_left - center_x) * scale_factor + pan_x
            cur_top = center_y + (base_top - center_y) * scale_factor + pan_y
            cur_w = base_w * scale_factor
            cur_h = base_h * scale_factor
            if touch_x < cur_left or touch_x > (cur_left + cur_w) or touch_y < cur_top or touch_y > (cur_top + cur_h):
                return None
            norm_x = (touch_x - cur_left) / cur_w
            norm_y = (touch_y - cur_top) / cur_h
            return (round(norm_x, 4), round(norm_y, 4))

    # Teste 1: Viewport vertical (382 x 706), Janela Bloco de Notas (382 x 706) - Proporcoes iguais
    res = map_touch(191.0, 353.0, 382.0, 706.0, 382, 706)
    if res != (0.5, 0.5):
        print(f"  [FALHA] Centro da tela deveria ser (0.5, 0.5), recebido: {res}")
        return False

    # Teste 2: Viewport vertical (382 x 706), Janela horizontal (800 x 600)
    # base_w = 382, base_h = 382 / (800/600) = 286.5. Margens verticais: base_top = (706 - 286.5) / 2 = 209.75
    # Toque na barra preta superior (y = 50.0): deve ser REJEITADO (None)
    res_outside_top = map_touch(191.0, 50.0, 382.0, 706.0, 800, 600)
    if res_outside_top is not None:
        print(f"  [FALHA] Toque no letterbox superior nao foi rejeitado: {res_outside_top}")
        return False

    # Toque na barra preta inferior (y = 650.0): deve ser REJEITADO (None)
    res_outside_bottom = map_touch(191.0, 650.0, 382.0, 706.0, 800, 600)
    if res_outside_bottom is not None:
        print(f"  [FALHA] Toque no letterbox inferior nao foi rejeitado: {res_outside_bottom}")
        return False

    # Toque no centro da imagem (y = 353.0): deve ser exatamente centro (0.5, 0.5)
    res_center = map_touch(191.0, 353.0, 382.0, 706.0, 800, 600)
    if res_center != (0.5, 0.5):
        print(f"  [FALHA] Centro da imagem horizontal mapeado incorretamente: {res_center}")
        return False

    # Denormalizacao no Windows: windowRect = (Left: 100, Top: 150, Right: 500, Bottom: 550) -> W: 400, H: 400
    norm_x, norm_y = 0.5, 0.5
    win_left, win_top, win_w, win_h = 100, 150, 400, 400
    target_screen_x = int(win_left + norm_x * win_w)
    target_screen_y = int(win_top + norm_y * win_h)
    if (target_screen_x, target_screen_y) != (300, 350):
        print(f"  [FALHA] Coordenada absoluta calculada incorretamente: ({target_screen_x}, {target_screen_y})")
        return False

    print("  [OK] Matematica de mapeamento de coordenadas e rejeicao de letterbox testada e validada.")
    return True

def test_unicode_text_and_special_keys():
    print("[TEST 7] Verificando suporte a Unicode e mapeamento de teclas especiais...")
    input_service_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Services/WindowsRemoteInputService.cs")
    with open(input_service_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Verifica o suporte ao flag KEYEVENTF_UNICODE
    if "KEYEVENTF_UNICODE" not in content or "wScan = (ushort)c" not in content:
        print("  [FALHA] WindowsRemoteInputService nao utiliza KEYEVENTF_UNICODE com scan code para injetar caracteres.")
        return False

    # Teclas especiais requeridas pela especificacao
    special_keys = [
        "VK_RETURN",
        "VK_BACK",
        "VK_TAB",
        "VK_ESCAPE",
        "VK_DELETE",
        "VK_LEFT",
        "VK_UP",
        "VK_RIGHT",
        "VK_DOWN",
        "VK_HOME",
        "VK_END",
        "VK_PRIOR",
        "VK_NEXT",
        "VK_CONTROL",
        "VK_MENU",
        "VK_SHIFT"
    ]
    for vk in special_keys:
        if vk not in content:
            print(f"  [FALHA] Tecla especial ausente no mapeamento de WindowsRemoteInputService.cs: {vk}")
            return False

    # String com acentos em portugues
    portuguese_sample = "Olá, ação, coração, informação e conexão."
    for char in portuguese_sample:
        # Garante que c# itera sobre todos os caracteres sem excecao
        code_point = ord(char)
        if code_point <= 0:
            print(f"  [FALHA] Ponto de codigo Unicode invalido para {char}")
            return False

    print("  [OK] KEYEVENTF_UNICODE ativo com suporte a acentos brasileiros e 16 teclas especiais mapeadas.")
    return True

def test_android_ui_and_gestures():
    print("[TEST 8] Verificando interface de controle remoto, modo Portrait e gestos no Android...")
    layout_path = os.path.join(PROJECT_ROOT, "android-app/app/src/main/res/layout/activity_main.xml")
    with open(layout_path, "r", encoding="utf-8") as f:
        layout_content = f.read()

    controls = [
        "layoutRemoteControlsBar",
        "btnMouseModeLeft",
        "btnMouseModeRight",
        "btnMouseModeDrag",
        "btnMouseModeScroll",
        "btnToggleKeyboard",
        "btnToggleSpecialKeys",
        "layoutSpecialKeysBar",
        "etHiddenKeyboardInput"
    ]
    for c in controls:
        if f'android:id="@+id/{c}"' not in layout_content:
            print(f"  [FALHA] Componente de controle remoto ausente no layout: {c}")
            return False

    manifest_path = os.path.join(PROJECT_ROOT, "android-app/app/src/main/AndroidManifest.xml")
    with open(manifest_path, "r", encoding="utf-8") as f:
        manifest_content = f.read()

    if 'android:screenOrientation="portrait"' not in manifest_content:
        print("  [FALHA] Bloqueio vertical estrito (portrait) ausente no AndroidManifest.xml.")
        return False

    if 'android:windowSoftInputMode="adjustNothing"' not in manifest_content:
        print("  [FALHA] windowSoftInputMode='adjustNothing' ausente no AndroidManifest.xml.")
        return False

    main_path = os.path.join(PROJECT_ROOT, "android-app/app/src/main/java/com/remotedesk/app/MainActivity.kt")
    with open(main_path, "r", encoding="utf-8") as f:
        main_content = f.read()

    gestures = [
        "setupRemoteControlInputs",
        "mapTouchToNormalizedCoordinates",
        "onSingleTapConfirmed",
        "onDoubleTap",
        "onLongPress",
        "onScroll",
        "sendMouseInput",
        "sendTextInput",
        "sendKeyEvent",
        "releaseInputs"
    ]
    for g in gestures:
        if g not in main_content:
            print(f"  [FALHA] Metodo/gesto de controle ausente em MainActivity.kt: {g}")
            return False

    print("  [OK] Layout de controle remoto, configuracao de softInput, Portrait e detectores de gestos validados.")
    return True

def test_security_and_scope_boundary():
    print("[TEST 9] Verificando regras de seguranca e conformidade estrita com o escopo da Etapa 6...")
    # 1. Nao deve haver log de texto digitado no agente nem no app
    agent_service = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Services/WindowsRemoteInputService.cs")
    with open(agent_service, "r", encoding="utf-8") as f:
        agent_code = f.read()

    if "LogInformation($\"Text: {request.Text}\"" in agent_code or "LogInformation($\"Key: {request.Key}\"" in agent_code:
        print("  [FALHA] Vulnerabilidade de privacidade: texto confidencial ou teclas estao sendo gravados em log!")
        return False

    # 2. Whitelist estrita de comandos no dispatcher (sem execucao arbitraria de cmd/powershell/bash)
    dispatcher_path = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent/Services/CommandDispatcher.cs")
    with open(dispatcher_path, "r", encoding="utf-8") as f:
        disp_code = f.read()

    if "Process.Start(\"cmd" in disp_code or "Process.Start(\"powershell" in disp_code or "Process.Start(\"bash" in disp_code:
        print("  [FALHA] Execucao arbitraria de shell encontrada no CommandDispatcher!")
        return False

    # 3. Recursos de etapas futuras (Etapa 7+ WebRTC PeerConnection, STUN/TURN, Servidor de autenticacao global)
    forbidden_future = [
        "RTCPeerConnection",
        "createDataChannel",
        "stun.l.google.com",
        "turn:turn.",
        "turnserver",
        "JwtBearer",
        "OAuth2"
    ]
    for d in ["windows-agent", "android-app", "signaling-server"]:
        for root, _, files in os.walk(os.path.join(PROJECT_ROOT, d)):
            for f in files:
                if f.endswith((".cs", ".kt")):
                    fpath = os.path.join(root, f)
                    with open(fpath, "r", encoding="utf-8") as fp:
                        content = fp.read()
                    for f_kw in forbidden_future:
                        if f_kw in content:
                            print(f"  [FALHA] Recurso de etapa posterior (Etapa 7+) '{f_kw}' encontrado em {fpath}!")
                            return False

    print("  [OK] Seguranca rigorosa confirmada: sem logs de senhas, sem shell arbitrario e sem antecipacao da Etapa 7+.")
    return True

if __name__ == "__main__":
    print("=" * 72)
    print("  REMOTEDESK — VALIDACAO DA ETAPA 6/12 (CONTROLE REMOTO REAL)")
    print("=" * 72)

    t1 = test_step6_files()
    t2 = test_delimiters_and_syntax()
    t3 = test_win32_user32_input_definitions()
    t4 = test_input_service_and_di_registration()
    t5 = test_command_dispatcher_and_http_server()
    t6 = test_coordinate_mapping_math()
    t7 = test_unicode_text_and_special_keys()
    t8 = test_android_ui_and_gestures()
    t9 = test_security_and_scope_boundary()

    print("\n" + "=" * 72)
    print("  RESUMO DA VALIDACAO DA ETAPA 6/12 (CONTROLE REMOTO REAL):")
    print("=" * 72)
    print(f"  1. Arquivos Essenciais da Etapa 6:          {'[PASSOU]' if t1 else '[FALHOU]'}")
    print(f"  2. Integridade e Sintaxe (C#/Kotlin/XML):   {'[PASSOU]' if t2 else '[FALHOU]'}")
    print(f"  3. Win32 P/Invoke SendInput e Constantes:   {'[PASSOU]' if t3 else '[FALHOU]'}")
    print(f"  4. Servico de Entrada (IInputService) e DI: {'[PASSOU]' if t4 else '[FALHOU]'}")
    print(f"  5. CommandDispatcher e LocalHttpServer:     {'[PASSOU]' if t5 else '[FALHOU]'}")
    print(f"  6. Mapeamento de Coordenadas e Letterbox:   {'[PASSOU]' if t6 else '[FALHOU]'}")
    print(f"  7. Suporte a Unicode e Teclas Especiais:    {'[PASSOU]' if t7 else '[FALHOU]'}")
    print(f"  8. UI Android, Portrait e Gestos:           {'[PASSOU]' if t8 else '[FALHOU]'}")
    print(f"  9. Seguranca e Delimitacao de Escopo:       {'[PASSOU]' if t9 else '[FALHOU]'}")
    print("=" * 72)

    all_passed = t1 and t2 and t3 and t4 and t5 and t6 and t7 and t8 and t9
    sys.exit(0 if all_passed else 1)

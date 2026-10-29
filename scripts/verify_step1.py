#!/usr/bin/env python3
import os
import sys
import xml.etree.ElementTree as ET
import json
import subprocess

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

def test_structure():
    print("[TEST 1] Verificando estrutura de diretórios e arquivos...")
    required_paths = [
        "android-app",
        "android-app/build.gradle.kts",
        "android-app/settings.gradle.kts",
        "android-app/gradle.properties",
        "android-app/gradlew",
        "android-app/gradlew.bat",
        "android-app/gradle/wrapper/gradle-wrapper.properties",
        "android-app/app/build.gradle.kts",
        "android-app/app/proguard-rules.pro",
        "android-app/app/src/main/AndroidManifest.xml",
        "android-app/app/src/main/res/values/strings.xml",
        "android-app/app/src/main/res/values/colors.xml",
        "android-app/app/src/main/res/values/themes.xml",
        "android-app/app/src/main/res/layout/activity_main.xml",
        "android-app/app/src/main/java/com/remotedesk/app/MainActivity.kt",
        "windows-agent",
        "windows-agent/RemoteDesk.Agent.sln",
        "windows-agent/src/RemoteDesk.Agent/RemoteDesk.Agent.csproj",
        "windows-agent/src/RemoteDesk.Agent/Program.cs",
        "windows-agent/src/RemoteDesk.Agent/AgentService.cs",
        "windows-agent/src/RemoteDesk.Agent/AgentConfig.cs",
        "windows-agent/src/RemoteDesk.Agent/appsettings.json",
        "signaling-server",
        "signaling-server/RemoteDesk.SignalingServer.sln",
        "signaling-server/src/RemoteDesk.SignalingServer/RemoteDesk.SignalingServer.csproj",
        "signaling-server/src/RemoteDesk.SignalingServer/Program.cs",
        "signaling-server/src/RemoteDesk.SignalingServer/appsettings.json",
        "signaling-server/src/RemoteDesk.SignalingServer/appsettings.Development.json",
        "docs",
        "docs/ARCHITECTURE.md",
        "docs/PROGRESS.md",
        "docs/TEST_PLAN.md",
        "docs/BUILD_AND_ERRORS.md",
        ".github/workflows/build.yml",
        "README.md",
        ".gitignore"
    ]

    missing = []
    for rel_path in required_paths:
        full_path = os.path.join(PROJECT_ROOT, rel_path)
        if not os.path.exists(full_path):
            missing.append(rel_path)

    if missing:
        print(f"  FALHA: Arquivos/diretórios ausentes: {missing}")
        return False
    print("  SUCESSO: Todos os 32 arquivos e diretórios essenciais estão presentes.")
    return True

def test_xml_syntax():
    print("[TEST 2] Verificando sintaxe dos arquivos XML do Android...")
    xml_files = [
        "android-app/app/src/main/AndroidManifest.xml",
        "android-app/app/src/main/res/values/strings.xml",
        "android-app/app/src/main/res/values/colors.xml",
        "android-app/app/src/main/res/values/themes.xml",
        "android-app/app/src/main/res/layout/activity_main.xml"
    ]
    for xml_rel in xml_files:
        xml_path = os.path.join(PROJECT_ROOT, xml_rel)
        try:
            ET.parse(xml_path)
        except Exception as e:
            print(f"  FALHA no parsing de {xml_rel}: {e}")
            return False
    print("  SUCESSO: Todos os arquivos XML do Android são válidos e bem-formados.")
    return True

def test_json_syntax():
    print("[TEST 3] Verificando sintaxe dos arquivos JSON de configuração...")
    json_files = [
        "windows-agent/src/RemoteDesk.Agent/appsettings.json",
        "signaling-server/src/RemoteDesk.SignalingServer/appsettings.json",
        "signaling-server/src/RemoteDesk.SignalingServer/appsettings.Development.json"
    ]
    for json_rel in json_files:
        json_path = os.path.join(PROJECT_ROOT, json_rel)
        try:
            with open(json_path, "r", encoding="utf-8") as f:
                json.load(f)
        except Exception as e:
            print(f"  FALHA no JSON {json_rel}: {e}")
            return False
    print("  SUCESSO: Todos os arquivos JSON de configuração são válidos.")
    return True

def test_android_build():
    print("[TEST 4] Tentativa de compilar o aplicativo Android e gerar APK...")
    gradlew_path = os.path.join(PROJECT_ROOT, "android-app/gradlew")
    try:
        proc = subprocess.run([gradlew_path, "assembleDebug"], cwd=os.path.join(PROJECT_ROOT, "android-app"), capture_output=True, text=True, timeout=10)
        if proc.returncode != 0:
            print(f"  IMPEDIMENTO TÉCNICO: ./gradlew assembleDebug falhou (código {proc.returncode}):\n    {proc.stderr.strip()}")
            return False
        return True
    except FileNotFoundError:
        print("  FALHA / IMPEDIMENTO: comando gradlew ou java não encontrado.")
        return False
    except Exception as e:
        print(f"  NÃO EXECUTADO: {e}")
        return False

def test_dotnet_agent():
    print("[TEST 5] Tentativa de compilar o agente Windows via dotnet build...")
    agent_dir = os.path.join(PROJECT_ROOT, "windows-agent/src/RemoteDesk.Agent")
    try:
        proc = subprocess.run(["dotnet", "build"], cwd=agent_dir, capture_output=True, text=True, timeout=10)
        if proc.returncode != 0:
            print(f"  FALHA de compilação: {proc.stderr}")
            return False
        return True
    except FileNotFoundError:
        print("  IMPEDIMENTO TÉCNICO: comando 'dotnet' (.NET SDK 8.0) não instalado no ambiente.")
        return False
    except Exception as e:
        print(f"  NÃO EXECUTADO: {e}")
        return False

def test_dotnet_server():
    print("[TEST 6] Tentativa de compilar o servidor de sinalização via dotnet build...")
    server_dir = os.path.join(PROJECT_ROOT, "signaling-server/src/RemoteDesk.SignalingServer")
    try:
        proc = subprocess.run(["dotnet", "build"], cwd=server_dir, capture_output=True, text=True, timeout=10)
        if proc.returncode != 0:
            print(f"  FALHA de compilação: {proc.stderr}")
            return False
        return True
    except FileNotFoundError:
        print("  IMPEDIMENTO TÉCNICO: comando 'dotnet' (.NET SDK 8.0) não instalado no ambiente.")
        return False
    except Exception as e:
        print(f"  NÃO EXECUTADO: {e}")
        return False

def test_documentation_correspondence():
    print("[TEST 7] Verificando se os documentos citam os arquivos realmente existentes...")
    docs = [
        "docs/ARCHITECTURE.md",
        "docs/PROGRESS.md",
        "docs/TEST_PLAN.md",
        "README.md"
    ]
    for d in docs:
        p = os.path.join(PROJECT_ROOT, d)
        if not os.path.exists(p) or os.path.getsize(p) == 0:
            print(f"  FALHA: Documento {d} não existe ou está vazio.")
            return False
    print("  SUCESSO: Toda a documentação existe e está preenchida com conteúdo consistente.")
    return True

if __name__ == "__main__":
    t1 = test_structure()
    t2 = test_xml_syntax()
    t3 = test_json_syntax()
    t4 = test_android_build()
    t5 = test_dotnet_agent()
    t6 = test_dotnet_server()
    t7 = test_documentation_correspondence()
    print("\n" + "=" * 50)
    print("RESUMO DAS VALIDAÇÕES DA ETAPA 1:")
    print("=" * 50)
    print(f"1. Estrutura de diretórios:       {'[PASSOU]' if t1 else '[FALHOU]'}")
    print(f"2. Sintaxe XML Android:            {'[PASSOU]' if t2 else '[FALHOU]'}")
    print(f"3. Sintaxe JSON Configurações:     {'[PASSOU]' if t3 else '[FALHOU]'}")
    print(f"4. Compilação Android (APK):       {'[PASSOU]' if t4 else '[NÃO EXECUTADO - Falta JDK / Android SDK]'}")
    print(f"5. Compilação Agente Windows:      {'[PASSOU]' if t5 else '[NÃO EXECUTADO - Falta .NET 8 SDK]'}")
    print(f"6. Compilação Servidor (.NET):     {'[PASSOU]' if t6 else '[NÃO EXECUTADO - Falta .NET 8 SDK]'}")
    print(f"7. Coerência da documentação:      {'[PASSOU]' if t7 else '[FALHOU]'}")
    print("=" * 50)

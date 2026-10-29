#!/usr/bin/env python3
"""
RemoteDesk - Utilitário de Compilação e Registro de Logs de Erros
Gera logs estruturados para diagnóstico de compilação do Android, Windows Agent e Servidor.
Compatível com Linux e Windows (incluindo terminais legados com codificação CP1252/OEM).
"""

import os
import sys
import re
import datetime
import subprocess

# Configuração de encoding robusto para evitar falhas em terminais Windows (CP1252/OEM/ASCII)
if hasattr(sys.stdout, "reconfigure"):
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass
if hasattr(sys.stderr, "reconfigure"):
    try:
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

def safe_print(*args, **kwargs):
    """
    Imprime no console garantindo compatibilidade total com qualquer codificação (CP1252/ASCII/UTF-8).
    Nunca lança UnicodeEncodeError.
    """
    sep = kwargs.get("sep", " ")
    end = kwargs.get("end", "\n")
    text = sep.join(str(a) for a in args)
    try:
        print(text, end=end)
    except UnicodeEncodeError:
        encoding = getattr(sys.stdout, "encoding", "ascii") or "ascii"
        safe_text = text.encode(encoding, errors="replace").decode(encoding)
        sys.stdout.write(safe_text + end)
        sys.stdout.flush()

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
LOGS_DIR = os.path.join(PROJECT_ROOT, "logs")
SIGNALING_BUILD_DIR = os.path.join(PROJECT_ROOT, ".build", "signaling", "bin", "Debug", "net8.0")
SIGNALING_DLL = os.path.join(SIGNALING_BUILD_DIR, "RemoteDesk.SignalingServer.dll")
SIGNALING_HEALTH_URL = "http://127.0.0.1:5000/health"
WINDOWS_AGENT_BUILD_DIR = os.path.join(
    PROJECT_ROOT,
    "windows-agent",
    "src",
    "RemoteDesk.Agent",
    "bin",
    "Debug",
    "net8.0"
)
WINDOWS_AGENT_DLL = os.path.join(WINDOWS_AGENT_BUILD_DIR, "RemoteDesk.Agent.dll")
GRADLE_WRAPPER = os.path.join(
    PROJECT_ROOT,
    "android-app",
    "gradlew.bat" if os.name == "nt" else "gradlew"
)

COMPONENTS = {
    "android": {
        "name": "Aplicativo Android",
        "log_file": "android_build.log",
        "cwd": os.path.join(PROJECT_ROOT, "android-app"),
        "command": [GRADLE_WRAPPER, ":app:assembleDebug"],
        "artifact_expected": os.path.join(PROJECT_ROOT, "android-app", "app", "build", "outputs", "apk", "debug", "app-debug.apk"),
        "error_patterns": [
            r"e:\s*(?:file://)?(?P<file>[a-zA-Z0-9_\-\./\\]+\.(?:kt|java)):(?:\s*\((?P<line>\d+),\s*(?P<col>\d+)\)|(?P<line2>\d+):(?P<col2>\d+)?):\s*(?P<msg>.*)",
            r"(?P<file>[a-zA-Z0-9_\-\./\\]+\.(?:kt|java|xml|gradle|kts)):(?P<line>\d+):(?P<col>\d+)?(?:\s*-\s*|\s*)(?P<msg>.*)",
            r"(?P<file>[a-zA-Z0-9_\-\./\\]+\.(?:kt|java|xml|gradle|kts)):(?P<line>\d+):\s*(?:AAPT:)?\s*(?:error:)?\s*(?P<msg>.*)",
            r"(?P<msg>AAPT:\s*error:.*)",
            r"(?P<msg>>\s*(?:Android resource linking failed|Failed to apply plugin|Execution failed for task).*)",
            r"(?P<msg>ERROR:.*)",
            r"(?P<msg>FAILURE:.*)"
        ],
        "solution_guide": (
            "1. Problema: O Gradle Wrapper requer o Java Development Kit (JDK 17+) e o Android SDK para compilar o APK.\n"
            "   A variável JAVA_HOME não foi definida ou o comando 'java' não está no PATH.\n"
            "2. Como resolver no Windows:\n"
            "   a) Instalar o OpenJDK 17 via Prompt de Comando / PowerShell (Administrador):\n"
            "      winget install EclipseAdoptium.Temurin.17.JDK\n"
            "   b) Definir a variável de ambiente JAVA_HOME:\n"
            "      setx JAVA_HOME \"C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.x-hotspot\" /M\n"
            "      e adicionar '%JAVA_HOME%\\bin' à variável de ambiente PATH.\n"
            "   c) Instalar o Android Studio (ou Android Command-line Tools):\n"
            "      winget install Google.AndroidStudio\n"
            "      Abrir o Android Studio para instalar o Android SDK (API 34) e Build-Tools (34.0.0).\n"
            "   d) Definir a variável ANDROID_HOME:\n"
            "      setx ANDROID_HOME \"%LOCALAPPDATA%\\Android\\Sdk\" /M\n"
            "      e adicionar '%ANDROID_HOME%\\platform-tools' ao PATH.\n"
            "   e) Abrir um novo terminal e executar novamente: scripts\\build_all.bat\n"
        )
    },
    "windows": {
        "name": "Agente Windows",
        "log_file": "windows_agent_build.log",
        "cwd": os.path.join(PROJECT_ROOT, "windows-agent", "src", "RemoteDesk.Agent"),
        "proj_file": os.path.join(PROJECT_ROOT, "windows-agent", "src", "RemoteDesk.Agent", "RemoteDesk.Agent.csproj"),
        "command": [
            "dotnet",
            "build",
            os.path.join(PROJECT_ROOT, "windows-agent", "src", "RemoteDesk.Agent", "RemoteDesk.Agent.csproj"),
            "--verbosity",
            "normal",
            "--no-incremental",
            "/nodeReuse:false",
            "/m:1",
            "-p:UseSharedCompilation=false",
            "-p:DisableRarCache=true"
        ],
        "artifact_expected": WINDOWS_AGENT_DLL,
        "error_patterns": [
            r"(?P<file>[a-zA-Z0-9_\-\./\\]+\.(?:cs|csproj)):(?:\((?P<line>\d+),(?P<col>\d+)\))?:\s*(?P<msg>error\s+[A-Z0-9]+:.*)",
            r"(?P<file>[a-zA-Z0-9_\-\./\\]+\.(?:cs|csproj))\((?P<line>\d+),(?P<col>\d+)\):\s*(?P<msg>.*error.*)",
            r"(?P<msg>MSB\d+:.*)"
        ],
        "solution_guide": (
            "1. Problema: O comando 'dotnet' (.NET 8.0 SDK) não foi encontrado no PATH do sistema ou o caminho do projeto está incorreto.\n"
            "2. Localização correta do projeto:\n"
            "   windows-agent\\src\\RemoteDesk.Agent\\RemoteDesk.Agent.csproj\n"
            "3. Como compilar e executar no Windows:\n"
            "   a) A partir da raiz do repositório (RemoteDesk):\n"
            "      dotnet build windows-agent\\src\\RemoteDesk.Agent\\RemoteDesk.Agent.csproj\n"
            "      dotnet run --project windows-agent\\src\\RemoteDesk.Agent\\RemoteDesk.Agent.csproj\n"
            "   b) Ou navegando até a pasta correta do agente:\n"
            "      cd windows-agent\\src\\RemoteDesk.Agent\n"
            "      dotnet build .\\RemoteDesk.Agent.csproj\n"
            "      dotnet run --project .\\RemoteDesk.Agent.csproj\n"
            "   c) ATENÇÃO: NÃO execute o Agente a partir da pasta signaling-server\\src\\RemoteDesk.SignalingServer.\n"
            "   d) Se o SDK não estiver instalado, instale via terminal (Administrador):\n"
            "      winget install Microsoft.DotNet.SDK.8\n"
        )
    },
    "server": {
        "name": "Servidor de Sinalização",
        "log_file": "signaling_server_build.log",
        "cwd": os.path.join(PROJECT_ROOT, "signaling-server", "src", "RemoteDesk.SignalingServer"),
        "proj_file": os.path.join(PROJECT_ROOT, "signaling-server", "src", "RemoteDesk.SignalingServer", "RemoteDesk.SignalingServer.csproj"),
        "command": ["dotnet", "build", os.path.join(PROJECT_ROOT, "signaling-server", "src", "RemoteDesk.SignalingServer", "RemoteDesk.SignalingServer.csproj"), "--verbosity", "normal"],
        "artifact_expected": SIGNALING_DLL,
        "error_patterns": [
            r"(?P<file>[a-zA-Z0-9_\-\./\\]+\.(?:cs|csproj)):(?:\((?P<line>\d+),(?P<col>\d+)\))?:\s*(?P<msg>error\s+[A-Z0-9]+:.*)",
            r"(?P<file>[a-zA-Z0-9_\-\./\\]+\.(?:cs|csproj))\((?P<line>\d+),(?P<col>\d+)\):\s*(?P<msg>.*error.*)",
            r"(?P<msg>MSB\d+:.*)"
        ],
        "solution_guide": (
            "1. Problema: O compilador .NET 8.0 SDK não foi encontrado no PATH do sistema ou o caminho do projeto está incorreto.\n"
            "2. Localização correta do projeto:\n"
            "   signaling-server\\src\\RemoteDesk.SignalingServer\\RemoteDesk.SignalingServer.csproj\n"
            "3. Como compilar e executar no Windows:\n"
            "   a) A partir da raiz do repositório (RemoteDesk):\n"
            "      dotnet build signaling-server\\src\\RemoteDesk.SignalingServer\\RemoteDesk.SignalingServer.csproj\n"
            "      dotnet run --project signaling-server\\src\\RemoteDesk.SignalingServer\\RemoteDesk.SignalingServer.csproj\n"
            "   b) Ou navegando até a pasta correta do servidor:\n"
            "      cd signaling-server\\src\\RemoteDesk.SignalingServer\n"
            "      dotnet build .\\RemoteDesk.SignalingServer.csproj\n"
            "      dotnet run --project .\\RemoteDesk.SignalingServer.csproj\n"
            "   c) Testar o endpoint /health:\n"
            "      curl http://127.0.0.1:5000/health\n"
            "   d) Se o SDK não estiver instalado, instale via terminal (Administrador):\n"
            "      winget install Microsoft.DotNet.SDK.8\n"
        )
    }
}

SENSITIVE_PATTERNS = [
    r"(?i)(password|passwd|pwd|secret|token|pin|apiKey|auth)[\s:=]+([^\s,;]+)",
]

def sanitize_output(text: str) -> str:
    """Oculta eventuais credenciais e tokens nos logs."""
    sanitized = text
    for pattern in SENSITIVE_PATTERNS:
        sanitized = re.sub(pattern, r"\1: [REDACTED]", sanitized)
    return sanitized

def extract_errors(output: str, error_patterns: list) -> list:
    """Extrai arquivos, linhas e descrições dos erros a partir da saída do compilador."""
    extracted = []
    lines = output.splitlines()

    # Captura a causa raiz detalhada sob '* What went wrong:' do Gradle
    what_went_wrong = []
    capture_www = False
    for line in lines:
        if "* What went wrong:" in line:
            capture_www = True
            continue
        elif capture_www:
            if line.startswith("*") or line.startswith("BUILD FAILED"):
                break
            if line.strip():
                clean_msg = line.strip().lstrip(">").strip()
                what_went_wrong.append(clean_msg)

    if what_went_wrong:
        main_cause = " | ".join(what_went_wrong[:3])
        extracted.append({"file": "Gradle / Tarefa", "line": "N/A", "message": main_cause})

    for line in lines:
        for pat in error_patterns:
            m = re.search(pat, line)
            if m:
                d = m.groupdict()
                file_name = d.get("file", "Desconhecido / Ambiente")
                line_no = d.get("line") or d.get("line2") or "N/A"
                msg = d.get("msg", line.strip())
                if "FAILURE: Build failed with an exception" in msg and what_went_wrong:
                    continue
                extracted.append({"file": file_name, "line": line_no, "message": msg.strip()})
                break
    return extracted

def test_initialization(component_key: str, build_success: bool) -> tuple:
    """Testa a inicialização do componente se a compilação tiver sido bem-sucedida."""
    if not build_success:
        return ("Não testada", "Bloqueada por falha prévia na compilação.")

    info = COMPONENTS[component_key]

    if component_key == "android":
        apk_path = info["artifact_expected"]
        if not os.path.exists(apk_path):
            return ("Pendente", f"APK não encontrado em {apk_path}.")
        try:
            adb_proc = subprocess.run(["adb", "devices"], capture_output=True, text=True, timeout=5)
            lines = [l for l in adb_proc.stdout.splitlines() if l.strip() and not l.startswith("List")]
            if lines:
                return ("Pronto para instalação", f"Dispositivo detectado: {lines[0]}. Execute 'adb install {apk_path}'.")
            return ("Não testada", "APK gerado, mas nenhum dispositivo Android ou emulador conectado via adb.")
        except Exception:
            return ("Não testada", "APK gerado, mas comando 'adb' não disponível para instalação automatizada.")

    elif component_key == "windows":
        try:
            if not os.path.isfile(WINDOWS_AGENT_DLL):
                return ("Pendente", f"Artefato do Windows Agent não encontrado em: {WINDOWS_AGENT_DLL}")

            cmd = ["dotnet", WINDOWS_AGENT_DLL, "--verify"]
            run_proc = subprocess.run(
                cmd,
                cwd=WINDOWS_AGENT_BUILD_DIR,
                capture_output=True,
                text=True,
                timeout=45
            )
            if run_proc.returncode == 0:
                return ("Aprovada", "Agente iniciou e encerrou com código 0 no teste de ciclo de vida (--verify).")
            return ("Pendente", f"Falha na inicialização do agente (código {run_proc.returncode}): {run_proc.stderr[:150]}")
        except Exception as ex:
            return ("Pendente", f"Erro ao tentar iniciar agente: {str(ex)}")

    elif component_key == "server":
        import time
        import urllib.request
        server_proc = None
        try:
            if not os.path.isfile(SIGNALING_DLL):
                return ("Pendente", f"Artefato do Servidor não encontrado em: {SIGNALING_DLL}")

            cmd = ["dotnet", SIGNALING_DLL, "--urls", "http://127.0.0.1:5000"]
            server_proc = subprocess.Popen(
                cmd,
                cwd=SIGNALING_BUILD_DIR,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True
            )

            deadline = time.monotonic() + 20
            last_error = "servidor ainda não respondeu"
            while time.monotonic() < deadline:
                if server_proc.poll() is not None:
                    stdout, stderr = server_proc.communicate()
                    details = (stderr or stdout or "sem detalhes").strip()
                    return ("Pendente", f"Servidor encerrou durante a inicialização (código {server_proc.returncode}): {details[:300]}")

                try:
                    req = urllib.request.Request(SIGNALING_HEALTH_URL)
                    with urllib.request.urlopen(req, timeout=2) as response:
                        body = response.read().decode("utf-8")
                        if response.status == 200 and "Healthy" in body:
                            return ("Aprovada", f"Servidor respondeu com HTTP 200 ao endpoint /health: {body[:100]}")
                        last_error = f"HTTP {response.status}: {body[:100]}"
                except Exception as ex:
                    last_error = str(ex)

                time.sleep(0.5)

            return ("Pendente", f"Timeout no health check {SIGNALING_HEALTH_URL}: {last_error}")
        except Exception as ex:
            return ("Pendente", f"Falha na checagem do endpoint /health: {str(ex)}")
        finally:
            if server_proc:
                server_proc.terminate()
                try:
                    server_proc.wait(timeout=3)
                except Exception:
                    server_proc.kill()

    return ("Não testada", "Nenhum teste de inicialização aplicável.")

def run_build(component_key: str) -> dict:
    info = COMPONENTS[component_key]
    os.makedirs(LOGS_DIR, exist_ok=True)
    log_path = os.path.join(LOGS_DIR, info["log_file"])
    
    timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    cmd_str = " ".join(info["command"])
    
    stdout_data = ""
    stderr_data = ""
    return_code = -1
    environment_error = None
    build_environment = os.environ.copy()
    build_environment["DOTNET_CLI_DO_NOT_USE_MSBUILD_SERVER"] = "1"

    proj_file = info.get("proj_file")
    if proj_file and not os.path.isfile(proj_file):
        environment_error = (
            f"Arquivo de projeto não encontrado em: '{proj_file}'.\n"
            f"Certifique-se de que o arquivo .csproj existe e o caminho está correto."
        )
        return_code = 1
    else:
        try:
            proc = subprocess.run(
                info["command"],
                cwd=info["cwd"],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                timeout=180,
                env=build_environment
            )
            return_code = proc.returncode
            stdout_data = proc.stdout
            stderr_data = proc.stderr
        except FileNotFoundError as fnf:
            missing_tool = fnf.filename or info['command'][0]
            environment_error = (
                f"Ferramenta executável não encontrada no sistema: '{missing_tool}'.\n"
                f"O SDK/compilador necessário para este componente não está instalado ou não foi adicionado ao PATH do ambiente."
            )
            return_code = 127
        except Exception as ex:
            environment_error = f"Exceção durante execução do processo: {str(ex)}"
            return_code = 1

    combined_output = sanitize_output((stdout_data + "\n" + stderr_data).strip())
    if environment_error:
        combined_output = (environment_error + "\n\n" + combined_output).strip()

    artifact_exists = os.path.exists(info["artifact_expected"])
    build_success = (return_code == 0 and artifact_exists)
    status_str = "SUCESSO" if build_success else "FALHA / IMPEDIMENTO"
    
    diagnostics = extract_errors(combined_output, info["error_patterns"])
    if not artifact_exists:
        diagnostics.append({
            "file": info["artifact_expected"],
            "line": "N/A",
            "message": "Artefato esperado não foi gerado."
        })
    artifact_status = f"Presente ({info['artifact_expected']})" if artifact_exists else f"Não gerado (Esperado em: {info['artifact_expected']})"
    init_status, init_detail = test_initialization(component_key, build_success)

    # Montagem do relatório do log em formato UTF-8 estruturado
    log_content = [
        "=" * 80,
        f"REMOTEDESK BUILD LOG - {info['name'].upper()}",
        "=" * 80,
        f"1. Data e Horário:       {timestamp}",
        f"2. Componente:           {info['name']} ({component_key})",
        f"   Diretório de Trabalho:{info['cwd']}",
        f"   Comando Utilizado:    {cmd_str}",
        f"3. Resultado e Código:   {status_str} (Código de Saída: {return_code})",
        f"   Artefato Produzido:   {artifact_status}",
        f"   Teste Inicialização:  {init_status} - {init_detail}",
        "=" * 80,
        "4. DIAGNÓSTICO E LOCALIZAÇÃO DE ERROS (ARQUIVO E LINHA):",
        "=" * 80,
    ]

    if build_success:
        log_content.append("  Nenhum erro de compilação detectado. Projeto compilado com sucesso.")
    else:
        if diagnostics:
            for idx, diag in enumerate(diagnostics[:10], 1):
                log_content.append(f"  [{idx}] Arquivo: {diag['file']} | Linha: {diag['line']}")
                log_content.append(f"      Mensagem: {diag['message']}")
        else:
            log_content.append("  Não foi possível identificar arquivo/linha de código-fonte específico.")
            log_content.append("  O erro decorre da ausência de compilador ou ambiente de execução:")
            for line in combined_output.splitlines()[:5]:
                log_content.append(f"    {line}")

    log_content.extend([
        "=" * 80,
        "5. EXPLICAÇÃO SIMPLES DO PROBLEMA E POSSÍVEIS SOLUÇÕES:",
        "=" * 80,
        info["solution_guide"].strip(),
        "=" * 80,
        "6. MENSAGEM COMPLETA E SAÍDA INTEGRAL DO PROCESSO:",
        "=" * 80,
        combined_output if combined_output else "(Nenhuma saída gerada)",
        "=" * 80,
        f"Fim do log. Arquivo gerado em: {log_path}",
        "=" * 80,
        ""
    ])

    with open(log_path, "w", encoding="utf-8") as f:
        f.write("\n".join(log_content))

    return {
        "key": component_key,
        "name": info["name"],
        "build_status": "Aprovada" if build_success else "Pendente",
        "init_status": init_status,
        "init_detail": init_detail,
        "return_code": return_code,
        "log_path": log_path,
        "artifact_exists": artifact_exists,
        "artifact_path": info["artifact_expected"],
        "diagnostics": diagnostics,
        "summary_err": environment_error or (diagnostics[0]["message"] if diagnostics else f"Código {return_code}")
    }

def main():
    raw_args = sys.argv[1:]
    strict_mode = "--strict" in raw_args
    args = [a for a in raw_args if a != "--strict"]
    
    target = args[0] if args else "--all"
    
    if target in ("--help", "-h"):
        safe_print("Uso: python3 scripts/build_runner.py [--all | android | windows | server] [--strict]")
        sys.exit(0)

    selected = []
    if target in ("--all", "all"):
        selected = ["android", "windows", "server"]
    elif target in COMPONENTS:
        selected = [target]
    elif target.startswith("--"):
        cleaned = target.lstrip("-")
        if cleaned in COMPONENTS:
            selected = [cleaned]
        else:
            safe_print(f"[ERRO] Componente desconhecido: {target}")
            sys.exit(1)
    else:
        safe_print(f"[ERRO] Componente desconhecido: {target}")
        sys.exit(1)

    safe_print("=" * 75)
    safe_print("  REMOTEDESK - EXECUCAO DE COMPILACOES REAIS E REGISTRO DE LOGS")
    safe_print("=" * 75)

    results = []
    for comp in selected:
        safe_print(f"[*] Executando compilação do componente: {COMPONENTS[comp]['name']}...")
        res = run_build(comp)
        results.append(res)
        safe_print(f"    Compilação:    {res['build_status']} (Código: {res['return_code']})")
        safe_print(f"    Inicialização: {res['init_status']}")
        safe_print(f"    Log:           {res['log_path']}")
        if res['return_code'] != 0:
            safe_print(f"    Erro Principal: {res['summary_err'][:110]}...")
        safe_print()

    # Criação do sumário consolidado
    summary_path = os.path.join(LOGS_DIR, "latest_build_summary.log")
    with open(summary_path, "w", encoding="utf-8") as sf:
        sf.write("=" * 75 + "\n")
        sf.write(f"REMOTEDESK - SUMÁRIO GERAL DE COMPILAÇÕES E LOGS ({datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')})\n")
        sf.write("=" * 75 + "\n\n")
        
        # Tabela de status
        sf.write("| Componente          | Compilação | Inicialização | Arquivo de Log               |\n")
        sf.write("|---------------------|------------|---------------|------------------------------|\n")
        for r in results:
            sf.write(f"| {r['name']:<19} | {r['build_status']:<10} | {r['init_status']:<13} | logs/{os.path.basename(r['log_path']):<24} |\n")
        
        sf.write("\n" + "=" * 75 + "\n")
        sf.write("DETALHAMENTO DE CADA COMPONENTE:\n")
        sf.write("=" * 75 + "\n\n")
        for r in results:
            sf.write(f"* Componente:          {r['name']}\n")
            sf.write(f"  Status Compilação:   {r['build_status']} (Código {r['return_code']})\n")
            sf.write(f"  Status Inicialização:{r['init_status']} ({r['init_detail']})\n")
            sf.write(f"  Artefato Esperado:   {r['artifact_path']}\n")
            sf.write(f"  Artefato Produzido:  {'SIM' if r['artifact_exists'] else 'NÃO'}\n")
            sf.write(f"  Arquivo de Log:      {r['log_path']}\n")
            sf.write(f"  Diagnóstico/Causa:   {r['summary_err']}\n\n")

    safe_print("=" * 75)
    safe_print(f"[OK] Sumário geral salvo em: {summary_path}")
    safe_print("=" * 75)

    if strict_mode and any(
        r["return_code"] != 0
        or not r["artifact_exists"]
        or (r["key"] in ("windows", "server") and r["init_status"] != "Aprovada")
        for r in results
    ):
        sys.exit(1)

if __name__ == "__main__":
    main()

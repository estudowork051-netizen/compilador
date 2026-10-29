# RemoteDesk — Acesso Remoto Android para Windows

O **RemoteDesk** é uma solução de acesso remoto pessoal que permite controlar computadores Windows a partir de dispositivos móveis Android via internet em tempo real, utilizando WebRTC para transmissão de baixa latência e canais de dados dedicados a controle de entrada (mouse, teclado e atalhos).

---

## 1. Estrutura do Repositório

O projeto está organizado nos seguintes diretórios principais:

* `android-app/`: Aplicativo móvel nativo para Android desenvolvido em Kotlin e Gradle.
* `windows-agent/`: Agente hospedeiro executado no Windows desenvolvido em C# (.NET 8.0).
* `signaling-server/`: Backend ASP.NET Core (.NET 8.0) para sinalização, registro e descoberta.
* `docs/`: Documentação detalhada do projeto:
  * `docs/ARCHITECTURE.md`: Arquitetura do sistema, protocolos, responsabilidades e segurança.
  * `docs/PROGRESS.md`: Registro de atividades executadas, arquivos criados e pendências.
  * `docs/TEST_PLAN.md`: Plano de testes para as 12 etapas do projeto.
  * `docs/BUILD_AND_ERRORS.md`: Guia de compilação, localização de erros e consulta aos arquivos de log.
* `logs/`: Diretório centralizado de logs de compilação gerados pelos scripts.
* `scripts/`: Utilitários de verificação e compilação unificada (`build_all.sh`, `build_all.bat`, `build_runner.py`).

---

## 2. Pré-requisitos do Ambiente

Para compilar e executar todos os componentes, o ambiente de desenvolvimento precisa dispor de:

1. **Aplicativo Android**:
   * JDK 17 (Java Development Kit) instalado e variável de ambiente `JAVA_HOME` configurada.
   * Android SDK com API Level 34 e Build Tools instalados (variável `ANDROID_HOME` configurada).
   * Android Studio (recomendado Hedgehog / Iguana / Jellyfish) ou ferramentas de linha de comando Gradle.

2. **Agente Windows e Servidor de Sinalização**:
   * .NET 8.0 SDK (versão 8.0.100 ou superior).
   * Sistema operacional Windows 10/11 (para execução do agente com integração à interface de desktop) ou Linux/macOS (para compilação e execução do servidor de sinalização).
   * Visual Studio 2022 ou VS Code com extensão C# Dev Kit (opcional).

---

## 3. Instruções de Compilação e Execução

### 3.0. Estrutura dos Projetos .NET 8 e Localização Real dos Arquivos `.csproj`

O RemoteDesk contém dois projetos .NET 8 com localizações bem definidas no repositório:

| Componente | Arquivo de Projeto (.csproj) | Diretório de Trabalho Real |
| :--- | :--- | :--- |
| **Agente Windows** | `windows-agent/src/RemoteDesk.Agent/RemoteDesk.Agent.csproj` | `windows-agent/src/RemoteDesk.Agent/` |
| **Servidor de Sinalização** | `signaling-server/src/RemoteDesk.SignalingServer/RemoteDesk.SignalingServer.csproj` | `signaling-server/src/RemoteDesk.SignalingServer/` |

> ⚠️ **ATENÇÃO AO DIRETÓRIO ATUAL DO TERMINAL (Erro MSB1009):**  
> Se o seu terminal estiver dentro da pasta do Servidor de Sinalização (`RemoteDesk\signaling-server\src\RemoteDesk.SignalingServer`), o comando `dotnet run --project .\RemoteDesk.Agent.csproj` **falhará com o erro `MSB1009: Arquivo de projeto não existe`**, pois o projeto do Agente está localizado em `windows-agent\src\RemoteDesk.Agent\`.  
> Para evitar esse problema, utilize os scripts convenientes da raiz ou forneça o caminho a partir da raiz do repositório conforme detalhado abaixo.

---

### 3.1. Execução Simplificada em 1 Clique (Scripts da Raiz)

Na raiz do repositório (`RemoteDesk/`), scripts prontos garantem a resolução automática de caminhos absolutos:

* **Iniciar Todo o Ambiente (Servidor + Túnel ngrok + Agente Windows):**
  ```cmd
  start-all.bat
  ```
* **Iniciar Apenas o Agente Windows:**
  ```cmd
  start-agent.bat
  ```
* **Iniciar Apenas o Servidor de Sinalização:**
  ```cmd
  start-signaling.bat
  ```

---

### 3.2. Execução Manual a partir da Raiz do Repositório (`RemoteDesk/`)

Se preferir utilizar a linha de comando `dotnet` diretamente da raiz do repositório:

* **Compilar e Iniciar o Servidor de Sinalização:**
  ```cmd
  dotnet build "signaling-server\src\RemoteDesk.SignalingServer\RemoteDesk.SignalingServer.csproj"
  dotnet run --project "signaling-server\src\RemoteDesk.SignalingServer\RemoteDesk.SignalingServer.csproj"
  ```
* **Compilar e Iniciar o Agente Windows:**
  ```cmd
  dotnet build "windows-agent\src\RemoteDesk.Agent\RemoteDesk.Agent.csproj"
  dotnet run --project "windows-agent\src\RemoteDesk.Agent\RemoteDesk.Agent.csproj"
  ```
* **Verificar o PIN do Agente a partir da Raiz:**
  ```cmd
  dotnet run --project "windows-agent\src\RemoteDesk.Agent\RemoteDesk.Agent.csproj" -- --pin
  ```

---

### 3.3. Execução Navegando até a Pasta de Cada Projeto

Caso prefira entrar na pasta específica de cada componente:

#### A. Servidor de Sinalização (`signaling-server/`)
1. Navegue até a pasta do servidor:
   ```cmd
   cd signaling-server\src\RemoteDesk.SignalingServer
   ```
2. Restaure e compile:
   ```cmd
   dotnet restore
   dotnet build
   ```
3. Inicie o servidor:
   ```cmd
   dotnet run
   ```
4. O servidor iniciará por padrão na porta `5000` (HTTP). Valide a integridade em outro terminal:
   ```cmd
   curl http://127.0.0.1:5000/health
   ```

#### B. Agente Windows (`windows-agent/`)
1. Retorne à raiz ou navegue até a pasta do agente:
   ```cmd
   cd ..\..\..\windows-agent\src\RemoteDesk.Agent
   ```
   *(Ou a partir da raiz do projeto: `cd windows-agent\src\RemoteDesk.Agent`)*
2. Restaure e compile:
   ```cmd
   dotnet restore
   dotnet build
   ```
3. Executar ações do agente:
   * **Executar o agente continuamente:**
     ```cmd
     dotnet run
     ```
   * **Exibir o PIN de pareamento atual:**
     ```cmd
     dotnet run -- --pin
     ```
   * **Listar janelas ativas da sessão:**
     ```cmd
     dotnet run -- --list-windows
     ```
   * **Autoteste de ciclo de vida (--verify):**
     ```cmd
     dotnet run -- --verify
     ```

---

### 3.3. Aplicativo Android (`android-app/`)

O aplicativo Android exibe na etapa atual a interface inicial de apresentação da fundação do RemoteDesk.

1. **Via Linha de Comando (Gradle)**:
   * No Linux/macOS:
     ```bash
     cd android-app
     ./gradlew assembleDebug
     ```
   * No Windows:
     ```cmd
     cd android-app
     gradlew.bat assembleDebug
     ```
   * O APK compilado será gerado em:
     `android-app/app/build/outputs/apk/debug/app-debug.apk`

2. **Instalação no dispositivo de teste / emulador**:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Via Android Studio**:
   * Abra o Android Studio.
   * Selecione `File` -> `Open` e aponte para o diretório `RemoteDesk/android-app`.
   * Aguarde a sincronização dos arquivos Gradle.
   * Selecione um dispositivo físico conectado via USB ou um emulador Android e clique em **Run (Shift+F10)**.

---

---

## 4. Compilação Automática na Nuvem via GitHub Actions

O repositório já está totalmente configurado para compilar todos os componentes na nuvem usando **GitHub Actions**, dispensando a instalação de JDK, Android SDK ou .NET SDK no computador local.

### 4.1. Como Enviar o Projeto para o GitHub
1. Crie um repositório vazio no GitHub (ex: `https://github.com/SEU_USUARIO/RemoteDesk.git`).
2. No seu computador, abra o terminal na pasta raiz do projeto e execute:
   ```bash
   git remote add origin https://github.com/SEU_USUARIO/RemoteDesk.git
   git push -u origin main
   ```

### 4.2. Como Executar a Compilação
* **Automática:** Ao realizar `git push` para a branch `main` ou `master`, o workflow é acionado automaticamente.
* **Manual (`workflow_dispatch`):**
  1. No GitHub, acesse a aba **Actions**.
  2. Selecione o workflow **RemoteDesk CI — Build & Artifacts**.
  3. Clique em **Run workflow**, escolha o componente desejado (`all`, `android`, `windows` ou `server`) e clique no botão verde.

### 4.3. Onde Baixar o APK e os Binários Gerados
Ao término da execução do workflow:
1. Clique na execução concluída na aba **Actions**.
2. No rodapé da página, na seção **Artifacts**, você encontrará para download:
   * **`remotedesk-android-debug-apk`**: Arquivo `app-debug.apk` pronto para instalar no celular via `adb install` ou instalador do Android.
   * **`remotedesk-windows-agent-binaries`**: Binários compilados do agente Windows (`net8.0`).
   * **`remotedesk-signaling-server-binaries`**: Binários compilados do servidor ASP.NET Core (`net8.0`).

### 4.4. Onde Consultar os Logs em Caso de Falha
Se alguma etapa falhar:
* Os logs estruturados são salvos automaticamente como artefatos (ex: `log-android-build`, `log-windows-agent-build`, `log-signaling-server-build`), disponíveis na seção **Artifacts** mesmo se a compilação falhar (`if: always()`).
* Na aba da execução, um resumo consolidado em tabela Markdown é exibido diretamente no painel **Summary**.

---

## 5. Status do Projeto

* **Etapa Atual**: 8/12 — Acesso Remoto pela Internet, Resolução do PIN, WebRTC, STUN/TURN e Transposição de CGNAT.
* **Planejamento Oficial**: `docs/ROADMAP.md` (Registrada formalmente a etapa futura de Interface Administrativa como **PLANEJADA / PENDENTE**).

---

## 6. Como Obter e Usar o PIN de Pareamento

1. **No Computador Windows**:
   - Ao iniciar o executável do agente (`RemoteDesk.Agent`), o PIN de 6 dígitos é exibido em destaque no console:
     `PIN DE ACESSO / PAREAMENTO: 492 813 <=== DIGITE ESTE PIN NO APP ANDROID`
   - Você também pode consultar o PIN a qualquer momento no terminal executando:
     ```cmd
     RemoteDesk.Agent --pin
     ```
   - Para definir um PIN personalizado de sua preferência:
     ```cmd
     RemoteDesk.Agent --set-pin SEUPIN
     ```
   - Para gerar e persistir um novo PIN numérico aleatório de 6 dígitos:
     ```cmd
     RemoteDesk.Agent --regenerate-pin
     ```

2. **No Aplicativo Android**:
   - Abra o DoHsktoP / RemoteDesk.
   - Digite o PIN exibido no computador no campo **PIN de Pareamento / Acesso** e toque em **Conectar ao Computador**.
   - O pareamento é persistido no Android Keystore com criptografia AES-GCM.
   - Nas sessões seguintes, a conexão ocorre automaticamente com 1 clique, sem necessidade de redigitar o PIN!

---

## 7. Implantação da Infraestrutura Pública (VPS e 4G/5G)

Para conectar o celular fora de casa (4G/5G) ao computador doméstico atrás de CGNAT:
Consulte o guia completo em `deployment/DEPLOYMENT.md` e as regras de firewall em `deployment/FIREWALL_RULES.md`.

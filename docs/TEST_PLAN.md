# Plano de Testes — RemoteDesk

Este documento detalha os critérios de aprovação de cada etapa e o planejamento de verificações e testes para todo o ciclo de desenvolvimento do RemoteDesk (Etapas 1 a 12).

---

## 1. Critérios de Aprovação — Etapa 1/12 (Fundação)

| Item | Critério de Aceitação | Método de Verificação | Status Atual no Ambiente |
| :--- | :--- | :--- | :--- |
| **1.1 Estrutura** | Diretórios `android-app/`, `windows-agent/`, `signaling-server/`, `docs/` organizados e coerentes. | Inspeção de arquivos e validação automatizada de integridade estrutural. | **CONCLUÍDO** |
| **1.2 Android** | Projeto Android Gradle configurado com Kotlin, SDK 34, Activity inicial com UI estática RemoteDesk. | Validação estática de manifesto, XMLs, scripts Gradle e código Kotlin. Compilação do APK de depuração com Android SDK/JDK. | **ARQUITETURA PRONTA** *(Compilação no GitHub Actions)* |
| **1.3 Windows Agent** | Projeto C# .NET 8.0 com arquitetura de agente em segundo plano, configuração e ciclo de vida seguro (Start/Stop). | Validação de sintaxe e dependências. Inicialização e encerramento com `dotnet run -- --verify`. | **ARQUITETURA PRONTA** *(Compilação no GitHub Actions)* |
| **1.4 Servidor** | Aplicação ASP.NET Core com inicialização mínima e endpoint de saúde `/health`. | Validação de sintaxe e endpoint HTTP GET `/health`. | **ARQUITETURA PRONTA** *(Compilação no GitHub Actions)* |
| **1.5 Documentação** | `ARCHITECTURE.md`, `PROGRESS.md`, `TEST_PLAN.md` e `README.md` completos e fiéis aos arquivos reais. | Revisão cruzada de referências de arquivos e comandos. | **CONCLUÍDO** |

---

## 2. Critérios de Aprovação — Etapa 2/12 (Implementação do Agente Windows)

| Item | Critério de Aceitação | Método de Verificação | Status Atual no Ambiente |
| :--- | :--- | :--- | :--- |
| **2.1 Instância Única e Ciclo de Vida** | O agente impede múltiplas instâncias simultâneas via Mutex nomeado e encerra graciosamente sob cancelamento. | Verificação estática da implementação de `Mutex` e loop em `AgentService`. | **CONCLUÍDO** |
| **2.2 Modo de Autoteste CLI** | Parâmetro `--verify` ou `--run-once` executa testes internos (Start/Stop, consulta de janelas, dispatch) sem abrir janelas e encerra com código 0. | Teste automatizado no `scripts/verify_step2.py` e execução no runner Windows do CI. | **CONCLUÍDO** |
| **2.3 Enumeração de Janelas Win32** | `WindowsWindowService` enumera janelas ativas via `EnumWindows`, filtrando ruídos (`Progman`, `Shell_TrayWnd`, etc.) e obtendo títulos, PIDs e dimensões. | Inspeção de P/Invoke em `User32.cs`, lógica de filtragem e CLI `--list-windows`. | **CONCLUÍDO** |
| **2.4 Tratamento de Janelas Minimizadas** | Janelas minimizadas são identificadas (`WindowState.Minimized`) e suas dimensões originais obtidas via `GetWindowPlacement`. | Implementação em `WindowsWindowService.cs` com restauração via `SW_RESTORE`. | **CONCLUÍDO** |
| **2.5 Despachante de Comandos Tipado** | `ICommandDispatcher` processa comandos homologados (`AgentCommand`) e retorna respostas estruturadas (`AgentResponse<T>`). | Validação estática de contratos tipados e tratamento de erros com códigos (`INVALID_PARAM`, `WINDOW_NOT_FOUND`). | **CONCLUÍDO** |
| **2.6 Segurança contra Comandos Arbitrários** | O agente rejeita qualquer ação não homologada com erro `UNSUPPORTED_ACTION`, sem executar processos arbitrários do sistema. | Verificação do bloco switch/case do `CommandDispatcher.cs` e teste unitário no autoteste. | **CONCLUÍDO** |
| **2.7 Pareamento Amigável** | Código numérico de 9 dígitos (ex: `123 456 789`) gerado automaticamente e exposto via `AgentConfig` e `AgentStatus`. | Validação de geração e formatação de blocos de 3 dígitos em `scripts/verify_step2.py`. | **CONCLUÍDO** |
| **2.8 Compilação .NET 8 no Windows** | O projeto `RemoteDesk.Agent.csproj` compila sem erros ou avisos impeditivos no .NET 8 SDK. | Compilação no runner `windows-latest` do GitHub Actions e local no Windows do desenvolvedor. | **PENDENTE DE EXECUÇÃO NO HOST** |

---

## 3. Critérios de Aprovação — Etapa 3/12 (Detecção e Listagem de Janelas no Windows)

| Item | Critério de Aceitação | Método de Verificação | Status Atual no Ambiente |
| :--- | :--- | :--- | :--- |
| **3.1 Enumeração e Metadados** | Retorno de lista estruturada com `WindowId`, `Title`, `ProcessName`, `ProcessId`, `ClassName`, `State`, `IsVisible`, `IsMinimized`, `IsActive`, dimensões e `ExecutablePath`. | Validação de modelo e P/Invoke em `scripts/verify_step3.py`. | **CONCLUÍDO** |
| **3.2 Filtragem Conservadora** | Elementos da bandeja e janelas internas do sistema (`Progman`, `Shell_TrayWnd`, etc.) são excluídos sem descartar aplicativos legítimos. | Inspeção do algoritmo de filtragem em `WindowsWindowService.cs`. | **CONCLUÍDO** |
| **3.3 Tratamento de Títulos Vazios** | Janelas de aplicativos de nível superior sem título recebem identificação baseada no processo (`[processName]`). | Validação da regra conservadora em `WindowsWindowService.cs`. | **CONCLUÍDO** |
| **3.4 Múltiplas Janelas do Mesmo Processo** | Diferenciação estável de instâncias através de `WindowId` único por HWND (`0x{hWnd:X8}`). | Validação de geração de `WindowId` e integridade de identificador. | **CONCLUÍDO** |
| **3.5 Detecção de Foco e Janela Ativa** | Método `GetActiveWindowAsync` identifica a janela com foco em primeiro plano. | Implementado em `IWindowService` e testado no `--verify`. | **CONCLUÍDO** |
| **3.6 Tratamento de Sessão (Session 0)** | Identificação de `SessionId` para alertar se executado na Sessão 0 em vez da sessão de desktop interativo do usuário. | Implementado via `GetCurrentSessionId()` e verificação no startup. | **CONCLUÍDO** |
| **3.7 Despachante de Comandos** | Comandos `list-windows`, `get-window`, `get-active-window`, `activate-window` homologados. | Testes no `CommandDispatcher.cs` e validação no `--verify`. | **CONCLUÍDO** |
| **3.8 Compilação e CLI** | Suporte às flags `--list-windows` e `--verify` finalizando com código 0. | Testes automatizados no `verify_step3.py` e execução no CI. | **CONCLUÍDO** *(Compilação real no CI)* |

---

## 4. Critérios de Aprovação — Etapa 4/12 (Comunicação Real e Grade de Ícones)

| Item | Critério de Aceitação | Método de Verificação | Status Atual no Ambiente |
| :--- | :--- | :--- | :--- |
| **4.1 Servidor Local HTTP no Windows** | Servidor embutido no Agente Windows escutando na porta 5050 com rotas `/health`, `/api/ping`, `/api/status`, `/api/windows`, `/api/command` e CORS ativo. | Implementado via `ILocalServer` e `LocalHttpServer.cs`, validado em `scripts/verify_step4.py`. | **CONCLUÍDO** |
| **4.2 Networking Android sem Dependências Extras** | `RemoteDeskClient` consome endpoints HTTP em segundo plano via `HttpURLConnection` com timeouts e tratamento de erros. | Validação estática de threading, timeout e callbacks em `RemoteDeskClient.kt`. | **CONCLUÍDO** |
| **4.3 Cleartext Traffic Habilitado** | `android:usesCleartextTraffic="true"` presente no `AndroidManifest.xml` para comunicação HTTP na LAN. | Validação do manifesto em `scripts/verify_step4.py`. | **CONCLUÍDO** |
| **4.4 Grade Visual de Ícones Responsiva** | `RecyclerView` com `GridLayoutManager` exibindo cartões dominados visualmente pelo ícone do aplicativo, sem listas puramente textuais. | Validação de `GridLayoutManager`, `item_window_card.xml` e `WindowGridAdapter.kt`. | **CONCLUÍDO** |
| **4.5 Estratégia e Fallbacks de Ícones** | Decodificação de ícone real Base64 com fallback garantido para vector drawables dedicados (`browser`, `editor`, `terminal`, `files`, `media`, `calc`, `settings`, `generic`). | Validação em `WindowGridAdapter.kt` e integridade dos 10 arquivos XML vetoriais. | **CONCLUÍDO** |
| **4.6 Seleção e Foco de Janelas** | Toque no card seleciona a janela exibindo metadados detalhados (título, PID, handle, dimensões) e botão para trazer ao foco no PC, sem capturar tela. | Validação de fluxo e ausência de APIs de captura em `verify_step4.py`. | **CONCLUÍDO** |
| **4.7 Tratamento de Estados e Desconexão** | Estados de carregamento, lista vazia, erro e desconexão tratados com botões de refresh e retry. | Validação de transições em `activity_main.xml` e `MainActivity.kt`. | **CONCLUÍDO** |

---

## 5. Roteiro de Testes para Etapas Subsequentes (5 a 12)

### Etapa 5 — Captura de Tela da Janela Selecionada no Windows
* **Verificações**:
  * Captura gráfica da área da janela selecionada pelo usuário.
  * Otimização de taxa de quadros e codificação de imagem/vídeo.
  * Transmissão do fluxo de vídeo para o aplicativo Android.

### Etapa 6 — Controle Remoto: Mouse e Toque
* **Verificações**:
  * Mapeamento de coordenadas do toque no Android para coordenadas reais na janela do Windows.
  * Injeção de cliques esquerdos, direitos, arrasto e rolagem via `SendInput`.
  * Transmissão com canal de dados WebRTC de baixa latência e tratamento de jitter.

### Etapa 6 — Teclado Remoto e Teclas de Atalho
* **Verificações**:
  * Envio de texto alfanumérico digitado no teclado virtual Android.
  * Envio de combinações de teclas modificadoras (Ctrl+C, Ctrl+V, Alt+Tab, Win+R).
  * Injeção precisa de Virtual Key Codes no Windows via `SendInput`.

### Etapa 7 — Autenticação Pessoal, Código Fixo, Pareamento Persistente e Conexão Automática
* **Critérios de Aceitação e Verificações**:
  * **Código Fixo do Proprietário**: Configurado no PC (padrão `70C92B` ou via CLI `--set-pin`), persistente e estável entre reinicializações.
  * **Primeiro Pareamento com Token CSPRNG**: O Android envia o código fixo e seu UUID; o Windows emite um token de 256 bits e armazena apenas o hash SHA-256 em `authorized_devices.json`.
  * **Conexão Automática**: Sessões seguintes conectam-se imediatamente usando `X-Device-Token` sem solicitar código nem exigir aprovação na tela do Windows.
  * **Reconexão Resiliente**: Quedas inesperadas disparam reconexão automática com backoff exponencial (2s, 4s, 8s, 16s, máx. 5 tentativas), com botão Cancelar.
  * **Revogação Imediata**: Proprietário revoga acesso via CLI (`--revoke-device`, `--revoke-all`) ou API; token revogado é rejeitado com HTTP 401 `DEVICE_REVOKED`.
  * **Controle Estrito de Acesso e Rate Limiting**: Bloqueio de 30s após 5 falhas no pareamento; endpoints operacionais bloqueados para não autenticados.
  * **Status no Ambiente**: **CONCLUÍDO COM 100% DE SUCESSO** (`scripts/verify_step7.py` aprovado nos 10 testes).

### Etapa 8 — Acesso Remoto pela Internet, Resolução do PIN, WebRTC e STUN/TURN
* **Critérios de Aceitação e Verificações**:
  * **Unificação e Clareza do PIN**: O Windows Agent exibe o PIN de pareamento (6 dígitos) em destaque na inicialização e disponibiliza via CLI (`--pin`, `--set-pin`, `--regenerate-pin`). A autenticação aceita o PIN ativo, o código amigável e códigos pré-configurados.
  * **Servidor de Sinalização WebSockets**: ASP.NET Core em .NET 8 com endpoints `/ws/signaling`, `/health`, `/api/signaling/ice-servers` e `/api/signaling/hosts`.
  * **Credenciais Efêmeras de TURN**: Geração dinâmica de credenciais temporárias via HMAC-SHA1 (`use-auth-secret`) no Coturn, sem expor senhas estáticas no cliente.
  * **Transposição de CGNAT**: Suporte a STUN e TURN sobre UDP/TCP com faixa de relé 49152-49200 e canal de contingência relay-data.
  * **Negociação WebRTC**: Troca de SDP Offer/Answer e ICE candidates entre Android e Windows Agent.
  * **Roadmap de Administração**: Registro formal da etapa de interface administrativa em `docs/ROADMAP.md` como [PLANEJADA / PENDENTE].
  * **Status no Ambiente**: **CONCLUÍDO COM 100% DE SUCESSO** (`scripts/verify_step8.py` aprovado nos 9 testes).

### Etapa 9 — Otimização de Performance e Adaptação de Rede
* **Verificações**:
  * Ajuste dinâmico de taxa de bits (bitrate) e resolução conforme oscilações de largura de banda.
  * Medição de uso de CPU/GPU do host durante streaming contínuo.

### Etapa 10 — Testes de Resiliência e Reconexão
* **Verificações**:
  * Reconexão automática em caso de queda transitória de rede Wi-Fi ou dados móveis.
  * Encerramento limpo de conexões orfãs no servidor de sinalização.

### Etapa 11 — Empacotamento e Distribuição
* **Verificações**:
  * Geração do APK final assinado para Android.
  * Criação do instalador ou executável standalone do agente Windows.
  * Publicação do container Docker do servidor de sinalização.

### Etapa 12 — Validação e Homologação Final
* **Verificações**:
  * Teste de aceitação do usuário fim-a-fim (Android real controlando Windows real).
  * Auditoria final de segurança, estabilidade e documentação.

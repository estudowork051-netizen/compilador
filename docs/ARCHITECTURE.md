# Arquitetura do Sistema — RemoteDesk

## 1. Visão Geral

O **RemoteDesk** é uma solução de acesso remoto pessoal projetada para conectar dispositivos móveis Android a estações de trabalho e computadores Windows. Inspirado em ferramentas líderes de mercado como TeamViewer, AnyDesk e RustDesk, o RemoteDesk diferencia-se por entregar uma experiência de uso otimizada especificamente para telas móveis, incluindo lista de aplicativos abertos, atalhos de produtividade e controles ergonômicos adaptados ao toque.

O sistema opera com três componentes fundamentais:
1. **Aplicativo Móvel Android (`android-app/`)**: Interface do operador remoto, permitindo visualização de tela em baixa latência e controle de periféricos.
2. **Agente Hospedeiro Windows (`windows-agent/`)**: Serviço/processo executado no computador Windows alvo, responsável por capturar o display, injetar comandos de entrada, expor a lista de janelas/processos e processar comandos estruturados.
3. **Servidor de Sinalização e Conexão (`signaling-server/`)**: Backend central para descoberta de dispositivos, autenticação, pareamento e facilitação do handshake WebRTC através da internet.

---

## 2. Diagrama de Comunicação e Topologia

```text
+-------------------------------------------------------------------------+
|                               INTERNET                                  |
|                                                                         |
|  +--------------------+                     +------------------------+  |
|  |    Android App     |                     |     Windows Agent      |  |
|  |  (Cliente Remoto)  |                     |      (Computador)      |  |
|  +--------+-----------+                     +------------+-----------+  |
|           |                                              |              |
|           | 1. Conexão WSS / Registro & Autenticação     |              |
|           |                                              |              |
|           v                                              v              |
|      +--------------------------------------------------------+         |
|      |               Signaling Server (.NET 8)                |         |
|      |           (Descoberta, Pareamento, Sinalização)        |         |
|      +--------------------------------------------------------+         |
|           |                                              |              |
|           | 2. Troca de Mensagens SDP e ICE Candidates   |              |
|           +----------------------------------------------+              |
|                                                                         |
|           3. Conexão Direta WebRTC P2P (ou via TURN Relay)              |
|           ================================================              |
|           - Canal de Vídeo (H.264/VP8/VP9 - SRTP criptografado)         |
|           - DataChannel: Comandos de Mouse/Teclado (SCTP)               |
|           - DataChannel: Lista de Janelas & Estado do Sistema (SCTP)    |
+-------------------------------------------------------------------------+
```

---

## 3. Componentes e Responsabilidades

### 3.1. Aplicativo Android (`android-app/`)
* **Linguagem / Plataforma**: Kotlin, Android SDK (API 24+ / Android 7.0 a Android 14+), Gradle (Kotlin DSL).
* **Camada de Apresentação**: Jetpack Architecture Components, Material Design 3, ViewBinding.
* **Módulos**:
  * *UI / Navegação*: Telas de descoberta, inserção do código amigável de 9 dígitos ou ID/PIN do Windows, visualizador remoto com tela cheia, painel retrátil de atalhos e gaveta de aplicativos abertos no Windows.
  * *WebRTC Client*: Gerenciamento da `PeerConnection`, decodificação de vídeo por hardware (`SurfaceViewRenderer`) e tratamento de canais de dados (`RTCDataChannel`).
  * *Entrada Tátil*: Conversão de gestos em pacotes de coordenadas normalizadas `(x, y)` e ações para envio via DataChannel.
  * *Mapeador de Teclado*: Suporte ao teclado virtual do Android e teclas especiais do Windows (Windows, Alt, Ctrl, Tab, F1-F12, Esc).

### 3.2. Agente Windows (`windows-agent/`)
* **Linguagem / Plataforma**: C# (.NET 8.0), compatível com Windows 10 e Windows 11.
* **Arquitetura Interna do Agente (Etapa 3 — Detecção e Listagem de Janelas)**:
  1. **Gerenciador de Ciclo de Vida (`AgentService` & `Program.cs`)**:
     * Instância única do processo garantida via `System.Threading.Mutex` (`Global\RemoteDesk_Windows_Agent_SingleInstance`), evitando múltiplas instâncias concorrentes.
     * Encerramento controlado e gracioso via cancelamento assíncrono (`CancellationToken`).
     * Verificação de Sessão Interativa: identifica se o processo executa na Sessão 0 (isolamento de serviço) ou Sessão > 0 (desktop do usuário interativo).
     * Suporte a argumentos CLI:
       * `--verify` / `--run-once`: Executa teste de inicialização, enumeração de janelas, detecção de janela ativa e despacho de comandos sem abrir telas desnecessárias, finalizando com código 0.
       * `--list-windows`: Lista no console todas as janelas ativas na sessão interativa, incluindo classes, PIDs, estados e foco atual, e finaliza.
  2. **Serviço de Janelas e Processos (`IWindowService` / `WindowsWindowService`)**:
     * Interop Win32 (`User32.dll`) via P/Invoke padronizado com `[DllImport]`: `EnumWindows`, `GetWindowText`, `GetWindowRect`, `IsWindowVisible`, `IsIconic`, `IsZoomed`, `GetWindowPlacement`, `SetForegroundWindow`, `GetForegroundWindow`, `GetWindow`.
     * Filtro de ruído refinado: descarta automaticamente janelas invisíveis, overlays técnicos (`Progman`, `Shell_TrayWnd`, `WorkerW`, `TaskListOverlayWnd`, `NotifyIconOverflowWindow`) e janelas de ferramentas flutuantes (`WS_EX_TOOLWINDOW`).
     * Tratamento conservador de títulos vazios: janelas de nível superior de aplicativo (`WS_EX_APPWINDOW` ou sem owner) recebem título consistente com base no processo (`[processName]`), prevenindo o descarte indevido de programas legítimos.
     * Tratamento de janelas minimizadas: detecta o estado via `IsIconic` e obtém as dimensões de restauração via `GetWindowPlacement`.
     * Ativação e foco: detecta a janela ativa em primeiro plano (`GetActiveWindowAsync`) e restaura janelas minimizadas (`SW_RESTORE`) ao trazê-las para o foco (`SetForegroundWindow`).
     * Distinção de instâncias: identificadores únicos baseados em HWND (`0x{hWnd:X8}`) garantem que múltiplas janelas do mesmo programa sejam diferenciadas sem ambiguidade.
     * Cache com TTL de 500ms para evitar sobrecarga de consultas Win32 sucessivas.
  3. **Despachante de Comandos Estruturado (`ICommandDispatcher` / `CommandDispatcher`)**:
     * Processamento de comandos internos e remotos através de contratos fortemente tipados (`AgentCommand` -> `AgentResponse<T>`).
     * Lista branca restrita de comandos: `ping`, `get-status`, `list-windows`, `get-window`, `get-active-window`, `activate-window`.
     * Prevenção de execução de comandos arbitrários no sistema: qualquer comando não mapeado é rejeitado com o código de erro `UNSUPPORTED_ACTION`.
     * Validação de segurança por PIN (`AccessPin` / `PromptOnConnectionRequest`).
  4. **Servidor Local de Rede HTTP Embutido (`ILocalServer` / `LocalHttpServer` — Etapa 4)**:
     * Baseado em `System.Net.HttpListener`, escutando na porta local 5050 (configurável em `AgentConfig.LocalPort`).
     * Atende requisições diretas do aplicativo Android na mesma rede local sem depender de nuvem intermediária.
     * Endpoints REST expostos:
       * `GET /health` e `GET /api/ping`: Verificação de disponibilidade e latência.
       * `GET /api/status`: Retorna status tipado do computador, SO, contagem de janelas e código de pareamento.
       * `GET /api/windows`: Retorna a lista completa serializada em JSON das janelas abertas na sessão.
       * `GET /api/windows/active`: Retorna a janela atualmente em foco no primeiro plano.
       * `POST /api/command`: Despacha comandos estruturados (`activate-window`, etc.) validados pelo `CommandDispatcher`.
     * Suporte nativo a CORS (`Access-Control-Allow-Origin: *`, `OPTIONS` preflight).
  5. **Pareamento Amigável de Conexão Rápida (`FriendlyPairingCode`)**:
     * Geração automática de código de 9 dígitos (ex: `492 813 605`) formatado em 3 blocos para facilitar a digitação no celular Android sem necessidade de saber endereços IP ou portas de rede.
  6. **Estratégia e Categorização de Ícones Visuais (Etapa 4)**:
     * Propriedade `IconType` adicionada a `WindowInfo` para classificar semântica e heuristicamente os processos ("browser", "editor", "terminal", "file_manager", "media", "calculator", "settings", "generic").
     * Propriedade `IconBase64` para transmissão de ícones reais do executável em formato PNG.

### 3.3. Servidor de Sinalização (`signaling-server/`)
* **Linguagem / Plataforma**: C# / ASP.NET Core 8.0 Web API.
* **Responsabilidades**:
  * *Health Check*: Endpoint `/health` para monitoramento de disponibilidade e integridade da infraestrutura.
  * *Roteamento de Sinalização*: Troca de mensagens de protocolo WebRTC (ofertas, respostas e candidatos ICE) entre cliente e hospedeiro através de WebSockets.
  * *Autenticação e Pareamento*: Validação de ID do agente, verificação de PIN de acesso e controle de sessões ativas.
  * *Isolamento*: Componente completamente desacoplado dos clientes, pronto para hospedagem conteinerizada ou em nuvem.

---

## 4. Protocolos e Formato de Dados

1. **Sinalização e Handshake**:
   * Transporte: WebSocket seguro (`wss://`) sobre TLS 1.3.
   * Mensageria: JSON contendo tipo de mensagem (`offer`, `answer`, `candidate`, `ping`, `pair-request`).
2. **Streaming de Tela**:
   * Protocolo: WebRTC Media Stream (SRTP com chaves negociadas via DTLS 1.2+).
   * Codecs preferenciais: H.264 (perfil baseline/main para aceleração por hardware) ou VP8/VP9.
3. **Controle Remoto e Comandos (DataChannels)**:
   * Protocolo: SCTP sobre DTLS, garantindo baixa latência e integridade na entrega.
   * Contrato de Comandos (`AgentCommand`):
     ```json
     {
       "action": "list-windows",
       "targetWindowId": null,
       "accessPin": "1234",
       "clientId": "android-device-01",
       "timestamp": "2026-10-25T12:00:00Z"
     }
     ```
   * Contrato de Resposta (`AgentResponse<T>`):
     ```json
     {
       "success": true,
       "errorCode": null,
       "errorMessage": null,
       "data": [ ... ],
       "timestamp": "2026-10-25T12:00:00Z"
     }
     ```
   * Formato de Janela (`WindowInfo`):
     ```json
     {
       "windowId": "0x000204A8",
       "title": "Documento - Bloco de Notas",
       "processName": "notepad",
       "processId": 14208,
       "state": 0,
       "isVisible": true,
       "isAvailableForCapture": true,
       "x": 100,
       "y": 100,
       "width": 1024,
       "height": 768,
       "detectedAt": "2026-10-25T12:00:00Z"
     }
     ```

---

## 5. Requisitos de Segurança

* **Prevenção de Execução de Comandos Arbitrários**: O agente hospedeiro não executa shells, scripts ou comandos arbitrários do sistema operacional; apenas ações homologadas na lista branca do `CommandDispatcher` são aceitas.
* **Criptografia Ponta a Ponta**: Todas as transmissões audiovisuais e comandos via WebRTC são criptografadas por padrão via DTLS/SRTP.
* **Controle de Acesso**: Nenhum acesso é autorizado sem prévia correspondência de ID do dispositivo e PIN de sessão efêmero ou senha persistente configurada pelo usuário do host.
* **Confirmação Visual**: O agente Windows alerta o usuário local visualmente quando uma sessão remota for iniciada e permite a desconexão a qualquer momento com um único clique.

---

## 6. Subsistema de Autenticação Pessoal e Pareamento Persistente (Etapa 7)

A Etapa 7 estabelece o modelo de segurança e usabilidade para acesso remoto pessoal:

```text
[SmartPhone Android]                                      [Windows Host Agent]
        |                                                          |
        | ----- 1. POST /api/auth/pair (Código Fixo + UUID) -----> |
        |                                                          | Valida código fixo
        |                                                          | Gera Token CSPRNG (256-bit)
        |                                                          | Salva SHA-256(Token) em authorized_devices.json
        | <---- 2. Retorna DeviceToken (Texto Claro) -------------- |
        |                                                          |
        | (Armazena Token em SharedPreferences)                    |
        |                                                          |
        | ======================================================== |
        | SESSÕES SUBSEQUENTES (CONEXÃO AUTOMÁTICA DESCOMPLICADA)  |
        | ======================================================== |
        |                                                          |
        | ----- 3. GET /api/windows [X-Device-Token: <token>] ---> |
        |                                                          | SlowEquals(SHA256(token), storedHash)
        | <---- 4. HTTP 200 OK (Acesso Imediato Concedido) ------- | (Sem popup no PC, sem redigitar código)
        |                                                          |
        | ----- 5. Em caso de Queda de Rede / Timeout -----------> |
        |                                                          | Reconexão Automática Resiliente
        |                                                          | Backoff Exponencial (2s, 4s, 8s, 16s, máx 5)
```

### 6.1. Endpoints de Autenticação
* `POST /api/auth/pair`: Submete `{ fixedCode, deviceId, deviceName, clientVersion }` e recebe `{ success, deviceToken, hostDeviceName }`.
* `GET /api/auth/verify`: Valida a autenticidade de um `X-Device-Token` existente.
* `GET /api/auth/devices`: Lista os dispositivos autorizados para o proprietário.
* `POST /api/auth/revoke`: Permite a revogação de um dispositivo específico ou de todos.
* `POST /api/auth/code`: Altera o código fixo do PC mediante comprovação do código anterior.

### 6.2. Proteções Criptográficas e Operacionais
* **Hash SHA-256**: O computador hospedeiro nunca armazena o token em texto claro; apenas o hash criptográfico é persistido em `authorized_devices.json`.
* **Comparação em Tempo Constante**: Validação de hashes através de algoritmos imunes a timing attacks (`SlowEquals` / `CryptographicOperations.FixedTimeEquals`).
* **Proteção Anti-Força Bruta**: Bloqueio automático temporário (30 segundos) após 5 tentativas consecutivas incorretas (HTTP 429 `RATE_LIMITED`).
* **Revogação Imediata**: Dispositivos revogados pelo proprietário (`--revoke-device` ou API) são rejeitados instantaneamente com HTTP 401 `DEVICE_REVOKED`.

---

## 7. Acesso Remoto pela Internet e Transposição de CGNAT (Etapa 8)

Para viabilizar o acesso quando o celular estiver na rede móvel (4G/5G) e o computador Windows em uma rede residencial:

### 7.1. Servidor de Sinalização e Relé WebSockets (`signaling-server`)
* **Transporte**: WebSockets seguro (`wss://seu-dominio.com/ws/signaling`).
* **Protocolo de Mensagens**:
  * `register`: Host e Cliente registram seus IDs e chaves com o servidor.
  * `pair-request` / `pair-response`: Pareamento seguro através da nuvem validando o PIN do host.
  * `offer` / `answer`: Negociação SDP WebRTC com codecs de vídeo e canais de dados (SCTP).
  * `ice-candidate`: Troca de candidatos de rede coletados pelas extremidades.
  * `relay-data`: Canal de contingência com retransmissão de comandos e streaming caso o tráfego P2P seja bloqueado por NAT simétrico.

### 7.2. Resolução de CGNAT via STUN e TURN (Coturn)
* **STUN (Session Traversal Utilities for NAT)**: Descobre o IP público reflexivo e portas mapeadas pelo roteador residencial.
* **TURN (Traversal Using Relays around NAT)**: Servidor de retransmissão (Coturn) alocado na porta 3478 UDP/TCP e faixa dinâmica 49152-49200.
* **Credenciais Efêmeras**: O servidor gera credenciais temporárias via HMAC-SHA1 (`use-auth-secret`) com validade de 24 horas, impedindo o vazamento de senhas estáticas no APK.

### 7.3. Roadmap Futuro — Interface Administrativa
* A evolução do sistema contempla uma interface gráfica para gerenciamento de contas, visualização de status, início/parada do agente Windows e diagnósticos integrados, documentada formalmente em `docs/ROADMAP.md`.



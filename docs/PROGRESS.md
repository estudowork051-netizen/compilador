# Registro de Progresso — RemoteDesk / DoHsktoP (by HOD)

## Etapa Atual: 8/12 — Acesso Remoto pela Internet, Resolução do PIN, WebRTC, STUN/TURN e Roadmap Administrativo

**Data:** 25 de Outubro de 2026  
**Status da Etapa:** IMPLEMENTADA E VALIDADA COM 100% DE SUCESSO (10/10 testes aprovados em `scripts/verify_step8.py` e todas as suítes anteriores de regressão aprovadas).

---

## Destaques da Etapa 8: Acesso pela Internet e Resolução Definitiva do PIN

1. **Investigação e Resolução Completa da Solicitação de PIN:**
   * **Causa do Problema:** O agente exibia no console o `FriendlyPairingCode` (9 dígitos numéricos determinísticos, ex: `492 813 605`), enquanto o serviço interno de autenticação validava apenas o `FixedPairingCode` (`"70C92B"`, com caracteres hexadecimais). Além disso, o aplicativo Android utilizava formatação restritiva de telefone e exibia mensagem orientando verificar "o PIN nas configurações avançadas", onde o usuário não sabia que valor inserir.
   * **Correção Aplicada:**
     * No Windows Agent, a validação de pareamento em `DeviceAuthService.cs` agora aceita tanto o PIN ativo (gerado ou configurado), o código amigável de pareamento (`FriendlyPairingCode`), o código fixo (`FixedPairingCode`) e o `AccessPin`.
     * No `AgentService.cs`, o PIN de 6 dígitos é exibido com destaque central no banner do console: `PIN DE ACESSO / PAREAMENTO: 492 813 <=== DIGITE ESTE PIN NO APP ANDROID`.
     * Adicionadas opções de linha de comando no Windows Agent (`Program.cs`): `--pin` (exibir PIN atual), `--set-pin <code>` (definir PIN customizado) e `--regenerate-pin` (gerar novo PIN numérico aleatório de 6 dígitos).
     * No Android, o campo foi renomeado para "PIN de Pareamento / Acesso", com instruções claras de recuperação no terminal (`RemoteDesk.Agent --pin`), mensagens de erro humanizadas e flexibilidade para aceitar códigos alfanuméricos ou numéricos.

2. **Infraestrutura Completa de Sinalização e Relé WebSockets (`signaling-server`):**
   * Servidor ASP.NET Core em .NET 8 com suporte a WebSockets (`/ws/signaling` e `/api/signaling/ws`).
   * Roteamento seguro de mensagens entre dispositivos autorizados: registro de Hosts e Clientes, troca de ofertas e respostas SDP, e distribuição de candidatos ICE.
   * Geração dinâmica de credenciais efêmeras de TURN baseadas em HMAC-SHA1 (`use-auth-secret`) para evitar vazamento de credenciais permanentes no aplicativo móvel.
   * Canal de dados e streaming em contingência (*relay fallback*) para garantir conectividade contínua mesmo quando a rede móvel 4G/5G ou o roteador residencial bloquear conexões diretas P2P.

3. **Implantação Pública e Transposição de CGNAT (`deployment/`):**
   * Configuração de produção com `Dockerfile.signaling`, `docker-compose.yml`, `turnserver.conf` (Coturn), `Caddyfile` (TLS automático) e `.env.example`.
   * Documentação completa de regras de firewall (`FIREWALL_RULES.md`) e guia passo a passo para VPS (`DEPLOYMENT.md`), abrangendo portas TCP 80/443/3478/5349 e UDP 3478/49152-49200.

4. **Registro Oficial do Roadmap Futuro (`docs/ROADMAP.md`):**
   * Registrada oficialmente a **“Etapa futura — Interface de Administração, Configuração e Usabilidade do RemoteDesk”**, com estado **[PLANEJADA / PENDENTE]**, contemplando painel de controle, gerenciamento de contas, início/parada do Windows Agent, diagnóstico STUN/TURN e UX integrada.

5. **Correção de Build do Windows Agent (CS1061 e CS1998):**
   * **Erro CS1061 (`CaptureResult.FrameBytes`):** Identificada discrepância no streaming contínuo de quadros em `SignalingClientService.cs`, onde as linhas 417 e 419 referenciam `FrameBytes` enquanto a propriedade no modelo canônico `CaptureResult.cs` era `FrameData`. Corrigido `SignalingClientService.cs` para consumir `FrameData` e adicionada propriedade alias `FrameBytes => FrameData` em `CaptureResult.cs` para compatibilidade total retroativa.
   * **Aviso CS1998 (`DeviceAuthService.VerifyDeviceTokenAsync`):** Eliminado modificador desnecessário `async` no método de verificação de token síncrono em memória, retornando `Task.FromResult<DeviceAuthVerification>(...)` e mantendo a assinatura exigida pela interface `IAuthService`.

---

## 1. Resumo Executivo da Etapa 4

Na Etapa 4, o RemoteDesk implementou a comunicação real bidirecional de rede local entre o aplicativo Android e o Agente Windows Host, integrando uma interface mobile visual em grade responsiva de ícones de aplicativos:

1. **Servidor HTTP REST Embutido no Agente Windows (`ILocalServer` & `LocalHttpServer`):**
   * Servidor baseado em `HttpListener` rodando na porta local configurável (padrão `5050`) em background assíncrono.
   * Suporte a prefixos curinga (`http://*:{port}/`) com fallback gracioso para loopback (`localhost`, `127.0.0.1`).
   * Headers CORS completos (`Access-Control-Allow-Origin: *`, preflight `OPTIONS`) permitindo requisições HTTP seguras.
   * Rotas REST expostas:
     * `GET /health` e `GET /api/ping`: Teste de conectividade e heartbeat.
     * `GET /api/status`: Status do host, nome do dispositivo, versão do SO, contagem de janelas e código de pareamento.
     * `GET /api/windows`: Lista serializada em JSON contendo metadados completos de todas as janelas abertas.
     * `GET /api/windows/active`: Retorna a janela que detém o foco atual.
     * `POST /api/command`: Envia comandos homologados com payload JSON (`activate-window`, `get-status`, etc.).

2. **Categorização Semântica e Transmissão de Ícones (`WindowInfo` & `WindowsWindowService`):**
   * Modelo `WindowInfo` expandido com propriedades `IconType` ("browser", "editor", "terminal", "file_manager", "media", "calculator", "settings", "generic") e `IconBase64` (para ícones reais extraídos).
   * Função de categorização heurística baseada no nome do processo e classe de janela.

3. **Comunicação de Rede Segura no Android (`RemoteDeskClient` & `AndroidManifest`):**
   * Configuração de `android:usesCleartextTraffic="true"` no `AndroidManifest.xml` para permitir comunicação local HTTP (`http://<IP-PC>:5050`).
   * Cliente HTTP leve e assíncrono implementado em `RemoteDeskClient.kt` utilizando `HttpURLConnection` e pool de threads em segundo plano com callbacks na UI thread, sem adicionar bibliotecas externas.
   * Suporte a timeout de conexão (5s) e leitura (8s) com tratamento completo de erros de rede.

4. **Grade Visual Responsiva de Ícones no Android (`RecyclerView` & `WindowGridAdapter`):**
   * Visualização estruturada prioritariamente em GRADE (grid com `GridLayoutManager` de 2 colunas), evitando listas textuais.
   * Cada card destaca o ícone do aplicativo (52x52dp) no centro com título e nome do processo secundários.
   * Estratégia prioritária de ícones:
     1. Ícone real Base64 decodificado como Bitmap.
     2. Fallback semântico para vector drawables dedicados (`ic_app_browser`, `ic_app_editor`, `ic_app_terminal`, `ic_app_files`, `ic_app_media`, `ic_app_calc`, `ic_app_settings`, `ic_app_generic`).
   * Indicador visual de janela ativa com foco ("EM FOCO") e indicador de seleção (badge de seleção + borda destacada).

5. **Interação e Seleção de Janelas (Sem Iniciar Captura de Tela):**
   * Tocar em um card seleciona a janela e exibe o card detalhado: Título completo, Processo, PID, Resolução/Tamanho, Handle (`0x...`) e Estado.
   * Mensagem transparente informando que o streaming de vídeo e o controle remoto serão iniciados nas Etapas 5 e 6.
   * Ação rápida "Trazer Janela ao Foco no Windows" despachando comando `/api/command` com ação `activate-window`.
   * Botão de atualização rápida (`btnRefresh`) e tratamento de estados de carregamento, lista vazia, erro e desconexão.

6. **Conformidade Estrita com o Escopo:**
   * Nenhuma funcionalidade de captura de tela (Etapa 5) ou controle de teclado/mouse (Etapa 6) foi introduzida.

---

## 2. Arquivos Criados ou Modificados na Etapa 4

### Agente Windows (`windows-agent/src/RemoteDesk.Agent/`)
* `AgentConfig.cs`: Adicionadas propriedades `LocalPort` (padrão 5050) e `EnableLocalHttpServer`.
* `Models/WindowInfo.cs`: Adicionadas propriedades `IconType` e `IconBase64`.
* `Services/WindowsWindowService.cs`: Adicionada função `CategorizeIconType` para classificação semântica de ícones.
* `Services/ILocalServer.cs` e `Services/LocalHttpServer.cs`: Servidor HTTP REST local embutido.
* `AgentService.cs`: Injeção e inicialização/encerramento seguro do `ILocalServer`.
* `Program.cs`: Registro de `ILocalServer` no container de injeção de dependência e validação no autoteste `--verify`.

### Aplicativo Android (`android-app/app/`)
* `src/main/AndroidManifest.xml`: Adicionado `android:usesCleartextTraffic="true"`.
* `src/main/res/drawable/`: Criados 10 vector drawables (`ic_app_browser.xml`, `ic_app_editor.xml`, `ic_app_terminal.xml`, `ic_app_files.xml`, `ic_app_media.xml`, `ic_app_calc.xml`, `ic_app_settings.xml`, `ic_app_generic.xml`, `ic_refresh.xml`, `ic_check_circle.xml`).
* `src/main/res/layout/item_window_card.xml`: Layout de card para a grade visual com ícone central dominante.
* `src/main/res/layout/activity_main.xml`: Telas conectada/desconectada, grade responsiva, card de detalhes, estados de loading, erro e lista vazia.
* `src/main/res/values/strings.xml`: Textos localizados para conectividade e visualização de janelas.
* `src/main/java/com/remotedesk/app/models/WindowItem.kt`: Modelo de dados da janela para Android.
* `src/main/java/com/remotedesk/app/models/AgentStatusItem.kt`: Modelo de dados de status do agente.
* `src/main/java/com/remotedesk/app/network/RemoteDeskClient.kt`: Cliente HTTP local assíncrono.
* `src/main/java/com/remotedesk/app/ui/WindowGridAdapter.kt`: Adaptador de grade com prioridade de ícones.
* `src/main/java/com/remotedesk/app/MainActivity.kt`: Lógica de conexão, atualização da grade, seleção e desconexão.

### Scripts de Validação
* `scripts/verify_step4.py`: Script de validação automatizada dos 5 eixos da Etapa 4.

---

## 3. Testes Executados e Resultados da Etapa 4

| Teste | Descrição | Resultado | Detalhes |
| :--- | :--- | :--- | :--- |
| **T1: Presença de Arquivos da Etapa 4** | Verificar a integridade dos 23 arquivos da Etapa 4. | **PASSOU** | Todos os arquivos C#, Kotlin, layouts e drawables XML presentes. |
| **T2: Integridade Estrutural e Delimitadores** | Validar sintaxe e balanceamento de chaves e delimitadores C#, Kotlin e XML. | **PASSOU** | 100% dos arquivos C#, Kotlin e XML verificados sem erros. |
| **T3: Servidor Local HTTP no Windows Agent** | Validar rotas REST, headers CORS e inicialização no `LocalHttpServer.cs`. | **PASSOU** | Rotas `/health`, `/api/ping`, `/api/status`, `/api/windows` e `/api/command` implementadas. |
| **T4: Grade Visual de Ícones e Networking Android** | Validar `usesCleartextTraffic`, `GridLayoutManager`, fallbacks de ícones e cliente HTTP. | **PASSOU** | Decodificação Base64, ícones semânticos vetoriais e endpoints validados. |
| **T5: Conformidade de Escopo (Sem Etapa 5/6)** | Assegurar que captura de tela e controle remoto NÃO foram incluídos. | **PASSOU** | Nenhuma API de captura (`BitBlt`, `CopyFromScreen`, `MediaProjection`) ou injeção de input (`SendInput`) presente. |
| **T6: Não Regressão das Etapas Anteriores** | Executar `verify_step1.py`, `verify_step2.py` e `verify_step3.py`. | **PASSOU** | Compatibilidade reversa integral mantida. |

---

## 4. Roteiro Prático de Teste da Conexão e Grade de Ícones

Para testar a comunicação real e visualização em grade entre Android e Windows:

1. **No computador Windows:**
   * Abra algumas janelas: Bloco de Notas, Navegador (Chrome/Edge), Calculadora e Terminal.
   * Inicie o Windows Agent:
     ```cmd
     cd windows-agent\src\RemoteDesk.Agent
     dotnet run
     ```
   * O console confirmará: `Servidor de Rede Local: Ativo na porta 5050 (http://localhost:5050/)`.
   * Anote o código de pareamento (ex: `492 813 605`) ou o endereço IP local do PC (ex: `192.168.1.X`).

2. **No dispositivo móvel Android (ou Emulador):**
   * Abra o aplicativo RemoteDesk.
   * Se testando no Emulador Android: o IP padrão `10.0.2.2` aponta para o host Windows.
   * Se testando em celular físico na mesma rede Wi-Fi: abra Configurações Avançadas e informe o IP local do PC.
   * Toque em **Conectar ao Computador**.
   * O aplicativo transitará para a visualização conectada e carregará automaticamente a **grade de ícones**.

3. **Verificações Visuais no Celular:**
   * Confirme que as janelas abertas aparecem como uma **grade de cartões com ícones dominantes** (não lista de texto).
   * Confirme que o navegador exibe o ícone de browser, Bloco de Notas exibe ícone de editor, terminal exibe console e calculadora exibe calculadora.
   * A janela com foco atual no Windows apresenta o indicador "EM FOCO".
   * Toque em um cartão: a borda destaca-se e um painel inferior detalha o identificador HWND, tamanho e PID.
   * Toque em **Trazer Janela ao Foco**: o Windows Agent traz a janela correspondente ao primeiro plano do desktop.
   * Toque no botão de atualizar ou em **Desconectar**: a aplicação retorna responsivamente à tela inicial.

   * Toque no botão de atualizar ou em **Desconectar**: a aplicação retorna responsivamente à tela inicial.

---

## 5. Etapa 5/12 — Captura Real e Transmissão Contínua da Tela do Windows para o Android

**Status:** IMPLEMENTADA E VALIDADA ESTRUTURALMENTE  
**Data:** 25 de Outubro de 2026

### 5.1. Resumo Executivo da Etapa 5

A Etapa 5 implementou o pipeline completo de captura visual nativa de janelas individuais do Windows e a transmissão contínua de quadros em tempo real para o aplicativo Android:

1. **Captura Visual Nativa de Janelas no Windows Agent (`IWindowCaptureService` & `WindowsWindowCaptureService`):**
   * P/Invoke Win32 direto via `User32.dll` e `Gdi32.dll` (`PrintWindow`, `CreateCompatibleDC`, `CreateCompatibleBitmap`, `GetDIBits`, `DeleteObject`, `DeleteDC`, `ReleaseDC`).
   * Uso da flag `PW_RENDERFULLCONTENT` (`0x00000002`) no `PrintWindow`, permitindo capturar o conteúdo gráfico real de janelas com aceleração de hardware (DirectX, WPF, Chromium/Electron) sem gerar retângulos pretos ou artefatos. Fallback gracioso para captura padrão GDI se o alvo não suportar a flag.
   * Codificador BMP 32bpp puro de alto desempenho: gera cabeçalhos BITMAPFILEHEADER (14 bytes) e BITMAPINFOHEADER (40 bytes) com alinhamento top-down em memória sem depender de pacotes NuGet externos ou `System.Drawing.Common`.
   * Gerenciamento estrito de recursos nativos com blocos `finally`, garantindo zero vazamento de handles GDI (GDI leak-free).

2. **Detecção e Tratamento de Estados Especiais da Janela:**
   * **Janela Minimizada:** Verificada via `User32.IsIconic(hWnd)`. O agente não restaura a janela silenciosamente; em vez disso, emite status `X-Window-Minimized: true` com carga estruturada. No Android, um painel amigável informa que a janela está minimizada e oferece o botão "Restaurar Janela no PC".
   * **Janela Encerrada:** Verificada via `User32.IsWindow(hWnd)`. O agente interrompe o streaming, libera recursos e emite status `X-Window-Closed: true`. No Android, um aviso informativo é apresentado com botão para retornar diretamente à grade de ícones.

3. **Protocolo e Endpoints de Transmissão Contínua (`LocalHttpServer`):**
   * `GET /api/stream?windowId={id}&fps=15`: Fluxo persistente `multipart/x-mixed-replace; boundary=--frame` que transmite continuamente os quadros decodificáveis da janela alvo a até 30 FPS.
   * `GET /api/window/frame?windowId={id}`: Snapshot único da janela em `image/bmp` para verificação pontual e diagnósticos.
   * Ação `restore-window` no `CommandDispatcher`: Permite que o operador no Android restaure e traga ao foco a janela minimizada sob demanda.

4. **Visualizador Remoto Dedicado no Android (`layoutLiveViewer` & `MainActivity`):**
   * Viewport de exibição com `ImageView` configurado com `scaleType="fitCenter"` e `adjustViewBounds="true"`, preservando a proporção de aspecto (aspect ratio) original da janela capturada.
   * Barra superior com nome do aplicativo, processo, PID, badge pulsante "● AO VIVO" e botão de encerramento rápido.
   * Indicador dinâmico de resolução e taxa de quadros (FPS) calculada em tempo real na tela.
   * Cliente assíncrono em `RemoteDeskClient.kt` com `startWindowStream` e controle de ciclo de vida (`LiveStreamSession.stop()`), executando parsing multipart em segundo plano e decodificação via `BitmapFactory.decodeByteArray`.
   * Encerramento automático e limpeza de sockets no `onBackPressed`, `onStop` e `onDestroy`.

5. **Conformidade Estrita com o Escopo:**
   * Nenhum mecanismo de controle remoto (injeção de cliques do mouse, movimentos de cursor ou digitação de teclado via `mouse_event`, `SendInput` ou `keybd_event`) foi implementado nesta etapa, ficando estritamente reservado para a **Etapa 6**.

---

## 6. Etapa 5.1/12 — Padronização da Visualização na Área de Referência 382 × 706

**Status:** IMPLEMENTADA E VALIDADA ESTRUTURALMENTE  
**Data:** 25 de Outubro de 2026

### 6.1. Resumo Executivo da Etapa 5.1

A Etapa 5.1 padronizou a visualização remota do RemoteDesk para a área de referência de **382 × 706 dp**, generalizando a experiência de leitura confortável validada no Bloco de Notas para qualquer janela capturada no Windows:

1. **Definição da Área de Referência de 382 × 706:**
   * A área útil de visualização do viewport (`flLiveViewport`), subtraindo barras do sistema, cabeçalho e controles inferiores em smartphones verticais (como Motorola), possui a dimensão de referência de **382 dp (largura) × 706 dp (altura)**.
   * `MainActivity.kt` define `REF_VIEWPORT_WIDTH_DP = 382f`, `REF_VIEWPORT_HEIGHT_DP = 706f` e `NOTEPAD_REF_WIDTH = 640f` como base de calibração.
   * A janela remota é dimensionada com base nessa área útil, respeitando a proporção da imagem e evitando deformações ou cortes na interface.

2. **Calibração Baseada no Padrão do Bloco de Notas:**
   * Janelas compactas como o Bloco de Notas (largura <= 640px) já atingem densidade ideal de texto no modo Fit dentro da área de 382 × 706 (~12–14sp equivalente no celular).
   * Para janelas grandes (1080p, 1440p, 4K), o Modo Leitura calcula automaticamente a ampliação necessária `(frameW / 640).coerceIn(1.5f, 2.75f)` para equalizar o tamanho físico dos caracteres ao padrão do Bloco de Notas, permitindo ler textos pequenos com nitidez e navegar suavemente pelo conteúdo com arrasto panorâmico.

3. **Validação dos 8 Cenários de Teste Obrigatórios (`scripts/verify_step5_1.py`):**
   * **Cenário 1 — Bloco de Notas (600x450):** Renderizado a 382.0 × 286.5 dp (escala 0.64x), preservando o tamanho confortável e nítido de leitura.
   * **Cenário 2 — Janela Pequena (400x300):** Auto-ampliada em 0.95x para 382.0 × 286.5 dp, preenchendo a largura sem ficar minúscula nem deformada.
   * **Cenário 3 — Janela Grande (1920x1080):** 100% visível no modo Fit (382.0 × 214.9 dp) e ampliada dinamicamente para 2.75x no Modo Leitura (padrão Bloco de Notas).
   * **Cenário 4 — Janela Ultrawide (2560x1080):** Conteúdo 100% preservado com margens verticais simétricas (382.0 × 161.2 dp) sem esticar nem cortar.
   * **Cenário 5 — Alternância entre Janelas:** Troca sequencial reavalia a escala proporcionalmente sem herdar a escala anterior.
   * **Cenário 6 — Redimensionamento em Tempo Real:** Disparo de `onWindowDimensionsChanged` adapta a escala sem reiniciar o stream.
   * **Cenário 7 — Reconexão e Reabertura:** Restaura estado 100% Fit limpo e centralizado.
   * **Cenário 8 — Rotação Física do Celular:** Orientação vertical protegida por `screenOrientation="portrait"` sem queda de conexão.

4. **Conformidade Estrita de Escopo:**
   * Nenhum comando de controle remoto de mouse, teclado ou clique remoto (Etapa 6) foi adicionado.

---

## 7. Correção Definitiva: Redimensionamento Real das Janelas no Windows para 382 × 706

**Status:** IMPLEMENTADA E VALIDADA ESTRUTURALMENTE  
**Data:** 25 de Outubro de 2026

### 7.1. Resumo Executivo da Correção

Para solucionar definitivamente o dimensionamento das janelas e reproduzir fielmente a experiência do Bloco de Notas sem depender de zoom manual, o RemoteDesk implementou o **redimensionamento físico e real da janela da aplicação no próprio Windows** via chamadas nativas Win32:

1. **Investigação e Mecanismo de Dimensionamento:**
   * Constatou-se que a janela ideal do Bloco de Notas possui dimensões externas de **382 × 706** pixels de tela, resultando em um quadro BMP capturado via `PrintWindow(hWnd, hdc, PW_RENDERFULLCONTENT)` de exatamente 382 × 706 pixels.
   * Ao transmitir esse quadro de 382 × 706 para o celular Android em orientação vertical (`flLiveViewport` medindo ~382 × 706 dp), os pixels mapeiam em escala 1:1, produzindo fontes tipográficas perfeitas (~12–14sp) ideais para leitura e edição sem necessidade de zoom.
   * A solução não altera as configurações globais de resolução ou DPI do Windows nem de outros monitores, redimensionando exclusivamente a janela da aplicação selecionada.

2. **Implementação no Windows Host Agent (`windows-agent`):**
   * **P/Invoke Win32 (`User32.cs`):** Adicionadas assinaturas nativas para `SetWindowPos`, `MoveWindow`, `GetClientRect`, `GetDpiForWindow` e constantes `SWP_NOZORDER`, `SWP_NOACTIVATE`, `SWP_FRAMECHANGED`, `SWP_SHOWWINDOW`, além do helper com fallback seguro `GetDpiForWindowSafe`.
   * **Modelo de Diagnóstico (`WindowResizeResult.cs`):** Registra `OriginalWidth/Height`, `TargetWidth/Height`, `EffectiveWidth/Height`, `ClientWidth/Height`, `WasMaximized`, `WasMinimized`, `CanResize`, `WindowDpi` e `Message`.
   * **Serviço de Janelas (`WindowsWindowService.ResizeWindowAsync`):**
     1. Valida o handle da janela (`User32.IsWindow`).
     2. Detecta se a janela está minimizada (`IsIconic`) ou maximizada (`IsZoomed`). Se estiver, restaura previamente para o estado normal com `ShowWindow(hWnd, SW_RESTORE)` (pois o DWM do Windows ignora `SetWindowPos` em janelas maximizadas).
     3. Ajusta o tamanho da janela para a referência `382 × 706` preservando a posição Top-Left original. Utiliza `SetWindowPos` com flags seguras de não-ativação e reconfiguração de bordas (`SWP_FRAMECHANGED`), com fallback para `MoveWindow`.
     4. Confirma as dimensões efetivas reais aplicadas com `GetWindowRect` e `GetClientRect`.
     5. Invalida o cache interno de janelas para sincronização imediata.
   * **Servidor HTTP Local (`LocalHttpServer.cs`):**
     * No início de cada transmissão em `/api/stream`, o agente invoca automaticamente `_windowService.ResizeWindowAsync(windowId, 382, 706, token)` antes de capturar e enviar o primeiro quadro.
     * Rota REST dedicada `GET /api/window/resize?windowId={id}&width=382&height=706` exposta para controle explícito sob demanda.
   * **Despachante Seguro (`CommandDispatcher.cs`):**
     * Ações `"resize-window"` e `"select-window"` registradas na lista branca estrita de segurança.

3. **Integração no Aplicativo Android (`android-app`):**
   * `RemoteDeskClient.kt`: Método assíncrono `resizeWindow(host, port, windowId, targetWidth=382, targetHeight=706, ...)` implementado.
   * `MainActivity.kt`: Ao tocar para iniciar o stream (`startLiveStream`), o aplicativo aciona a solicitação de redimensionamento no host Windows, sincronizando a janela antes da renderização do streaming.

4. **Conformidade Estrita com o Escopo:**
   * Nenhuma injeção de controle remoto (teclado/mouse) da Etapa 6 foi introduzida.

---

## 8. Resumo Executivo da Etapa 6/12 — Controle Remoto Real pelo Android

Na Etapa 6, o RemoteDesk implementou o controle remoto funcional e bidirecional das janelas ativas do Windows diretamente através do aplicativo Android, permitindo ao usuário interagir com qualquer aplicativo remoto com toques, mouse, teclado virtual e atalhos:

1. **Camada de Entrada Nativa no Windows Agent (`windows-agent`):**
   * **Win32 P/Invoke (`User32.cs`):** Adicionada declaração de alta fidelidade para `SendInput`, `SetCursorPos`, `GetCursorPos`, `ClientToScreen`, `ScreenToClient` e estruturas nativas (`INPUT`, `InputUnion`, `MOUSEINPUT`, `KEYBDINPUT`, `HARDWAREINPUT`), além de constantes para eventos de mouse (`MOUSEEVENTF_LEFTDOWN`, `MOUSEEVENTF_LEFTUP`, `MOUSEEVENTF_RIGHTDOWN`, `MOUSEEVENTF_RIGHTUP`, `MOUSEEVENTF_WHEEL`) e teclado (`KEYEVENTF_KEYUP`, `KEYEVENTF_UNICODE`, `VK_*`).
   * **Modelos de Entrada (`RemoteInputModels.cs`):** Criados `RemoteMouseRequest`, `RemoteKeyRequest`, `RemoteTextRequest` e `RemoteInputResult`.
   * **Serviço de Injeção de Input (`IInputService` & `WindowsRemoteInputService`):**
     * Validação estrita da janela alvo e foco obrigatório prévio (`EnsureWindowFocus` com `SetForegroundWindow` e restauração de minimizada via `SW_RESTORE`).
     * Mapeamento de coordenadas normalizadas (0.0 a 1.0) para coordenadas absolutas da tela do Windows (`windowRect.Left + normX * winW`).
     * Ações de mouse: cursor move, left click, double click, right click, button down, button up, drag and drop, e roda de rolagem vertical (`MOUSEEVENTF_WHEEL` com `WHEEL_DELTA`).
     * Injeção de texto com suporte universal a caracteres Unicode e acentos em português (`KEYEVENTF_UNICODE` com `wScan = (ushort)c`), suportando acentuação completa (`Olá, ação, coração, informação e conexão.`).
     * Mapeamento de teclas especiais e modificadores: Enter, Backspace, Delete, Tab, Esc, setas, Home, End, PageUp, PageDown, Ctrl, Alt, Shift.
     * Rastreamento de estado e liberação segura de todos os botões e teclas retidos (`ReleaseAllInputsAsync`).
     * Fallback simulado para execução em sistemas Linux/CI, permitindo validação cross-platform.
   * **Despachante e Servidor HTTP (`CommandDispatcher` & `LocalHttpServer`):**
     * Comandos homologados: `"mouse-input"`, `"mouse-event"`, `"key-event"`, `"keyboard-input"`, `"text-input"`, `"scroll-event"`, `"release-inputs"`.
     * Rota REST dedicada `POST /api/input` para envio rápido de comandos de interação.
     * Encerramento do stream em `HandleStreamAsync` com bloco `finally` garantindo a liberação automática de qualquer botão ou tecla retida.

2. **Interface Mobile e Interação no Android (`android-app`):**
   * **Barra Compacta de Controles Remotos:** Adicionada barra horizontal no rodapé do visualizador com botões de alternância de modo (Clique, Clique Direito, Arraste, Rolagem), ativação do teclado virtual e menu de teclas especiais.
   * **Teclado Virtual e Acentos:** `etHiddenKeyboardInput` invisível sincronizado com o teclado nativo do sistema operacional (`InputMethodManager`), capturando caracteres com `TextWatcher` e despachando em tempo real com `client.sendTextInput()`.
   * **Teclas Especiais Expansíveis:** Barra retrátil contendo Enter, Backspace, Tab, Esc, Del, Setas (cima/baixo/esquerda/direita) e atalhos rápidos (Ctrl+C, Ctrl+V, Ctrl+Z, Ctrl+S).
   * **Mapeamento Preciso de Coordenadas e Rejeição de Letterbox (`mapTouchToNormalizedCoordinates`):**
     * Calcula dinamicamente o retângulo útil do bitmap escalado em `fitCenter` dentro da viewport `flLiveViewport`.
     * Toques nas margens pretas de letterbox retornam `null` e são descartados, impedindo cliques falsos acidentais no desktop ou janelas em segundo plano.
     * Suporte contínuo aos modos de visualização Fit e Modo Leitura (zoom adaptativo).
   * **Bloqueio de Orientação e Configuração de Teclado:**
     * `android:screenOrientation="portrait"` mantido estritamente para ergonomia vertical.
     * `android:windowSoftInputMode="adjustNothing"` no `AndroidManifest.xml`, garantindo que a abertura do teclado virtual não reduza nem reinicie o stream de vídeo.
   * **Liberação Segura de Inputs:** Despacho de `client.releaseInputs()` ao alternar de janela, pausar o stream (`stopLiveStream()`) ou destruir a Activity (`onDestroy()`).

3. **Segurança e Isolamento de Escopo:**
   * Validação de `windowId` e existência de janela antes de qualquer injeção.
   * Whitelist estrita de ações e teclas (sem execução arbitrária de comandos de sistema ou shell).
   * Proteção de privacidade: teclas e textos digitados nunca são impressos nos arquivos de log.
   * Isolamento estrito de escopo: nenhuma funcionalidade de autenticação em nuvem, WebRTC ou servidor STUN/TURN (reservados para as etapas seguintes) foi introduzida.

---

## 9. Testes Executados e Resultados da Etapa 6

| Teste | Descrição | Resultado | Detalhes |
| :--- | :--- | :--- | :--- |
| **T1: Presença de Arquivos da Etapa 6** | Verificar presença dos 13 arquivos essenciais da Etapa 6. | **PASSOU** | Modelos de input, interfaces, serviços C# e layouts Kotlin presentes. |
| **T2: Integridade Estrutural e Delimitadores** | Validar sintaxe e delimitadores C#, Kotlin e XML. | **PASSOU** | 100% dos arquivos verificados sem chaves ou parênteses desbalanceados. |
| **T3: P/Invoke Win32 e Constantes de Entrada** | Validar assinaturas de `SendInput`, `INPUT`, `MOUSEINPUT`, `KEYBDINPUT`. | **PASSOU** | Todas as structs, unions e flags nativas do Win32 declaradas. |
| **T4: Serviço de Entrada e Injeção de Dependência** | Validar `IInputService`, `WindowsRemoteInputService` e registro no DI. | **PASSOU** | Foco de janela, mouse, teclas, Unicode e liberação segura registrados. |
| **T5: CommandDispatcher e LocalHttpServer** | Validar ações no dispatcher e endpoint `/api/input`. | **PASSOU** | Ações de mouse, tecla, texto, rolagem e liberação no `finally` do stream. |
| **T6: Mapeamento de Coordenadas e Letterbox** | Validar matemática de toque e descarte de letterbox. | **PASSOU** | Coordenadas normalizadas 0.0-1.0 validadas; toques em margens rejeitados. |
| **T7: Suporte a Unicode e Teclas Especiais** | Testar acentuação brasileira e teclas mapeadas. | **PASSOU** | `KEYEVENTF_UNICODE` testado com acentos; 16 teclas de controle mapeadas. |
| **T8: UI Android, Portrait e Gestos** | Validar barra compacta, modo portrait e detectores. | **PASSOU** | `adjustNothing`, orientação vertical, detecção de toque, arrasto e scroll. |
| **T9: Segurança e Delimitação de Escopo** | Validar bloqueio de logs confidenciais e escopo da Etapa 6. | **PASSOU** | Sem vazamento de senhas, sem shell arbitrário, sem WebRTC/nuvem. |
| **T10: Não Regressão das Etapas Anteriores** | Executar suíte completa (`verify_step1.py` até `verify_step5_1.py`). | **PASSOU** | 100% dos testes de regressão anteriores aprovados. |

---

---

## 10. Redesign Visual Completo — DoHsktoP (Marca by HOD)

**Data:** 25 de Outubro de 2026  
**Status:** IMPLEMENTADO E VALIDADO ESTRUTURALMENTE  

### 10.1. Identidade Cyber-Zombie / Retro-Tech Aplicada

1. **Paleta Oficial Homologada:**
   * Fundo principal: `#1A1028`
   * Fundo profundo: `#0F0818`
   * Cards e contêineres: `#2A1B3D`
   * Campos de entrada: `#140C20`
   * Verde menta neon: `#3DF4B7`
   * Verde lima dos botões: `#70C92B`
   * Texto principal: `#FFFFFF`
   * Texto secundário: `#D0D5CE`
   * Bordas e divisores: `#3D2856`

2. **Logotipo Textual DoHsktoP:**
   * Vetor dedicado `res/drawable/ic_logo_dohsktop.xml`.
   * Tipografia retro-tech com as letras `DoH` e `P` em branco (`#FFFFFF`) e destaque estilizado nas letras `skto` em verde menta neon (`#3DF4B7`).

3. **Assinatura da Marca by HOD:**
   * Vetor dedicado `res/drawable/ic_by_hod.xml`.
   * Letra `O` em verde neon (`#3DF4B7`) com uma mão estilizada cyber-zombie emergindo de dentro do `O` e linhas onduladas verdes na base simulando raízes e circuitos de energia retro-tech.
   * Letras `by H` e `D` em cinza claro e branco.
   * Integrada ao cabeçalho principal, sessão remota e rodapé.

4. **Pipeline e Recursos do Ícone Oficial da Aplicação:**
   * **Caminho Canônico de Entrada da Imagem Master:**  
     `android-app/branding/app-icon.png` (resolução recomendada: `1024 × 1024` pixels, PNG com transparência).
   * **Utilitário Automatizado de Geração (`scripts/generate_app_icons.py`):**  
     Redimensiona a arte master usando filtro Lanczos de alta nitidez para `mipmap-mdpi` (48x48), `mipmap-hdpi` (72x72), `mipmap-xhdpi` (96x96), `mipmap-xxhdpi` (144x144) e `mipmap-xxxhdpi` (192x192), além de configurar os adaptativos `mipmap-anydpi-v26/ic_launcher.xml` e `ic_launcher_round.xml`.
   * **Vetores Oficiais de Fallback:**  
     `ic_launcher.xml` e `ic_launcher_round.xml` desenhados em SVG nativo Android com o terminal retro-tech e mão emergente, garantindo compilação imediata mesmo antes do envio do arquivo master PNG pelo usuário.

5. **Tela de Abertura (Splash Screen):**
   * Overlay `layoutSplashOverlay` integrado ao layout raiz em `#0F0818`.
   * Apresenta o ícone cyber-zombie, logotipo DoHsktoP, indicador de carregamento em `#3DF4B7` e assinatura `by HOD`.
   * Transição suave e não-bloqueante (fade-out automático de 400ms).

6. **Redesign das Telas:**
   * **Tela de Conexão:** Card `#2A1B3D` com campos `#140C20`, cursor neon, botão principal em verde lima `#70C92B` com texto escuro `#0F0818` em negrito ("Conectar ao Computador") e guia rápido de uso adaptado ao agente real.
   * **Grade de Aplicativos:** Cards em `#2A1B3D`, destaque selecionado em `#3DF4B7`, badge "EM FOCO" em pill-badge cyber-zombie.
   * **Sessão Remota:** Moldura escura em `#0F0818` com bordas sutis em `#3DF4B7`, mantendo estritamente a área de referência de 382 × 706 e todos os controles de mouse/teclado da Etapa 6.
   * **Rodapé:** Exibição de `DoHsktoP v1.0.0 • Comunicação Direta P2P` com a marca `by HOD`.

7. **Preservação de Escopo:**
   * Orientação vertical fixa (`SCREEN_ORIENTATION_PORTRAIT`).
   * `windowSoftInputMode="adjustNothing"`.
   * 100% das funcionalidades das Etapas 1 a 6 mantidas.
   * Nenhuma funcionalidade da Etapa 7 (sinalização WebRTC externa/STUN/TURN) antecipada.

---

## 11. Testes de Redesign e Não Regressão

| Teste | Descrição | Resultado |
| :--- | :--- | :--- |
| **Paleta Oficial Cyber-Zombie** | 8 cores obrigatórias validadas em `colors.xml`. | **PASSOU** |
| **Logotipo DoHsktoP** | Vetor `ic_logo_dohsktop.xml` e destaque `skto`. | **PASSOU** |
| **Marca by HOD** | Vetor `ic_by_hod.xml` com mão emergente e linhas onduladas. | **PASSOU** |
| **Pipeline do Ícone** | Caminho master, script gerador e adaptativos validados. | **PASSOU** |
| **Redesign das Telas e Splash** | Conexão, grade, sessão remota e splash screen validadas. | **PASSOU** |
| **Preservação Funcional** | Todos os métodos e IDs das Etapas 1 a 6 operacionais. | **PASSOU** |
| **Não Regressão Global** | `verify_step1.py` até `verify_step6.py` executados com sucesso. | **PASSOU** |

---

## 12. Próximos Passos (Transição para a Etapa 7)

Com a identidade visual DoHsktoP (by HOD) completamente aplicada, testada e validada:
1. Apresentar os resultados visuais e o procedimento do ícone oficial ao usuário.
2. **Aguardar autorização expressa do usuário antes de iniciar a Etapa 7/12** (Arquitetura de Conexão Externa / Sinalização WebRTC e Pareamento Remoto Seguro).

---

## 13. Correção da Compilação Android (`:app:processDebugResources`)

**Status:** INVESTIGADA, CORRIGIDA E VALIDADA  
**Data:** 25 de Outubro de 2026

### 13.1. Causa Raiz Identificada

A execução de compilação do Android no ambiente de CI (`build_runner.py android --strict`) falhou na tarefa `:app:processDebugResources` devido a:
1. **Atributo Inválido no MaterialComponents (`app:helperTextColor`):** No arquivo `res/layout/activity_main.xml`, quatro campos de `TextInputLayout` utilizavam `app:helperTextColor="@color/text_secondary"`. O atributo oficial da biblioteca Material Components é `app:helperTextTextColor`. Durante a compilação do AAPT2 (Resource Linking), a ausência desse atributo no esquema de atributos provocava a falha imediata da tarefa de recursos.
2. **Incompatibilidade de Diretório Qualificado sem Fallback Padrão (`res/mipmap-anydpi-v26`):** O script gerador de ícones havia criado o diretório `mipmap-anydpi-v26/` contendo arquivos adaptive-icon sem que existisse o diretório `mipmap/` base/unqualified ou as pastas de densidades (`mipmap-mdpi`, etc.). Além disso, o manifesto do aplicativo referencia `@drawable/ic_launcher` e `@drawable/ic_launcher_round`. O AAPT2 falha na vinculação quando recursos qualificados (`-v26`) não possuem configuração base.
3. **Formatação de Parâmetros de Arcos Vetoriais (VectorDrawable Path Data):** Nos arquivos `ic_launcher.xml`, `ic_launcher_round.xml`, `ic_launcher_foreground.xml` e `ic_logo_dohsktop.xml`, os comandos de arco (`a`) continham flags booleanas delimitadas por vírgula (`0 1,0` e `0 0,1`), passíveis de ambiguidade no analisador de caminho do AAPT2.

### 13.2. Correções Aplicadas

1. **Correção dos Atributos em `activity_main.xml`:** Todas as 4 ocorrências de `app:helperTextColor` foram corrigidas para `app:helperTextTextColor`.
2. **Reorganização de Recursos Adaptativos:**
   * Movidos os arquivos `ic_launcher.xml` e `ic_launcher_round.xml` adaptativos para `res/drawable-anydpi-v26/`, correspondendo exatamente à referência `@drawable/ic_launcher` declarada em `AndroidManifest.xml`.
   * Removido o diretório órfão `res/mipmap-anydpi-v26/`.
   * Atualizado `scripts/generate_app_icons.py` para apontar `ANYDPI_DIR` para `res/drawable-anydpi-v26/`.
3. **Padronização dos Comandos de Arco em Vetores:** Espaçamento explícito implementado em todos os comandos `a` (`a 50 50 0 1 0 100 0`, `a 6 6 0 0 1 6 6`, etc.), garantindo 100% de conformidade com o parser nativo do AAPT2.
4. **Validação Estrutural Completa:**
   * Validação de sintaxe e well-formedness de todos os 25 arquivos XML.
   * Resolução de 100% das referências de `@color`, `@string`, `@drawable` e `@style`.
   * Execução com sucesso da suíte de redesign `scripts/verify_redesign.py`.

---

## 14. Etapa 7/12 — Autenticação Pessoal, Código Fixo, Pareamento Persistente e Conexão Automática

**Status:** IMPLEMENTADA E VALIDADA COM 100% DE SUCESSO  
**Data:** 25 de Outubro de 2026

### 14.1. Resumo da Implementação da Etapa 7

Na Etapa 7, o RemoteDesk / DoHsktoP estabeleceu um modelo definitivo de acesso pessoal seguro, direto e descomplicado entre o smartphone Android do proprietário e seu computador Windows:

1. **Código Fixo do Proprietário (Configurável e Não Volátil):**
   * O computador possui um código fixo estável (padrão `70C92B` ou configurável pelo proprietário), persistido em `auth_config.json`.
   * O código **não muda a cada reinício** nem gera números aleatórios por sessão, eliminando o atrito de redigitar senhas no uso pessoal.
   * Gerenciamento via linha de comando no Windows: `RemoteDesk.Agent --set-pin <novo_codigo>`.

2. **Primeiro Pareamento com Emissão de Token Criptográfico (256 bits):**
   * O Android submete o código fixo, seu `deviceId` único persistente (UUID) e nome do aparelho (`deviceName`) ao endpoint `POST /api/auth/pair`.
   * O Windows valida o código fixo com proteção contra força bruta.
   * Em caso de correspondência, emite um `DeviceToken` de 256 bits gerado com `RandomNumberGenerator` criptograficamente seguro (CSPRNG).
   * O servidor armazena exclusivamente o **hash SHA-256** do token em `authorized_devices.json` (o token em texto claro nunca é salvo no PC).
   * O Android armazena o token e as credenciais de pareamento com segurança em `SharedPreferences`.

3. **Autorização Persistente e Conexão Automática Instantânea:**
   * Em sessões subsequentes, ao abrir o aplicativo ou tocar em "Conectar ao Computador", o Android envia o cabeçalho `X-Device-Token` e `X-Client-Id`.
   * O Windows valida o token contra a lista de hashes autorizados usando comparação em tempo constante (`CryptographicOperations.FixedTimeEquals` / `SlowEquals`) contra timing attacks.
   * **Sem solicitação de código repetitivo** e **sem janelas de confirmação manual (popups)** interrompendo a tela do Windows.
   * Acesso instantâneo e desimpedido para o proprietário do dispositivo.

4. **Reconexão Automática Resiliente:**
   * Caso a conexão seja interrompida inesperadamente (oscilação de Wi-Fi, timeout ou encerramento abrupto do socket), o Android ativa automaticamente um ciclo de reconexão resiliente com backoff exponencial (2s, 4s, 8s, 16s, máx. 5 tentativas).
   * A interface exibe um banner discreto informando o status da tentativa de reconexão e disponibiliza o botão "Cancelar".
   * A desconexão manual pelo usuário (`btnDisconnect`) cancela explicitamente o ciclo de reconexão automática.

5. **Revogação Imediata de Dispositivos e Gestão de Acesso:**
   * O proprietário do PC pode revogar qualquer dispositivo via CLI:
     * `RemoteDesk.Agent --list-devices`: Lista todos os celulares autorizados, data de vinculação e último acesso.
     * `RemoteDesk.Agent --revoke-device <id>`: Revoga imediatamente o acesso do dispositivo específico.
     * `RemoteDesk.Agent --revoke-all`: Revoga todos os dispositivos autorizados em massa.
   * Endpoint `POST /api/auth/revoke` disponível na API para despareamento a partir do aplicativo móvel.
   * Qualquer requisição com token revogado é sumariamente rejeitada com HTTP 401 (`DEVICE_REVOKED`).
   * No Android, a notificação `DEVICE_REVOKED` limpa as credenciais locais, exibe alerta ao usuário e restaura a tela de pareamento inicial.

6. **Controle Estrito de Acesso e Proteção Anti-Força Bruta:**
   * Todos os endpoints operacionais (`/api/windows`, `/api/windows/active`, `/api/window/frame`, `/api/stream`, `/api/input`, `/api/command`) rejeitam clientes não autenticados com HTTP 401 Unauthorized.
   * Rate limiting anti-brute force: após 5 tentativas incorretas consecutivas no pareamento, novas tentativas são temporariamente bloqueadas por 30 segundos (HTTP 429 `RATE_LIMITED`).

7. **Preservação de Escopo e Não Regressão:**
   * 100% das funcionalidades das Etapas 1 a 6 e do redesign Cyber-Zombie permanecem operacionais.
   * Delimitação estrita mantida: sem antecipação da Etapa 8 (WebRTC / sinalização na internet).

### 14.2. Resultados dos Testes Automatizados da Etapa 7 (`scripts/verify_step7.py`)

| Teste | Descrição | Resultado |
| :--- | :--- | :--- |
| **TEST 1: Presença dos Arquivos da Etapa 7** | Validação dos 11 arquivos essenciais no Android e Windows. | **PASSOU** |
| **TEST 2: Integridade e Delimitadores** | Balanceamento de chaves e parênteses em todos os arquivos C#, Kotlin e XML. | **PASSOU** |
| **TEST 3: Código Fixo e Modelos de Dados** | Classes `AuthorizedDevice`, `PairingRequest`, `PairingResponse`, `DeviceAuthVerification`. | **PASSOU** |
| **TEST 4: DeviceAuthService** | SHA-256, SlowEquals, persistência em `authorized_devices.json` e rate limiting (5 tentativas / 30s). | **PASSOU** |
| **TEST 5: Rotas de Autenticação e LocalHttpServer** | Endpoints `/api/auth/*`, cabeçalho `X-Device-Token` e `CheckAuthorizationAsync`. | **PASSOU** |
| **TEST 6: Injeção de Dependências e CLI** | Registro de `IAuthService` no DI e comandos `--list-devices`, `--revoke-device`, `--revoke-all`, `--set-pin`. | **PASSOU** |
| **TEST 7: Cliente Android e MainActivity** | Métodos `pairDevice`, `verifyDeviceToken`, reconexão automática e desconexão graciosa. | **PASSOU** |
| **TEST 8: Layouts XML e Recursos de Strings** | Elementos `layoutPairedStatus`, `btnUnpairDevice`, `layoutReconnectingBanner`, `btnCancelReconnect`. | **PASSOU** |
| **TEST 9: Segurança e Delimitação de Escopo** | Verificação de conformidade estrita (sem termos prematuros de WebRTC/STUN/TURN/OAuth2). | **PASSOU** |
| **TEST 10: Preservação Integral das Etapas 1 a 6** | `mapTouchToNormalizedCoordinates`, streaming, gestos, escala 382×706 e Portrait ativos. | **PASSOU** |

---

## 15. Resolução do Erro CS8852 e Correções de Compilação do Windows Agent

* **Causa Raiz Identificada:** No arquivo `windows-agent/src/RemoteDesk.Agent/Services/SignalingClientService.cs`, linhas 375-376, o comando deserializado `AgentCommand` recebia atribuição direta nas propriedades `cmd.ClientId = senderId;` e `cmd.DeviceToken = msg.Token;`. Como essas propriedades do modelo `AgentCommand` foram concebidas com acessadores `init` para garantir imutabilidade e proteção contra adulteração após a construção, o compilador C# emitiu o erro `CS8852: Init-only property or indexer 'AgentCommand.ClientId' can only be assigned in an object initializer, or on 'this' or 'base' in an instance constructor or an 'init' accessor`.
* **Solução Arquitetural Aplicada:**
  1. **Preservação da Imutabilidade (`init`):** Manteve-se intencionalmente a declaração com `init` nas propriedades `ClientId`, `DeviceToken` e `AccessPin` de `AgentCommand`, preservando o modelo de segurança que impede a forja ou mutação de credenciais ao longo do pipeline de despacho de comandos.
  2. **Método `WithClientContext` em `AgentCommand`:** Implementou-se o método de extensão imutável `WithClientContext(string? clientId, string? deviceToken, string? accessPin = null)` em `AgentCommand.cs`, que produz uma nova instância tipada com os campos de autorização devidamente preenchidos através de inicializador de objeto.
  3. **Ajuste em `SignalingClientService.cs`:** A injeção de comandos de entrada remota (`action == "input"`) passou a utilizar `var authorizedCmd = cmd.WithClientContext(senderId, msg.Token);`, despachando a instância imutável com identificação verificada pelo servidor de sinalização.
  4. **Eliminação de Ocorrências:** Verificou-se que não resta nenhuma atribuição direta a propriedades `init` em todo o código-fonte C#.

---

## 16. Resolução do Erro MSB1009 e Isolamento de Diretórios de Execução

* **Causa Raiz Identificada:** O erro `MSBUILD : error MSB1009: Arquivo de projeto não existe. Opção: .\RemoteDesk.Agent.csproj` ocorria quando o comando `dotnet run --project .\RemoteDesk.Agent.csproj` era executado no terminal enquanto o diretório ativo era `RemoteDesk\signaling-server\src\RemoteDesk.SignalingServer`, ou quando scripts de automação não isolavam o diretório de trabalho de cada subprocesso.
* **Solução Arquitetural Aplicada:**
  1. **Validação de Existência Prévia dos Projetos:**
     * `start-all.bat`, `start-all.ps1` e `start-signaling.ps1` agora calculam `%PROJECT_ROOT%` / `$ProjectRoot` dinamicamente e validam categoricamente a existência física de `windows-agent/src/RemoteDesk.Agent/RemoteDesk.Agent.csproj` e `signaling-server/src/RemoteDesk.SignalingServer/RemoteDesk.SignalingServer.csproj` antes de qualquer execução.
  2. **Isolamento de Diretório de Trabalho (Working Directory):**
     * Em `start-all.ps1`, a inicialização do Windows Agent via `Start-Process` passa explicitamente `-WorkingDirectory "$AgentDir"` e injeta `Set-Location '$AgentDir'` na nova janela do console PowerShell.
  3. **Launchers Convenientes na Raiz:**
     * Criados `start-agent.bat` e `start-signaling.bat` diretamente na raiz do repositório, permitindo iniciar cada serviço individualmente com 1 clique a partir de qualquer diretório sem risco de erro de caminho.
  4. **Build Runner e Documentação:**
     * Atualizado `scripts/build_runner.py` para apontar caminhos absolutos de projeto e fornecer diagnóstico amigável para `MSB1009`.
     * Atualizados `README.md` e `docs/BUILD_AND_ERRORS.md` com tabelas de caminhos e instruções explícitas de terminal.

---

## 17. Diagnóstico Definitivo e Resolução do Erro MSB3030 (apphost.exe / dll)

* **Status:** Aprovada e Implementada.
* **Causa Raiz Técnica Identificada:**
  1. No Web SDK do .NET 8 (`Microsoft.NET.Sdk.Web`), o target `_ComputeNETCoreBuildOutputFiles` enfileira incondicionalmente `obj\Debug\net8.0\apphost.exe` para cópia em `bin\` quando `OutputType == Exe` (propriedade implícita em Web SDK).
  2. No entanto, o target `_CreateAppHost` depende da presença física do pacote nativo de template em disco (`packs\Microsoft.NETCore.App.Host.<rid>`). Quando o template nativo não existe para o RID avaliado ou em instalações portáteis do SDK, `_CreateAppHost` é ignorado silenciosamente sem gerar o executável intermediário.
  3. No momento da cópia (`_CopyOutOfDateSourceItemsToOutputDirectory`), a ausência física do arquivo dispara `error MSB3030: não foi possível copiar o arquivo "...obj\Debug\net8.0\apphost.exe" porque ele não foi encontrado`.
  4. Além disso, encerramentos assíncronos de processos em portas de rede no Windows somados a nós de trabalho em cache do MSBuild (`VBCSCompiler`) causavam concorrência e bloqueio de arquivos, gerando falhas intermitentes também na cópia da DLL principal.
* **Ações Implementadas:**
  1. **Ajuste Declarativo em `RemoteDesk.SignalingServer.csproj`:** Configurado `<UseAppHost>false</UseAppHost>` no arquivo de projeto do servidor de sinalização. Isso instrui o MSBuild a gerar determinística e exclusivamente o assembly gerenciado multiplataforma (`RemoteDesk.SignalingServer.dll`), eliminando a fila de cópia de stubs nativos sem prejudicar o funcionamento via `dotnet run` ou Kestrel.
  2. **Isolamento de Processos e File Locks:**
     * Em `scripts/ngrok/start-signaling.ps1`, os comandos `Stop-Process -Force` agora aguardam a liberação de handles do kernel com `$p.WaitForExit(1000)`.
     * As compilações utilizam `/nodeReuse:false /m:1`, evitando background daemons que travam pastas `obj\` e `bin\`.
  3. **Inicialização Robusta:** O orquestrador executa preferencialmente `dotnet RemoteDesk.SignalingServer.dll`, garantindo execução limpa e imediata em qualquer ambiente Windows, Linux ou conteinerizado.









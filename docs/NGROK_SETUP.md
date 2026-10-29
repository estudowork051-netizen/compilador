# GUIA DE ACESSO REMOTO EXTERNO COM NGROK — REMOTEDESK

Este documento apresenta o guia completo, técnico e prático para disponibilizar o **RemoteDesk** pela internet via **ngrok**, permitindo o controle remoto do seu computador Windows a partir do aplicativo móvel Android em redes 4G/5G, **sem a necessidade de contratar ou configurar servidores VPS**.

---

## 1. VISÃO GERAL E ARQUITETURA DE REDE

### 1.1. O que o ngrok faz no RemoteDesk
* **Túnel Seguro de Sinalização (Porta 5000):** O ngrok cria uma ponte criptografada reversa (HTTPS/WSS) entre o endereço público do ngrok (`https://*.ngrok-free.app`) e o `RemoteDesk.SignalingServer` rodando na porta local `5000`.
* **Descoberta e Troca de Sessões:** O celular Android na rede celular (4G/5G) acessa o endpoint seguro do ngrok para registrar-se, listar hosts online e trocar as ofertas e respostas SDP (WebRTC) e ICE Candidates com o computador de casa.
* **Canal de Relé de Contingência (WebSocket Relay):** Caso a rede móvel utilize CGNAT estrito e a negociação P2P direta seja bloqueada, o tráfego de controle e os quadros de vídeo são transmitidos pelo canal de relé WebSockets do servidor de sinalização através do túnel ngrok.

### 1.2. O que o ngrok NÃO faz
* **Não expõe a porta 5050 (Segurança Preservada):** O servidor de controle local do Windows Agent (`LocalHttpServer`) na porta 5050 permanece estritamente restrito a `localhost`/rede local. Ele **NUNCA** é exposto ao túnel ngrok.
* **Não é um servidor TURN UDP:** O ngrok transporta tráfego HTTP/HTTPS e WebSockets TCP. Ele não encapsula portas UDP de mídia TURN de baixa camada.
* **Não substitui a autenticação do RemoteDesk:** O túnel apenas viabiliza a rota TCP; a autorização só é concedida mediante validação do PIN de acesso e tokens de sessão criptografados.

---

## 2. PRÉ-REQUISITOS E INSTALAÇÃO DO NGROK

### 2.1. Instalação no Windows
Você pode instalar o ngrok oficial de duas maneiras rápidas:

**Opção A — Pelo Gerenciador de Pacotes Windows (Recomendado):**
Abra o PowerShell ou Terminal do Windows e execute:
```powershell
winget install ngrok/ngrok
```

**Opção B — Download Direto:**
1. Baixe o executável para Windows em [ngrok.com/download](https://ngrok.com/download).
2. Extraia o `ngrok.exe` e mova-o para uma pasta no seu `PATH` (ex: `C:\Windows\System32` ou pasta de ferramentas).

### 2.2. Configuração do Authtoken Pessoal
1. Crie uma conta gratuita em [ngrok.com](https://ngrok.com).
2. Acesse seu painel e copie seu Authtoken pessoal em `Dashboard > Getting Started > Your Authtoken`.
3. No terminal do Windows, adicione o token ao cliente local:
```powershell
ngrok config add-authtoken SEU_AUTHTOKEN_AQUI
```
> **Aviso de Segurança:** Nunca compartilhe nem publique o seu Authtoken em repositórios Git, mensagens de chat ou logs públicos.

### 2.3. Dica: Domínio Estático Gratuito do ngrok
As contas gratuitas do ngrok oferecem **1 domínio estático gratuito** (ex: `meupc-remotedesk.ngrok-free.app`). Ao usá-lo, a URL pública nunca muda ao reiniciar o túnel, evitando que você precise reconfigurar a URL no aplicativo Android!
* Para reivindicar seu domínio gratuito, acesse: `ngrok Dashboard > Domains > Claim a static domain`.

---

## 3. SCRIPTS AUTOMATIZADOS DE EXECUÇÃO

O repositório possui uma suíte completa de automação na pasta `scripts/ngrok/`:

| Script | Finalidade |
| :--- | :--- |
| `start-all.bat` | **Iniciador em 1 clique:** Executa verificações, sobe servidor, abre túnel e exibe instruções de conexão. |
| `start-all.ps1` | Orquestrador PowerShell detalhado com suporte a parâmetros customizados. |
| `check-prerequisites.ps1` | Diagnostica a presença do .NET 8, ngrok no PATH, arquivo de configuração e disponibilidade da porta 5000. |
| `start-signaling.ps1` | Inicia o Servidor de Sinalização na porta 5000 e monitora o endpoint `/health`. |
| `start-tunnel.ps1` | Inicia o túnel `ngrok http 5000` e captura a URL pública via API local (`http://127.0.0.1:4040/api/tunnels`). |
| `verify-tunnel.ps1` | Testa a conectividade externa da URL do ngrok (endpoints `/`, `/health` e `/api/signaling/ice-servers`). |
| `stop-session.ps1` | Encerra os processos associados de forma limpa usando seus arquivos de PID. |

### 3.1. Como Iniciar Tudo Rapidamente
Basta dar um duplo clique em:
```text
scripts\ngrok\start-all.bat
```
Ou rodar no PowerShell:
```powershell
.\scripts\ngrok\start-all.ps1
```

Se você registrou um domínio estático gratuito no ngrok:
```powershell
.\scripts\ngrok\start-all.ps1 -Domain meupc-remotedesk.ngrok-free.app
```

### 3.2. Como Encerrar a Sessão
Para liberar as portas e parar os serviços:
```powershell
.\scripts\ngrok\stop-session.ps1
```

---

## 4. O SISTEMA DE PIN — COMO CONSULTAR, DEFINIR E RECUPERAR

O RemoteDesk exige um PIN de acesso para garantir que apenas o proprietário controle a máquina:

### 4.1. Como Consultar o PIN Atual
No terminal do Windows, execute:
```powershell
dotnet run --project windows-agent/src/RemoteDesk.Agent -- --pin
```
O console exibirá:
```text
[PIN ATIVO] O PIN configurado atualmente para este agente é: 70C92B
```

Além disso, sempre que o Windows Agent inicia, um destaque chamativo é impresso no console:
```text
======================================================================
  PIN DE ACESSO / PAREAMENTO: 70C92B <=== DIGITE ESTE PIN NO APP ANDROID
======================================================================
```

### 4.2. Como Definir um PIN Personalizado
Você pode escolher um PIN fixo de sua preferência (ex: `123456` ou `MEUPC1`):
```powershell
dotnet run --project windows-agent/src/RemoteDesk.Agent -- --set-pin 123456
```

### 4.3. Como Gerar um Novo PIN Aleatório
```powershell
dotnet run --project windows-agent/src/RemoteDesk.Agent -- --regenerate-pin
```

---

## 5. COMO CONECTAR PELO CELULAR (4G/5G)

1. **Deixe o computador de casa rodando o `start-all.bat`:**
   O console exibirá a URL pública, por exemplo: `https://abcd-123-45.ngrok-free.app`.
2. **No celular com dados móveis (4G ou 5G):**
   * Abra o aplicativo **DoHsktoP (RemoteDesk)**.
   * No campo **Servidor de Sinalização**, insira a URL completa:
     `https://abcd-123-45.ngrok-free.app`
     *(Não precisa se preocupar com portas; portas HTTPS 443 são resolvidas automaticamente).*
   * No campo **PIN de Pareamento / Acesso**, digite seu PIN (ex: `70C92B`).
   * Toque em **CONECTAR AO HOST**.
3. **Acesso Não Assistido (Unattended Access):**
   A conexão é estabelecida imediatamente, o pareamento persistente é registrado e a tela e lista de janelas são abertas no seu celular, sem precisar que ninguém em casa clique para aceitar!

---

## 6. CONTORNANDO A TELA DE AVISO DO NGROK (FREE-TIER)

A camada gratuita do ngrok injeta uma página HTML de aviso intermediário (*interstitial warning*) ao receber requisições de navegadores comuns.

Para garantir que a comunicação do aplicativo Android e do Windows Agent ocorra perfeitamente e sem bloqueios:
* O cliente Android injeta automaticamente os cabeçalhos:
  * `ngrok-skip-browser-warning: true`
  * `User-Agent: RemoteDesk-Android/1.0`
* O cliente de sinalização do Windows Agent inclui os mesmos cabeçalhos em suas conexões `ClientWebSocket`.
* A conexão TLS/SSL do Android trata o SNI (*Server Name Indication*) e aceita conexões na porta 443 com validação estrita de certificados da cadeia pública do ngrok.

---

## 7. RESOLUÇÃO DE PROBLEMAS (TROUBLESHOOTING)

| Problema | Causa Mais Provável | Solução |
| :--- | :--- | :--- |
| **Porta 5000 já em uso** | Instância anterior do servidor ainda aberta. | Execute `.\scripts\ngrok\stop-session.ps1` ou localize o processo com `Get-Process -Id (Get-NetTCPConnection -LocalPort 5000).OwningProcess | Stop-Process -Force`. |
| **ngrok Authtoken Pendente** | O ngrok salvou o arquivo em `%APPDATA%\ngrok\ngrok.yml` em vez de `%LOCALAPPDATA%`. | O script `check-prerequisites.ps1` foi atualizado para escanear ambos os caminhos e usar `ngrok config check` dinamicamente. Execute `ngrok config check` para confirmar. |
| **Erro MSB3030 (apphost.exe)** | Concorrência ou cache corrompido em `obj/Debug/net8.0`. | O script `start-signaling.ps1` agora compila previamente e executa diretamente a DLL gerenciada (`RemoteDesk.SignalingServer.dll`), limpando `obj` automaticamente se houver inconsistência. |
| **Erro 401 Unauthorized** | PIN digitado no Android difere do configurado no Agent. | Verifique o PIN correto rodando `dotnet run --project windows-agent/src/RemoteDesk.Agent -- --pin` e digite o valor exibido. |
| **Erro 404 ao conectar** | A URL do ngrok mudou ou expirou após reinício. | Se você não usa domínio estático, copie a nova URL gerada pelo `start-tunnel.ps1` e cole no app. Recomenda-se registrar o domínio estático gratuito do ngrok. |
| **Túnel offline / Erro 502** | O processo do Servidor de Sinalização caiu. | Verifique se o servidor na porta 5000 está saudável executando `Invoke-RestMethod http://127.0.0.1:5000/health`. |
| **Tela preta ou sem streaming P2P** | NAT simétrico restritivo na operadora móvel. | O RemoteDesk ativa automaticamente a contingência via canal de relé WebSockets (`relay-data`) através do túnel ngrok. |

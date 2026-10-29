# Planejamento Oficial de Evolução e Roadmap — RemoteDesk / DoHsktoP

Este documento consolida as etapas de evolução do RemoteDesk, registrando o estado de entrega das fases técnicas de infraestrutura e o planejamento formal das melhorias futuras de facilidade de uso, gerenciamento e administração.

---

## 1. Visão Geral das Etapas

| Etapa | Título | Estado | Foco Principal |
|---|---|---|---|
| **Etapa 1** | Fundação e Arquitetura | **CONCLUÍDA** | Estrutura de diretórios, padrões de compilação, documentos base. |
| **Etapa 2** | Comunicação Básica | **CONCLUÍDA** | Protocolo de comandos, Ping/Pong, endpoints HTTP essenciais. |
| **Etapa 3** | Captura de Tela Local | **CONCLUÍDA** | Captura de janelas Win32, codificação de quadros JPEG/PNG. |
| **Etapa 4** | Injeção de Entrada Remota | **CONCLUÍDA** | Mapeamento e injeção de eventos de mouse e teclado no Windows. |
| **Etapa 5** | Gestor de Janelas e Streaming | **CONCLUÍDA** | Lista de processos ativos, foco de janelas, streaming contínuo. |
| **Etapa 5.1** | Atalhos e Teclado Virtual | **CONCLUÍDA** | Teclas especiais (Ctrl, Alt, Win), atalhos de produtividade. |
| **Etapa 6** | Reconexão e Resiliência | **CONCLUÍDA** | Recuperação após desconexões breves, timeouts defensivos. |
| **Redesign** | Cyber-Zombie / Retro-Tech | **CONCLUÍDA** | Identidade visual DoHsktoP by HOD, viewport padronizado. |
| **Etapa 7** | Autenticação Pessoal e Token | **CONCLUÍDA** | Pareamento persistente, tokens criptográficos 256 bits, Keystore/DPAPI. |
| **Etapa 8** | Acesso Remoto pela Internet | **CONCLUÍDA** | Sinalização WebSockets, WebRTC, STUN/TURN (Coturn), suporte a 4G/5G/CGNAT. |
| **Etapa Futura** | Interface de Administração e Usabilidade | **[PLANEJADA / PENDENTE]** | Gestão de contas, serviços, dispositivos, inicialização/parada e usabilidade. |

---

## 2. Detalhamento da Etapa Futura Obrigatória

### **Etapa futura — Interface de Administração, Configuração e Usabilidade do RemoteDesk**
- **Estado Oficial:** **PLANEJADA / PENDENTE** *(Não concluída nesta fase)*
- **Objetivo Estratégico:** Transformar o RemoteDesk em uma plataforma autônoma, intuitiva e acessível para qualquer usuário, eliminando a necessidade de editar arquivos manuais de configuração (`appsettings.json`, `.env`), consultar logs brutos ou operar comandos de terminal para tarefas do dia a dia.

---

### 2.1. Funcionalidades Planejadas

#### A. Interface de Configuração Inicial e Contas
1. **Configuração da Conta do Usuário:**
   - Criação e login de conta pessoal para vincular com segurança os dispositivos do proprietário.
   - Associação de credenciais mestras protegidas por chave criptográfica derivada (PBKDF2/Argon2).
   - Separação estrita entre credenciais permanentes da conta e o PIN temporário de pareamento inicial.
   - Proteção de privacidade: a interface nunca revelará senhas ou chaves privadas já configuradas.
2. **Gerenciamento de Dispositivos Autorizados:**
   - Listagem gráfica de todos os dispositivos móveis (Android) e computadores (Windows) vinculados à conta.
   - Visualização do estado de cada dispositivo (Online, Ocupado, Offline, Último Acesso).
   - Botão de revogação imediata de acesso para celulares perdidos ou não mais utilizados.
   - Configuração de permissões de acesso (ex: somente visualização vs. controle total de mouse/teclado).
3. **Configuração Amigável de Endereços e Serviços:**
   - Interface com validação em tempo real para configurar o endereço do Servidor de Sinalização e servidores STUN/TURN.
   - Botão **"Testar Conexão"**: valida a conectividade com o servidor de sinalização e exibe o tempo de resposta (latência RTT).
   - Diagnóstico ICE automatizado: testa se o servidor TURN responde na porta 3478 e se o tráfego 4G/5G está liberado.

#### B. Gerenciamento Seguro dos Serviços
A interface administrativa permitirá consultar e atuar sobre o ciclo de vida dos componentes conforme as permissões do sistema operacional:

1. **Controle do Windows Host Agent:**
   - Visualizar o estado do agente (Executando, Parado, Em Sessão Ativa).
   - Iniciar o agente Windows sob demanda ou configurá-lo para inicialização automática com o sistema operacional.
   - Encerrar o agente com encerramento gracioso (*graceful shutdown*), liberando recursos de vídeo e fechando portas de rede.
   - Consultar o PIN de pareamento atual diretamente na janela do aplicativo, com botão de cópia rápida e opção de regenerar um novo PIN com 1 clique.
2. **Administração do Servidor de Sinalização e Relé:**
   - Consultar o status de saúde (`/health`) do backend na nuvem.
   - Exibir contadores operacionais: Hosts conectados, Clientes conectados, Sessões WebRTC ativas.
   - Visualização de logs operacionais filtrados (erros de autenticação, tentativas de força bruta bloqueadas), sem expor tokens ou segredos de usuários.
   - *Segurança Arquitetural:* Comandos administrativos remotos para servidores na nuvem (VPS) exigirão autenticação administrativa via API separada com token de alta entropia. O aplicativo Android não terá controle arbitrário sobre o VPS sem credenciais administrativas dedicadas.
3. **Administração do Servidor TURN/STUN (Coturn):**
   - Verificação de disponibilidade da porta 3478 UDP/TCP e estado das credenciais efêmeras.
   - Alerta visual caso o servidor TURN esteja inacessível, informando que clientes em redes 4G/5G restritivas podem ter dificuldades de conexão.

#### C. Usabilidade e Experiência do Usuário (UX/UI)
1. **Painel de Status Unificado (Dashboard):**
   - Indicador visual claro de estado:
     - 🟢 *Computador de Casa: Online e Pronto para Conexão*
     - 🟡 *Servidor de Sinalização: Conectado (STUN Ativo, TURN Aguardando)*
     - 🔴 *Computador Offline ou Desligado*
2. **Mensagens de Erro Humanizadas com Guias de Recuperação:**
   - Em vez de códigos técnicos genéricos (ex: `SocketException 10060`), a interface explicará o motivo exato e sugerirá ações práticas:
     - Exemplo: *"O computador parece estar desligado ou o Windows Agent foi fechado. Certifique-se de que o PC permaneça ligado."*
     - Exemplo: *"PIN incorreto. No computador, consulte o PIN atual na janela do agente ou execute 'RemoteDesk.Agent --pin'."*
3. **Consistência Visual DoHsktoP:**
   - A interface administrativa adotará a mesma identidade visual Retro-Tech / Cyber-Zombie (paleta verde menta `#00E5A3`, fundo escuro `#0A1413`, cards com borda fina e tipografia legível), mantendo harmonia com o aplicativo móvel.

---

### 2.2. Dependências Técnicas e Requisitos de Segurança
1. **Autenticação em Múltiplos Níveis:**
   - Separação rigorosa entre a autorização de pareamento local (PIN/Token DPAPI/Keystore) e as credenciais administrativas do servidor.
2. **APIs Administrativas Restritas:**
   - Todos os endpoints de administração no servidor de sinalização serão protegidos por chaves administrativas específicas com rate limiting rígido.
3. **Permissões do Windows:**
   - Operações como inicialização e parada de serviços Windows exigirão permissões administrativas locais adequadas (UAC).
4. **Isolamento de Segredos:**
   - Logs administrativos jamais registrarão senhas, chaves do TURN em texto claro ou tokens de sessão de usuários.

---

### 2.3. Critérios de Conclusão da Etapa Futura
Esta etapa somente será considerada concluída quando:
- A interface gráfica administrativa estiver implementada e funcional nos componentes previstos.
- O usuário conseguir configurar suas contas, PINs e servidores visualmente.
- O início e a parada dos serviços locais funcionarem de forma segura.
- O diagnóstico de STUN/TURN reportar erros com precisão.
- A suíte de testes de usabilidade e segurança for executada e aprovada com 100% de sucesso.

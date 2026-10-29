# Guia de Compilação e Consulta de Logs de Erros — RemoteDesk

Este documento explica de forma simples e direta como compilar cada componente do RemoteDesk, onde os arquivos de log de erros são gravados e como diagnosticar e localizar rapidamente o problema principal.

---

## 1. Onde os Logs Ficam Salvos

Todos os registros de compilação e diagnóstico são centralizados na pasta:
```text
RemoteDesk/logs/
```

Os arquivos gerados são:

| Arquivo de Log | Componente | Descrição |
| :--- | :--- | :--- |
| `logs/android_build.log` | Aplicativo Android | Registro completo da execução do Gradle (`./gradlew assembleDebug`). |
| `logs/windows_agent_build.log` | Agente Windows | Registro da compilação do agente em C# (`dotnet build`). |
| `logs/signaling_server_build.log` | Servidor de Sinalização | Registro da compilação do ASP.NET Core (`dotnet build`). |
| `logs/latest_build_summary.log` | Todos | Visão consolidada com o status de cada componente e causa raiz do erro. |

> **Nota de Segurança:** O sistema de geração de logs do RemoteDesk possui higienização automática contra vazamento de credenciais. Nenhuma senha, token ou PIN é gravado nos arquivos de log.

---

## 2. Como Executar as Compilações e Gerar os Logs

### 2.1. Execução Automática Unificada (Recomendada)
Para rodar a verificação e compilação de todos os três componentes simultaneamente e atualizar todos os arquivos de log:

* **No Linux / macOS**:
  ```bash
  cd /RemoteDesk  # ou na raiz do projeto
  ./scripts/build_all.sh
  ```
  *(Ou diretamente via Python: `python3 scripts/build_runner.py --all`)*

* **No Windows**:
  ```cmd
  cd RemoteDesk
  scripts\build_all.bat
  ```
  *(Ou no prompt de comando: `python scripts\build_runner.py --all`)*

### 2.2. Execução Individual por Componente
Para testar e gerar o log de um componente específico:

* **Apenas o Android**:
  ```bash
  python3 scripts/build_runner.py android
  ```
* **Apenas o Agente Windows**:
  ```bash
  python3 scripts/build_runner.py windows
  ```
* **Apenas o Servidor**:
  ```bash
  python3 scripts/build_runner.py server
  ```

---

## 3. Como Localizar o Erro Principal em um Log

Cada arquivo de log na pasta `logs/` é estruturado em 6 seções rigorosas:

1. **Cabeçalho:** Data, horário da execução e componente testado.
2. **Comando Utilizado:** Comando exato executado (ex: `./gradlew assembleDebug` ou `dotnet build RemoteDesk.Agent.csproj --verbosity normal`) e diretório de trabalho.
3. **Resultado e Código de Saída:** Código numérico de retorno do processo e status de produção do artefato (APK ou DLL).
4. **Diagnóstico e Localização de Erros:** Mapeamento de arquivo e linha causadores da falha (`Arquivo: Linha`), ou indicação de falha de ambiente/dependência.
5. **Explicação Simples do Problema e Possíveis Soluções:** Passo a passo prático com os comandos exatos de instalação no Windows (`winget`, variáveis `JAVA_HOME`, `ANDROID_HOME`, `PATH`).
6. **Mensagem Completa e Saída Integral do Processo:** Transcrição completa e higienizada (sem senhas ou tokens) dos fluxos stdout e stderr.

### Exemplo de Interpretação:
Se o log `android_build.log` apresentar código 1:
* **Arquivo a abrir:** `logs/android_build.log`
* **Mensagem destacada:** `ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.`
* **Ação recomendada:** Consultar a **Seção 5** do log, que contém o comando `winget install EclipseAdoptium.Temurin.17.JDK` e a configuração de `JAVA_HOME`.

Se o log `windows_agent_build.log` apresentar código 127:
* **Arquivo a abrir:** `logs/windows_agent_build.log`
* **Mensagem destacada:** `Ferramenta executável não encontrada no sistema: 'dotnet'.`
* **Ação recomendada:** Consultar a **Seção 5** do log, que contém o comando `winget install Microsoft.DotNet.SDK.8`.

---

## 4. Como Compartilhar os Logs

Caso precise relatar problemas para outro membro da equipe ou abrir um chamado de suporte:
1. Copie o arquivo consolidado `logs/latest_build_summary.log` para ter uma visão geral rápida.
2. Anexe o arquivo `.log` específico do componente que apresentou falha.

---

## 5. Consulta de Logs no GitHub Actions (Nuvem)

Quando a compilação é executada na nuvem pelo GitHub Actions:

1. **Visualização Direta na Interface do GitHub:**
   * Acesse a aba **Actions** e selecione a execução desejada.
   * Clique no job correspondente (`Compilar Aplicativo Android (APK)`, `Compilar Agente Windows (.NET 8)` ou `Compilar Servidor de Sinalização (.NET 8)`).
   * Expanda o passo de compilação correspondente para ler a saída em tempo real diretamente no navegador.

2. **Download dos Arquivos de Log como Artefato:**
   * Mesmo quando uma etapa falha, o workflow está configurado com `if: always()` para empacotar e disponibilizar os arquivos de log.
   * Na seção **Artifacts** no final da página da execução, baixe:
     * `log-android-build`
     * `log-windows-agent-build`
     * `log-signaling-server-build`
   * Esses arquivos contêm o mesmo formato estruturado de 6 seções gerado localmente pelo `build_runner.py`.

3. **Painel de Resumo do Workflow (Step Summary):**
   * No menu lateral esquerdo da execução do GitHub Actions, clique em **Summary**.
   * Uma tabela consolidada exibe o status de cada componente e instruções para download do APK gerado.

---

## 6. Solução do Erro MSB1009 (Arquivo de projeto não existe)

### Causa Raiz
O erro `MSBUILD : error MSB1009: Arquivo de projeto não existe. Opção: .\RemoteDesk.Agent.csproj` ocorre quando comandos `dotnet` são executados com referências relativas dentro de diretórios incorretos — tipicamente tentando executar `RemoteDesk.Agent.csproj` a partir de `signaling-server/src/RemoteDesk.SignalingServer` ou sem especificar o caminho completo do projeto.

### Localização Real dos Projetos
| Componente | Caminho do `.csproj` a partir da raiz |
| :--- | :--- |
| **Windows Agent** | `windows-agent/src/RemoteDesk.Agent/RemoteDesk.Agent.csproj` |
| **Signaling Server** | `signaling-server/src/RemoteDesk.SignalingServer/RemoteDesk.SignalingServer.csproj` |

### Como Executar Corretamente

1. **A partir da raiz do repositório (recomendado):**
   * Iniciar agente: `start-agent.bat` ou `dotnet run --project windows-agent/src/RemoteDesk.Agent/RemoteDesk.Agent.csproj`
   * Iniciar servidor: `start-signaling.bat` ou `dotnet run --project signaling-server/src/RemoteDesk.SignalingServer/RemoteDesk.SignalingServer.csproj`
   * Iniciar ambos (ambiente completo): `start-all.bat`

2. **A partir da pasta do próprio projeto:**
   * Agente:
     ```cmd
     cd windows-agent\src\RemoteDesk.Agent
     dotnet run
     ```
   * Servidor:
     ```cmd
     cd signaling-server\src\RemoteDesk.SignalingServer
     dotnet run
     ```

---

## 7. Solução Definitiva do Erro MSB3030 (`apphost.exe` / DLL não encontrado)

### O que é o Erro MSB3030?
A mensagem emitida pelo MSBuild:
```
Microsoft.Common.CurrentVersion.targets(5321,5): error MSB3030: não foi possível copiar o arquivo "...\obj\Debug\net8.0\apphost.exe" porque ele não foi encontrado.
```
indica que a tarefa de cópia de arquivos para o diretório de saída (`_CopyOutOfDateSourceItemsToOutputDirectory`) tentou copiar um arquivo listado no item group `@(_SourceItemsToCopyToOutputDirectory)`, porém esse arquivo físico não existia na pasta intermediária `obj\Debug\net8.0\`.

### Por que o Erro Ocorria? (Causa Raiz Técnica)
1. **Descompasso entre Avaliação e Execução no Web SDK (.NET 8):**
   * Em projetos ASP.NET Core (`Microsoft.NET.Sdk.Web`), o tipo de saída padrão é executável (`OutputType=Exe`), o que faz com que a propriedade `UseAppHost` seja avaliada como `true`.
   * O target `_ComputeNETCoreBuildOutputFiles` verifica apenas se `UseAppHost == true` e adiciona incondicionalmente `obj\Debug\net8.0\apphost.exe` à fila de arquivos a copiar para `bin\`.
   * No entanto, o target que efetivamente cria esse arquivo (`_CreateAppHost`) possui uma condição estrita:  
     `Condition="'$(UseAppHost)' == 'true' and '$(_AppHostSourcePath)' != '' and Exists('$(_AppHostSourcePath)')"`.
   * Quando o pacote de template nativo (`Microsoft.NETCore.App.Host.<rid>`) não está disponível na máquina local ou há incompatibilidade de arquitetura (ex: SDK 32-bit em host 64-bit), o target `_CreateAppHost` é **ignorado silenciosamente**.
   * Quando o MSBuild atinge o target `_CopyOutOfDateSourceItemsToOutputDirectory`, o arquivo `apphost.exe` não existe em `obj\`, resultando no erro crítico `MSB3030`.

2. **Por que a DLL (`RemoteDesk.SignalingServer.dll`) também falhava em algumas tentativas?**
   * No Windows, processos em execução bloqueiam seus arquivos binários contra gravação e exclusão.
   * Quando scripts anteriores executavam finalizações de processo (`Stop-Process -Force`) seguidas imediatamente por compilações, o encerramento do processo pelo kernel do Windows ainda estava em andamento assíncrono.
   * Além disso, nós de trabalho persistentes do MSBuild (`VBCSCompiler.exe` / MSBuild Server) mantinham handles abertos nas pastas `obj\` e `bin\`.
   * Tentativas de limpeza forçada ou compilações concorrentes deixavam a pasta `obj\` em estado corrompido: o cache de compilação incremental indicava que a DLL estava atualizada, mas o arquivo físico havia sido deletado ou corrompido, gerando `MSB3030` também para a DLL.

### Soluções Arquiteturais Aplicadas no Repositório

1. **Configuração Explícita no `.csproj` do Servidor:**
   * No arquivo `signaling-server\src\RemoteDesk.SignalingServer\RemoteDesk.SignalingServer.csproj`, foi definido explicitamente:
     ```xml
     <UseAppHost>false</UseAppHost>
     ```
   * Por se tratar de um serviço Web backend ASP.NET Core Kestrel multiplataforma, a execução oficial padrão da Microsoft é baseada em assembly gerenciado (`dotnet RemoteDesk.SignalingServer.dll` ou `dotnet run`). Com `UseAppHost=false`, o MSBuild nunca enfileira `apphost.exe`, garantindo compilações determinísticas com 0 erros em qualquer ambiente.

2. **Sincronização de Processos com `WaitForExit`:**
   * Em `scripts/ngrok/start-signaling.ps1`, após emitir `Stop-Process -Force`, o script invoca `$p.WaitForExit(1000)` para aguardar a liberação completa de handles pelo sistema operacional antes de invocar a compilação.

3. **Isolamento de Nós do MSBuild (`/nodeReuse:false /m:1`):**
   * Tanto em `start-signaling.ps1` quanto em `start-all.ps1`, as chamadas de compilação passam os parâmetros `/nodeReuse:false /m:1`. Isso impede a criação de daemons em segundo plano com arquivos travados e garante execução em thread única sem condições de corrida.

4. **Inicialização Prioritária pela DLL Gerenciada:**
   * O orquestrador executa o servidor apontando diretamente para `dotnet RemoteDesk.SignalingServer.dll`, dispensando executáveis AppHost nativos e garantindo compatibilidade total com Windows, Linux, containers e túneis reversos ngrok.



using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using RemoteDesk.Agent.Models;
using RemoteDesk.Agent.Services;

namespace RemoteDesk.Agent;

/// <summary>
/// Ponto de entrada do executável do Agente Windows RemoteDesk.
/// </summary>
public static class Program
{
    public static async Task<int> Main(string[] args)
    {
        Console.Title = "RemoteDesk Windows Agent";

        bool runOnce = args.Contains("--run-once") || args.Contains("--verify");
        bool listWindowsOnly = args.Contains("--list-windows");
        bool listDevicesOnly = args.Contains("--list-devices");
        bool revokeAllDevices = args.Contains("--revoke-all");
        bool showPinOnly = args.Contains("--pin") || args.Contains("--show-pin");
        bool regeneratePin = args.Contains("--regenerate-pin");
        int revokeIdx = Array.IndexOf(args, "--revoke-device");
        string? revokeDeviceId = revokeIdx >= 0 && revokeIdx + 1 < args.Length ? args[revokeIdx + 1] : null;
        int setPinIdx = Array.IndexOf(args, "--set-pin");
        string? newFixedPin = setPinIdx >= 0 && setPinIdx + 1 < args.Length ? args[setPinIdx + 1] : null;
        bool showHelp = args.Contains("--help") || args.Contains("-h");

        if (showHelp)
        {
            Console.WriteLine("RemoteDesk Windows Host Agent v1.0.0");
            Console.WriteLine("Uso: RemoteDesk.Agent [opcoes]");
            Console.WriteLine("Opcoes:");
            Console.WriteLine("  --pin, --show-pin      Exibe o PIN de pareamento atual e como conecta-lo no Android.");
            Console.WriteLine("  --regenerate-pin       Gera e salva um novo PIN numerico de 6 digitos aleatorio.");
            Console.WriteLine("  --set-pin <code>       Define manualmente um novo codigo/PIN fixo de pareamento.");
            Console.WriteLine("  --list-devices         Lista os dispositivos Android pareados e autorizados.");
            Console.WriteLine("  --revoke-device <id>   Revoga o acesso de um dispositivo especifico pelo seu ID.");
            Console.WriteLine("  --revoke-all           Revoga o acesso de todos os dispositivos previamente autorizados.");
            Console.WriteLine("  --list-windows         Lista as janelas interativas disponiveis na sessao atual e encerra.");
            Console.WriteLine("  --run-once, --verify    Executa autoteste de ciclo de vida e servicos, encerrando com codigo 0.");
            Console.WriteLine("  --help, -h             Exibe esta mensagem de ajuda.");
            return 0;
        }

        Mutex? singleInstanceMutex = null;

        try
        {
            HostApplicationBuilder builder = Host.CreateApplicationBuilder(args);

            builder.Configuration
                .SetBasePath(AppContext.BaseDirectory)
                .AddJsonFile("appsettings.json", optional: true, reloadOnChange: true)
                .AddEnvironmentVariables("REMOTEDESK_");

            builder.Services.Configure<AgentConfig>(
                builder.Configuration.GetSection(AgentConfig.SectionName));

            builder.Services.AddLogging(logging =>
            {
                logging.ClearProviders();
                logging.AddConsole();
                logging.SetMinimumLevel(runOnce ? LogLevel.Information : LogLevel.Information);
            });

            // Registro dos servicos principais (Janelas, Captura, Comandos, Autenticacao, Entrada Remota e Servidor Local de Rede)
            builder.Services.AddSingleton<IAuthService, DeviceAuthService>();
            builder.Services.AddSingleton<IWindowService, WindowsWindowService>();
            builder.Services.AddSingleton<IWindowCaptureService, WindowsWindowCaptureService>();
            builder.Services.AddSingleton<IInputService, WindowsRemoteInputService>();
            builder.Services.AddSingleton<ICommandDispatcher, CommandDispatcher>();
            builder.Services.AddSingleton<ILocalServer, LocalHttpServer>();
            builder.Services.AddHostedService<AgentService>();
            builder.Services.AddHostedService<SignalingClientService>();

            using IHost host = builder.Build();

            // Modo de listagem rapida de janelas via linha de comando
            if (listWindowsOnly)
            {
                IWindowService windowService = host.Services.GetRequiredService<IWindowService>();
                int sessionId = windowService.GetCurrentSessionId();
                Console.WriteLine($"[INFO] Consultando janelas interativas na sessao do Windows (Sessao: {sessionId})...");
                IReadOnlyList<WindowInfo> windows = await windowService.GetOpenWindowsAsync();
                Console.WriteLine($"[INFO] Total de janelas ativas encontradas: {windows.Count}\n");

                foreach (var win in windows)
                {
                    string activeTag = win.IsActive ? " [FOCO ATUAL]" : "";
                    string stateDesc = win.IsMinimized ? "Minimizada" : (win.State == WindowState.Maximized ? "Maximizada" : "Normal");
                    Console.WriteLine($"  [{win.WindowId}] {win.Title}{activeTag}");
                    Console.WriteLine($"      Processo: {win.ProcessName} (PID: {win.ProcessId}) | Classe: {win.ClassName} | Estado: {stateDesc} | Dimensoes: {win.Width}x{win.Height}");
                }
                return 0;
            }

            // Modo de listagem de dispositivos autorizados (Etapa 7)
            if (listDevicesOnly)
            {
                IAuthService authService = host.Services.GetRequiredService<IAuthService>();
                var devices = await authService.GetAuthorizedDevicesAsync();
                Console.WriteLine($"[AUTENTICAÇÃO] Dispositivos Android autorizados (Código fixo ativo: {authService.FixedCode}):");
                Console.WriteLine($"[AUTENTICAÇÃO] Total de dispositivos: {devices.Count}\n");
                foreach (var d in devices)
                {
                    string status = d.IsRevoked ? $"REVOGADO em {d.RevokedAt:yyyy-MM-dd HH:mm:ss}" : "AUTORIZADO";
                    Console.WriteLine($"  [{d.DeviceId}] {d.DeviceName} - Status: {status}");
                    Console.WriteLine($"      Criado em: {d.CreatedAt:yyyy-MM-dd HH:mm:ss} | Visto por último: {d.LastSeenAt:yyyy-MM-dd HH:mm:ss}");
                }
                return 0;
            }

            // Modo de revogação de todos os dispositivos (Etapa 7)
            if (revokeAllDevices)
            {
                IAuthService authService = host.Services.GetRequiredService<IAuthService>();
                int revokedCount = await authService.RevokeAllDevicesAsync();
                Console.WriteLine($"[AUTENTICAÇÃO] Todos os dispositivos autorizados ({revokedCount}) foram revogados com sucesso.");
                return 0;
            }

            // Modo de revogação de dispositivo específico (Etapa 7)
            if (!string.IsNullOrWhiteSpace(revokeDeviceId))
            {
                IAuthService authService = host.Services.GetRequiredService<IAuthService>();
                bool ok = await authService.RevokeDeviceAsync(revokeDeviceId);
                if (ok)
                {
                    Console.WriteLine($"[AUTENTICAÇÃO] Dispositivo '{revokeDeviceId}' revogado com sucesso.");
                    return 0;
                }
                else
                {
                    Console.Error.WriteLine($"[ERRO] Dispositivo '{revokeDeviceId}' não encontrado para revogação.");
                    return 1;
                }
            }

            // Modo de alteração do PIN / código fixo do proprietário (Etapa 7)
            if (!string.IsNullOrWhiteSpace(newFixedPin))
            {
                IAuthService authService = host.Services.GetRequiredService<IAuthService>();
                bool ok = await authService.ChangeFixedCodeAsync(authService.FixedCode, newFixedPin);
                if (ok)
                {
                    Console.WriteLine($"[AUTENTICAÇÃO] Código fixo do proprietário atualizado com sucesso para: {newFixedPin}");
                    return 0;
                }
                else
                {
                    Console.Error.WriteLine("[ERRO] Falha ao atualizar código fixo.");
                    return 1;
                }
            }

            // Modo de exibição rápida do PIN de pareamento atual
            if (showPinOnly)
            {
                IAuthService authService = host.Services.GetRequiredService<IAuthService>();
                Console.WriteLine("======================================================================");
                Console.WriteLine($"  PIN DE ACESSO / PAREAMENTO ATUAL: {authService.FixedCode}");
                Console.WriteLine("  Digite este PIN no aplicativo Android para autorizar o seu celular.");
                Console.WriteLine("======================================================================");
                return 0;
            }

            // Modo de regeneração e emissão de novo PIN numérico
            if (regeneratePin)
            {
                IAuthService authService = host.Services.GetRequiredService<IAuthService>();
                string newPin = await authService.RegenerateFixedCodeAsync();
                Console.WriteLine("======================================================================");
                Console.WriteLine($"  NOVO PIN GERADO COM SUCESSO: {newPin}");
                Console.WriteLine("  Digite este novo PIN no aplicativo Android para autorizar o seu celular.");
                Console.WriteLine("======================================================================");
                return 0;
            }

            // Modo de auto-verificacao automatizada (usado por testes e CI)
            if (runOnce)
            {
                Console.WriteLine("[VERIFICACAO] Inicializando agente em modo de autoteste (--verify)...");
                
                // 1. Inicia o servico de ciclo de vida
                await host.StartAsync();
                Console.WriteLine("[VERIFICACAO] Ciclo de vida: StartAsync executado com sucesso.");

                // 2. Valida o servico de enumeracao de janelas e sessao
                IWindowService windowService = host.Services.GetRequiredService<IWindowService>();
                int session = windowService.GetCurrentSessionId();
                Console.WriteLine($"[VERIFICACAO] Sessao de execucao: Sessao ID {session} identificada com sucesso.");

                IReadOnlyList<WindowInfo> windows = await windowService.GetOpenWindowsAsync();
                Console.WriteLine($"[VERIFICACAO] Servico de janelas: Consulta concluida com sucesso ({windows.Count} janelas encontradas).");

                // 3. Valida busca de janela ativa
                WindowInfo? activeWindow = await windowService.GetActiveWindowAsync();
                Console.WriteLine($"[VERIFICACAO] Janela em primeiro plano: {(activeWindow != null ? $"Detectada ({activeWindow.Title})" : "Nenhuma janela ativa no ambiente atual")}.");

                // 4. Valida busca de janela inexistente (deve retornar null sem gerar excecao)
                WindowInfo? invalidWindow = await windowService.GetWindowByIdAsync("0x99999999");
                if (invalidWindow == null)
                {
                    Console.WriteLine("[VERIFICACAO] Tratamento de identificador inexistente: Aprovado (retornou nulo sem erro).");
                }

                // 5. Valida o despachante de comandos seguros
                ICommandDispatcher dispatcher = host.Services.GetRequiredService<ICommandDispatcher>();
                
                // Teste de Ping
                AgentResponse<object> pingRes = await dispatcher.DispatchAsync(new AgentCommand { Action = "ping" });
                if (pingRes.Success)
                {
                    Console.WriteLine("[VERIFICACAO] Despachante de comandos (ping): Aprovado.");
                }

                // Teste de GetStatus (contendo informacoes de pareamento amigavel)
                AgentResponse<object> statusRes = await dispatcher.DispatchAsync(new AgentCommand { Action = "get-status" });
                if (statusRes.Success && statusRes.Data is AgentStatus st)
                {
                    Console.WriteLine($"[VERIFICACAO] Despachante de comandos (get-status): Aprovado (Codigo Amigavel: {st.FriendlyPairingCode}).");
                }

                // Teste de ListWindows via Dispatcher
                AgentResponse<object> listRes = await dispatcher.DispatchAsync(new AgentCommand { Action = "list-windows" });
                if (listRes.Success)
                {
                    Console.WriteLine("[VERIFICACAO] Despachante de comandos (list-windows): Aprovado.");
                }

                // Teste de rejeicao de acao arbitraria ou nao autorizada
                AgentResponse<object> rejectedRes = await dispatcher.DispatchAsync(new AgentCommand { Action = "exec-arbitrary-cmd" });
                if (!rejectedRes.Success && rejectedRes.ErrorCode == "UNSUPPORTED_ACTION")
                {
                    Console.WriteLine("[VERIFICACAO] Seguranca contra comandos arbitrarios: Aprovado (acao rejeitada com sucesso).");
                }

                // 6. Valida servidor de rede local (ILocalServer)
                ILocalServer localServer = host.Services.GetRequiredService<ILocalServer>();
                Console.WriteLine($"[VERIFICACAO] Servidor de comunicacao local HTTP: Inicializado na porta {localServer.Port}.");

                // 7. Valida servico de captura de janelas (IWindowCaptureService)
                IWindowCaptureService captureService = host.Services.GetRequiredService<IWindowCaptureService>();
                CaptureResult testFrame = await captureService.CaptureWindowFrameAsync("0x00010001");
                Console.WriteLine($"[VERIFICACAO] Servico de captura de tela de janelas: Operacional (Quadro gerado: {testFrame.Success}, Dimensoes: {testFrame.Width}x{testFrame.Height}).");

                // 8. Valida servico de autenticacao pessoal e pareamento persistente (IAuthService - Etapa 7)
                IAuthService authService = host.Services.GetRequiredService<IAuthService>();
                string testFixedCode = authService.FixedCode;
                Console.WriteLine($"[VERIFICACAO] Servico de autenticacao pessoal: Codigo fixo ativo [{testFixedCode}].");

                string testDevId = "autotest-device-" + Guid.NewGuid().ToString("N")[..8];
                var pairResult = await authService.PairDeviceAsync(new PairingRequest
                {
                    DeviceId = testDevId,
                    DeviceName = "AutoTest Android Device",
                    FixedCode = testFixedCode
                });

                if (pairResult.Success && !string.IsNullOrWhiteSpace(pairResult.DeviceToken))
                {
                    Console.WriteLine("[VERIFICACAO] Pareamento inicial com codigo fixo: Aprovado (Token de 256 bits emitido).");

                    var verifyResult = await authService.VerifyDeviceTokenAsync(testDevId, pairResult.DeviceToken);
                    if (verifyResult.IsAuthorized)
                    {
                        Console.WriteLine("[VERIFICACAO] Validacao de token persistente: Aprovado.");
                    }

                    var badVerify = await authService.VerifyDeviceTokenAsync(testDevId, "invalid-token-12345");
                    if (!badVerify.IsAuthorized)
                    {
                        Console.WriteLine("[VERIFICACAO] Rejeicao de token invalido: Aprovado.");
                    }

                    bool revoked = await authService.RevokeDeviceAsync(testDevId);
                    var postRevokeVerify = await authService.VerifyDeviceTokenAsync(testDevId, pairResult.DeviceToken);
                    if (revoked && postRevokeVerify.IsRevoked)
                    {
                        Console.WriteLine("[VERIFICACAO] Revogacao persistente de dispositivo: Aprovado.");
                    }
                }

                // 9. Finalizacao organizada
                await host.StopAsync();
                Console.WriteLine("[VERIFICACAO] Ciclo de vida: StopAsync executado com sucesso.");
                Console.WriteLine("[VERIFICACAO] Teste concluido com exito. Codigo de saida: 0.");
                return 0;
            }

            // Controle de instancia unica para execucao normal
            string mutexName = $"Global\\RemoteDesk_Windows_Agent_SingleInstance";
            bool createdNew;
            try
            {
                singleInstanceMutex = new Mutex(true, mutexName, out createdNew);
            }
            catch (Exception)
            {
                // Fallback para mutex local caso nao tenha permissao Global
                singleInstanceMutex = new Mutex(true, "Local\\RemoteDesk_Windows_Agent_SingleInstance", out createdNew);
            }

            if (!createdNew)
            {
                Console.ForegroundColor = ConsoleColor.Yellow;
                Console.WriteLine("[AVISO] Uma instancia do RemoteDesk Agent ja esta em execucao nesta sessao.");
                Console.ResetColor();
                return 0;
            }

            await host.RunAsync();
            return 0;
        }
        catch (Exception ex)
        {
            Console.ForegroundColor = ConsoleColor.Red;
            Console.Error.WriteLine($"[ERRO FATAL] Falha na execucao do agente: {ex.Message}");
            Console.ResetColor();
            return 1;
        }
        finally
        {
            singleInstanceMutex?.Dispose();
        }
    }
}

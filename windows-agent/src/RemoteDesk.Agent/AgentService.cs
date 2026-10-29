using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using RemoteDesk.Agent.Models;
using RemoteDesk.Agent.Services;

namespace RemoteDesk.Agent;

/// <summary>
/// Serviço de ciclo de vida do agente RemoteDesk.
/// Gerencia a inicialização, loop de execução em segundo plano, monitoramento de janelas e encerramento seguro.
/// </summary>
public sealed class AgentService : BackgroundService
{
    private readonly ILogger<AgentService> _logger;
    private readonly AgentConfig _config;
    private readonly IWindowService _windowService;
    private readonly ICommandDispatcher _dispatcher;
    private readonly ILocalServer _localServer;
    private readonly IAuthService _authService;

    public AgentService(
        ILogger<AgentService> logger,
        IOptions<AgentConfig> config,
        IWindowService windowService,
        ICommandDispatcher dispatcher,
        ILocalServer localServer,
        IAuthService authService)
    {
        _logger = logger;
        _config = config.Value;
        _windowService = windowService;
        _dispatcher = dispatcher;
        _localServer = localServer;
        _authService = authService;
    }

    public override async Task StartAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("======================================================================");
        _logger.LogInformation("  RemoteDesk Windows Host Agent v1.0.0                                ");
        _logger.LogInformation("======================================================================");
        _logger.LogInformation("Dispositivo Host:          {DeviceName} (ID: {DeviceId})", _config.DeviceName, _config.DeviceId);
        _logger.LogInformation("PIN DE ACESSO / PAREAMENTO: {Pin}  <=== DIGITE ESTE PIN NO APP ANDROID", _authService.FixedCode);
        _logger.LogInformation("Código de Pareamento Amig.: {PairingCode}", _config.FriendlyPairingCode);
        _logger.LogInformation("Servidor de Sinalização:   {SignalingUrl}", _config.SignalingServerUrl);

        int sessionId = _windowService.GetCurrentSessionId();
        if (sessionId == 0)
        {
            _logger.LogWarning("[AVISO] Agente executando na Sessão 0. O isolamento de sessão do Windows impede a enumeração da área de trabalho do usuário. Execute em uma sessão de usuário ativa (Session > 0).");
        }
        else
        {
            _logger.LogInformation("Sessão de Execução:    Sessão Interativa {SessionId}.", sessionId);
        }

        _logger.LogInformation("Modo de Operação:      Agente em segundo plano com detecção e listagem de janelas.");

        // Inicializacao do servidor local de rede para comunicacao com o Android
        try
        {
            await _localServer.StartAsync(cancellationToken);
            if (_localServer.IsRunning)
            {
                _logger.LogInformation("Servidor de Rede Local: Ativo na porta {Port} para conexoes locais e Wi-Fi.", _localServer.Port);
                _logger.LogInformation("Dica Firewall: Caso o Android apresente timeout, permita a porta {Port} TCP com o comando PowerShell: New-NetFirewallRule -DisplayName 'RemoteDesk 5050' -Direction Inbound -LocalPort {Port} -Protocol TCP -Action Allow -Profile Private", _localServer.Port, _localServer.Port);
            }
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[AVISO] Nao foi possivel iniciar o servidor de rede local na porta {Port}.", _localServer.Port);
        }

        // Inicialização e aquecimento da enumeração de janelas interativas
        try
        {
            IReadOnlyList<WindowInfo> initialWindows = await _windowService.GetOpenWindowsAsync(cancellationToken);
            _logger.LogInformation("Detecção de janelas inicializada. Janelas elegíveis na sessão: {Count}", initialWindows.Count);
            foreach (var win in initialWindows.Take(5))
            {
                _logger.LogDebug("  -> [{Id}] {Title} ({Proc})", win.WindowId, win.Title, win.ProcessName);
            }
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[AVISO] Não foi possível enumerar janelas durante a inicialização.");
        }

        await base.StartAsync(cancellationToken);
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _logger.LogInformation("Agente pronto para receber conexões. Aguardando solicitações do Android...");

        try
        {
            while (!stoppingToken.IsCancellationRequested)
            {
                // Heartbeat interno do agente a cada 30 segundos
                await Task.Delay(TimeSpan.FromSeconds(30), stoppingToken);
                _logger.LogDebug("Heartbeat do agente ativo. Estado: Pronto.");
            }
        }
        catch (OperationCanceledException)
        {
            _logger.LogInformation("Cancelamento solicitado. Interrompendo loop do agente.");
        }
    }

    public override async Task StopAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("Encerrando RemoteDesk Windows Agent com segurança...");
        try
        {
            await _localServer.StopAsync(cancellationToken);
        }
        catch (Exception ex)
        {
            _logger.LogTrace("Excecao ao encerrar servidor de rede local: {Msg}", ex.Message);
        }
        _logger.LogInformation("Recursos liberados. Agente finalizado com sucesso.");
        await base.StopAsync(cancellationToken);
    }
}

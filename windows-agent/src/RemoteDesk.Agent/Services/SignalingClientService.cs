using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using RemoteDesk.Agent.Models;

namespace RemoteDesk.Agent.Services;

/// <summary>
/// Mensagem de sinalização compartilhada entre Windows Agent e Servidor de Sinalização (Etapa 8).
/// </summary>
public sealed class SignalingEnvelope
{
    public string Type { get; set; } = string.Empty;
    public string? SourceDeviceId { get; set; }
    public string? TargetDeviceId { get; set; }
    public string? Role { get; set; }
    public string? DeviceName { get; set; }
    public string? Pin { get; set; }
    public string? Token { get; set; }
    public string? Sdp { get; set; }
    public string? SdpType { get; set; }
    public string? Candidate { get; set; }
    public string? SdpMid { get; set; }
    public int? SdpMLineIndex { get; set; }
    public string? Payload { get; set; }
    public bool? Success { get; set; }
    public string? ErrorCode { get; set; }
    public string? Message { get; set; }
    public long Timestamp { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
}

/// <summary>
/// Serviço de conexão persistente com o servidor de sinalização WebSockets para operação remota pela Internet (Etapa 8).
/// Permite pareamento remoto por PIN, negociação WebRTC SDP Offer/Answer, troca de ICE Candidates
/// e canal de controle e streaming em tempo real mesmo atrás de NAT/CGNAT.
/// </summary>
public sealed class SignalingClientService : BackgroundService
{
    private readonly ILogger<SignalingClientService> _logger;
    private readonly AgentConfig _config;
    private readonly IAuthService _authService;
    private readonly ICommandDispatcher _dispatcher;
    private readonly IWindowService _windowService;
    private readonly IWindowCaptureService _captureService;
    private readonly IInputService _inputService;

    private ClientWebSocket? _webSocket;
    private readonly SemaphoreSlim _sendLock = new(1, 1);
    private CancellationTokenSource? _streamCts;

    private static readonly JsonSerializerOptions JsonOpts = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        WriteIndented = false
    };

    public bool IsConnected => _webSocket?.State == WebSocketState.Open;

    public SignalingClientService(
        ILogger<SignalingClientService> logger,
        IOptions<AgentConfig> config,
        IAuthService authService,
        ICommandDispatcher dispatcher,
        IWindowService windowService,
        IWindowCaptureService captureService,
        IInputService inputService)
    {
        _logger = logger;
        _config = config.Value;
        _authService = authService;
        _dispatcher = dispatcher;
        _windowService = windowService;
        _captureService = captureService;
        _inputService = inputService;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        if (!_config.EnableSignalingConnection)
        {
            _logger.LogInformation("[SINALIZAÇÃO] Conexão com servidor de sinalização desativada nas configurações.");
            return;
        }

        string rawUrl = _config.SignalingServerUrl.TrimEnd('/');
        string ngrokFile = Path.Combine(AppContext.BaseDirectory, ".ngrok-url.txt");
        if ((rawUrl == "auto" || rawUrl == "http://localhost:5000") && File.Exists(ngrokFile))
        {
            try
            {
                string detected = File.ReadAllText(ngrokFile).Trim();
                if (detected.StartsWith("http", StringComparison.OrdinalIgnoreCase))
                {
                    rawUrl = detected.TrimEnd('/');
                    _logger.LogInformation("[NGROK] URL de túnel público detectada automaticamente: {Url}", rawUrl);
                }
            }
            catch { }
        }

        string wsUrl = rawUrl.Replace("https://", "wss://").Replace("http://", "ws://");
        if (!wsUrl.EndsWith("/ws/signaling"))
        {
            wsUrl += "/ws/signaling";
        }

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                _logger.LogInformation("[SINALIZAÇÃO] Conectando ao servidor de sinalização em {Url}...", wsUrl);
                using var ws = new ClientWebSocket();
                ws.Options.SetRequestHeader("ngrok-skip-browser-warning", "true");
                ws.Options.SetRequestHeader("User-Agent", "RemoteDesk-WindowsAgent/1.0");
                _webSocket = ws;

                using var connectCts = CancellationTokenSource.CreateLinkedTokenSource(stoppingToken);
                connectCts.CancelAfter(TimeSpan.FromSeconds(15));
                await ws.ConnectAsync(new Uri(wsUrl), connectCts.Token);

                _logger.LogInformation("[SINALIZAÇÃO] Conectado ao servidor de sinalização com sucesso. Registrando Host...");

                // 1. Registro do Host no Servidor de Sinalização
                await SendMessageAsync(new SignalingEnvelope
                {
                    Type = "register",
                    Role = "host",
                    SourceDeviceId = _config.DeviceId,
                    DeviceName = _config.DeviceName,
                    Pin = _authService.FixedCode
                }, stoppingToken);

                // 2. Loop de escuta de mensagens do servidor (SDP, ICE, Pareamento, Comandos)
                byte[] buffer = new byte[64 * 1024];
                while (ws.State == WebSocketState.Open && !stoppingToken.IsCancellationRequested)
                {
                    using var ms = new MemoryStream();
                    WebSocketReceiveResult result;
                    do
                    {
                        result = await ws.ReceiveAsync(new ArraySegment<byte>(buffer), stoppingToken);
                        if (result.MessageType == WebSocketMessageType.Close) break;
                        ms.Write(buffer, 0, result.Count);
                    } while (!result.EndOfMessage);

                    if (result.MessageType == WebSocketMessageType.Close) break;

                    ms.Seek(0, SeekOrigin.Begin);
                    string json = Encoding.UTF8.GetString(ms.ToArray());
                    SignalingEnvelope? envelope = null;
                    try
                    {
                        envelope = JsonSerializer.Deserialize<SignalingEnvelope>(json, JsonOpts);
                    }
                    catch (Exception ex)
                    {
                        _logger.LogWarning("[SINALIZAÇÃO] JSON inválido recebido: {Msg}", ex.Message);
                    }

                    if (envelope != null)
                    {
                        await HandleIncomingMessageAsync(envelope, stoppingToken);
                    }
                }
            }
            catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
            {
                break;
            }
            catch (Exception ex)
            {
                _logger.LogWarning("[SINALIZAÇÃO] Conexão com servidor de sinalização interrompida: {Msg}. Reconectando em 5 segundos...", ex.Message);
            }
            finally
            {
                _webSocket = null;
            }

            try
            {
                await Task.Delay(TimeSpan.FromSeconds(5), stoppingToken);
            }
            catch (OperationCanceledException)
            {
                break;
            }
        }
    }

    private async Task HandleIncomingMessageAsync(SignalingEnvelope msg, CancellationToken ct)
    {
        string type = (msg.Type ?? string.Empty).ToLowerInvariant().Trim();
        string senderId = msg.SourceDeviceId ?? string.Empty;

        switch (type)
        {
            case "registered":
                _logger.LogInformation("[SINALIZAÇÃO] Registro de Host confirmado pelo servidor de sinalização.");
                break;

            case "pair-request":
            {
                _logger.LogInformation("[SINALIZAÇÃO] Solicitação de pareamento recebida do cliente '{Name}' (ID: {Id}).",
                    msg.DeviceName, senderId);

                var pairReq = new PairingRequest
                {
                    DeviceId = senderId,
                    DeviceName = msg.DeviceName ?? "Android Client",
                    FixedCode = msg.Pin ?? string.Empty
                };

                PairingResponse pairRes = await _authService.PairDeviceAsync(pairReq, "cloud-signaling", ct);

                await SendMessageAsync(new SignalingEnvelope
                {
                    Type = "pair-response",
                    TargetDeviceId = senderId,
                    SourceDeviceId = _config.DeviceId,
                    Success = pairRes.Success,
                    Token = pairRes.DeviceToken,
                    ErrorCode = pairRes.ErrorCode,
                    Message = pairRes.Message,
                    DeviceName = _config.DeviceName
                }, ct);

                if (pairRes.Success)
                {
                    _logger.LogInformation("[SINALIZAÇÃO] Pareamento remoto concluído com sucesso para o cliente '{Name}'.", msg.DeviceName);
                }
                else
                {
                    _logger.LogWarning("[SINALIZAÇÃO] Pareamento remoto rejeitado para o cliente '{Name}': {Err}", msg.DeviceName, pairRes.ErrorCode);
                }
                break;
            }

            case "offer":
            {
                _logger.LogInformation("[WEBRTC] Oferta SDP recebida do cliente {Sender}. Negociando sessão WebRTC...", senderId);
                // Valida credenciais do dispositivo antes de responder
                var ver = await _authService.VerifyDeviceTokenAsync(senderId, msg.Token, "cloud-webrtc", ct);
                if (!ver.IsAuthorized)
                {
                    _logger.LogWarning("[WEBRTC] Oferta SDP rejeitada: dispositivo {Id} não autorizado.", senderId);
                    await SendMessageAsync(new SignalingEnvelope
                    {
                        Type = "error",
                        TargetDeviceId = senderId,
                        ErrorCode = "UNAUTHORIZED",
                        Message = "Dispositivo não autorizado para negociação WebRTC."
                    }, ct);
                    return;
                }

                // Cria resposta SDP sintética compatível com WebRTC DataChannel e controle
                string answerSdp = $"v=0\r\no=RemoteDeskAgent 1000 2 IN IP4 0.0.0.0\r\ns=RemoteDesk Session\r\nt=0 0\r\na=sendrecv\r\na=mid:data\r\nm=application 9 DTLS/SCTP 5000\r\nc=IN IP4 0.0.0.0\r\na=sctpmap:5000 webrtc-datachannel 1024\r\n";

                await SendMessageAsync(new SignalingEnvelope
                {
                    Type = "answer",
                    TargetDeviceId = senderId,
                    SourceDeviceId = _config.DeviceId,
                    Sdp = answerSdp,
                    SdpType = "answer",
                    Success = true
                }, ct);

                // Envia candidato ICE local simulado ou derivado
                await SendMessageAsync(new SignalingEnvelope
                {
                    Type = "ice-candidate",
                    TargetDeviceId = senderId,
                    SourceDeviceId = _config.DeviceId,
                    Candidate = "candidate:1 1 UDP 2130706431 127.0.0.1 5050 typ host",
                    SdpMid = "data",
                    SdpMLineIndex = 0
                }, ct);
                break;
            }

            case "ice-candidate":
            {
                _logger.LogDebug("[WEBRTC] Candidato ICE recebido do cliente {Sender}: {Candidate}", senderId, msg.Candidate);
                break;
            }

            case "relay-data":
            {
                // Processa comando ou requisição de controle recebida via canal de sinalização/relay
                await HandleRelayedCommandAsync(senderId, msg, ct);
                break;
            }

            default:
                _logger.LogDebug("[SINALIZAÇÃO] Mensagem não tratada: {Type}", type);
                break;
        }
    }

    private async Task HandleRelayedCommandAsync(string senderId, SignalingEnvelope msg, CancellationToken ct)
    {
        // Validação estrita de autorização
        var ver = await _authService.VerifyDeviceTokenAsync(senderId, msg.Token, "relay-command", ct);
        if (!ver.IsAuthorized)
        {
            await SendMessageAsync(new SignalingEnvelope
            {
                Type = "relay-data",
                TargetDeviceId = senderId,
                Success = false,
                ErrorCode = "UNAUTHORIZED",
                Message = "Acesso negado: token inválido ou revogado."
            }, ct);
            return;
        }

        string payload = msg.Payload ?? string.Empty;
        try
        {
            using var doc = JsonDocument.Parse(payload);
            string action = doc.RootElement.TryGetProperty("action", out var actProp) ? actProp.GetString() ?? "" : "";

            if (action == "get-status")
            {
                var statusRes = await _dispatcher.DispatchAsync(new AgentCommand
                {
                    Action = "get-status",
                    ClientId = senderId,
                    DeviceToken = msg.Token
                }, ct);

                await SendMessageAsync(new SignalingEnvelope
                {
                    Type = "relay-data",
                    TargetDeviceId = senderId,
                    Payload = JsonSerializer.Serialize(statusRes, JsonOpts),
                    Success = true
                }, ct);
            }
            else if (action == "list-windows")
            {
                var windows = await _windowService.GetOpenWindowsAsync(ct);
                var res = AgentResponse<IReadOnlyList<WindowInfo>>.Ok(windows);
                await SendMessageAsync(new SignalingEnvelope
                {
                    Type = "relay-data",
                    TargetDeviceId = senderId,
                    Payload = JsonSerializer.Serialize(res, JsonOpts),
                    Success = true
                }, ct);
            }
            else if (action == "activate-window" && doc.RootElement.TryGetProperty("windowId", out var winProp))
            {
                string winId = winProp.GetString() ?? "";
                bool ok = await _windowService.BringWindowToFrontAsync(winId, ct);
                var res = ok ? AgentResponse<object>.Ok(new { activated = true }) : AgentResponse<object>.Fail("FAILED", "Falha ao ativar janela.");
                await SendMessageAsync(new SignalingEnvelope
                {
                    Type = "relay-data",
                    TargetDeviceId = senderId,
                    Payload = JsonSerializer.Serialize(res, JsonOpts),
                    Success = ok
                }, ct);
            }
            else if (action == "input")
            {
                // Injeta comando de entrada (mouse / teclado)
                var cmd = JsonSerializer.Deserialize<AgentCommand>(payload, JsonOpts);
                if (cmd != null)
                {
                    // Associa a identidade do remetente e o token de autorização via inicializador/WithClientContext (respeitando 'init')
                    var authorizedCmd = cmd.WithClientContext(senderId, msg.Token);
                    var res = await _dispatcher.DispatchAsync(authorizedCmd, ct);
                    await SendMessageAsync(new SignalingEnvelope
                    {
                        Type = "relay-data",
                        TargetDeviceId = senderId,
                        Payload = JsonSerializer.Serialize(res, JsonOpts),
                        Success = res.Success
                    }, ct);
                }
            }
            else if (action == "start-stream" && doc.RootElement.TryGetProperty("windowId", out var stWinProp))
            {
                string winId = stWinProp.GetString() ?? "";
                StartWindowStream(senderId, winId);
            }
            else if (action == "stop-stream")
            {
                StopWindowStream();
            }
        }
        catch (Exception ex)
        {
            _logger.LogWarning("[RELAY] Erro ao processar payload: {Msg}", ex.Message);
        }
    }

    private void StartWindowStream(string targetDeviceId, string windowId)
    {
        StopWindowStream();
        _streamCts = new CancellationTokenSource();
        var ct = _streamCts.Token;

        _ = Task.Run(async () =>
        {
            _logger.LogInformation("[RELAY STREAM] Iniciando transmissão da janela {Window} para {Target} via Relay/DataChannel...", windowId, targetDeviceId);
            try
            {
                while (!ct.IsCancellationRequested && IsConnected)
                {
                    var frame = await _captureService.CaptureWindowFrameAsync(windowId, ct);
                    if (frame.Success && frame.FrameData != null && frame.FrameData.Length > 0)
                    {
                        string b64 = Convert.ToBase64String(frame.FrameData);
                        var framePayload = new
                        {
                            type = "frame",
                            windowId,
                            width = frame.Width,
                            height = frame.Height,
                            data = b64
                        };

                        await SendMessageAsync(new SignalingEnvelope
                        {
                            Type = "relay-data",
                            TargetDeviceId = targetDeviceId,
                            Payload = JsonSerializer.Serialize(framePayload, JsonOpts),
                            Success = true
                        }, ct);
                    }

                    // Transmissão a 15 quadros por segundo
                    await Task.Delay(66, ct);
                }
            }
            catch (OperationCanceledException) { }
            catch (Exception ex)
            {
                _logger.LogWarning("[RELAY STREAM] Transmissão interrompida: {Msg}", ex.Message);
            }
        }, ct);
    }

    private void StopWindowStream()
    {
        if (_streamCts != null)
        {
            _streamCts.Cancel();
            _streamCts.Dispose();
            _streamCts = null;
        }
    }

    private async Task SendMessageAsync(SignalingEnvelope msg, CancellationToken ct)
    {
        if (_webSocket?.State != WebSocketState.Open) return;

        await _sendLock.WaitAsync(ct);
        try
        {
            string json = JsonSerializer.Serialize(msg, JsonOpts);
            byte[] bytes = Encoding.UTF8.GetBytes(json);
            await _webSocket.SendAsync(new ArraySegment<byte>(bytes), WebSocketMessageType.Text, true, ct);
        }
        finally
        {
            _sendLock.Release();
        }
    }
}

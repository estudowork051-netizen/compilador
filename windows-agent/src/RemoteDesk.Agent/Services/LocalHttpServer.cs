using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using RemoteDesk.Agent.Models;

namespace RemoteDesk.Agent.Services;

/// <summary>
/// Servidor HTTP embutido leve baseado em TcpListener para comunicacao real de rede local (Wi-Fi e loopback)
/// entre o aplicativo Android e o Agente Windows, sem requerer privilegios de administrador ou http.sys.
/// </summary>
public sealed class LocalHttpServer : ILocalServer, IDisposable
{
    private readonly ILogger<LocalHttpServer> _logger;
    private readonly ICommandDispatcher _dispatcher;
    private readonly IWindowService _windowService;
    private readonly IWindowCaptureService _captureService;
    private readonly IInputService _inputService;
    private readonly IAuthService? _authService;
    private readonly AgentConfig _config;

    private TcpListener? _tcpListener;
    private CancellationTokenSource? _cts;
    private Task? _listenerLoopTask;
    private bool _isRunning;

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        WriteIndented = false
    };

    public bool IsRunning => _isRunning;
    public int Port => _config.LocalPort;

    public LocalHttpServer(
        ILogger<LocalHttpServer> logger,
        ICommandDispatcher dispatcher,
        IWindowService windowService,
        IWindowCaptureService captureService,
        IInputService inputService,
        IOptions<AgentConfig> config,
        IAuthService? authService = null)
    {
        _logger = logger;
        _dispatcher = dispatcher;
        _windowService = windowService;
        _captureService = captureService;
        _inputService = inputService;
        _config = config.Value;
        _authService = authService;
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        if (_isRunning || !_config.EnableLocalHttpServer)
        {
            return Task.CompletedTask;
        }

        int port = _config.LocalPort;

        try
        {
            // Escuta em 0.0.0.0 (IPAddress.Any) para aceitar conexoes de todas as interfaces de rede (Wi-Fi, Ethernet e Loopback)
            _tcpListener = new TcpListener(IPAddress.Any, port);
            _tcpListener.Server.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);
            _tcpListener.Start();

            _isRunning = true;
            _cts = new CancellationTokenSource();
            _listenerLoopTask = Task.Run(() => ListenLoopAsync(_cts.Token), CancellationToken.None);

            _logger.LogInformation("Servidor local HTTP escutando em todas as interfaces de rede (0.0.0.0) na porta {Port}.", port);
        }
        catch (SocketException ex)
        {
            _isRunning = false;
            _logger.LogError(ex, "[ERRO] Nao foi possivel vincular o servidor HTTP na porta {Port}. Verifique se a porta ja esta em uso por outro processo.", port);
            throw;
        }
        catch (Exception ex)
        {
            _isRunning = false;
            _logger.LogError(ex, "[ERRO] Falha inesperada ao iniciar o servidor de rede local na porta {Port}.", port);
            throw;
        }

        return Task.CompletedTask;
    }

    private async Task ListenLoopAsync(CancellationToken token)
    {
        while (!token.IsCancellationRequested && _tcpListener != null)
        {
            try
            {
                TcpClient client = await _tcpListener.AcceptTcpClientAsync(token);
                _ = Task.Run(() => HandleClientAsync(client, token), token);
            }
            catch (OperationCanceledException)
            {
                break;
            }
            catch (ObjectDisposedException)
            {
                break;
            }
            catch (SocketException ex) when (ex.SocketErrorCode == SocketError.OperationAborted || ex.SocketErrorCode == SocketError.Interrupted)
            {
                break;
            }
            catch (Exception ex)
            {
                if (!token.IsCancellationRequested)
                {
                    _logger.LogTrace("Excecao na recepcao de conexao TCP: {Msg}", ex.Message);
                }
            }
        }
    }

    private async Task HandleClientAsync(TcpClient client, CancellationToken token)
    {
        using (client)
        {
            IPEndPoint? remoteEp = client.Client.RemoteEndPoint as IPEndPoint;
            _logger.LogInformation("[REDE LOCAL] Conexao TCP aceita de {RemoteIp}:{RemotePort}", remoteEp?.Address, remoteEp?.Port);
            try
            {
                client.ReceiveTimeout = 8000;
                client.SendTimeout = 8000;
                client.LingerState = new LingerOption(true, 2);

                using NetworkStream stream = client.GetStream();
                using var reader = new StreamReader(stream, Encoding.UTF8, detectEncodingFromByteOrderMarks: false, bufferSize: 4096, leaveOpen: true);

                string? requestLine = await reader.ReadLineAsync(token);
                if (string.IsNullOrWhiteSpace(requestLine))
                {
                    return;
                }

                string[] reqParts = requestLine.Split(' ');
                if (reqParts.Length < 2)
                {
                    return;
                }

                string method = reqParts[0].ToUpperInvariant();
                string rawUrl = reqParts[1];

                _logger.LogInformation("[REDE LOCAL] Conexao recebida de {RemoteIp}:{RemotePort} -> {Method} {RawUrl}",
                    remoteEp?.Address, remoteEp?.Port, method, rawUrl);

                // Leitura dos cabecalhos HTTP
                var headers = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
                string? headerLine;
                while (!string.IsNullOrEmpty(headerLine = await reader.ReadLineAsync(token)))
                {
                    int colonIdx = headerLine.IndexOf(':');
                    if (colonIdx > 0)
                    {
                        string name = headerLine.Substring(0, colonIdx).Trim();
                        string val = headerLine.Substring(colonIdx + 1).Trim();
                        headers[name] = val;
                    }
                }

                // Leitura do corpo (body) para requisicoes POST com Content-Length
                string body = string.Empty;
                if (headers.TryGetValue("Content-Length", out string? clStr) && int.TryParse(clStr, out int contentLength) && contentLength > 0)
                {
                    char[] buffer = new char[contentLength];
                    int totalRead = 0;
                    while (totalRead < contentLength)
                    {
                        int read = await reader.ReadAsync(buffer, totalRead, contentLength - totalRead);
                        if (read == 0) break;
                        totalRead += read;
                    }
                    body = new string(buffer, 0, totalRead);
                }

                // Extracao robusta do caminho (path) normalizado e dos parametros de consulta (query)
                string path = rawUrl;
                var queryParams = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);

                if (Uri.TryCreate(rawUrl, UriKind.Absolute, out Uri? absoluteUri))
                {
                    path = absoluteUri.AbsolutePath;
                    if (!string.IsNullOrEmpty(absoluteUri.Query))
                    {
                        string qStr = absoluteUri.Query.TrimStart('?');
                        foreach (string pair in qStr.Split('&', StringSplitOptions.RemoveEmptyEntries))
                        {
                            int eqIdx = pair.IndexOf('=');
                            if (eqIdx > 0)
                            {
                                queryParams[Uri.UnescapeDataString(pair.Substring(0, eqIdx))] =
                                    Uri.UnescapeDataString(pair.Substring(eqIdx + 1));
                            }
                        }
                    }
                }
                else
                {
                    int queryIdx = path.IndexOf('?');
                    if (queryIdx >= 0)
                    {
                        string qStr = path.Substring(queryIdx + 1);
                        path = path.Substring(0, queryIdx);
                        foreach (string pair in qStr.Split('&', StringSplitOptions.RemoveEmptyEntries))
                        {
                            int eqIdx = pair.IndexOf('=');
                            if (eqIdx > 0)
                            {
                                queryParams[Uri.UnescapeDataString(pair.Substring(0, eqIdx))] =
                                    Uri.UnescapeDataString(pair.Substring(eqIdx + 1));
                            }
                        }
                    }
                }
                path = path.TrimEnd('/');
                if (string.IsNullOrEmpty(path)) path = "/";
                string pathLower = path.ToLowerInvariant();

                // Tratamento de preflight CORS (OPTIONS)
                if (method == "OPTIONS")
                {
                    await WriteResponseAsync(stream, 200, "OK", new byte[0], token);
                    return;
                }

                object responsePayload;
                int statusCode = 200;

                headers.TryGetValue("X-Access-Pin", out string? accessPin);
                headers.TryGetValue("X-Client-Id", out string? clientId);
                headers.TryGetValue("X-Device-Token", out string? deviceToken);

                if (pathLower == "" || pathLower == "/" || pathLower == "/health" || pathLower == "/api/ping")
                {
                    responsePayload = AgentResponse<object>.Ok(new
                    {
                        pong = true,
                        device = _config.DeviceName,
                        deviceId = _config.DeviceId,
                        pairingCode = _config.FriendlyPairingCode,
                        fixedCodeConfigured = !string.IsNullOrEmpty(_authService?.FixedCode),
                        time = DateTime.UtcNow
                    });
                }
                else if (pathLower == "/api/auth/pair" && method == "POST")
                {
                    PairingRequest? pairReq = null;
                    if (!string.IsNullOrWhiteSpace(body))
                    {
                        try { pairReq = JsonSerializer.Deserialize<PairingRequest>(body, JsonOptions); } catch { }
                    }

                    if (pairReq == null)
                    {
                        responsePayload = new PairingResponse
                        {
                            Success = false,
                            ErrorCode = "INVALID_JSON",
                            Message = "Corpo da requisição de pareamento inválido."
                        };
                        statusCode = 400;
                    }
                    else if (_authService != null)
                    {
                        PairingResponse pRes = await _authService.PairDeviceAsync(pairReq, remoteEp?.Address?.ToString(), token);
                        responsePayload = pRes;
                        statusCode = pRes.Success ? 200 : (pRes.ErrorCode == "RATE_LIMITED" ? 429 : 401);
                    }
                    else
                    {
                        responsePayload = new PairingResponse
                        {
                            Success = false,
                            ErrorCode = "AUTH_SERVICE_UNAVAILABLE",
                            Message = "Serviço de autenticação pessoal indisponível."
                        };
                        statusCode = 500;
                    }
                }
                else if (pathLower == "/api/auth/verify")
                {
                    if (_authService != null)
                    {
                        DeviceAuthVerification ver = await _authService.VerifyDeviceTokenAsync(clientId, deviceToken, remoteEp?.Address?.ToString(), token);
                        if (ver.IsAuthorized && ver.Device != null)
                        {
                            responsePayload = AgentResponse<object>.Ok(new
                            {
                                authorized = true,
                                deviceId = ver.Device.DeviceId,
                                deviceName = ver.Device.DeviceName,
                                host = _config.DeviceName,
                                lastSeenAt = ver.Device.LastSeenAt
                            });
                            statusCode = 200;
                        }
                        else if (ver.IsRevoked)
                        {
                            responsePayload = AgentResponse<object>.Fail("DEVICE_REVOKED", ver.ErrorMessage ?? "A autorização deste dispositivo foi revogada pelo computador.");
                            statusCode = 401;
                        }
                        else
                        {
                            responsePayload = AgentResponse<object>.Fail("UNAUTHORIZED", ver.ErrorMessage ?? "Dispositivo não autorizado.");
                            statusCode = 401;
                        }
                    }
                    else
                    {
                        responsePayload = AgentResponse<object>.Ok(new { authorized = true });
                        statusCode = 200;
                    }
                }
                else if (pathLower == "/api/auth/devices" && method == "GET")
                {
                    bool isAuth = _authService == null || _authService.ValidateAccess(clientId, deviceToken, accessPin, remoteEp?.Address?.ToString()) || IPAddress.IsLoopback(remoteEp?.Address ?? IPAddress.None);
                    if (!isAuth)
                    {
                        responsePayload = AgentResponse<object>.Fail("UNAUTHORIZED", "Acesso não autorizado para consulta de dispositivos.");
                        statusCode = 401;
                    }
                    else
                    {
                        var devs = _authService != null ? await _authService.GetAuthorizedDevicesAsync(token) : Array.Empty<AuthorizedDevice>();
                        responsePayload = AgentResponse<object>.Ok(devs);
                        statusCode = 200;
                    }
                }
                else if (pathLower == "/api/auth/revoke" && method == "POST")
                {
                    bool isAuth = _authService == null || _authService.ValidateAccess(clientId, deviceToken, accessPin, remoteEp?.Address?.ToString()) || IPAddress.IsLoopback(remoteEp?.Address ?? IPAddress.None);
                    if (!isAuth)
                    {
                        responsePayload = AgentResponse<object>.Fail("UNAUTHORIZED", "Acesso não autorizado para revogação.");
                        statusCode = 401;
                    }
                    else
                    {
                        RevokeDeviceRequest? revReq = null;
                        if (!string.IsNullOrWhiteSpace(body))
                        {
                            try { revReq = JsonSerializer.Deserialize<RevokeDeviceRequest>(body, JsonOptions); } catch { }
                        }

                        if (revReq == null)
                        {
                            responsePayload = AgentResponse<object>.Fail("INVALID_JSON", "Payload inválido para revogação.");
                            statusCode = 400;
                        }
                        else if (revReq.RevokeAll)
                        {
                            int count = _authService != null ? await _authService.RevokeAllDevicesAsync(token) : 0;
                            responsePayload = AgentResponse<object>.Ok(new { revokedAll = true, count });
                            statusCode = 200;
                        }
                        else if (!string.IsNullOrWhiteSpace(revReq.DeviceId))
                        {
                            bool ok = _authService != null && await _authService.RevokeDeviceAsync(revReq.DeviceId, token);
                            responsePayload = ok
                                ? AgentResponse<object>.Ok(new { revoked = true, deviceId = revReq.DeviceId })
                                : AgentResponse<object>.Fail("DEVICE_NOT_FOUND", "Dispositivo não encontrado para revogação.");
                            statusCode = ok ? 200 : 404;
                        }
                        else
                        {
                            responsePayload = AgentResponse<object>.Fail("INVALID_PARAM", "Informe deviceId ou revokeAll: true.");
                            statusCode = 400;
                        }
                    }
                }
                else if (pathLower == "/api/auth/code" && method == "POST")
                {
                    bool isAuth = _authService == null || _authService.ValidateAccess(clientId, deviceToken, accessPin, remoteEp?.Address?.ToString()) || IPAddress.IsLoopback(remoteEp?.Address ?? IPAddress.None);
                    if (!isAuth)
                    {
                        responsePayload = AgentResponse<object>.Fail("UNAUTHORIZED", "Acesso não autorizado para alteração do código fixo.");
                        statusCode = 401;
                    }
                    else
                    {
                        ChangeFixedCodeRequest? chgReq = null;
                        if (!string.IsNullOrWhiteSpace(body))
                        {
                            try { chgReq = JsonSerializer.Deserialize<ChangeFixedCodeRequest>(body, JsonOptions); } catch { }
                        }

                        if (chgReq == null || string.IsNullOrWhiteSpace(chgReq.NewCode))
                        {
                            responsePayload = AgentResponse<object>.Fail("INVALID_PARAM", "Novo código fixo inválido ou ausente.");
                            statusCode = 400;
                        }
                        else
                        {
                            bool changed = _authService != null && await _authService.ChangeFixedCodeAsync(chgReq.CurrentCode, chgReq.NewCode, token);
                            responsePayload = changed
                                ? AgentResponse<object>.Ok(new { success = true, message = "Código fixo alterado com sucesso." })
                                : AgentResponse<object>.Fail("INVALID_CURRENT_CODE", "Código atual incorreto ou novo código não atende aos requisitos mínimos.");
                            statusCode = changed ? 200 : 400;
                        }
                    }
                }
                else if (pathLower == "/api/status")
                {
                    var authCheck = await CheckAuthorizationAsync(clientId, deviceToken, accessPin, remoteEp, token);
                    if (!authCheck.Allowed)
                    {
                        responsePayload = AgentResponse<object>.Fail(authCheck.ErrorCode ?? "UNAUTHORIZED", authCheck.Message ?? "Acesso não autorizado.");
                        statusCode = 401;
                    }
                    else
                    {
                        AgentResponse<object> resStatus = await _dispatcher.DispatchAsync(new AgentCommand
                        {
                            Action = "get-status",
                            AccessPin = accessPin,
                            ClientId = clientId,
                            DeviceToken = deviceToken
                        }, token);
                        responsePayload = resStatus;
                        if (!resStatus.Success) statusCode = resStatus.ErrorCode == "UNAUTHORIZED" ? 401 : 400;
                    }
                }
                else if (pathLower == "/api/windows")
                {
                    var authCheck = await CheckAuthorizationAsync(clientId, deviceToken, accessPin, remoteEp, token);
                    if (!authCheck.Allowed)
                    {
                        responsePayload = AgentResponse<object>.Fail(authCheck.ErrorCode ?? "UNAUTHORIZED", authCheck.Message ?? "Acesso não autorizado.");
                        statusCode = 401;
                    }
                    else
                    {
                        AgentResponse<object> resWindows = await _dispatcher.DispatchAsync(new AgentCommand
                        {
                            Action = "list-windows",
                            AccessPin = accessPin,
                            ClientId = clientId,
                            DeviceToken = deviceToken
                        }, token);
                        responsePayload = resWindows;
                        if (!resWindows.Success) statusCode = resWindows.ErrorCode == "UNAUTHORIZED" ? 401 : 400;
                    }
                }
                else if (pathLower == "/api/windows/active")
                {
                    var authCheck = await CheckAuthorizationAsync(clientId, deviceToken, accessPin, remoteEp, token);
                    if (!authCheck.Allowed)
                    {
                        responsePayload = AgentResponse<object>.Fail(authCheck.ErrorCode ?? "UNAUTHORIZED", authCheck.Message ?? "Acesso não autorizado.");
                        statusCode = 401;
                    }
                    else
                    {
                        AgentResponse<object> resActive = await _dispatcher.DispatchAsync(new AgentCommand
                        {
                            Action = "get-active-window",
                            AccessPin = accessPin,
                            ClientId = clientId,
                            DeviceToken = deviceToken
                        }, token);
                        responsePayload = resActive;
                        if (!resActive.Success) statusCode = resActive.ErrorCode == "UNAUTHORIZED" ? 401 : 400;
                    }
                }
                else if (pathLower == "/api/stream")
                {
                    var authCheck = await CheckAuthorizationAsync(clientId, deviceToken, accessPin, remoteEp, token);
                    if (!authCheck.Allowed)
                    {
                        byte[] err = JsonSerializer.SerializeToUtf8Bytes(AgentResponse<object>.Fail(authCheck.ErrorCode ?? "UNAUTHORIZED", authCheck.Message ?? "PIN ou token incorreto ou ausente."), JsonOptions);
                        await WriteResponseAsync(stream, 401, "Unauthorized", err, token);
                        return;
                    }

                    string targetWindowId = queryParams.TryGetValue("windowId", out var qWid) ? qWid : (headers.TryGetValue("X-Window-Id", out var hWid) ? hWid : string.Empty);
                    int fps = queryParams.TryGetValue("fps", out var qFps) && int.TryParse(qFps, out int parsedFps) ? Math.Clamp(parsedFps, 1, 30) : 15;

                    if (string.IsNullOrWhiteSpace(targetWindowId))
                    {
                        byte[] err = JsonSerializer.SerializeToUtf8Bytes(AgentResponse<object>.Fail("INVALID_PARAM", "O parametro 'windowId' e obrigatorio."), JsonOptions);
                        await WriteResponseAsync(stream, 400, "Bad Request", err, token);
                        return;
                    }

                    await HandleStreamAsync(stream, targetWindowId, fps, remoteEp, token);
                    return;
                }
                else if (pathLower == "/api/window/frame")
                {
                    var authCheck = await CheckAuthorizationAsync(clientId, deviceToken, accessPin, remoteEp, token);
                    if (!authCheck.Allowed)
                    {
                        byte[] err = JsonSerializer.SerializeToUtf8Bytes(AgentResponse<object>.Fail(authCheck.ErrorCode ?? "UNAUTHORIZED", authCheck.Message ?? "PIN ou token incorreto ou ausente."), JsonOptions);
                        await WriteResponseAsync(stream, 401, "Unauthorized", err, token);
                        return;
                    }

                    string targetWindowId = queryParams.TryGetValue("windowId", out var qWid) ? qWid : (headers.TryGetValue("X-Window-Id", out var hWid) ? hWid : string.Empty);
                    if (string.IsNullOrWhiteSpace(targetWindowId))
                    {
                        byte[] err = JsonSerializer.SerializeToUtf8Bytes(AgentResponse<object>.Fail("INVALID_PARAM", "O parametro 'windowId' e obrigatorio."), JsonOptions);
                        await WriteResponseAsync(stream, 400, "Bad Request", err, token);
                        return;
                    }

                    CaptureResult frameResult = await _captureService.CaptureWindowFrameAsync(targetWindowId, token);
                    if (frameResult.Success && frameResult.FrameData != null)
                    {
                        var extraHeaders = new Dictionary<string, string>
                        {
                            ["X-Window-Id"] = frameResult.WindowId,
                            ["X-Window-Width"] = frameResult.Width.ToString(),
                            ["X-Window-Height"] = frameResult.Height.ToString(),
                            ["X-Window-Minimized"] = "false",
                            ["X-Window-Closed"] = "false"
                        };
                        await WriteRawResponseAsync(stream, 200, "OK", "image/bmp", frameResult.FrameData, extraHeaders, token);
                        return;
                    }
                    else if (frameResult.IsMinimized)
                    {
                        var extraHeaders = new Dictionary<string, string>
                        {
                            ["X-Window-Id"] = frameResult.WindowId,
                            ["X-Window-Minimized"] = "true",
                            ["X-Window-Closed"] = "false"
                        };
                        byte[] payload = JsonSerializer.SerializeToUtf8Bytes(AgentResponse<object>.Fail("WINDOW_MINIMIZED", frameResult.ErrorMessage ?? "A janela esta minimizada."), JsonOptions);
                        await WriteRawResponseAsync(stream, 200, "OK", "application/json; charset=utf-8", payload, extraHeaders, token);
                        return;
                    }
                    else if (frameResult.WindowClosed)
                    {
                        var extraHeaders = new Dictionary<string, string>
                        {
                            ["X-Window-Id"] = frameResult.WindowId,
                            ["X-Window-Minimized"] = "false",
                            ["X-Window-Closed"] = "true"
                        };
                        byte[] payload = JsonSerializer.SerializeToUtf8Bytes(AgentResponse<object>.Fail("WINDOW_CLOSED", frameResult.ErrorMessage ?? "A janela foi fechada."), JsonOptions);
                        await WriteRawResponseAsync(stream, 404, "Not Found", "application/json; charset=utf-8", payload, extraHeaders, token);
                        return;
                    }
                    else
                    {
                        byte[] payload = JsonSerializer.SerializeToUtf8Bytes(AgentResponse<object>.Fail("CAPTURE_FAILED", frameResult.ErrorMessage ?? "Falha ao capturar a janela."), JsonOptions);
                        await WriteResponseAsync(stream, 500, "Internal Server Error", payload, token);
                        return;
                    }
                }
                else if (pathLower == "/api/window/resize")
                {
                    var authCheck = await CheckAuthorizationAsync(clientId, deviceToken, accessPin, remoteEp, token);
                    if (!authCheck.Allowed)
                    {
                        byte[] err = JsonSerializer.SerializeToUtf8Bytes(AgentResponse<object>.Fail(authCheck.ErrorCode ?? "UNAUTHORIZED", authCheck.Message ?? "PIN ou token incorreto ou ausente."), JsonOptions);
                        await WriteResponseAsync(stream, 401, "Unauthorized", err, token);
                        return;
                    }

                    string targetWindowId = queryParams.TryGetValue("windowId", out var qWid) ? qWid : (headers.TryGetValue("X-Window-Id", out var hWid) ? hWid : string.Empty);
                    int targetW = queryParams.TryGetValue("width", out var qW) && int.TryParse(qW, out int pw) ? pw : 382;
                    int targetH = queryParams.TryGetValue("height", out var qH) && int.TryParse(qH, out int ph) ? ph : 706;

                    if (string.IsNullOrWhiteSpace(targetWindowId))
                    {
                        byte[] err = JsonSerializer.SerializeToUtf8Bytes(AgentResponse<object>.Fail("INVALID_PARAM", "O parametro 'windowId' e obrigatorio."), JsonOptions);
                        await WriteResponseAsync(stream, 400, "Bad Request", err, token);
                        return;
                    }

                    WindowResizeResult resizeResult = await _windowService.ResizeWindowAsync(targetWindowId, targetW, targetH, token);
                    responsePayload = AgentResponse<object>.Ok(resizeResult);
                    statusCode = resizeResult.Success ? 200 : 400;
                }
                else if (pathLower == "/api/input" && method == "POST")
                {
                    var authCheck = await CheckAuthorizationAsync(clientId, deviceToken, accessPin, remoteEp, token);
                    if (!authCheck.Allowed)
                    {
                        byte[] err = JsonSerializer.SerializeToUtf8Bytes(AgentResponse<object>.Fail(authCheck.ErrorCode ?? "UNAUTHORIZED", authCheck.Message ?? "PIN ou token incorreto ou ausente."), JsonOptions);
                        await WriteResponseAsync(stream, 401, "Unauthorized", err, token);
                        return;
                    }

                    AgentCommand? inputCmd = null;
                    if (!string.IsNullOrWhiteSpace(body))
                    {
                        try { inputCmd = JsonSerializer.Deserialize<AgentCommand>(body, JsonOptions); } catch { }
                    }

                    if (inputCmd == null)
                    {
                        responsePayload = AgentResponse<object>.Fail("INVALID_JSON", "Payload inválido para entrada.");
                        statusCode = 400;
                    }
                    else
                    {
                        AgentCommand toDispatch = new()
                        {
                            Action = !string.IsNullOrWhiteSpace(inputCmd.Action) ? inputCmd.Action : "mouse-input",
                            TargetWindowId = inputCmd.TargetWindowId,
                            AccessPin = !string.IsNullOrWhiteSpace(inputCmd.AccessPin) ? inputCmd.AccessPin : accessPin,
                            ClientId = !string.IsNullOrWhiteSpace(inputCmd.ClientId) ? inputCmd.ClientId : clientId,
                            DeviceToken = !string.IsNullOrWhiteSpace(inputCmd.DeviceToken) ? inputCmd.DeviceToken : deviceToken,
                            X = inputCmd.X,
                            Y = inputCmd.Y,
                            NormalizedX = inputCmd.NormalizedX,
                            NormalizedY = inputCmd.NormalizedY,
                            MouseAction = inputCmd.MouseAction,
                            Button = inputCmd.Button,
                            DeltaY = inputCmd.DeltaY,
                            Key = inputCmd.Key,
                            Modifiers = inputCmd.Modifiers,
                            IsKeyDown = inputCmd.IsKeyDown,
                            Text = inputCmd.Text,
                            Timestamp = inputCmd.Timestamp
                        };
                        AgentResponse<object> resInput = await _dispatcher.DispatchAsync(toDispatch, token);
                        responsePayload = resInput;
                        statusCode = resInput.Success ? 200 : 400;
                    }
                }
                else if (pathLower == "/api/command" && method == "POST")
                {
                    var authCheck = await CheckAuthorizationAsync(clientId, deviceToken, accessPin, remoteEp, token);
                    if (!authCheck.Allowed)
                    {
                        responsePayload = AgentResponse<object>.Fail(authCheck.ErrorCode ?? "UNAUTHORIZED", authCheck.Message ?? "Acesso não autorizado.");
                        statusCode = 401;
                    }
                    else
                    {
                        AgentCommand? cmd = null;
                        if (!string.IsNullOrWhiteSpace(body))
                        {
                            try
                            {
                                cmd = JsonSerializer.Deserialize<AgentCommand>(body, JsonOptions);
                            }
                            catch
                            {
                                // JSON malformado
                            }
                        }

                        if (cmd == null)
                        {
                            responsePayload = AgentResponse<object>.Fail("INVALID_JSON", "O corpo da requisicao deve ser um JSON valido no formato AgentCommand.");
                            statusCode = 400;
                        }
                        else
                        {
                            // Inicializa o comando final utilizando inicializador de objeto para respeitar as propriedades 'init'.
                            // Preserva credenciais informadas no corpo e utiliza os cabecalhos HTTP como fallback.
                            AgentCommand commandToDispatch = new()
                            {
                                Action = cmd.Action,
                                TargetWindowId = cmd.TargetWindowId,
                                AccessPin = !string.IsNullOrWhiteSpace(cmd.AccessPin) ? cmd.AccessPin : accessPin,
                                ClientId = !string.IsNullOrWhiteSpace(cmd.ClientId) ? cmd.ClientId : clientId,
                                DeviceToken = !string.IsNullOrWhiteSpace(cmd.DeviceToken) ? cmd.DeviceToken : deviceToken,
                                Timestamp = cmd.Timestamp
                            };

                            AgentResponse<object> dispatchRes = await _dispatcher.DispatchAsync(commandToDispatch, token);
                            responsePayload = dispatchRes;
                            if (!dispatchRes.Success)
                            {
                                statusCode = dispatchRes.ErrorCode == "UNAUTHORIZED" ? 401 : 400;
                            }
                        }
                    }
                }
                else
                {
                    responsePayload = AgentResponse<object>.Fail("NOT_FOUND", $"Rota '{path}' nao encontrada no Agente.");
                    statusCode = 404;
                }

                byte[] jsonBytes = JsonSerializer.SerializeToUtf8Bytes(responsePayload, JsonOptions);
                string statusDesc = statusCode switch
                {
                    200 => "OK",
                    400 => "Bad Request",
                    401 => "Unauthorized",
                    404 => "Not Found",
                    500 => "Internal Server Error",
                    _ => "OK"
                };

                await WriteResponseAsync(stream, statusCode, statusDesc, jsonBytes, token);
                _logger.LogInformation("[REDE LOCAL] Resposta enviada: HTTP {StatusCode} ({Bytes} bytes) para {RemoteIp}",
                    statusCode, jsonBytes.Length, remoteEp?.Address);

                try
                {
                    client.Client.Shutdown(SocketShutdown.Send);
                }
                catch
                {
                    // Ignora falha de shutdown caso o cliente ja tenha encerrado o canal
                }
            }
            catch (Exception ex)
            {
                _logger.LogWarning("[REDE LOCAL] Erro ao processar conexao de {RemoteIp}: {Msg}",
                    remoteEp?.Address, ex.Message);
            }
        }
    }

    private async Task<(bool Allowed, string? ErrorCode, string? Message)> CheckAuthorizationAsync(
        string? clientId,
        string? deviceToken,
        string? accessPin,
        IPEndPoint? remoteEp,
        CancellationToken token)
    {
        if (_authService == null)
        {
            return (true, null, null);
        }

        // 1. Verificação primária via Token Persistente (Etapa 7)
        if (!string.IsNullOrWhiteSpace(clientId) && !string.IsNullOrWhiteSpace(deviceToken))
        {
            var verification = await _authService.VerifyDeviceTokenAsync(clientId, deviceToken, remoteEp?.Address?.ToString(), token);
            if (verification.IsAuthorized)
            {
                return (true, null, null);
            }

            if (verification.IsRevoked)
            {
                return (false, "DEVICE_REVOKED", verification.ErrorMessage ?? "A autorização deste dispositivo foi revogada pelo computador.");
            }

            return (false, verification.ErrorCode ?? "UNAUTHORIZED", verification.ErrorMessage ?? "Dispositivo não autorizado.");
        }

        // 2. Validação rápida (Token, PIN de acesso fixo ou compatibilidade legada)
        if (_authService.ValidateAccess(clientId, deviceToken, accessPin, remoteEp?.Address?.ToString()))
        {
            return (true, null, null);
        }

        // 3. Credenciais ausentes ou inválidas
        if (string.IsNullOrWhiteSpace(deviceToken) && string.IsNullOrWhiteSpace(accessPin))
        {
            return (false, "UNAUTHORIZED", "Credenciais de autorização ausentes. É necessário informar o token persistente (X-Device-Token) ou PIN.");
        }

        return (false, "UNAUTHORIZED", "PIN ou token de autorização incorreto.");
    }

    private static async Task WriteResponseAsync(NetworkStream stream, int statusCode, string statusDesc, byte[] bodyBytes, CancellationToken token)
    {
        StringBuilder headerBuilder = new();
        headerBuilder.Append($"HTTP/1.1 {statusCode} {statusDesc}\r\n");
        headerBuilder.Append("Content-Type: application/json; charset=utf-8\r\n");
        headerBuilder.Append($"Content-Length: {bodyBytes.Length}\r\n");
        headerBuilder.Append("Access-Control-Allow-Origin: *\r\n");
        headerBuilder.Append("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n");
        headerBuilder.Append("Access-Control-Allow-Headers: Content-Type, X-Access-Pin, X-Client-Id, X-Device-Token\r\n");
        headerBuilder.Append("Connection: close\r\n");
        headerBuilder.Append("\r\n");

        byte[] headerBytes = Encoding.UTF8.GetBytes(headerBuilder.ToString());
        await stream.WriteAsync(headerBytes, token);
        if (bodyBytes.Length > 0)
        {
            await stream.WriteAsync(bodyBytes, token);
        }
        await stream.FlushAsync(token);
    }

    private static async Task WriteRawResponseAsync(
        NetworkStream stream,
        int statusCode,
        string statusDesc,
        string contentType,
        byte[] bodyBytes,
        IDictionary<string, string>? extraHeaders,
        CancellationToken token)
    {
        StringBuilder headerBuilder = new();
        headerBuilder.Append($"HTTP/1.1 {statusCode} {statusDesc}\r\n");
        headerBuilder.Append($"Content-Type: {contentType}\r\n");
        headerBuilder.Append($"Content-Length: {bodyBytes.Length}\r\n");
        headerBuilder.Append("Access-Control-Allow-Origin: *\r\n");
        headerBuilder.Append("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n");
        headerBuilder.Append("Access-Control-Allow-Headers: Content-Type, X-Access-Pin, X-Client-Id, X-Device-Token, X-Window-Id\r\n");
        headerBuilder.Append("Access-Control-Expose-Headers: X-Window-Id, X-Window-Width, X-Window-Height, X-Window-Minimized, X-Window-Closed\r\n");
        headerBuilder.Append("Connection: close\r\n");

        if (extraHeaders != null)
        {
            foreach (var kvp in extraHeaders)
            {
                headerBuilder.Append($"{kvp.Key}: {kvp.Value}\r\n");
            }
        }
        headerBuilder.Append("\r\n");

        byte[] headerBytes = Encoding.UTF8.GetBytes(headerBuilder.ToString());
        await stream.WriteAsync(headerBytes, token);
        if (bodyBytes.Length > 0)
        {
            await stream.WriteAsync(bodyBytes, token);
        }
        await stream.FlushAsync(token);
    }

    private async Task HandleStreamAsync(NetworkStream stream, string windowId, int fps, IPEndPoint? remoteEp, CancellationToken token)
    {
        _logger.LogInformation("[STREAM] Iniciando transmissão contínua da janela {WindowId} para {RemoteIp} ({Fps} FPS)",
            windowId, remoteEp?.Address, fps);

        StringBuilder multipartInit = new();
        multipartInit.Append("HTTP/1.1 200 OK\r\n");
        multipartInit.Append("Content-Type: multipart/x-mixed-replace; boundary=--frame\r\n");
        multipartInit.Append("Cache-Control: no-cache, no-store, must-revalidate\r\n");
        multipartInit.Append("Pragma: no-cache\r\n");
        multipartInit.Append("Connection: close\r\n");
        multipartInit.Append("Access-Control-Allow-Origin: *\r\n");
        multipartInit.Append("Access-Control-Expose-Headers: X-Window-Id, X-Window-Width, X-Window-Height, X-Window-Minimized, X-Window-Closed\r\n");
        multipartInit.Append("\r\n");

        byte[] initBytes = Encoding.UTF8.GetBytes(multipartInit.ToString());
        await stream.WriteAsync(initBytes, token);
        await stream.FlushAsync(token);

        int frameIntervalMs = Math.Max(33, 1000 / fps);
        byte[] boundaryMarker = Encoding.UTF8.GetBytes("\r\n--frame\r\n");

        // Redimensionamento real e automatico da janela no Windows para a referencia 382 x 706
        // Garante que a aplicacao transmita no tamanho ideal de leitura calibrado pelo Bloco de Notas
        try
        {
            var resizeRes = await _windowService.ResizeWindowAsync(windowId, 382, 706, token);
            _logger.LogInformation("[STREAM] Janela {WindowId} auto-ajustada: original={OrigW}x{OrigH} -> efetivo={EffW}x{EffH} (Sucesso={Success})",
                windowId, resizeRes.OriginalWidth, resizeRes.OriginalHeight, resizeRes.EffectiveWidth, resizeRes.EffectiveHeight, resizeRes.Success);
        }
        catch (Exception ex)
        {
            _logger.LogWarning("[STREAM] Falha ao auto-ajustar dimensoes da janela {WindowId}: {Msg}", windowId, ex.Message);
        }

        try
        {
            while (!token.IsCancellationRequested)
            {
                var cap = await _captureService.CaptureWindowFrameAsync(windowId, token);

                if (cap.WindowClosed)
                {
                    _logger.LogInformation("[STREAM] Janela {WindowId} foi fechada no Windows. Notificando cliente.", windowId);
                    StringBuilder closedHeader = new();
                    closedHeader.Append("Content-Type: application/json\r\n");
                    closedHeader.Append("X-Window-Closed: true\r\n");
                    closedHeader.Append("Content-Length: 0\r\n\r\n");
                    byte[] chBytes = Encoding.UTF8.GetBytes(closedHeader.ToString());
                    await stream.WriteAsync(boundaryMarker, token);
                    await stream.WriteAsync(chBytes, token);
                    await stream.FlushAsync(token);
                    break;
                }

                if (cap.IsMinimized)
                {
                    StringBuilder minHeader = new();
                    minHeader.Append("Content-Type: application/json\r\n");
                    minHeader.Append("X-Window-Minimized: true\r\n");
                    minHeader.Append("Content-Length: 0\r\n\r\n");
                    byte[] mhBytes = Encoding.UTF8.GetBytes(minHeader.ToString());
                    await stream.WriteAsync(boundaryMarker, token);
                    await stream.WriteAsync(mhBytes, token);
                    await stream.FlushAsync(token);

                    await Task.Delay(500, token);
                    continue;
                }

                if (cap.Success && cap.FrameData != null)
                {
                    StringBuilder frameHeader = new();
                    frameHeader.Append("Content-Type: image/bmp\r\n");
                    frameHeader.Append($"Content-Length: {cap.FrameData.Length}\r\n");
                    frameHeader.Append($"X-Window-Id: {cap.WindowId}\r\n");
                    frameHeader.Append($"X-Window-Width: {cap.Width}\r\n");
                    frameHeader.Append($"X-Window-Height: {cap.Height}\r\n");
                    frameHeader.Append("X-Window-Minimized: false\r\n");
                    frameHeader.Append("X-Window-Closed: false\r\n");
                    frameHeader.Append("\r\n");

                    byte[] fhBytes = Encoding.UTF8.GetBytes(frameHeader.ToString());
                    await stream.WriteAsync(boundaryMarker, token);
                    await stream.WriteAsync(fhBytes, token);
                    await stream.WriteAsync(cap.FrameData, token);
                    await stream.FlushAsync(token);
                }

                await Task.Delay(frameIntervalMs, token);
            }
        }
        catch (Exception ex) when (ex is IOException or SocketException or OperationCanceledException)
        {
            _logger.LogInformation("[STREAM] Transmissao contínua encerrada para {WindowId} ({RemoteIp}): cliente desconectado.",
                windowId, remoteEp?.Address);
        }
        finally
        {
            // Libera com seguranca quaisquer botoes de mouse ou teclas retidos durante a sessao
            try
            {
                await _inputService.ReleaseAllInputsAsync(CancellationToken.None);
            }
            catch (Exception ex)
            {
                _logger.LogTrace("Excecao na liberacao de entradas no encerramento do stream: {Msg}", ex.Message);
            }
        }
    }

    public Task StopAsync(CancellationToken cancellationToken)
    {
        if (!_isRunning)
        {
            return Task.CompletedTask;
        }

        _isRunning = false;
        _cts?.Cancel();

        try
        {
            _tcpListener?.Stop();
        }
        catch (Exception ex)
        {
            _logger.LogTrace("Excecao ao fechar TcpListener: {Msg}", ex.Message);
        }

        _logger.LogInformation("Servidor local HTTP finalizado com sucesso.");
        return Task.CompletedTask;
    }

    public void Dispose()
    {
        _cts?.Dispose();
        _tcpListener?.Stop();
    }
}

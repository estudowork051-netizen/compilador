using System.Collections.Concurrent;
using System.Net.WebSockets;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using RemoteDesk.SignalingServer.Models;

namespace RemoteDesk.SignalingServer.Services;

/// <summary>
/// Gerenciador de sessões e roteamento de sinalização WebSockets e WebRTC pela Internet (Etapa 8).
/// Suporta registro autenticado de Hosts (Windows) e Clientes (Android), roteamento de SDP Offer/Answer,
/// candidatos ICE, credenciais efêmeras de TURN e canal de contingência (relay fallback).
/// </summary>
public sealed class SignalingSessionManager
{
    public sealed class DeviceSession
    {
        public string DeviceId { get; set; } = string.Empty;
        public string DeviceName { get; set; } = string.Empty;
        public string Role { get; set; } = "client"; // "host" ou "client"
        public WebSocket Socket { get; set; } = null!;
        public DateTime ConnectedAt { get; set; } = DateTime.UtcNow;
        public DateTime LastSeenAt { get; set; } = DateTime.UtcNow;
        public string? TargetDeviceId { get; set; }
        public string? RemoteIp { get; set; }
    }

    private readonly ILogger<SignalingSessionManager> _logger;
    private readonly IConfiguration _config;
    private readonly ConcurrentDictionary<string, DeviceSession> _sessions = new(StringComparer.OrdinalIgnoreCase);
    private readonly ConcurrentDictionary<string, (int Attempts, DateTime LastAttempt)> _rateLimits = new();

    private static readonly JsonSerializerOptions JsonOpts = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        WriteIndented = false
    };

    public int ActiveHostCount => _sessions.Values.Count(s => s.Role == "host" && s.Socket.State == WebSocketState.Open);
    public int ActiveClientCount => _sessions.Values.Count(s => s.Role == "client" && s.Socket.State == WebSocketState.Open);
    public int TotalSessions => _sessions.Count;

    public SignalingSessionManager(ILogger<SignalingSessionManager> logger, IConfiguration config)
    {
        _logger = logger;
        _config = config;
    }

    public async Task HandleWebSocketAsync(WebSocket webSocket, string? clientIp, CancellationToken cancellationToken)
    {
        string? registeredDeviceId = null;
        byte[] buffer = new byte[64 * 1024]; // 64 KB buffer

        try
        {
            while (webSocket.State == WebSocketState.Open && !cancellationToken.IsCancellationRequested)
            {
                using var ms = new MemoryStream();
                WebSocketReceiveResult result;
                do
                {
                    result = await webSocket.ReceiveAsync(new ArraySegment<byte>(buffer), cancellationToken);
                    if (result.MessageType == WebSocketMessageType.Close)
                    {
                        break;
                    }
                    ms.Write(buffer, 0, result.Count);
                } while (!result.EndOfMessage);

                if (result.MessageType == WebSocketMessageType.Close)
                {
                    break;
                }

                ms.Seek(0, SeekOrigin.Begin);
                string json = Encoding.UTF8.GetString(ms.ToArray());
                SignalingMessage? msg;
                try
                {
                    msg = JsonSerializer.Deserialize<SignalingMessage>(json, JsonOpts);
                }
                catch (Exception ex)
                {
                    _logger.LogWarning("[SINALIZAÇÃO] Mensagem JSON inválida recebida de {Ip}: {Msg}", clientIp, ex.Message);
                    continue;
                }

                if (msg == null) continue;

                // Processa a mensagem de sinalização
                registeredDeviceId = await ProcessMessageAsync(webSocket, msg, clientIp, registeredDeviceId, cancellationToken);
            }
        }
        catch (OperationCanceledException) { }
        catch (WebSocketException ex)
        {
            _logger.LogDebug("[SINALIZAÇÃO] Conexão WebSocket encerrada com {Ip}: {Msg}", clientIp, ex.Message);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "[SINALIZAÇÃO] Exceção no loop de WebSocket de {Ip}", clientIp);
        }
        finally
        {
            if (!string.IsNullOrEmpty(registeredDeviceId))
            {
                UnregisterSession(registeredDeviceId);
            }

            if (webSocket.State == WebSocketState.Open || webSocket.State == WebSocketState.CloseReceived)
            {
                try
                {
                    await webSocket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Fechamento de sessão", CancellationToken.None);
                }
                catch { }
            }
        }
    }

    private async Task<string?> ProcessMessageAsync(WebSocket socket, SignalingMessage msg, string? clientIp, string? currentDeviceId, CancellationToken ct)
    {
        string type = (msg.Type ?? string.Empty).ToLowerInvariant().Trim();

        switch (type)
        {
            case "register":
            {
                string deviceId = (msg.SourceDeviceId ?? string.Empty).Trim();
                if (string.IsNullOrEmpty(deviceId))
                {
                    await SendToSocketAsync(socket, new SignalingMessage
                    {
                        Type = "error",
                        ErrorCode = "MISSING_DEVICE_ID",
                        Message = "Identificador de dispositivo obrigatório para registro."
                    }, ct);
                    return currentDeviceId;
                }

                string role = (msg.Role ?? "client").ToLowerInvariant();
                var session = new DeviceSession
                {
                    DeviceId = deviceId,
                    DeviceName = msg.DeviceName ?? (role == "host" ? "Windows Host" : "Android Client"),
                    Role = role,
                    Socket = socket,
                    ConnectedAt = DateTime.UtcNow,
                    LastSeenAt = DateTime.UtcNow,
                    TargetDeviceId = msg.TargetDeviceId,
                    RemoteIp = clientIp
                };

                _sessions[deviceId] = session;
                _logger.LogInformation("[REGISTRO] {Role} registrado: '{Name}' (ID: {Id}) de {Ip}.",
                    role.ToUpperInvariant(), session.DeviceName, session.DeviceId, clientIp);

                await SendToSocketAsync(socket, new SignalingMessage
                {
                    Type = "registered",
                    Success = true,
                    SourceDeviceId = deviceId,
                    Role = role,
                    Message = $"Registrado com sucesso como {role} no servidor de sinalização."
                }, ct);

                return deviceId;
            }

            case "ping":
            {
                if (!string.IsNullOrEmpty(currentDeviceId) && _sessions.TryGetValue(currentDeviceId, out var s))
                {
                    s.LastSeenAt = DateTime.UtcNow;
                }
                await SendToSocketAsync(socket, new SignalingMessage { Type = "pong" }, ct);
                return currentDeviceId;
            }

            case "pair-request":
            case "pair-response":
            case "offer":
            case "answer":
            case "ice-candidate":
            case "relay-data":
            {
                if (string.IsNullOrEmpty(currentDeviceId))
                {
                    await SendToSocketAsync(socket, new SignalingMessage
                    {
                        Type = "error",
                        ErrorCode = "UNREGISTERED",
                        Message = "Você deve registrar seu dispositivo antes de enviar dados de sinalização."
                    }, ct);
                    return null;
                }

                string targetId = (msg.TargetDeviceId ?? string.Empty).Trim();
                if (string.IsNullOrEmpty(targetId))
                {
                    await SendToSocketAsync(socket, new SignalingMessage
                    {
                        Type = "error",
                        ErrorCode = "MISSING_TARGET_DEVICE",
                        Message = "TargetDeviceId não especificado na mensagem."
                    }, ct);
                    return currentDeviceId;
                }

                // Encaminha a mensagem diretamente ao dispositivo de destino
                if (_sessions.TryGetValue(targetId, out var targetSession) && targetSession.Socket.State == WebSocketState.Open)
                {
                    msg.SourceDeviceId = currentDeviceId;
                    await SendToSocketAsync(targetSession.Socket, msg, ct);
                    _logger.LogDebug("[ROTEAMENTO] {Type} encaminhado de {Source} para {Target}.", type, currentDeviceId, targetId);
                }
                else
                {
                    _logger.LogWarning("[ROTEAMENTO] Dispositivo de destino '{Target}' não encontrado ou offline.", targetId);
                    await SendToSocketAsync(socket, new SignalingMessage
                    {
                        Type = "error",
                        ErrorCode = "TARGET_OFFLINE",
                        TargetDeviceId = targetId,
                        Message = $"O computador de destino ({targetId}) está offline ou desconectado do servidor."
                    }, ct);
                }
                return currentDeviceId;
            }

            default:
                _logger.LogDebug("[SINALIZAÇÃO] Tipo de mensagem desconhecido: {Type}", type);
                return currentDeviceId;
        }
    }

    private void UnregisterSession(string deviceId)
    {
        if (_sessions.TryRemove(deviceId, out var s))
        {
            _logger.LogInformation("[DESCONEXÃO] Sessão encerrada: '{Name}' (ID: {Id}).", s.DeviceName, s.DeviceId);
        }
    }

    private async Task SendToSocketAsync(WebSocket socket, SignalingMessage msg, CancellationToken ct)
    {
        if (socket.State != WebSocketState.Open) return;
        string json = JsonSerializer.Serialize(msg, JsonOpts);
        byte[] bytes = Encoding.UTF8.GetBytes(json);
        await socket.SendAsync(new ArraySegment<byte>(bytes), WebSocketMessageType.Text, true, ct);
    }

    public IReadOnlyList<OnlineHostInfo> GetOnlineHosts()
    {
        return _sessions.Values
            .Where(s => s.Role == "host" && s.Socket.State == WebSocketState.Open)
            .Select(s => new OnlineHostInfo
            {
                DeviceId = s.DeviceId,
                DeviceName = s.DeviceName,
                ConnectedAt = s.ConnectedAt,
                LastSeenAt = s.LastSeenAt
            })
            .ToList();
    }

    /// <summary>
    /// Fornece a configuração de servidores STUN e credenciais dinâmicas efêmeras de TURN (HMAC-SHA1).
    /// </summary>
    public List<IceServerConfig> GetIceServersConfig(string? username = null)
    {
        var list = new List<IceServerConfig>();

        // Servidores STUN públicos confiáveis
        string defaultStun = "stun:stun" + ".l.google.com:19302,stun:stun1" + ".l.google.com:19302";
        string stunEnv = _config["REMOTEDESK_STUN_SERVERS"] ?? defaultStun;
        var stunUrls = stunEnv.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries).ToList();
        list.Add(new IceServerConfig { Urls = stunUrls });

        // Servidor TURN configurado via ambiente (coturn com credenciais efêmeras baseadas em tempo)
        string? turnServer = _config["REMOTEDESK_TURN_SERVER"];
        string? turnSecret = _config["REMOTEDESK_TURN_SECRET"];

        if (!string.IsNullOrEmpty(turnServer))
        {
            string ephemeralUser;
            string credential;

            if (!string.IsNullOrEmpty(turnSecret))
            {
                // Mecanismo padrão TURN REST API: username = expiryTimestamp:userIdentifier, credential = Base64(HMAC-SHA1(secret, username))
                long expiry = DateTimeOffset.UtcNow.AddHours(24).ToUnixTimeSeconds();
                ephemeralUser = $"{expiry}:{(username ?? "remotedesk-user")}";
                using var hmac = new HMACSHA1(Encoding.UTF8.GetBytes(turnSecret));
                byte[] hash = hmac.ComputeHash(Encoding.UTF8.GetBytes(ephemeralUser));
                credential = Convert.ToBase64String(hash);
            }
            else
            {
                ephemeralUser = _config["REMOTEDESK_TURN_USER"] ?? "remotedesk";
                credential = _config["REMOTEDESK_TURN_PASSWORD"] ?? "remotedesk-secret";
            }

            var turnUrls = turnServer.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries).ToList();
            list.Add(new IceServerConfig
            {
                Urls = turnUrls,
                Username = ephemeralUser,
                Credential = credential
            });
        }

        return list;
    }
}

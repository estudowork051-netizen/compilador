using System.Text.Json.Serialization;

namespace RemoteDesk.SignalingServer.Models;

/// <summary>
/// Mensagem de sinalização WebSockets transmitida entre clientes Android e Windows Agent.
/// </summary>
public sealed class SignalingMessage
{
    [JsonPropertyName("type")]
    public string Type { get; set; } = string.Empty;

    [JsonPropertyName("sourceDeviceId")]
    public string? SourceDeviceId { get; set; }

    [JsonPropertyName("targetDeviceId")]
    public string? TargetDeviceId { get; set; }

    [JsonPropertyName("role")]
    public string? Role { get; set; } // "host" ou "client"

    [JsonPropertyName("deviceName")]
    public string? DeviceName { get; set; }

    [JsonPropertyName("pin")]
    public string? Pin { get; set; }

    [JsonPropertyName("token")]
    public string? Token { get; set; }

    [JsonPropertyName("sdp")]
    public string? Sdp { get; set; }

    [JsonPropertyName("sdpType")]
    public string? SdpType { get; set; } // "offer" ou "answer"

    [JsonPropertyName("candidate")]
    public string? Candidate { get; set; }

    [JsonPropertyName("sdpMid")]
    public string? SdpMid { get; set; }

    [JsonPropertyName("sdpMLineIndex")]
    public int? SdpMLineIndex { get; set; }

    [JsonPropertyName("payload")]
    public string? Payload { get; set; }

    [JsonPropertyName("success")]
    public bool? Success { get; set; }

    [JsonPropertyName("errorCode")]
    public string? ErrorCode { get; set; }

    [JsonPropertyName("message")]
    public string? Message { get; set; }

    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
}

/// <summary>
/// Configuração de servidores ICE (STUN e TURN) fornecida aos clientes.
/// </summary>
public sealed class IceServerConfig
{
    [JsonPropertyName("urls")]
    public List<string> Urls { get; set; } = new();

    [JsonPropertyName("username")]
    public string? Username { get; set; }

    [JsonPropertyName("credential")]
    public string? Credential { get; set; }
}

/// <summary>
/// Informações públicas sobre um dispositivo host online registrado.
/// </summary>
public sealed class OnlineHostInfo
{
    [JsonPropertyName("deviceId")]
    public string DeviceId { get; set; } = string.Empty;

    [JsonPropertyName("deviceName")]
    public string DeviceName { get; set; } = string.Empty;

    [JsonPropertyName("connectedAt")]
    public DateTime ConnectedAt { get; set; }

    [JsonPropertyName("lastSeenAt")]
    public DateTime LastSeenAt { get; set; }
}

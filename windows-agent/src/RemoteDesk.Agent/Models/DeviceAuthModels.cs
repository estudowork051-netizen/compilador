using System.Text.Json.Serialization;

namespace RemoteDesk.Agent.Models;

/// <summary>
/// Representa um dispositivo cliente (ex: smartphone Android) autorizado a conectar-se ao agente Windows.
/// </summary>
public sealed class AuthorizedDevice
{
    /// <summary>
    /// Identificador único persistente do dispositivo cliente (ex: UUID gerado no Android).
    /// </summary>
    public string DeviceId { get; set; } = string.Empty;

    /// <summary>
    /// Nome amigável do dispositivo (ex: "Motorola Moto G", "DoHsktoP Mobile").
    /// </summary>
    public string DeviceName { get; set; } = "Android Client";

    /// <summary>
    /// Hash criptográfico (SHA-256) do token secreto de autorização gerado para o dispositivo.
    /// O token em texto claro nunca é armazenado no servidor.
    /// </summary>
    public string TokenHash { get; set; } = string.Empty;

    /// <summary>
    /// Data e hora UTC do pareamento inicial.
    /// </summary>
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    /// <summary>
    /// Data e hora UTC da última conexão bem-sucedida.
    /// </summary>
    public DateTime LastSeenAt { get; set; } = DateTime.UtcNow;

    /// <summary>
    /// Indica se a autorização deste dispositivo foi revogada pelo proprietário do PC.
    /// </summary>
    public bool IsRevoked { get; set; }

    /// <summary>
    /// Data e hora UTC da revogação, se aplicável.
    /// </summary>
    public DateTime? RevokedAt { get; set; }
}

/// <summary>
/// Modelo de solicitação para o primeiro pareamento entre o Android e o Windows.
/// </summary>
public sealed class PairingRequest
{
    /// <summary>
    /// Código fixo de pareamento definido pelo proprietário do computador.
    /// </summary>
    [JsonPropertyName("fixedCode")]
    public string FixedCode { get; set; } = string.Empty;

    /// <summary>
    /// Identificador único e persistente do dispositivo cliente.
    /// </summary>
    [JsonPropertyName("deviceId")]
    public string DeviceId { get; set; } = string.Empty;

    /// <summary>
    /// Nome amigável do dispositivo cliente.
    /// </summary>
    [JsonPropertyName("deviceName")]
    public string DeviceName { get; set; } = "Android Client";

    /// <summary>
    /// Versão do aplicativo cliente.
    /// </summary>
    [JsonPropertyName("clientVersion")]
    public string ClientVersion { get; set; } = "1.0.0";
}

/// <summary>
/// Resposta enviada ao cliente após tentativa de pareamento.
/// </summary>
public sealed class PairingResponse
{
    [JsonPropertyName("success")]
    public bool Success { get; set; }

    /// <summary>
    /// Token de autorização persistente emitido exclusivamente no pareamento bem-sucedido.
    /// Deve ser armazenado com segurança pelo Android no Keystore/SharedPreferences.
    /// </summary>
    [JsonPropertyName("deviceToken")]
    public string? DeviceToken { get; set; }

    [JsonPropertyName("deviceId")]
    public string DeviceId { get; set; } = string.Empty;

    [JsonPropertyName("hostDeviceName")]
    public string HostDeviceName { get; set; } = string.Empty;

    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    [JsonPropertyName("errorCode")]
    public string? ErrorCode { get; set; }
}

/// <summary>
/// Resultado da verificação de autorização de um dispositivo.
/// </summary>
public sealed class DeviceAuthVerification
{
    public bool IsAuthorized { get; set; }
    public bool IsRevoked { get; set; }
    public AuthorizedDevice? Device { get; set; }
    public string? ErrorCode { get; set; }
    public string? ErrorMessage { get; set; }

    public static DeviceAuthVerification Success(AuthorizedDevice device) =>
        new() { IsAuthorized = true, Device = device };

    public static DeviceAuthVerification Revoked(AuthorizedDevice device) =>
        new()
        {
            IsAuthorized = false,
            IsRevoked = true,
            Device = device,
            ErrorCode = "DEVICE_REVOKED",
            ErrorMessage = "A autorização deste dispositivo foi revogada pelo computador."
        };

    public static DeviceAuthVerification Failed(string code, string message) =>
        new()
        {
            IsAuthorized = false,
            ErrorCode = code,
            ErrorMessage = message
        };
}

/// <summary>
/// Requisição para revogação de dispositivo.
/// </summary>
public sealed class RevokeDeviceRequest
{
    [JsonPropertyName("deviceId")]
    public string? DeviceId { get; set; }

    [JsonPropertyName("revokeAll")]
    public bool RevokeAll { get; set; }
}

/// <summary>
/// Requisição para alteração do código fixo pelo proprietário.
/// </summary>
public sealed class ChangeFixedCodeRequest
{
    [JsonPropertyName("currentCode")]
    public string CurrentCode { get; set; } = string.Empty;

    [JsonPropertyName("newCode")]
    public string NewCode { get; set; } = string.Empty;
}

using System.Text.Json.Serialization;

namespace RemoteDesk.Agent.Models;

/// <summary>
/// Resposta padronizada emitida pelo agente Windows para qualquer solicitação interna ou remota.
/// </summary>
/// <typeparam name="T">Tipo do dado retornado no payload de sucesso.</typeparam>
public sealed class AgentResponse<T>
{
    [JsonPropertyName("success")]
    public bool Success { get; init; }

    [JsonPropertyName("errorCode")]
    public string? ErrorCode { get; init; }

    [JsonPropertyName("errorMessage")]
    public string? ErrorMessage { get; init; }

    [JsonPropertyName("data")]
    public T? Data { get; init; }

    [JsonPropertyName("timestamp")]
    public DateTime Timestamp { get; init; } = DateTime.UtcNow;

    public static AgentResponse<T> Ok(T data) => new()
    {
        Success = true,
        Data = data
    };

    public static AgentResponse<T> Fail(string errorCode, string errorMessage) => new()
    {
        Success = false,
        ErrorCode = errorCode,
        ErrorMessage = errorMessage
    };
}

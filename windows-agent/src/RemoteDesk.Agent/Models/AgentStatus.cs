using System.Text.Json.Serialization;

namespace RemoteDesk.Agent.Models;

/// <summary>
/// Informações de status operacional e pareamento do agente Windows.
/// Estruturado para facilitar a conexão simples entre o computador e o aplicativo Android.
/// </summary>
public sealed class AgentStatus
{
    /// <summary>
    /// Identificador persistente ou técnico do dispositivo.
    /// </summary>
    [JsonPropertyName("deviceId")]
    public string DeviceId { get; init; } = string.Empty;

    /// <summary>
    /// Nome amigável do computador Windows (ex: "DESKTOP-TRABALHO").
    /// </summary>
    [JsonPropertyName("deviceName")]
    public string DeviceName { get; init; } = string.Empty;

    /// <summary>
    /// Código amigável de pareamento (formato 9 dígitos: "123 456 789") para conexão rápida sem IP/porta.
    /// </summary>
    [JsonPropertyName("friendlyPairingCode")]
    public string FriendlyPairingCode { get; init; } = string.Empty;

    /// <summary>
    /// Estado atual da disponibilidade (ex: "Aguardando Conexão", "Em Sessão", "Pausado").
    /// </summary>
    [JsonPropertyName("availabilityState")]
    public string AvailabilityState { get; init; } = "Aguardando Conexão";

    /// <summary>
    /// Se o agente está pronto para receber solicitações de pareamento.
    /// </summary>
    [JsonPropertyName("isReady")]
    public bool IsReady { get; init; } = true;

    /// <summary>
    /// Quantidade de janelas de aplicativos disponíveis na sessão no momento.
    /// </summary>
    [JsonPropertyName("openWindowsCount")]
    public int OpenWindowsCount { get; init; }

    /// <summary>
    /// Quantidade de sessões ativas de controle remoto.
    /// </summary>
    [JsonPropertyName("activeSessionsCount")]
    public int ActiveSessionsCount { get; init; }

    /// <summary>
    /// Versão do agente hospedeiro.
    /// </summary>
    [JsonPropertyName("version")]
    public string Version { get; init; } = "1.0.0";

    /// <summary>
    /// Momento de inicialização do agente.
    /// </summary>
    [JsonPropertyName("startedAt")]
    public DateTime StartedAt { get; init; } = DateTime.UtcNow;
}

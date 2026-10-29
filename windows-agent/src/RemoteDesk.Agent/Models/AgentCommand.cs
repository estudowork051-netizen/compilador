using System.Text.Json.Serialization;

namespace RemoteDesk.Agent.Models;

/// <summary>
/// Contrato tipado de solicitação/comando enviado ao agente Windows.
/// </summary>
public sealed class AgentCommand
{
    /// <summary>
    /// Ação solicitada (ex: "list-windows", "get-status", "ping").
    /// </summary>
    [JsonPropertyName("action")]
    public string Action { get; init; } = string.Empty;

    /// <summary>
    /// Identificador opcional da janela alvo para comandos específicos.
    /// </summary>
    [JsonPropertyName("targetWindowId")]
    public string? TargetWindowId { get; init; }

    /// <summary>
    /// Código PIN ou token de acesso para validação de segurança.
    /// </summary>
    [JsonPropertyName("accessPin")]
    public string? AccessPin { get; init; }

    /// <summary>
    /// Identificador do cliente solicitante (ex: ID do dispositivo Android).
    /// </summary>
    [JsonPropertyName("clientId")]
    public string? ClientId { get; init; }

    /// <summary>
    /// Token de autorização persistente do dispositivo cliente (Etapa 7).
    /// </summary>
    [JsonPropertyName("deviceToken")]
    public string? DeviceToken { get; init; }

    /// <summary>
    /// Carimbo de data/hora do comando.
    /// </summary>
    [JsonPropertyName("timestamp")]
    public DateTime Timestamp { get; init; } = DateTime.UtcNow;

    [JsonPropertyName("x")]
    public int? X { get; init; }

    [JsonPropertyName("y")]
    public int? Y { get; init; }

    [JsonPropertyName("normalizedX")]
    public double? NormalizedX { get; init; }

    [JsonPropertyName("normalizedY")]
    public double? NormalizedY { get; init; }

    [JsonPropertyName("mouseAction")]
    public string? MouseAction { get; init; }

    [JsonPropertyName("button")]
    public string? Button { get; init; }

    [JsonPropertyName("deltaY")]
    public int? DeltaY { get; init; }

    [JsonPropertyName("key")]
    public string? Key { get; init; }

    [JsonPropertyName("modifiers")]
    public List<string>? Modifiers { get; init; }

    [JsonPropertyName("isKeyDown")]
    public bool? IsKeyDown { get; init; }

    [JsonPropertyName("text")]
    public string? Text { get; init; }

    /// <summary>
    /// Cria uma nova instância de AgentCommand associando a identidade do cliente autenticado e o token de sessão,
    /// preservando a imutabilidade das propriedades declaradas com 'init'.
    /// </summary>
    public AgentCommand WithClientContext(string? clientId, string? deviceToken, string? accessPin = null)
    {
        return new AgentCommand
        {
            Action = Action,
            TargetWindowId = TargetWindowId,
            AccessPin = !string.IsNullOrWhiteSpace(accessPin) ? accessPin : AccessPin,
            ClientId = !string.IsNullOrWhiteSpace(clientId) ? clientId : ClientId,
            DeviceToken = !string.IsNullOrWhiteSpace(deviceToken) ? deviceToken : DeviceToken,
            Timestamp = Timestamp,
            X = X,
            Y = Y,
            NormalizedX = NormalizedX,
            NormalizedY = NormalizedY,
            MouseAction = MouseAction,
            Button = Button,
            DeltaY = DeltaY,
            Key = Key,
            Modifiers = Modifiers != null ? new List<string>(Modifiers) : null,
            IsKeyDown = IsKeyDown,
            Text = Text
        };
    }
}

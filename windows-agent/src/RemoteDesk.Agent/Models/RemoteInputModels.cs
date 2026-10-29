using System.Text.Json.Serialization;

namespace RemoteDesk.Agent.Models;

/// <summary>
/// Solicitação de entrada de mouse remoto enviada pelo cliente Android.
/// </summary>
public sealed class RemoteMouseRequest
{
    [JsonPropertyName("targetWindowId")]
    public string TargetWindowId { get; init; } = string.Empty;

    [JsonPropertyName("x")]
    public int? X { get; init; }

    [JsonPropertyName("y")]
    public int? Y { get; init; }

    [JsonPropertyName("normalizedX")]
    public double? NormalizedX { get; init; }

    [JsonPropertyName("normalizedY")]
    public double? NormalizedY { get; init; }

    /// <summary>
    /// Ação do mouse: "move", "click", "down", "up", "double-click", "wheel".
    /// </summary>
    [JsonPropertyName("mouseAction")]
    public string MouseAction { get; init; } = "click";

    /// <summary>
    /// Botão: "left", "right", "middle".
    /// </summary>
    [JsonPropertyName("button")]
    public string Button { get; init; } = "left";

    /// <summary>
    /// Deslocamento vertical da roda de rolagem (positivo para cima, negativo para baixo).
    /// </summary>
    [JsonPropertyName("deltaY")]
    public int? DeltaY { get; init; }
}

/// <summary>
/// Solicitação de entrada de tecla virtual/física.
/// </summary>
public sealed class RemoteKeyRequest
{
    [JsonPropertyName("targetWindowId")]
    public string TargetWindowId { get; init; } = string.Empty;

    /// <summary>
    /// Nome da tecla especial ("Enter", "Backspace", "Tab", "Escape", "Delete", "Up", "Down", "Left", "Right", "Home", "End", "PageUp", "PageDown").
    /// </summary>
    [JsonPropertyName("key")]
    public string Key { get; init; } = string.Empty;

    /// <summary>
    /// Modificadores ativos ("Ctrl", "Alt", "Shift").
    /// </summary>
    [JsonPropertyName("modifiers")]
    public List<string>? Modifiers { get; init; }

    /// <summary>
    /// True para key down, False para key up, null para pressionamento completo (down + up).
    /// </summary>
    [JsonPropertyName("isKeyDown")]
    public bool? IsKeyDown { get; init; }
}

/// <summary>
/// Solicitação de digitação de texto Unicode.
/// </summary>
public sealed class RemoteTextRequest
{
    [JsonPropertyName("targetWindowId")]
    public string TargetWindowId { get; init; } = string.Empty;

    [JsonPropertyName("text")]
    public string Text { get; init; } = string.Empty;
}

/// <summary>
/// Resultado da execução de um comando de entrada remota no host.
/// </summary>
public sealed class RemoteInputResult
{
    public bool Success { get; init; }
    public string Action { get; init; } = string.Empty;
    public string TargetWindowId { get; init; } = string.Empty;
    public int ScreenX { get; init; }
    public int ScreenY { get; init; }
    public string? Message { get; init; }

    public static RemoteInputResult Ok(string action, string windowId, int screenX = 0, int screenY = 0, string? message = null)
    {
        return new RemoteInputResult
        {
            Success = true,
            Action = action,
            TargetWindowId = windowId,
            ScreenX = screenX,
            ScreenY = screenY,
            Message = message ?? "Entrada executada com sucesso."
        };
    }

    public static RemoteInputResult Fail(string action, string windowId, string message)
    {
        return new RemoteInputResult
        {
            Success = false,
            Action = action,
            TargetWindowId = windowId,
            Message = message
        };
    }
}

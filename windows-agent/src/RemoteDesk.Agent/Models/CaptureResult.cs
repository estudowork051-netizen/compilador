namespace RemoteDesk.Agent.Models;

/// <summary>
/// Representa o resultado de uma operacao de captura de quadro (frame) de uma janela especifica no Windows.
/// Contem os bytes da imagem (formato BMP 32bpp) ou o estado de minimizacao / encerramento da janela.
/// </summary>
public sealed class CaptureResult
{
    public string WindowId { get; init; } = string.Empty;
    public bool Success { get; init; }
    public byte[]? FrameData { get; init; }

    /// <summary>
    /// Alias para FrameData para compatibilidade com consumidores que utilizam FrameBytes.
    /// </summary>
    [System.Text.Json.Serialization.JsonIgnore]
    public byte[]? FrameBytes => FrameData;

    public int Width { get; init; }
    public int Height { get; init; }
    public bool IsMinimized { get; init; }
    public bool WindowClosed { get; init; }
    public string? ErrorMessage { get; init; }
    public DateTime Timestamp { get; init; } = DateTime.UtcNow;

    public static CaptureResult Ok(string windowId, byte[] frameData, int width, int height) => new()
    {
        WindowId = windowId,
        Success = true,
        FrameData = frameData,
        Width = width,
        Height = height,
        IsMinimized = false,
        WindowClosed = false,
        Timestamp = DateTime.UtcNow
    };

    public static CaptureResult Minimized(string windowId, string message = "A janela selecionada está minimizada.") => new()
    {
        WindowId = windowId,
        Success = false,
        IsMinimized = true,
        WindowClosed = false,
        ErrorMessage = message,
        Timestamp = DateTime.UtcNow
    };

    public static CaptureResult Closed(string windowId, string message = "A janela selecionada foi encerrada ou não existe.") => new()
    {
        WindowId = windowId,
        Success = false,
        IsMinimized = false,
        WindowClosed = true,
        ErrorMessage = message,
        Timestamp = DateTime.UtcNow
    };

    public static CaptureResult Fail(string windowId, string error) => new()
    {
        WindowId = windowId,
        Success = false,
        IsMinimized = false,
        WindowClosed = false,
        ErrorMessage = error,
        Timestamp = DateTime.UtcNow
    };
}

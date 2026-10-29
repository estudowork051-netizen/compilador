namespace RemoteDesk.Agent.Models;

/// <summary>
/// Resultado detalhado da operacao de redimensionamento real de janela no Windows.
/// Registra dimensoes anteriores, solicitadas e efetivamente aplicadas pelo sistema operacional,
/// alem do estado anterior de maximizacao/minimizacao e escala DPI detectada.
/// </summary>
public sealed record WindowResizeResult
{
    public bool Success { get; init; }
    public string WindowId { get; init; } = string.Empty;
    public int OriginalWidth { get; init; }
    public int OriginalHeight { get; init; }
    public int TargetWidth { get; init; }
    public int TargetHeight { get; init; }
    public int EffectiveWidth { get; init; }
    public int EffectiveHeight { get; init; }
    public int ClientWidth { get; init; }
    public int ClientHeight { get; init; }
    public bool WasMaximized { get; init; }
    public bool WasMinimized { get; init; }
    public bool CanResize { get; init; } = true;
    public uint WindowDpi { get; init; } = 96;
    public string? Message { get; init; }
}

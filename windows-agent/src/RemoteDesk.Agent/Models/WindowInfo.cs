using System.Text.Json.Serialization;

namespace RemoteDesk.Agent.Models;

/// <summary>
/// Modelo estruturado contendo informacoes de uma janela de aplicativo na sessao interativa do Windows.
/// </summary>
public sealed class WindowInfo
{
    /// <summary>
    /// Identificador numerico nativo da janela (HWND) do Windows.
    /// Ignorado na serializacao JSON para seguranca de tipos de ponteiro.
    /// </summary>
    [JsonIgnore]
    public nint Handle { get; init; }

    /// <summary>
    /// Identificador textual unico da janela (ex: "0x000204A8") para transmissao segura ao Android.
    /// </summary>
    [JsonPropertyName("windowId")]
    public string WindowId { get; init; } = string.Empty;

    /// <summary>
    /// Titulo visivel exibido na barra de titulo da janela.
    /// </summary>
    [JsonPropertyName("title")]
    public string Title { get; init; } = string.Empty;

    /// <summary>
    /// Nome amigavel do executavel/processo proprietario (ex: "notepad", "chrome", "code").
    /// </summary>
    [JsonPropertyName("processName")]
    public string ProcessName { get; init; } = string.Empty;

    /// <summary>
    /// Identificador do Processo (PID) no sistema operacional.
    /// </summary>
    [JsonPropertyName("processId")]
    public int ProcessId { get; init; }

    /// <summary>
    /// Nome da classe Win32 da janela (ex: "Notepad", "Chrome_WidgetWin_1", "CabinetWClass").
    /// </summary>
    [JsonPropertyName("className")]
    public string ClassName { get; init; } = string.Empty;

    /// <summary>
    /// Estado visual da janela (Normal, Minimizada, Maximizada).
    /// </summary>
    [JsonPropertyName("state")]
    public WindowState State { get; init; } = WindowState.Normal;

    /// <summary>
    /// Indica se a janela esta visivel na area de trabalho.
    /// </summary>
    [JsonPropertyName("isVisible")]
    public bool IsVisible { get; init; }

    /// <summary>
    /// Indica se a janela esta minimizada na barra de tarefas (permanece ativa na sessao).
    /// </summary>
    [JsonPropertyName("isMinimized")]
    public bool IsMinimized => State == WindowState.Minimized;

    /// <summary>
    /// Indica se a janela possui o foco atual do usuario (foreground window).
    /// </summary>
    [JsonPropertyName("isActive")]
    public bool IsActive { get; init; }

    /// <summary>
    /// Categoria semantica do icone para exibicao visual em grade no Android
    /// (ex: "browser", "editor", "terminal", "file_manager", "media", "calculator", "settings", "generic").
    /// </summary>
    [JsonPropertyName("iconType")]
    public string IconType { get; init; } = "generic";

    /// <summary>
    /// Icone do aplicativo ou executavel em formato Base64 (PNG), quando extraido com sucesso.
    /// </summary>
    [JsonPropertyName("iconBase64")]
    public string? IconBase64 { get; init; }

    /// <summary>
    /// Indica se a janela esta disponivel para futuras operacoes de captura e controle remoto.
    /// </summary>
    [JsonPropertyName("isAvailableForCapture")]
    public bool IsAvailableForCapture { get; init; }

    /// <summary>
    /// Coordenada horizontal superior esquerda da janela.
    /// </summary>
    [JsonPropertyName("x")]
    public int X { get; init; }

    /// <summary>
    /// Coordenada vertical superior esquerda da janela.
    /// </summary>
    [JsonPropertyName("y")]
    public int Y { get; init; }

    /// <summary>
    /// Largura da janela em pixels.
    /// </summary>
    [JsonPropertyName("width")]
    public int Width { get; init; }

    /// <summary>
    /// Altura da janela em pixels.
    /// </summary>
    [JsonPropertyName("height")]
    public int Height { get; init; }

    /// <summary>
    /// Caminho do arquivo executavel no disco, quando acessivel.
    /// </summary>
    [JsonPropertyName("executablePath")]
    public string? ExecutablePath { get; init; }

    /// <summary>
    /// Momento da deteccao da janela.
    /// </summary>
    [JsonPropertyName("detectedAt")]
    public DateTime DetectedAt { get; init; } = DateTime.UtcNow;

    public override string ToString()
    {
        return $"[{WindowId}] {Title} ({ProcessName}, PID: {ProcessId}, Classe: {ClassName}, Estado: {State}, Ativa: {IsActive})";
    }
}

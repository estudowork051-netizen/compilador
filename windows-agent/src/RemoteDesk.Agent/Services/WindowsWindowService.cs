using System.ComponentModel;
using System.Diagnostics;
using System.Text;
using Microsoft.Extensions.Logging;
using RemoteDesk.Agent.Models;
using RemoteDesk.Agent.Native;

namespace RemoteDesk.Agent.Services;

/// <summary>
/// Implementacao avancada do servico de janelas utilizando chamadas nativas Win32 (User32)
/// para enumerar, filtrar e inspecionar janelas de aplicativos reais na sessao interativa do usuario.
/// </summary>
public sealed class WindowsWindowService : IWindowService
{
    private readonly ILogger<WindowsWindowService> _logger;

    // Classes de janelas tecnicas e de infraestrutura do Windows que devem ser filtradas
    private static readonly HashSet<string> IgnoredWindowClasses = new(StringComparer.OrdinalIgnoreCase)
    {
        "Progman",
        "WorkerW",
        "Shell_TrayWnd",
        "Shell_SecondaryTrayWnd",
        "NotifyIconOverflowWindow",
        "EdgeUiInputTopWndClass",
        "EdgeUiInputWndClass",
        "TaskListOverlayWnd",
        "DummyDWMListenerWindow",
        "Dwm"
    };

    // Cache simples com controle de concorrencia e TTL para evitar chamadas excessivas ao User32
    private readonly object _cacheLock = new();
    private IReadOnlyList<WindowInfo>? _cachedWindows;
    private DateTime _cacheTimestamp = DateTime.MinValue;
    private static readonly TimeSpan CacheDuration = TimeSpan.FromMilliseconds(500);

    public WindowsWindowService(ILogger<WindowsWindowService> logger)
    {
        _logger = logger;
    }

    public int GetCurrentSessionId()
    {
        try
        {
            return Process.GetCurrentProcess().SessionId;
        }
        catch (Exception ex)
        {
            _logger.LogTrace("Nao foi possivel determinar o SessionId: {Msg}", ex.Message);
            return -1;
        }
    }

    public Task<IReadOnlyList<WindowInfo>> GetOpenWindowsAsync(CancellationToken cancellationToken = default)
    {
        if (!OperatingSystem.IsWindows())
        {
            _logger.LogWarning("[AVISO] Enumeracao de janelas Win32 nao suportada no sistema operacional atual ({OS}). Retornando colecao vazia.", Environment.OSVersion.Platform);
            IReadOnlyList<WindowInfo> empty = Array.Empty<WindowInfo>();
            return Task.FromResult(empty);
        }

        // Verifica cache recente para poupar ciclos de CPU
        lock (_cacheLock)
        {
            if (_cachedWindows != null && (DateTime.UtcNow - _cacheTimestamp) < CacheDuration)
            {
                return Task.FromResult(_cachedWindows);
            }
        }

        List<WindowInfo> result = new();
        HashSet<nint> visitedHandles = new();
        nint shellWindow = User32.GetShellWindow();
        nint desktopWindow = User32.GetDesktopWindow();
        nint foregroundWindow = User32.GetForegroundWindow();

        try
        {
            User32.EnumWindows((hWnd, lParam) =>
            {
                if (cancellationToken.IsCancellationRequested)
                {
                    return false; // Interrompe enumeracao se cancelamento foi solicitado
                }

                // Ignora janelas invalidas, ja visitadas ou da area de trabalho/shell
                if (hWnd == 0 || hWnd == shellWindow || hWnd == desktopWindow || !visitedHandles.Add(hWnd))
                {
                    return true;
                }

                try
                {
                    // 1. Obter classe da janela para filtragem previa de infraestrutura
                    StringBuilder classBuilder = new(256);
                    User32.GetClassName(hWnd, classBuilder, classBuilder.Capacity);
                    string className = classBuilder.ToString().Trim();

                    if (IgnoredWindowClasses.Contains(className))
                    {
                        return true;
                    }

                    // 2. Verificar visibilidade e estado da janela
                    bool isVisible = User32.IsWindowVisible(hWnd);
                    bool isIconic = User32.IsIconic(hWnd); // Minimizada
                    bool isZoomed = User32.IsZoomed(hWnd); // Maximizada

                    // Janelas minimizadas continuam ativas para selecao e foco, mas podem nao ser visiveis
                    if (!isVisible && !isIconic)
                    {
                        return true;
                    }

                    // 3. Filtrar estilos estendidos (ToolWindows tecnicas sem AppWindow)
                    int exStyle = User32.GetWindowLong(hWnd, User32.GWL_EXSTYLE);
                    bool isToolWindow = (exStyle & User32.WS_EX_TOOLWINDOW) != 0;
                    bool isAppWindow = (exStyle & User32.WS_EX_APPWINDOW) != 0;

                    if (isToolWindow && !isAppWindow)
                    {
                        return true;
                    }

                    // 4. Obter informacoes do processo proprietario
                    User32.GetWindowThreadProcessId(hWnd, out uint processId);
                    if (processId == 0)
                    {
                        return true;
                    }

                    string processName = "Desconhecido";
                    string? executablePath = null;
                    try
                    {
                        using Process proc = Process.GetProcessById((int)processId);
                        processName = proc.ProcessName;
                        try
                        {
                            executablePath = proc.MainModule?.FileName;
                        }
                        catch
                        {
                            // Acesso negado para modulos de processos de outros usuarios ou elevados
                        }
                    }
                    catch (ArgumentException)
                    {
                        // O processo encerrou durante a consulta; ignora com seguranca sem falhar
                        return true;
                    }
                    catch (InvalidOperationException)
                    {
                        return true;
                    }
                    catch (Win32Exception)
                    {
                        processName = "Sistema / Protegido";
                    }
                    catch (Exception ex)
                    {
                        _logger.LogTrace("Excecao ao inspecionar PID {PID}: {Msg}", processId, ex.Message);
                    }

                    // 5. Obter titulo da janela com tratamento consistente para titulos vazios
                    int textLength = User32.GetWindowTextLength(hWnd);
                    string title = string.Empty;
                    if (textLength > 0)
                    {
                        StringBuilder titleBuilder = new(textLength + 1);
                        User32.GetWindowText(hWnd, titleBuilder, titleBuilder.Capacity);
                        title = titleBuilder.ToString().Trim();
                    }

                    // Tratamento conservador para titulos vazios:
                    // Se o titulo for vazio, apenas inclui se for uma janela principal de aplicativo (AppWindow ou sem Owner)
                    if (string.IsNullOrWhiteSpace(title))
                    {
                        nint owner = User32.GetWindow(hWnd, User32.GW_OWNER);
                        bool isTopLevelApp = isAppWindow || (owner == 0 && !isToolWindow);

                        if (!isTopLevelApp || string.Equals(className, "Windows.UI.Core.CoreWindow", StringComparison.OrdinalIgnoreCase))
                        {
                            return true; // Descarta janelas internas sem titulo
                        }

                        // Fornece titulo de identificacao consistente baseado no executavel
                        title = (!string.IsNullOrWhiteSpace(processName) && processName != "Desconhecido")
                            ? $"[{processName}]"
                            : "[Aplicativo sem título]";
                    }

                    // 6. Obter retangulo e dimensoes da janela
                    User32.GetWindowRect(hWnd, out User32.RECT rect);
                    int width = rect.Width;
                    int height = rect.Height;

                    // Para janelas minimizadas, obter posicao e tamanho normais de restauracao
                    if (isIconic)
                    {
                        User32.WINDOWPLACEMENT placement = new() { Length = MarshalSizeOfPlacement() };
                        if (User32.GetWindowPlacement(hWnd, ref placement))
                        {
                            width = placement.RcNormalPosition.Width;
                            height = placement.RcNormalPosition.Height;
                        }
                    }
                    else if (width <= 0 || height <= 0)
                    {
                        return true; // Janelas sem area util visivel
                    }

                    WindowState state = isIconic ? WindowState.Minimized
                        : isZoomed ? WindowState.Maximized
                        : WindowState.Normal;

                    string windowId = $"0x{hWnd:X8}";
                    bool isActive = (hWnd == foregroundWindow);

                    string iconType = CategorizeIconType(processName, className);

                    WindowInfo info = new()
                    {
                        Handle = hWnd,
                        WindowId = windowId,
                        Title = title,
                        ProcessName = processName,
                        ProcessId = (int)processId,
                        ClassName = className,
                        State = state,
                        IsVisible = isVisible || isIconic,
                        IsActive = isActive,
                        IconType = iconType,
                        IsAvailableForCapture = true,
                        X = rect.Left,
                        Y = rect.Top,
                        Width = width,
                        Height = height,
                        ExecutablePath = executablePath,
                        DetectedAt = DateTime.UtcNow
                    };

                    result.Add(info);
                }
                catch (Exception ex)
                {
                    _logger.LogTrace("Excecao ao processar janela hWnd {Hwnd}: {Msg}", hWnd, ex.Message);
                }

                return true; // Continua a enumeracao das proximas janelas
            }, 0);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "[ERRO] Falha ao executar EnumWindows na sessao do usuario.");
        }

        lock (_cacheLock)
        {
            _cachedWindows = result;
            _cacheTimestamp = DateTime.UtcNow;
        }

        _logger.LogDebug("Enumeracao concluida. Janelas ativas encontradas: {Count}", result.Count);
        return Task.FromResult<IReadOnlyList<WindowInfo>>(result);
    }

    public async Task<WindowInfo?> GetWindowByIdAsync(string windowId, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(windowId))
        {
            return null;
        }

        IReadOnlyList<WindowInfo> windows = await GetOpenWindowsAsync(cancellationToken);
        return windows.FirstOrDefault(w => string.Equals(w.WindowId, windowId, StringComparison.OrdinalIgnoreCase));
    }

    public async Task<WindowInfo?> GetActiveWindowAsync(CancellationToken cancellationToken = default)
    {
        if (!OperatingSystem.IsWindows())
        {
            return null;
        }

        try
        {
            nint foreground = User32.GetForegroundWindow();
            if (foreground == 0)
            {
                return null;
            }

            string targetId = $"0x{foreground:X8}";
            return await GetWindowByIdAsync(targetId, cancellationToken);
        }
        catch (Exception ex)
        {
            _logger.LogTrace("Excecao ao obter janela ativa: {Msg}", ex.Message);
            return null;
        }
    }

    public Task<bool> ActivateWindowAsync(string windowId, CancellationToken cancellationToken = default)
    {
        if (!OperatingSystem.IsWindows() || string.IsNullOrWhiteSpace(windowId))
        {
            return Task.FromResult(false);
        }

        try
        {
            cancellationToken.ThrowIfCancellationRequested();

            nint handle;
            if (windowId.StartsWith("0x", StringComparison.OrdinalIgnoreCase))
            {
                handle = (nint)Convert.ToInt64(windowId[2..], 16);
            }
            else if (!long.TryParse(windowId, out long numericId))
            {
                return Task.FromResult(false);
            }
            else
            {
                handle = (nint)numericId;
            }

            if (handle == 0 || !User32.IsWindow(handle))
            {
                _logger.LogWarning("[AVISO] Janela {WindowId} não é válida ou já foi encerrada.", windowId);
                return Task.FromResult(false);
            }

            // Se estiver minimizada, restaura antes de colocar em primeiro plano
            if (User32.IsIconic(handle))
            {
                User32.ShowWindow(handle, User32.SW_RESTORE);
            }

            bool activated = User32.SetForegroundWindow(handle);
            return Task.FromResult(activated);
        }
        catch (OperationCanceledException)
        {
            return Task.FromCanceled<bool>(cancellationToken);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[AVISO] Falha ao tentar ativar a janela {WindowId}.", windowId);
            return Task.FromResult(false);
        }
    }

    /// <summary>
    /// Traz a janela especificada para o primeiro plano (foco do usuário), restaurando-a caso esteja minimizada.
    /// Implementação direta do contrato BringWindowToFrontAsync compatível com ActivateWindowAsync.
    /// </summary>
    public Task<bool> BringWindowToFrontAsync(string windowId, CancellationToken cancellationToken = default)
    {
        return ActivateWindowAsync(windowId, cancellationToken);
    }

    public Task<WindowResizeResult> ResizeWindowAsync(string windowId, int targetWidth = 382, int targetHeight = 706, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(windowId))
        {
            return Task.FromResult(new WindowResizeResult
            {
                Success = false,
                WindowId = windowId ?? string.Empty,
                TargetWidth = targetWidth,
                TargetHeight = targetHeight,
                Message = "Identificador de janela nulo ou inválido."
            });
        }

        if (!OperatingSystem.IsWindows())
        {
            _logger.LogInformation("[RESIZE] Ambiente não-Windows ({OS}). Retornando simulação sintética de redimensionamento para {W}x{H}.",
                Environment.OSVersion.Platform, targetWidth, targetHeight);
            return Task.FromResult(new WindowResizeResult
            {
                Success = true,
                WindowId = windowId,
                OriginalWidth = 800,
                OriginalHeight = 600,
                TargetWidth = targetWidth,
                TargetHeight = targetHeight,
                EffectiveWidth = targetWidth,
                EffectiveHeight = targetHeight,
                ClientWidth = targetWidth - 16,
                ClientHeight = targetHeight - 39,
                WasMaximized = false,
                WasMinimized = false,
                CanResize = true,
                WindowDpi = 96,
                Message = "Simulação em ambiente não-Windows (CI/Linux)"
            });
        }

        try
        {
            nint handle;
            if (windowId.StartsWith("0x", StringComparison.OrdinalIgnoreCase))
            {
                handle = (nint)Convert.ToInt64(windowId[2..], 16);
            }
            else if (!long.TryParse(windowId, out long numericId))
            {
                return Task.FromResult(new WindowResizeResult
                {
                    Success = false,
                    WindowId = windowId,
                    TargetWidth = targetWidth,
                    TargetHeight = targetHeight,
                    Message = "Formato de identificador de janela inválido."
                });
            }
            else
            {
                handle = (nint)numericId;
            }

            if (handle == 0 || !User32.IsWindow(handle))
            {
                return Task.FromResult(new WindowResizeResult
                {
                    Success = false,
                    WindowId = windowId,
                    TargetWidth = targetWidth,
                    TargetHeight = targetHeight,
                    Message = "A janela informada não existe ou foi encerrada."
                });
            }

            // 1. Inspeciona dimensões originais antes do ajuste
            User32.GetWindowRect(handle, out User32.RECT origRect);
            int origWidth = origRect.Width;
            int origHeight = origRect.Height;

            bool wasMinimized = User32.IsIconic(handle);
            bool wasMaximized = User32.IsZoomed(handle);

            // 2. Se estiver minimizada ou maximizada, restaura para o estado normal (SW_RESTORE)
            // pois o Windows DWM ignora SetWindowPos quando a janela está em estado Maximizado.
            if (wasMinimized || wasMaximized)
            {
                _logger.LogInformation("[RESIZE] Janela {WindowId} estava {State}. Restaurando antes de aplicar dimensões...",
                    windowId, wasMinimized ? "minimizada" : "maximizada");
                User32.ShowWindow(handle, User32.SW_RESTORE);
                // Atualiza o retângulo após restauração
                User32.GetWindowRect(handle, out origRect);
            }

            // 3. Verifica capacidade de redimensionamento pelos estilos de janela (WS_THICKFRAME)
            int style = User32.GetWindowLong(handle, User32.GWL_STYLE);
            bool canResize = (style & 0x00040000) != 0; // WS_THICKFRAME (WS_SIZEBOX)

            uint dpi = User32.GetDpiForWindowSafe(handle);

            // 4. Aplica novo tamanho preservando a posição (Top-Left) na tela
            int targetX = origRect.Left;
            int targetY = origRect.Top;

            uint swpFlags = User32.SWP_NOZORDER | User32.SWP_NOACTIVATE | User32.SWP_FRAMECHANGED | User32.SWP_SHOWWINDOW;
            bool setPosSuccess = User32.SetWindowPos(handle, 0, targetX, targetY, targetWidth, targetHeight, swpFlags);

            if (!setPosSuccess)
            {
                _logger.LogWarning("[RESIZE] SetWindowPos falhou para {WindowId}. Tentando MoveWindow como fallback...", windowId);
                User32.MoveWindow(handle, targetX, targetY, targetWidth, targetHeight, true);
            }

            // 5. Confirma dimensões efetivas reais após o ajuste solicitado
            User32.GetWindowRect(handle, out User32.RECT effectiveRect);
            User32.GetClientRect(handle, out User32.RECT clientRect);

            int effectiveW = effectiveRect.Width;
            int effectiveH = effectiveRect.Height;
            int clientW = clientRect.Width;
            int clientH = clientRect.Height;

            // Invalida cache de janelas para que consultas subsequentes reflitam o novo tamanho
            lock (_cacheLock)
            {
                _cachedWindows = null;
                _cacheTimestamp = DateTime.MinValue;
            }

            bool isDimensionApplied = (effectiveW > 0 && effectiveH > 0);
            string msg = (effectiveW == targetWidth && effectiveH == targetHeight)
                ? "Redimensionamento para 382x706 aplicado com sucesso."
                : $"Redimensionamento aplicado. Dimensões efetivas: {effectiveW}x{effectiveH} (alvo: {targetWidth}x{targetHeight}).";

            _logger.LogInformation("[RESIZE] Janela {WindowId} redimensionada: original={OrigW}x{OrigH} -> efetivo={EffW}x{EffH} (cliente={CliW}x{CliH}, DPI={Dpi})",
                windowId, origWidth, origHeight, effectiveW, effectiveH, clientW, clientH, dpi);

            return Task.FromResult(new WindowResizeResult
            {
                Success = isDimensionApplied,
                WindowId = windowId,
                OriginalWidth = origWidth,
                OriginalHeight = origHeight,
                TargetWidth = targetWidth,
                TargetHeight = targetHeight,
                EffectiveWidth = effectiveW,
                EffectiveHeight = effectiveH,
                ClientWidth = clientW,
                ClientHeight = clientH,
                WasMaximized = wasMaximized,
                WasMinimized = wasMinimized,
                CanResize = canResize,
                WindowDpi = dpi,
                Message = msg
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "[RESIZE] Exceção ao redimensionar janela {WindowId}", windowId);
            return Task.FromResult(new WindowResizeResult
            {
                Success = false,
                WindowId = windowId,
                TargetWidth = targetWidth,
                TargetHeight = targetHeight,
                Message = $"Erro ao redimensionar: {ex.Message}"
            });
        }
    }

    private static int MarshalSizeOfPlacement()
    {
        return System.Runtime.InteropServices.Marshal.SizeOf<User32.WINDOWPLACEMENT>();
    }

    private static string CategorizeIconType(string processName, string className)
    {
        string p = processName.ToLowerInvariant();
        string c = className.ToLowerInvariant();

        if (p.Contains("chrome") || p.Contains("edge") || p.Contains("firefox") || p.Contains("brave") || p.Contains("opera") || c.Contains("chrome"))
            return "browser";
        if (p.Contains("code") || p.Contains("devenv") || p.Contains("notepad") || p.Contains("sublime") || c.Contains("notepad"))
            return "editor";
        if (p.Contains("cmd") || p.Contains("powershell") || p.Contains("wt") || p.Contains("terminal") || p.Contains("bash"))
            return "terminal";
        if (p.Contains("explorer") || c.Contains("cabinetwclass"))
            return "file_manager";
        if (p.Contains("spotify") || p.Contains("vlc") || p.Contains("wmplayer") || p.Contains("music") || p.Contains("video"))
            return "media";
        if (p.Contains("calc") || c.Contains("calc"))
            return "calculator";
        if (p.Contains("settings") || c.Contains("systemsettings"))
            return "settings";

        return "generic";
    }
}


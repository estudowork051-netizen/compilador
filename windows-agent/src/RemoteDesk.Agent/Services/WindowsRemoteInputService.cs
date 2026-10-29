using System.Runtime.InteropServices;
using Microsoft.Extensions.Logging;
using RemoteDesk.Agent.Models;
using RemoteDesk.Agent.Native;

namespace RemoteDesk.Agent.Services;

/// <summary>
/// Implementacao de controle remoto real utilizando P/Invoke User32 (SendInput e SetCursorPos).
/// Traduz com precisao toques e gestos do Android em acoes nativas de mouse e teclado no Windows.
/// </summary>
public sealed class WindowsRemoteInputService : IInputService
{
    private readonly ILogger<WindowsRemoteInputService> _logger;
    private readonly object _stateLock = new();

    private bool _isLeftButtonDown;
    private bool _isRightButtonDown;
    private bool _isMiddleButtonDown;
    private readonly HashSet<ushort> _heldKeys = new();

    public WindowsRemoteInputService(ILogger<WindowsRemoteInputService> logger)
    {
        _logger = logger;
    }

    public Task<RemoteInputResult> ProcessMouseAsync(RemoteMouseRequest request, CancellationToken cancellationToken = default)
    {
        if (request == null)
        {
            return Task.FromResult(RemoteInputResult.Fail("mouse", string.Empty, "Solicitação nula."));
        }

        string action = request.MouseAction?.ToLowerInvariant().Trim() ?? "click";
        string button = request.Button?.ToLowerInvariant().Trim() ?? "left";

        if (!OperatingSystem.IsWindows())
        {
            _logger.LogDebug("[INPUT-MOCK] ProcessMouseAsync em plataforma não-Windows ({OS}): Ação={Action}",
                Environment.OSVersion.Platform, action);
            return Task.FromResult(RemoteInputResult.Ok(action, request.TargetWindowId, screenX: request.X ?? 100, screenY: request.Y ?? 100, "Simulação não-Windows"));
        }

        if (!TryResolveWindow(request.TargetWindowId, out nint hWnd, out string err))
        {
            return Task.FromResult(RemoteInputResult.Fail(action, request.TargetWindowId, err));
        }

        try
        {
            // Garante foco na janela alvo
            EnsureWindowFocus(hWnd);

            User32.GetWindowRect(hWnd, out User32.RECT winRect);
            int winW = Math.Max(1, winRect.Width);
            int winH = Math.Max(1, winRect.Height);

            int screenX = winRect.Left + (winW / 2);
            int screenY = winRect.Top + (winH / 2);

            // Mapeamento de coordenadas precisas
            if (request.NormalizedX.HasValue && request.NormalizedY.HasValue)
            {
                double normX = Math.Clamp(request.NormalizedX.Value, 0.0, 1.0);
                double normY = Math.Clamp(request.NormalizedY.Value, 0.0, 1.0);
                screenX = winRect.Left + (int)Math.Round(normX * winW);
                screenY = winRect.Top + (int)Math.Round(normY * winH);
            }
            else if (request.X.HasValue && request.Y.HasValue)
            {
                screenX = winRect.Left + Math.Clamp(request.X.Value, 0, winW);
                screenY = winRect.Top + Math.Clamp(request.Y.Value, 0, winH);
            }

            // Move o cursor nativo para a posição mapeada
            if (action != "wheel" || request.NormalizedX.HasValue || request.X.HasValue)
            {
                User32.SetCursorPos(screenX, screenY);
            }

            switch (action)
            {
                case "move":
                    // Apenas movimento do cursor
                    break;

                case "click":
                    if (button == "right")
                    {
                        SendMouseButton(User32.MOUSEEVENTF_RIGHTDOWN);
                        SendMouseButton(User32.MOUSEEVENTF_RIGHTUP);
                    }
                    else if (button == "middle")
                    {
                        SendMouseButton(User32.MOUSEEVENTF_MIDDLEDOWN);
                        SendMouseButton(User32.MOUSEEVENTF_MIDDLEUP);
                    }
                    else
                    {
                        SendMouseButton(User32.MOUSEEVENTF_LEFTDOWN);
                        SendMouseButton(User32.MOUSEEVENTF_LEFTUP);
                    }
                    break;

                case "double-click":
                    SendMouseButton(User32.MOUSEEVENTF_LEFTDOWN);
                    SendMouseButton(User32.MOUSEEVENTF_LEFTUP);
                    Thread.Sleep(20);
                    SendMouseButton(User32.MOUSEEVENTF_LEFTDOWN);
                    SendMouseButton(User32.MOUSEEVENTF_LEFTUP);
                    break;

                case "down":
                    lock (_stateLock)
                    {
                        if (button == "right")
                        {
                            _isRightButtonDown = true;
                            SendMouseButton(User32.MOUSEEVENTF_RIGHTDOWN);
                        }
                        else
                        {
                            _isLeftButtonDown = true;
                            SendMouseButton(User32.MOUSEEVENTF_LEFTDOWN);
                        }
                    }
                    break;

                case "up":
                    lock (_stateLock)
                    {
                        if (button == "right")
                        {
                            _isRightButtonDown = false;
                            SendMouseButton(User32.MOUSEEVENTF_RIGHTUP);
                        }
                        else
                        {
                            _isLeftButtonDown = false;
                            SendMouseButton(User32.MOUSEEVENTF_LEFTUP);
                        }
                    }
                    break;

                case "wheel":
                    int delta = (request.DeltaY ?? 0) * User32.WHEEL_DELTA;
                    if (delta != 0)
                    {
                        SendMouseWheel(delta);
                    }
                    break;

                default:
                    return Task.FromResult(RemoteInputResult.Fail(action, request.TargetWindowId, $"Ação de mouse '{action}' não suportada."));
            }

            return Task.FromResult(RemoteInputResult.Ok(action, request.TargetWindowId, screenX, screenY));
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "[INPUT] Falha ao processar mouse para janela {WindowId}", request.TargetWindowId);
            return Task.FromResult(RemoteInputResult.Fail(action, request.TargetWindowId, ex.Message));
        }
    }

    public Task<RemoteInputResult> ProcessKeyAsync(RemoteKeyRequest request, CancellationToken cancellationToken = default)
    {
        if (request == null || string.IsNullOrWhiteSpace(request.Key))
        {
            return Task.FromResult(RemoteInputResult.Fail("key", string.Empty, "Tecla não especificada."));
        }

        string keyName = request.Key.Trim();

        if (!OperatingSystem.IsWindows())
        {
            _logger.LogDebug("[INPUT-MOCK] ProcessKeyAsync em não-Windows ({OS}): Tecla={Key}", Environment.OSVersion.Platform, keyName);
            return Task.FromResult(RemoteInputResult.Ok("key", request.TargetWindowId, 0, 0, $"Simulação de tecla {keyName}"));
        }

        if (!TryResolveWindow(request.TargetWindowId, out nint hWnd, out string err))
        {
            return Task.FromResult(RemoteInputResult.Fail("key", request.TargetWindowId, err));
        }

        if (!MapVirtualKey(keyName, out ushort vk))
        {
            return Task.FromResult(RemoteInputResult.Fail("key", request.TargetWindowId, $"Tecla especial '{keyName}' desconhecida ou não permitida."));
        }

        try
        {
            EnsureWindowFocus(hWnd);

            // Pressiona modificadores auxiliares (Ctrl, Alt, Shift)
            List<ushort> pressedModifiers = new();
            if (request.Modifiers != null)
            {
                foreach (string mod in request.Modifiers)
                {
                    if (MapVirtualKey(mod, out ushort modVk))
                    {
                        SendKeyEvent(modVk, isKeyUp: false);
                        pressedModifiers.Add(modVk);
                    }
                }
            }

            if (request.IsKeyDown.HasValue)
            {
                bool isUp = !request.IsKeyDown.Value;
                SendKeyEvent(vk, isKeyUp: isUp);
                lock (_stateLock)
                {
                    if (isUp) _heldKeys.Remove(vk);
                    else _heldKeys.Add(vk);
                }
            }
            else
            {
                // Pressionamento padrão (down + up)
                SendKeyEvent(vk, isKeyUp: false);
                Thread.Sleep(10);
                SendKeyEvent(vk, isKeyUp: true);
            }

            // Libera modificadores aplicados pontualmente
            foreach (ushort modVk in pressedModifiers)
            {
                SendKeyEvent(modVk, isKeyUp: true);
            }

            return Task.FromResult(RemoteInputResult.Ok("key", request.TargetWindowId, 0, 0, $"Tecla {keyName} enviada com sucesso."));
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "[INPUT] Falha ao enviar tecla '{Key}' para janela {WindowId}", keyName, request.TargetWindowId);
            return Task.FromResult(RemoteInputResult.Fail("key", request.TargetWindowId, ex.Message));
        }
    }

    public Task<RemoteInputResult> ProcessTextAsync(RemoteTextRequest request, CancellationToken cancellationToken = default)
    {
        if (request == null || string.IsNullOrEmpty(request.Text))
        {
            return Task.FromResult(RemoteInputResult.Fail("text", string.Empty, "Texto não fornecido."));
        }

        string text = request.Text;

        if (!OperatingSystem.IsWindows())
        {
            _logger.LogDebug("[INPUT-MOCK] ProcessTextAsync em não-Windows ({OS}): {Count} caracteres", Environment.OSVersion.Platform, text.Length);
            return Task.FromResult(RemoteInputResult.Ok("text", request.TargetWindowId, 0, 0, "Texto simulado em não-Windows."));
        }

        if (!TryResolveWindow(request.TargetWindowId, out nint hWnd, out string err))
        {
            return Task.FromResult(RemoteInputResult.Fail("text", request.TargetWindowId, err));
        }

        try
        {
            EnsureWindowFocus(hWnd);

            // Não registra o conteúdo em logs por privacidade e segurança
            _logger.LogInformation("[INPUT] Injetando sequência de texto Unicode ({Length} caracteres) na janela {WindowId}",
                text.Length, request.TargetWindowId);

            foreach (char c in text)
            {
                SendUnicodeChar(c);
            }

            return Task.FromResult(RemoteInputResult.Ok("text", request.TargetWindowId, 0, 0, "Texto Unicode enviado com sucesso."));
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "[INPUT] Falha ao injetar texto na janela {WindowId}", request.TargetWindowId);
            return Task.FromResult(RemoteInputResult.Fail("text", request.TargetWindowId, ex.Message));
        }
    }

    public Task<RemoteInputResult> ReleaseAllInputsAsync(CancellationToken cancellationToken = default)
    {
        if (!OperatingSystem.IsWindows())
        {
            return Task.FromResult(RemoteInputResult.Ok("release-inputs", string.Empty, 0, 0, "Liberação simulada em não-Windows."));
        }

        try
        {
            lock (_stateLock)
            {
                if (_isLeftButtonDown)
                {
                    SendMouseButton(User32.MOUSEEVENTF_LEFTUP);
                    _isLeftButtonDown = false;
                }
                if (_isRightButtonDown)
                {
                    SendMouseButton(User32.MOUSEEVENTF_RIGHTUP);
                    _isRightButtonDown = false;
                }
                if (_isMiddleButtonDown)
                {
                    SendMouseButton(User32.MOUSEEVENTF_MIDDLEUP);
                    _isMiddleButtonDown = false;
                }

                foreach (ushort key in _heldKeys)
                {
                    SendKeyEvent(key, isKeyUp: true);
                }
                _heldKeys.Clear();

                // Libera modificadores comuns por precaução
                SendKeyEvent(User32.VK_CONTROL, isKeyUp: true);
                SendKeyEvent(User32.VK_SHIFT, isKeyUp: true);
                SendKeyEvent(User32.VK_MENU, isKeyUp: true);
            }

            _logger.LogInformation("[INPUT] Todas as teclas e botões de mouse foram liberados com segurança.");
            return Task.FromResult(RemoteInputResult.Ok("release-inputs", string.Empty, 0, 0, "Todos os botões e teclas foram liberados."));
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[INPUT] Falha ao liberar entradas.");
            return Task.FromResult(RemoteInputResult.Fail("release-inputs", string.Empty, ex.Message));
        }
    }

    private static void EnsureWindowFocus(nint hWnd)
    {
        nint foreground = User32.GetForegroundWindow();
        if (foreground != hWnd)
        {
            User32.SetForegroundWindow(hWnd);
        }
    }

    private static bool TryResolveWindow(string? windowId, out nint hWnd, out string error)
    {
        hWnd = 0;
        error = string.Empty;

        if (string.IsNullOrWhiteSpace(windowId))
        {
            error = "Identificador de janela ausente.";
            return false;
        }

        if (windowId.StartsWith("0x", StringComparison.OrdinalIgnoreCase))
        {
            hWnd = (nint)Convert.ToInt64(windowId[2..], 16);
        }
        else if (long.TryParse(windowId, out long numericId))
        {
            hWnd = (nint)numericId;
        }

        if (hWnd == 0 || !User32.IsWindow(hWnd))
        {
            error = $"A janela '{windowId}' não é válida ou foi fechada.";
            return false;
        }

        return true;
    }

    private static void SendMouseButton(uint flags)
    {
        User32.INPUT[] inputs = new User32.INPUT[1];
        inputs[0].type = User32.INPUT_MOUSE;
        inputs[0].u.mi.dwFlags = flags;
        User32.SendInput(1, inputs, Marshal.SizeOf<User32.INPUT>());
    }

    private static void SendMouseWheel(int delta)
    {
        User32.INPUT[] inputs = new User32.INPUT[1];
        inputs[0].type = User32.INPUT_MOUSE;
        inputs[0].u.mi.dwFlags = User32.MOUSEEVENTF_WHEEL;
        inputs[0].u.mi.mouseData = (uint)delta;
        User32.SendInput(1, inputs, Marshal.SizeOf<User32.INPUT>());
    }

    private static void SendUnicodeChar(char c)
    {
        User32.INPUT[] inputs = new User32.INPUT[2];

        // Key Down com flag UNICODE
        inputs[0].type = User32.INPUT_KEYBOARD;
        inputs[0].u.ki.wVk = 0;
        inputs[0].u.ki.wScan = (ushort)c;
        inputs[0].u.ki.dwFlags = User32.KEYEVENTF_UNICODE;

        // Key Up com flag UNICODE
        inputs[1].type = User32.INPUT_KEYBOARD;
        inputs[1].u.ki.wVk = 0;
        inputs[1].u.ki.wScan = (ushort)c;
        inputs[1].u.ki.dwFlags = User32.KEYEVENTF_UNICODE | User32.KEYEVENTF_KEYUP;

        User32.SendInput(2, inputs, Marshal.SizeOf<User32.INPUT>());
    }

    private static void SendKeyEvent(ushort vk, bool isKeyUp)
    {
        User32.INPUT[] inputs = new User32.INPUT[1];
        inputs[0].type = User32.INPUT_KEYBOARD;
        inputs[0].u.ki.wVk = vk;
        inputs[0].u.ki.wScan = 0;
        inputs[0].u.ki.dwFlags = isKeyUp ? User32.KEYEVENTF_KEYUP : 0;
        User32.SendInput(1, inputs, Marshal.SizeOf<User32.INPUT>());
    }

    private static bool MapVirtualKey(string keyName, out ushort vk)
    {
        vk = 0;
        string k = keyName.Trim().ToLowerInvariant();

        switch (k)
        {
            case "enter":
            case "return":
                vk = User32.VK_RETURN;
                return true;
            case "backspace":
            case "bksp":
            case "back":
                vk = User32.VK_BACK;
                return true;
            case "tab":
                vk = User32.VK_TAB;
                return true;
            case "escape":
            case "esc":
                vk = User32.VK_ESCAPE;
                return true;
            case "delete":
            case "del":
                vk = User32.VK_DELETE;
                return true;
            case "up":
            case "arrowup":
                vk = User32.VK_UP;
                return true;
            case "down":
            case "arrowdown":
                vk = User32.VK_DOWN;
                return true;
            case "left":
            case "arrowleft":
                vk = User32.VK_LEFT;
                return true;
            case "right":
            case "arrowright":
                vk = User32.VK_RIGHT;
                return true;
            case "home":
                vk = User32.VK_HOME;
                return true;
            case "end":
                vk = User32.VK_END;
                return true;
            case "pageup":
            case "prior":
                vk = User32.VK_PRIOR;
                return true;
            case "pagedown":
            case "next":
                vk = User32.VK_NEXT;
                return true;
            case "ctrl":
            case "control":
                vk = User32.VK_CONTROL;
                return true;
            case "shift":
                vk = User32.VK_SHIFT;
                return true;
            case "alt":
            case "menu":
                vk = User32.VK_MENU;
                return true;
            default:
                return false;
        }
    }
}

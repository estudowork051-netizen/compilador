using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using RemoteDesk.Agent.Models;

namespace RemoteDesk.Agent.Services;

/// <summary>
/// Despachante e validador seguro de comandos do agente Windows.
/// Garante que apenas ações homologadas sejam processadas, prevenindo execução arbitrária.
/// </summary>
public sealed class CommandDispatcher : ICommandDispatcher
{
    private readonly ILogger<CommandDispatcher> _logger;
    private readonly IWindowService _windowService;
    private readonly IWindowCaptureService _windowCaptureService;
    private readonly IInputService _inputService;
    private readonly IAuthService? _authService;
    private readonly AgentConfig _config;

    public CommandDispatcher(
        ILogger<CommandDispatcher> logger,
        IWindowService windowService,
        IWindowCaptureService windowCaptureService,
        IInputService inputService,
        IOptions<AgentConfig> config,
        IAuthService? authService = null)
    {
        _logger = logger;
        _windowService = windowService;
        _windowCaptureService = windowCaptureService;
        _inputService = inputService;
        _authService = authService;
        _config = config.Value;
    }

    public async Task<AgentResponse<object>> DispatchAsync(AgentCommand command, CancellationToken cancellationToken = default)
    {
        if (command == null || string.IsNullOrWhiteSpace(command.Action))
        {
            return AgentResponse<object>.Fail("INVALID_REQUEST", "O comando ou ação solicitada não pode ser nulo ou vazio.");
        }

        string action = command.Action.Trim().ToLowerInvariant();

        // 1. Validação de segurança por PIN ou Token de Dispositivo quando configurado
        if (_config.PromptOnConnectionRequest)
        {
            bool isAuthorized = _authService != null
                ? _authService.ValidateAccess(command.ClientId, command.DeviceToken, command.AccessPin)
                : (string.IsNullOrEmpty(_config.AccessPin) || string.Equals(command.AccessPin, _config.AccessPin, StringComparison.Ordinal));

            if (!isAuthorized && (!string.IsNullOrEmpty(_config.AccessPin) || _authService != null))
            {
                _logger.LogWarning("[SEGURANÇA] Tentativa de comando '{Action}' rejeitada por credencial inválida ou ausente. Cliente: {Client}", action, command.ClientId ?? "Desconhecido");
                return AgentResponse<object>.Fail("UNAUTHORIZED", "Acesso não autorizado. Código PIN de segurança inválido ou ausente.");
            }
        }

        try
        {
            switch (action)
            {
                case "ping":
                    return AgentResponse<object>.Ok(new
                    {
                        pong = true,
                        serverTime = DateTime.UtcNow,
                        device = _config.DeviceName
                    });

                case "get-status":
                    IReadOnlyList<WindowInfo> currentWindows = await _windowService.GetOpenWindowsAsync(cancellationToken);
                    AgentStatus status = new()
                    {
                        DeviceId = _config.DeviceId,
                        DeviceName = _config.DeviceName,
                        FriendlyPairingCode = _config.FriendlyPairingCode,
                        AvailabilityState = "Disponível para Conexão",
                        IsReady = true,
                        OpenWindowsCount = currentWindows.Count,
                        ActiveSessionsCount = 0,
                        Version = "1.0.0",
                        StartedAt = DateTime.UtcNow
                    };
                    return AgentResponse<object>.Ok(status);

                case "list-windows":
                    IReadOnlyList<WindowInfo> windows = await _windowService.GetOpenWindowsAsync(cancellationToken);
                    _logger.LogInformation("Consulta de janelas processada com sucesso. Retornadas {Count} janelas.", windows.Count);
                    return AgentResponse<object>.Ok(windows);

                case "get-window":
                    if (string.IsNullOrWhiteSpace(command.TargetWindowId))
                    {
                        return AgentResponse<object>.Fail("INVALID_PARAM", "O identificador da janela (targetWindowId) é obrigatório.");
                    }
                    WindowInfo? target = await _windowService.GetWindowByIdAsync(command.TargetWindowId, cancellationToken);
                    if (target == null)
                    {
                        return AgentResponse<object>.Fail("WINDOW_NOT_FOUND", $"Janela com identificador '{command.TargetWindowId}' não encontrada.");
                    }
                    return AgentResponse<object>.Ok(target);

                case "get-active-window":
                    WindowInfo? active = await _windowService.GetActiveWindowAsync(cancellationToken);
                    if (active == null)
                    {
                        return AgentResponse<object>.Fail("NO_ACTIVE_WINDOW", "Nenhuma janela ativa em primeiro plano detectada.");
                    }
                    return AgentResponse<object>.Ok(active);

                case "activate-window":
                    if (string.IsNullOrWhiteSpace(command.TargetWindowId))
                    {
                        return AgentResponse<object>.Fail("INVALID_PARAM", "O identificador da janela (targetWindowId) é obrigatório.");
                    }
                    bool activated = await _windowService.ActivateWindowAsync(command.TargetWindowId, cancellationToken);
                    if (!activated)
                    {
                        return AgentResponse<object>.Fail("ACTIVATION_FAILED", $"Não foi possível ativar a janela '{command.TargetWindowId}'.");
                    }
                    return AgentResponse<object>.Ok(new { activated = true, windowId = command.TargetWindowId });

                case "restore-window":
                    if (string.IsNullOrWhiteSpace(command.TargetWindowId))
                    {
                        return AgentResponse<object>.Fail("INVALID_PARAM", "O identificador da janela (targetWindowId) é obrigatório.");
                    }
                    bool restored = await _windowService.ActivateWindowAsync(command.TargetWindowId, cancellationToken);
                    if (!restored)
                    {
                        return AgentResponse<object>.Fail("RESTORE_FAILED", $"Não foi possível restaurar a janela '{command.TargetWindowId}'.");
                    }
                    return AgentResponse<object>.Ok(new { restored = true, windowId = command.TargetWindowId });

                case "capture-frame":
                    if (string.IsNullOrWhiteSpace(command.TargetWindowId))
                    {
                        return AgentResponse<object>.Fail("INVALID_PARAM", "O identificador da janela (targetWindowId) é obrigatório.");
                    }
                    CaptureResult cap = await _windowCaptureService.CaptureWindowFrameAsync(command.TargetWindowId, cancellationToken);
                    if (!cap.Success)
                    {
                        return AgentResponse<object>.Fail(
                            cap.IsMinimized ? "WINDOW_MINIMIZED" : (cap.WindowClosed ? "WINDOW_CLOSED" : "CAPTURE_FAILED"),
                            cap.ErrorMessage ?? "Falha na captura do quadro.");
                    }
                    return AgentResponse<object>.Ok(new
                    {
                        windowId = cap.WindowId,
                        width = cap.Width,
                        height = cap.Height,
                        format = "bmp",
                        frameBase64 = cap.FrameData != null ? Convert.ToBase64String(cap.FrameData) : null
                    });

                case "resize-window":
                    if (string.IsNullOrWhiteSpace(command.TargetWindowId))
                    {
                        return AgentResponse<object>.Fail("INVALID_PARAM", "O identificador da janela (targetWindowId) é obrigatório.");
                    }
                    WindowResizeResult resizeRes = await _windowService.ResizeWindowAsync(
                        command.TargetWindowId,
                        targetWidth: 382,
                        targetHeight: 706,
                        cancellationToken: cancellationToken);
                    return AgentResponse<object>.Ok(resizeRes);

                case "select-window":
                    if (string.IsNullOrWhiteSpace(command.TargetWindowId))
                    {
                        return AgentResponse<object>.Fail("INVALID_PARAM", "O identificador da janela (targetWindowId) é obrigatório.");
                    }
                    await _windowService.ActivateWindowAsync(command.TargetWindowId, cancellationToken);
                    WindowResizeResult selectResize = await _windowService.ResizeWindowAsync(
                        command.TargetWindowId,
                        targetWidth: 382,
                        targetHeight: 706,
                        cancellationToken: cancellationToken);
                    return AgentResponse<object>.Ok(new
                    {
                        selected = true,
                        windowId = command.TargetWindowId,
                        resize = selectResize
                    });

                case "mouse-input":
                case "mouse-event":
                    if (string.IsNullOrWhiteSpace(command.TargetWindowId))
                    {
                        return AgentResponse<object>.Fail("INVALID_PARAM", "O identificador da janela (targetWindowId) é obrigatório.");
                    }
                    RemoteMouseRequest mouseReq = new()
                    {
                        TargetWindowId = command.TargetWindowId,
                        X = command.X,
                        Y = command.Y,
                        NormalizedX = command.NormalizedX,
                        NormalizedY = command.NormalizedY,
                        MouseAction = command.MouseAction ?? "click",
                        Button = command.Button ?? "left",
                        DeltaY = command.DeltaY
                    };
                    RemoteInputResult mouseRes = await _inputService.ProcessMouseAsync(mouseReq, cancellationToken);
                    return mouseRes.Success
                        ? AgentResponse<object>.Ok(mouseRes)
                        : AgentResponse<object>.Fail("INPUT_FAILED", mouseRes.Message ?? "Falha no comando de mouse.");

                case "key-event":
                case "keyboard-input":
                    if (string.IsNullOrWhiteSpace(command.TargetWindowId))
                    {
                        return AgentResponse<object>.Fail("INVALID_PARAM", "O identificador da janela (targetWindowId) é obrigatório.");
                    }
                    if (string.IsNullOrWhiteSpace(command.Key))
                    {
                        return AgentResponse<object>.Fail("INVALID_PARAM", "O nome da tecla (key) é obrigatório.");
                    }
                    RemoteKeyRequest keyReq = new()
                    {
                        TargetWindowId = command.TargetWindowId,
                        Key = command.Key,
                        Modifiers = command.Modifiers,
                        IsKeyDown = command.IsKeyDown
                    };
                    RemoteInputResult keyRes = await _inputService.ProcessKeyAsync(keyReq, cancellationToken);
                    return keyRes.Success
                        ? AgentResponse<object>.Ok(keyRes)
                        : AgentResponse<object>.Fail("INPUT_FAILED", keyRes.Message ?? "Falha no comando de tecla.");

                case "text-input":
                    if (string.IsNullOrWhiteSpace(command.TargetWindowId))
                    {
                        return AgentResponse<object>.Fail("INVALID_PARAM", "O identificador da janela (targetWindowId) é obrigatório.");
                    }
                    RemoteTextRequest textReq = new()
                    {
                        TargetWindowId = command.TargetWindowId,
                        Text = command.Text ?? string.Empty
                    };
                    RemoteInputResult textRes = await _inputService.ProcessTextAsync(textReq, cancellationToken);
                    return textRes.Success
                        ? AgentResponse<object>.Ok(textRes)
                        : AgentResponse<object>.Fail("INPUT_FAILED", textRes.Message ?? "Falha na injeção de texto.");

                case "scroll-event":
                    if (string.IsNullOrWhiteSpace(command.TargetWindowId))
                    {
                        return AgentResponse<object>.Fail("INVALID_PARAM", "O identificador da janela (targetWindowId) é obrigatório.");
                    }
                    RemoteMouseRequest scrollReq = new()
                    {
                        TargetWindowId = command.TargetWindowId,
                        X = command.X,
                        Y = command.Y,
                        NormalizedX = command.NormalizedX,
                        NormalizedY = command.NormalizedY,
                        MouseAction = "wheel",
                        DeltaY = command.DeltaY ?? 0
                    };
                    RemoteInputResult scrollRes = await _inputService.ProcessMouseAsync(scrollReq, cancellationToken);
                    return scrollRes.Success
                        ? AgentResponse<object>.Ok(scrollRes)
                        : AgentResponse<object>.Fail("INPUT_FAILED", scrollRes.Message ?? "Falha na rolagem.");

                case "release-inputs":
                    RemoteInputResult releaseRes = await _inputService.ReleaseAllInputsAsync(cancellationToken);
                    return releaseRes.Success
                        ? AgentResponse<object>.Ok(releaseRes)
                        : AgentResponse<object>.Fail("INPUT_FAILED", releaseRes.Message ?? "Falha na liberação de entradas.");

                default:
                    _logger.LogWarning("[SEGURANÇA] Tentativa de execução de ação desconhecida ou não permitida: '{Action}'", action);
                    return AgentResponse<object>.Fail("UNSUPPORTED_ACTION", $"A ação '{action}' não é reconhecida ou não é permitida por razões de segurança.");
            }
        }
        catch (OperationCanceledException)
        {
            return AgentResponse<object>.Fail("CANCELED", "A operação foi cancelada antes da conclusão.");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "[ERRO] Exceção durante o processamento do comando '{Action}'.", action);
            return AgentResponse<object>.Fail("INTERNAL_ERROR", "Ocorreu um erro interno ao processar a solicitação.");
        }
    }
}

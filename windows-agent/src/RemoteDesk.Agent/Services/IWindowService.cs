using RemoteDesk.Agent.Models;

namespace RemoteDesk.Agent.Services;

/// <summary>
/// Contrato do servico de enumeracao e consulta de janelas na sessao interativa do host.
/// </summary>
public interface IWindowService
{
    /// <summary>
    /// Enumera todas as janelas ativas e utilizaveis na sessao interativa atual do Windows.
    /// </summary>
    Task<IReadOnlyList<WindowInfo>> GetOpenWindowsAsync(CancellationToken cancellationToken = default);

    /// <summary>
    /// Obtem detalhes de uma janela especifica pelo seu identificador unico.
    /// </summary>
    Task<WindowInfo?> GetWindowByIdAsync(string windowId, CancellationToken cancellationToken = default);

    /// <summary>
    /// Obtem a janela que atualmente detem o foco do usuario em primeiro plano (foreground window).
    /// </summary>
    Task<WindowInfo?> GetActiveWindowAsync(CancellationToken cancellationToken = default);

    /// <summary>
    /// Tenta colocar uma janela em primeiro plano (foco), restaurando-a caso esteja minimizada.
    /// </summary>
    Task<bool> ActivateWindowAsync(string windowId, CancellationToken cancellationToken = default);

    /// <summary>
    /// Traz a janela especificada para o primeiro plano (foco do usuário), restaurando-a caso esteja minimizada.
    /// </summary>
    Task<bool> BringWindowToFrontAsync(string windowId, CancellationToken cancellationToken = default);

    /// <summary>
    /// Retorna o identificador da sessao do Windows onde o agente esta executando.
    /// Sessao 0 indica servico de sistema (isolado); Sessao > 0 indica sessao interativa do usuario.
    /// </summary>
    int GetCurrentSessionId();

    /// <summary>
    /// Redimensiona a janela especificada no Windows para as dimensoes de referencia (padrao 382 x 706),
    /// restaurando-a se estiver minimizada ou maximizada, ajustando coordenadas com SetWindowPos/MoveWindow
    /// e confirmando as dimensoes efetivas reais aplicadas pelo sistema operacional.
    /// </summary>
    Task<WindowResizeResult> ResizeWindowAsync(string windowId, int targetWidth = 382, int targetHeight = 706, CancellationToken cancellationToken = default);
}

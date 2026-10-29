using RemoteDesk.Agent.Models;

namespace RemoteDesk.Agent.Services;

/// <summary>
/// Contrato do servico de captura em tempo real de quadros visuais de janelas do Windows.
/// </summary>
public interface IWindowCaptureService
{
    /// <summary>
    /// Captura o conteudo visual real da janela especificada por seu identificador unico (windowId).
    /// Retorna CaptureResult contendo os bytes da imagem (BMP 32bpp) ou o estado de erro/minimizacao/fechamento.
    /// </summary>
    Task<CaptureResult> CaptureWindowFrameAsync(string windowId, CancellationToken cancellationToken = default);

    /// <summary>
    /// Verifica se a janela indicada ainda e valida e existente na sessao do Windows.
    /// </summary>
    Task<bool> IsWindowValidAsync(string windowId, CancellationToken cancellationToken = default);

    /// <summary>
    /// Verifica se a janela indicada encontra-se atualmente em estado minimizado (iconizado).
    /// </summary>
    Task<bool> IsWindowMinimizedAsync(string windowId, CancellationToken cancellationToken = default);
}

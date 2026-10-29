using RemoteDesk.Agent.Models;

namespace RemoteDesk.Agent.Services;

/// <summary>
/// Contrato do servico de injecao de entrada remota (mouse e teclado) na sessao do Windows.
/// </summary>
public interface IInputService
{
    /// <summary>
    /// Processa comandos de mouse (movimento, clique, duplo clique, clique direito, down, up, arrastar, rolagem).
    /// Mapeia com precisao coordenadas relativas a janela indicada e injeta os eventos nativos.
    /// </summary>
    Task<RemoteInputResult> ProcessMouseAsync(RemoteMouseRequest request, CancellationToken cancellationToken = default);

    /// <summary>
    /// Processa comandos de teclas fisicas e especiais (Enter, Backspace, Tab, Esc, setas, modificadores).
    /// </summary>
    Task<RemoteInputResult> ProcessKeyAsync(RemoteKeyRequest request, CancellationToken cancellationToken = default);

    /// <summary>
    /// Injeta texto digitado Unicode (incluindo acentuacao, pontuacao e caracteres especiais)
    /// diretamente na aplicacao em foco via SendInput com flag KEYEVENTF_UNICODE.
    /// </summary>
    Task<RemoteInputResult> ProcessTextAsync(RemoteTextRequest request, CancellationToken cancellationToken = default);

    /// <summary>
    /// Libera com seguranca todos os botoes de mouse e teclas modificadoras (Ctrl, Alt, Shift)
    /// eventualmente retidos em operacoes de arrasto ou atalhos interrompidos.
    /// </summary>
    Task<RemoteInputResult> ReleaseAllInputsAsync(CancellationToken cancellationToken = default);
}

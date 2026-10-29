namespace RemoteDesk.Agent.Models;

/// <summary>
/// Representa o estado visual de uma janela na área de trabalho do Windows.
/// </summary>
public enum WindowState
{
    /// <summary>
    /// Janela em tamanho normal e visível na área de trabalho.
    /// </summary>
    Normal = 0,

    /// <summary>
    /// Janela minimizada na barra de tarefas (permanece ativa e disponível para foco).
    /// </summary>
    Minimized = 1,

    /// <summary>
    /// Janela maximizada ocupando toda a área da tela.
    /// </summary>
    Maximized = 2,

    /// <summary>
    /// Janela oculta ou em estado indeterminado.
    /// </summary>
    Unknown = 3
}

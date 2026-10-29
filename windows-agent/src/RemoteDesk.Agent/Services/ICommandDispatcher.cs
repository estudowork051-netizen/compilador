using RemoteDesk.Agent.Models;

namespace RemoteDesk.Agent.Services;

/// <summary>
/// Contrato do despachante seguro de comandos internos e remotos do agente.
/// </summary>
public interface ICommandDispatcher
{
    /// <summary>
    /// Processa e valida um comando estruturado, retornando resposta tipada.
    /// </summary>
    Task<AgentResponse<object>> DispatchAsync(AgentCommand command, CancellationToken cancellationToken = default);
}

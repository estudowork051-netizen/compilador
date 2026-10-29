namespace RemoteDesk.Agent.Services;

/// <summary>
/// Contrato do servidor local de comunicacao direta entre o Agente Windows e clientes na rede local.
/// </summary>
public interface ILocalServer
{
    /// <summary>
    /// Inicia a escuta de requisicoes locais.
    /// </summary>
    Task StartAsync(CancellationToken cancellationToken);

    /// <summary>
    /// Encerra a escuta e libera recursos de rede.
    /// </summary>
    Task StopAsync(CancellationToken cancellationToken);

    /// <summary>
    /// Indica se o servidor esta ativo e ouvindo conexoes.
    /// </summary>
    bool IsRunning { get; }

    /// <summary>
    /// Porta TCP em uso.
    /// </summary>
    int Port { get; }
}

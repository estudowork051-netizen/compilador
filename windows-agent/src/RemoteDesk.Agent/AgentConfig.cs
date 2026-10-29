namespace RemoteDesk.Agent;

/// <summary>
/// Configurações do Agente Windows RemoteDesk.
/// </summary>
public sealed class AgentConfig
{
    public const string SectionName = "RemoteDeskAgent";

    /// <summary>
    /// Identificador único deste computador na rede RemoteDesk.
    /// </summary>
    public string DeviceId { get; set; } = Guid.NewGuid().ToString("N")[..10].ToUpperInvariant();

    /// <summary>
    /// Nome amigável do computador.
    /// </summary>
    public string DeviceName { get; set; } = Environment.MachineName;

    /// <summary>
    /// Endereço do servidor de sinalização para pareamento.
    /// </summary>
    public string SignalingServerUrl { get; set; } = "http://localhost:5000";

    /// <summary>
    /// Indica se a conexão contínua com o servidor público de sinalização deve ser mantida.
    /// </summary>
    public bool EnableSignalingConnection { get; set; } = true;

    /// <summary>
    /// Se o agente deve iniciar minimizado na bandeja do sistema.
    /// </summary>
    public bool StartMinimizedToTray { get; set; } = true;

    /// <summary>
    /// Requerer confirmação do usuário local antes de aceitar conexões.
    /// </summary>
    public bool PromptOnConnectionRequest { get; set; } = true;

    /// <summary>
    /// Senha ou PIN de uso único (em branco para geração automática na etapa de segurança).
    /// </summary>
    public string AccessPin { get; set; } = string.Empty;

    /// <summary>
    /// Código fixo de pareamento pessoal configurado pelo proprietário para autorizar dispositivos móveis (Etapa 7).
    /// </summary>
    public string FixedPairingCode { get; set; } = "70C92B";

    /// <summary>
    /// Porta local para recebimento de comandos do aplicativo Android na rede local.
    /// Padrão: 5050.
    /// </summary>
    public int LocalPort { get; set; } = 5050;

    /// <summary>
    /// Indica se o servidor local HTTP para comunicação direta deve ser iniciado.
    /// </summary>
    public bool EnableLocalHttpServer { get; set; } = true;

    /// <summary>
    /// Código amigável de conexão rápida de 9 dígitos (ex: "492 813 605") para pareamento intuitivo com o Android.
    /// </summary>
    public string FriendlyPairingCode
    {
        get
        {
            if (!string.IsNullOrWhiteSpace(_friendlyPairingCode))
            {
                return _friendlyPairingCode;
            }
            // Gera código determinístico de 9 dígitos formatado em blocos de 3 a partir do DeviceId
            uint hash = (uint)Math.Abs(DeviceId.GetHashCode());
            string raw = (hash % 900_000_000 + 100_000_000).ToString();
            return $"{raw[..3]} {raw[3..6]} {raw[6..9]}";
        }
        set => _friendlyPairingCode = value;
    }
    private string? _friendlyPairingCode;
}

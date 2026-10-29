using RemoteDesk.Agent.Models;

namespace RemoteDesk.Agent.Services;

/// <summary>
/// Contrato do serviço de autenticação pessoal, pareamento persistente e gerenciamento de dispositivos autorizados (Etapa 7).
/// </summary>
public interface IAuthService
{
    /// <summary>
    /// Código fixo de pareamento atualmente configurado para este computador.
    /// </summary>
    string FixedCode { get; }

    /// <summary>
    /// Realiza o pareamento inicial do dispositivo cliente utilizando o código fixo do proprietário.
    /// </summary>
    Task<PairingResponse> PairDeviceAsync(PairingRequest request, string? clientIp = null, CancellationToken token = default);

    /// <summary>
    /// Verifica a autenticidade e validade do token de autorização persistente de um dispositivo cliente.
    /// </summary>
    Task<DeviceAuthVerification> VerifyDeviceTokenAsync(string? deviceId, string? deviceToken, string? clientIp = null, CancellationToken token = default);

    /// <summary>
    /// Revoga a autorização de um dispositivo cliente específico.
    /// </summary>
    Task<bool> RevokeDeviceAsync(string deviceId, CancellationToken token = default);

    /// <summary>
    /// Revoga a autorização de todos os dispositivos clientes pareados.
    /// </summary>
    Task<int> RevokeAllDevicesAsync(CancellationToken token = default);

    /// <summary>
    /// Obtém a lista de todos os dispositivos clientes autorizados e seu status.
    /// </summary>
    Task<IReadOnlyList<AuthorizedDevice>> GetAuthorizedDevicesAsync(CancellationToken token = default);

    /// <summary>
    /// Altera o código fixo de pareamento, exigindo validação prévia do código atual.
    /// Não invalida dispositivos que já foram pareados com sucesso.
    /// </summary>
    Task<bool> ChangeFixedCodeAsync(string currentCode, string newCode, CancellationToken token = default);

    /// <summary>
    /// Regenera e persiste um novo PIN de 6 dígitos criptograficamente seguro para pareamento.
    /// </summary>
    Task<string> RegenerateFixedCodeAsync(CancellationToken token = default);

    /// <summary>
    /// Valida rapidamente o acesso verificando o token de dispositivo ou PIN em cabeçalhos HTTP.
    /// Suporta compatibilidade retroativa com requisições legadas.
    /// </summary>
    bool ValidateAccess(string? deviceId, string? deviceToken, string? accessPin, string? clientIp = null);
}

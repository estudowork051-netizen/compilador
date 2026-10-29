using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using RemoteDesk.Agent.Models;

namespace RemoteDesk.Agent.Services;

/// <summary>
/// Serviço de autenticação pessoal, pareamento persistente e autorização de dispositivos (Etapa 7).
/// Gerencia o código fixo do proprietário, emissão de tokens criptográficos, proteção contra força bruta
/// e revogação persistente de dispositivos clientes.
/// </summary>
public sealed class DeviceAuthService : IAuthService
{
    private readonly ILogger<DeviceAuthService> _logger;
    private readonly AgentConfig _config;
    private readonly string _storagePath;
    private readonly string _configStoragePath;
    private readonly SemaphoreSlim _fileLock = new(1, 1);

    private readonly Dictionary<string, AuthorizedDevice> _devices = new(StringComparer.OrdinalIgnoreCase);
    private readonly Dictionary<string, (int Attempts, DateTime LastAttempt)> _failedAttempts = new();
    private string _activeFixedCode;

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        WriteIndented = true
    };

    public string FixedCode => _activeFixedCode;

    public DeviceAuthService(ILogger<DeviceAuthService> logger, IOptions<AgentConfig> config)
    {
        _logger = logger;
        _config = config.Value;

        string baseDir = AppContext.BaseDirectory;
        _storagePath = Path.Combine(baseDir, "authorized_devices.json");
        _configStoragePath = Path.Combine(baseDir, "auth_config.json");

        // Inicializa o código fixo com prioridade:
        // 1. Arquivo de configuração persistido (auth_config.json)
        // 2. AgentConfig.FixedPairingCode / AccessPin
        // 3. Padrão inicial estável ("70C92B")
        _activeFixedCode = LoadPersistedFixedCode() ??
                           (!string.IsNullOrWhiteSpace(_config.FixedPairingCode) ? _config.FixedPairingCode :
                            (!string.IsNullOrWhiteSpace(_config.AccessPin) ? _config.AccessPin : "70C92B"));

        LoadAuthorizedDevices();
        _logger.LogInformation("Serviço de autenticação pessoal inicializado. Dispositivos autorizados: {Count}.", _devices.Count);
    }

    public async Task<PairingResponse> PairDeviceAsync(PairingRequest request, string? clientIp = null, CancellationToken token = default)
    {
        if (request == null)
        {
            return new PairingResponse
            {
                Success = false,
                ErrorCode = "INVALID_REQUEST",
                Message = "Requisição de pareamento inválida."
            };
        }

        string ipKey = clientIp ?? "local";

        // 1. Verificação de bloqueio temporário por tentativas excessivas (Anti-Brute Force)
        lock (_failedAttempts)
        {
            if (_failedAttempts.TryGetValue(ipKey, out var record))
            {
                if (record.Attempts >= 5 && (DateTime.UtcNow - record.LastAttempt).TotalSeconds < 30)
                {
                    double remaining = Math.Ceiling(30 - (DateTime.UtcNow - record.LastAttempt).TotalSeconds);
                    _logger.LogWarning("[SEGURANÇA] Tentativa de pareamento bloqueada por taxa de falhas para {Ip}. Restam {Sec}s.", clientIp, remaining);
                    return new PairingResponse
                    {
                        Success = false,
                        ErrorCode = "RATE_LIMITED",
                        Message = $"Muitas tentativas inválidas consecutivas. Aguarde {remaining} segundos antes de tentar novamente."
                    };
                }

                if ((DateTime.UtcNow - record.LastAttempt).TotalMinutes > 10)
                {
                    _failedAttempts.Remove(ipKey);
                }
            }
        }

        // 2. Validação de formato e preenchimento
        string providedCode = (request.FixedCode ?? string.Empty).Trim().Replace(" ", "");
        string targetCode = _activeFixedCode.Trim().Replace(" ", "");

        if (string.IsNullOrWhiteSpace(providedCode))
        {
            return new PairingResponse
            {
                Success = false,
                ErrorCode = "EMPTY_CODE",
                Message = "O código fixo de pareamento não pode estar vazio."
            };
        }

        if (string.IsNullOrWhiteSpace(request.DeviceId))
        {
            return new PairingResponse
            {
                Success = false,
                ErrorCode = "MISSING_DEVICE_ID",
                Message = "O identificador único do dispositivo cliente é obrigatório."
            };
        }

        // 3. Validação do código fixo ou amigável com comparação em tempo constante
        string friendlyTarget = (_config.FriendlyPairingCode ?? string.Empty).Trim().Replace(" ", "");
        string pinTarget = (_config.AccessPin ?? string.Empty).Trim().Replace(" ", "");
        string configFixed = (_config.FixedPairingCode ?? string.Empty).Trim().Replace(" ", "");

        bool isCodeValid = SlowEquals(providedCode, targetCode) ||
                           (!string.IsNullOrEmpty(friendlyTarget) && SlowEquals(providedCode, friendlyTarget)) ||
                           (!string.IsNullOrEmpty(configFixed) && SlowEquals(providedCode, configFixed)) ||
                           (!string.IsNullOrEmpty(pinTarget) && SlowEquals(providedCode, pinTarget));

        if (!isCodeValid)
        {
            lock (_failedAttempts)
            {
                if (_failedAttempts.TryGetValue(ipKey, out var record))
                {
                    _failedAttempts[ipKey] = (record.Attempts + 1, DateTime.UtcNow);
                }
                else
                {
                    _failedAttempts[ipKey] = (1, DateTime.UtcNow);
                }
            }

            _logger.LogWarning("[SEGURANÇA] Tentativa de pareamento falhou: código/PIN incorreto fornecido por {Ip} (Device: {Device})",
                clientIp ?? "Desconhecido", request.DeviceName);

            return new PairingResponse
            {
                Success = false,
                ErrorCode = "INVALID_PAIRING_CODE",
                Message = "Código ou PIN de pareamento incorreto. Verifique o PIN de 6 dígitos exibido no Windows Agent."
            };
        }

        // Limpa registro de tentativas falhas após sucesso
        lock (_failedAttempts)
        {
            _failedAttempts.Remove(ipKey);
        }

        // 4. Geração de token criptográfico seguro de 256 bits (32 bytes)
        byte[] tokenBytes = new byte[32];
        using (var rng = RandomNumberGenerator.Create())
        {
            rng.GetBytes(tokenBytes);
        }
        string deviceToken = Convert.ToHexString(tokenBytes).ToLowerInvariant();
        string tokenHash = ComputeSha256(deviceToken);

        // 5. Criação ou atualização do dispositivo autorizado
        AuthorizedDevice device = new()
        {
            DeviceId = request.DeviceId.Trim(),
            DeviceName = !string.IsNullOrWhiteSpace(request.DeviceName) ? request.DeviceName.Trim() : "Android Client",
            TokenHash = tokenHash,
            CreatedAt = DateTime.UtcNow,
            LastSeenAt = DateTime.UtcNow,
            IsRevoked = false,
            RevokedAt = null
        };

        await _fileLock.WaitAsync(token);
        try
        {
            _devices[device.DeviceId] = device;
            await SaveAuthorizedDevicesAsync(token);
        }
        finally
        {
            _fileLock.Release();
        }

        _logger.LogInformation("[PAREAMENTO] Dispositivo '{DeviceName}' (ID: {DeviceId}) pareado e autorizado com sucesso via código fixo.",
            device.DeviceName, device.DeviceId);

        return new PairingResponse
        {
            Success = true,
            DeviceId = device.DeviceId,
            DeviceToken = deviceToken,
            HostDeviceName = _config.DeviceName,
            Message = "Pareamento persistente concluído com sucesso."
        };
    }

    public Task<DeviceAuthVerification> VerifyDeviceTokenAsync(string? deviceId, string? deviceToken, string? clientIp = null, CancellationToken token = default)
    {
        if (string.IsNullOrWhiteSpace(deviceId) || string.IsNullOrWhiteSpace(deviceToken))
        {
            return Task.FromResult(DeviceAuthVerification.Failed("MISSING_CREDENTIALS", "Identificador do cliente ou token ausente."));
        }

        AuthorizedDevice? device;
        lock (_devices)
        {
            _devices.TryGetValue(deviceId.Trim(), out device);
        }

        if (device == null)
        {
            return Task.FromResult(DeviceAuthVerification.Failed("DEVICE_NOT_FOUND", "Dispositivo não encontrado ou não pareado previamente."));
        }

        if (device.IsRevoked)
        {
            _logger.LogWarning("[SEGURANÇA] Tentativa de conexão rejeitada: Dispositivo {DeviceName} (ID: {DeviceId}) está REVOGADO.",
                device.DeviceName, device.DeviceId);
            return Task.FromResult(DeviceAuthVerification.Revoked(device));
        }

        string incomingHash = ComputeSha256(deviceToken.Trim());
        if (!SlowEquals(incomingHash, device.TokenHash))
        {
            _logger.LogWarning("[SEGURANÇA] Tentativa de autenticação falhou: token inválido para o dispositivo {DeviceId} ({Ip}).",
                deviceId, clientIp ?? "Desconhecido");
            return Task.FromResult(DeviceAuthVerification.Failed("INVALID_TOKEN", "Token de autorização inválido ou corrompido."));
        }

        // Atualiza carimbo de última conexão
        device.LastSeenAt = DateTime.UtcNow;

        return Task.FromResult(DeviceAuthVerification.Success(device));
    }

    public bool ValidateAccess(string? deviceId, string? deviceToken, string? accessPin, string? clientIp = null)
    {
        // 1. Validação via Token Persistente (Caminho Oficial Etapa 7)
        if (!string.IsNullOrWhiteSpace(deviceId) && !string.IsNullOrWhiteSpace(deviceToken))
        {
            AuthorizedDevice? dev;
            lock (_devices)
            {
                _devices.TryGetValue(deviceId.Trim(), out dev);
            }
            if (dev != null && !dev.IsRevoked)
            {
                string incomingHash = ComputeSha256(deviceToken.Trim());
                if (SlowEquals(incomingHash, dev.TokenHash))
                {
                    dev.LastSeenAt = DateTime.UtcNow;
                    return true;
                }
            }
        }

        // 2. Validação via Código Fixo / PIN informado no cabeçalho (Retrocompatibilidade)
        if (!string.IsNullOrWhiteSpace(accessPin))
        {
            string cleanPin = accessPin.Trim().Replace(" ", "");
            string cleanFixed = _activeFixedCode.Trim().Replace(" ", "");
            string cleanFriendly = (_config.FriendlyPairingCode ?? string.Empty).Trim().Replace(" ", "");
            string cleanCfg = (_config.AccessPin ?? string.Empty).Trim().Replace(" ", "");
            string cleanConfigFixed = (_config.FixedPairingCode ?? string.Empty).Trim().Replace(" ", "");

            if (SlowEquals(cleanPin, cleanFixed) ||
                (!string.IsNullOrEmpty(cleanFriendly) && SlowEquals(cleanPin, cleanFriendly)) ||
                (!string.IsNullOrEmpty(cleanConfigFixed) && SlowEquals(cleanPin, cleanConfigFixed)) ||
                (!string.IsNullOrEmpty(cleanCfg) && SlowEquals(cleanPin, cleanCfg)))
            {
                return true;
            }
        }

        // 3. Se nenhuma proteção for requerida e não houver código fixo definido
        if (!_config.PromptOnConnectionRequest && string.IsNullOrEmpty(_activeFixedCode) && string.IsNullOrEmpty(_config.AccessPin))
        {
            return true;
        }

        return false;
    }

    public async Task<bool> RevokeDeviceAsync(string deviceId, CancellationToken token = default)
    {
        if (string.IsNullOrWhiteSpace(deviceId)) return false;

        await _fileLock.WaitAsync(token);
        try
        {
            if (_devices.TryGetValue(deviceId.Trim(), out var device))
            {
                device.IsRevoked = true;
                device.RevokedAt = DateTime.UtcNow;
                await SaveAuthorizedDevicesAsync(token);
                _logger.LogInformation("[REVOGAÇÃO] Dispositivo '{Name}' (ID: {Id}) foi REVOGADO com sucesso pelo proprietário.",
                    device.DeviceName, device.DeviceId);
                return true;
            }
        }
        finally
        {
            _fileLock.Release();
        }

        return false;
    }

    public async Task<int> RevokeAllDevicesAsync(CancellationToken token = default)
    {
        int count = 0;
        await _fileLock.WaitAsync(token);
        try
        {
            foreach (var kvp in _devices)
            {
                if (!kvp.Value.IsRevoked)
                {
                    kvp.Value.IsRevoked = true;
                    kvp.Value.RevokedAt = DateTime.UtcNow;
                    count++;
                }
            }
            if (count > 0)
            {
                await SaveAuthorizedDevicesAsync(token);
                _logger.LogInformation("[REVOGAÇÃO GERAL] Todos os {Count} dispositivos clientes autorizados foram revogados.", count);
            }
        }
        finally
        {
            _fileLock.Release();
        }

        return count;
    }

    public Task<IReadOnlyList<AuthorizedDevice>> GetAuthorizedDevicesAsync(CancellationToken token = default)
    {
        lock (_devices)
        {
            var list = _devices.Values
                .Select(d => new AuthorizedDevice
                {
                    DeviceId = d.DeviceId,
                    DeviceName = d.DeviceName,
                    TokenHash = "[PROTEGIDO]",
                    CreatedAt = d.CreatedAt,
                    LastSeenAt = d.LastSeenAt,
                    IsRevoked = d.IsRevoked,
                    RevokedAt = d.RevokedAt
                })
                .ToList();
            return Task.FromResult<IReadOnlyList<AuthorizedDevice>>(list);
        }
    }

    public async Task<bool> ChangeFixedCodeAsync(string currentCode, string newCode, CancellationToken token = default)
    {
        if (string.IsNullOrWhiteSpace(newCode) || newCode.Trim().Length < 4)
        {
            _logger.LogWarning("[SEGURANÇA] Tentativa de alteração para código fixo fraco ou vazio rejeitada.");
            return false;
        }

        string cleanCurrent = (currentCode ?? "").Trim().Replace(" ", "");
        string cleanActive = _activeFixedCode.Trim().Replace(" ", "");

        if (!SlowEquals(cleanCurrent, cleanActive))
        {
            _logger.LogWarning("[SEGURANÇA] Falha ao alterar código fixo: código atual incorreto.");
            return false;
        }

        _activeFixedCode = newCode.Trim();
        await SavePersistedFixedCodeAsync(_activeFixedCode, token);

        _logger.LogInformation("[CONFIGURAÇÃO] Código fixo de pareamento atualizado com sucesso pelo proprietário. Dispositivos já autorizados continuam operacionais.");
        return true;
    }

    public async Task<string> RegenerateFixedCodeAsync(CancellationToken token = default)
    {
        // Gera novo PIN numérico de 6 dígitos seguro
        int randomPin = RandomNumberGenerator.GetInt32(100_000, 1_000_000);
        string newPin = randomPin.ToString();
        _activeFixedCode = newPin;
        await SavePersistedFixedCodeAsync(_activeFixedCode, token);
        _logger.LogInformation("[CONFIGURAÇÃO] Novo PIN numérico de 6 dígitos gerado e persistido: {Pin}", newPin);
        return newPin;
    }

    private void LoadAuthorizedDevices()
    {
        try
        {
            if (File.Exists(_storagePath))
            {
                string json = File.ReadAllText(_storagePath, Encoding.UTF8);
                var list = JsonSerializer.Deserialize<List<AuthorizedDevice>>(json, JsonOptions);
                if (list != null)
                {
                    lock (_devices)
                    {
                        _devices.Clear();
                        foreach (var d in list)
                        {
                            if (!string.IsNullOrWhiteSpace(d.DeviceId))
                            {
                                _devices[d.DeviceId] = d;
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[AVISO] Falha ao carregar dispositivos autorizados de {Path}.", _storagePath);
        }
    }

    private async Task SaveAuthorizedDevicesAsync(CancellationToken token)
    {
        try
        {
            List<AuthorizedDevice> list;
            lock (_devices)
            {
                list = _devices.Values.ToList();
            }

            string json = JsonSerializer.Serialize(list, JsonOptions);
            await File.WriteAllTextAsync(_storagePath, json, Encoding.UTF8, token);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "[ERRO] Não foi possível salvar lista de dispositivos autorizados em {Path}.", _storagePath);
        }
    }

    private string? LoadPersistedFixedCode()
    {
        try
        {
            if (File.Exists(_configStoragePath))
            {
                string json = File.ReadAllText(_configStoragePath, Encoding.UTF8);
                using var doc = JsonDocument.Parse(json);
                if (doc.RootElement.TryGetProperty("fixedCode", out var prop))
                {
                    string? code = prop.GetString();
                    if (!string.IsNullOrWhiteSpace(code)) return code.Trim();
                }
            }
        }
        catch { }
        return null;
    }

    private async Task SavePersistedFixedCodeAsync(string code, CancellationToken token)
    {
        try
        {
            var obj = new { fixedCode = code, updatedAt = DateTime.UtcNow };
            string json = JsonSerializer.Serialize(obj, JsonOptions);
            await File.WriteAllTextAsync(_configStoragePath, json, Encoding.UTF8, token);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "[ERRO] Não foi possível salvar código fixo persistido em {Path}.", _configStoragePath);
        }
    }

    private static string ComputeSha256(string input)
    {
        byte[] bytes = SHA256.HashData(Encoding.UTF8.GetBytes(input));
        return Convert.ToHexString(bytes).ToLowerInvariant();
    }

    private static bool SlowEquals(string a, string b)
    {
        byte[] aBytes = Encoding.UTF8.GetBytes(a);
        byte[] bBytes = Encoding.UTF8.GetBytes(b);
        return CryptographicOperations.FixedTimeEquals(aBytes, bBytes);
    }
}

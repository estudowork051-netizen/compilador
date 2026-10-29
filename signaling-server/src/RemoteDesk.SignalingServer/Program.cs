using System.Net.WebSockets;
using RemoteDesk.SignalingServer.Models;
using RemoteDesk.SignalingServer.Services;

var builder = WebApplication.CreateBuilder(args);

// Configuração por variáveis de ambiente (REMOTEDESK_*)
builder.Configuration.AddEnvironmentVariables("REMOTEDESK_");

// Serviços essenciais de sinalização e negociação WebRTC pela Internet (Etapa 8)
builder.Services.AddSingleton<SignalingSessionManager>();
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyHeader()
              .AllowAnyMethod();
    });
});

var app = builder.Build();

app.UseCors();
app.UseWebSockets(new WebSocketOptions
{
    KeepAliveInterval = TimeSpan.FromSeconds(25)
});

var sessionManager = app.Services.GetRequiredService<SignalingSessionManager>();
var startTime = DateTime.UtcNow;

// Endpoint raiz de identificação da API
app.MapGet("/", () => Results.Ok(new
{
    service = "RemoteDesk Signaling & WebRTC Relay Server",
    version = "1.0.0",
    stage = "Etapa 8/12 - Acesso Remoto pela Internet",
    status = "Online",
    activeHosts = sessionManager.ActiveHostCount,
    activeClients = sessionManager.ActiveClientCount,
    endpoints = new[]
    {
        "/health",
        "/api/signaling/ice-servers",
        "/api/signaling/hosts",
        "/ws/signaling"
    }
}));

// Verificação de saúde detalhada (Healthcheck)
app.MapGet("/health", () => Results.Ok(new
{
    status = "Healthy",
    service = "RemoteDesk Signaling Server",
    version = "1.0.0",
    stage = "8/12",
    uptimeSeconds = (int)(DateTime.UtcNow - startTime).TotalSeconds,
    activeHosts = sessionManager.ActiveHostCount,
    activeClients = sessionManager.ActiveClientCount,
    totalSessions = sessionManager.TotalSessions,
    timestamp = DateTime.UtcNow
}));

// Endpoint para obtenção dinâmica de servidores ICE (STUN/TURN) e credenciais efêmeras
app.MapGet("/api/signaling/ice-servers", (string? user) =>
{
    var iceServers = sessionManager.GetIceServersConfig(user);
    return Results.Ok(new
    {
        success = true,
        iceServers = iceServers,
        count = iceServers.Count
    });
});

// Endpoint para consulta de computadores host disponíveis (descoberta remota segura)
app.MapGet("/api/signaling/hosts", () =>
{
    var hosts = sessionManager.GetOnlineHosts();
    return Results.Ok(new
    {
        success = true,
        hosts = hosts,
        count = hosts.Count
    });
});

// Endpoint de conexão bidirecional WebSockets para sinalização WebRTC (/ws/signaling)
app.Map("/ws/signaling", async (HttpContext context) =>
{
    if (context.WebSockets.IsWebSocketRequest)
    {
        using var webSocket = await context.WebSockets.AcceptWebSocketAsync();
        string clientIp = context.Connection.RemoteIpAddress?.ToString() ?? "unknown";
        await sessionManager.HandleWebSocketAsync(webSocket, clientIp, context.RequestAborted);
    }
    else
    {
        context.Response.StatusCode = StatusCodes.Status400BadRequest;
        await context.Response.WriteAsync("Requisição WebSocket esperada.");
    }
});

// Rota alternativa /api/signaling/ws para compatibilidade com proxies reversos
app.Map("/api/signaling/ws", async (HttpContext context) =>
{
    if (context.WebSockets.IsWebSocketRequest)
    {
        using var webSocket = await context.WebSockets.AcceptWebSocketAsync();
        string clientIp = context.Connection.RemoteIpAddress?.ToString() ?? "unknown";
        await sessionManager.HandleWebSocketAsync(webSocket, clientIp, context.RequestAborted);
    }
    else
    {
        context.Response.StatusCode = StatusCodes.Status400BadRequest;
        await context.Response.WriteAsync("Requisição WebSocket esperada.");
    }
});

app.Run();

// Tornar a classe Program acessível para testes integrados
public partial class Program { }

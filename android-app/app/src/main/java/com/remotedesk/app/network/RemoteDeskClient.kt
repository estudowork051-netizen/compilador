package com.remotedesk.app.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.remotedesk.app.models.AgentStatusItem
import com.remotedesk.app.models.WindowItem
import com.remotedesk.app.models.PairingRequest
import com.remotedesk.app.models.PairingResponse
import com.remotedesk.app.models.DeviceAuthVerification
import com.remotedesk.app.models.AuthorizedDeviceItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Representa uma sessão ativa de transmissão contínua de vídeo da janela remota (Etapa 5).
 */
class LiveStreamSession(
    private val isCancelled: AtomicBoolean,
    private val disconnectAction: () -> Unit
) {
    fun stop() {
        isCancelled.set(true)
        disconnectAction()
    }

    val isActive: Boolean
        get() = !isCancelled.get()
}

/**
 * Resultado de uma etapa individual do diagnóstico de conectividade.
 */
data class DiagnosticStep(
    val title: String,
    val success: Boolean,
    val durationMs: Long,
    val message: String
)

/**
 * Relatório completo de diagnóstico de rede entre Android e Windows.
 */
data class DiagnosticReport(
    val host: String,
    val port: Int,
    val steps: List<DiagnosticStep>,
    val isTcpReachable: Boolean,
    val isHttpReachable: Boolean,
    val summary: String,
    val solution: String
)

/**
 * Cliente HTTP local para comunicacao direta bidirecional entre o aplicativo Android
 * e o Windows Host Agent (porta padrao 5050).
 * Executa todas as requisicoes em segundo plano sem bloquear a UI thread.
 */
class RemoteDeskClient {

    private val executor = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "RemoteDeskClient"
        private const val CONNECT_TIMEOUT_MS = 5000
        private const val READ_TIMEOUT_MS = 8000
    }

    /**
     * Testa conectividade com o Windows Agent via /health ou /api/ping.
     */
    fun ping(host: String, port: Int, accessPin: String? = null, clientId: String? = null, deviceToken: String? = null, callback: (Result<Boolean>) -> Unit) {
        executor.execute {
            try {
                Log.d(TAG, "[CONNECT_FLOW] Preparando requisição de ping em http://$host:$port/health...")
                val url = URL("http://$host:$port/health")
                Log.i(TAG, "[CONNECT_NETWORK] Tentativa de conexão iniciada para $url")
                val responseText = executeGet(url, accessPin, clientId, deviceToken)
                val json = JSONObject(responseText)
                val success = json.optBoolean("success", true)
                Log.i(TAG, "[CONNECT_NETWORK] Resposta recebida do ping em $host:$port: sucesso=$success")
                mainHandler.post { callback(Result.success(success)) }
            } catch (e: Exception) {
                Log.e(TAG, "[CONNECT_NETWORK] Resposta recebida ou exceção identificada no ping para $host:$port: ${e.message}", e)
                mainHandler.post { callback(Result.failure(e)) }
            }
        }
    }

    /**
     * Obtem o status completo do Agente Windows (/api/status).
     */
    fun getStatus(host: String, port: Int, accessPin: String? = null, clientId: String? = null, deviceToken: String? = null, callback: (Result<AgentStatusItem>) -> Unit) {
        executor.execute {
            try {
                Log.i(TAG, "[CONNECT_FLOW] Preparando requisição de rede: GET http://$host:$port/api/status")
                val url = URL("http://$host:$port/api/status")
                Log.i(TAG, "[CONNECT_NETWORK] Tentativa de conexão iniciada para http://$host:$port/api/status")
                val responseText = executeGet(url, accessPin, clientId, deviceToken)
                Log.i(TAG, "[CONNECT_NETWORK] Resposta recebida: HTTP 200 de $host:$port (${responseText.length} bytes)")

                val json = JSONObject(responseText)
                if (json.has("success") && !json.optBoolean("success", true)) {
                    val msg = json.optString("errorMessage", "Falha de autorização ou erro reportado pelo Agente.")
                    Log.w(TAG, "[CONNECT_NETWORK] Resposta de erro do agente em /api/status: $msg")
                    throw Exception(msg)
                }

                val status = AgentStatusItem.fromJson(json)
                Log.i(TAG, "[CONNECT_NETWORK] Objeto de status desserializado com sucesso: PC=${status.deviceName}, Pareamento=${status.friendlyPairingCode}")
                mainHandler.post { callback(Result.success(status)) }
            } catch (e: Exception) {
                Log.e(TAG, "[CONNECT_NETWORK] Resposta recebida ou exceção identificada ao conectar com $host:$port: ${e.javaClass.simpleName} - ${e.message}", e)
                mainHandler.post { callback(Result.failure(e)) }
            }
        }
    }

    /**
     * Realiza o pareamento inicial do dispositivo com o Agente Windows usando o código fixo (/api/auth/pair).
     */
    fun pairDevice(
        host: String,
        port: Int,
        request: PairingRequest,
        callback: (Result<PairingResponse>) -> Unit
    ) {
        executor.execute {
            try {
                Log.i(TAG, "[AUTH] Solicitando pareamento inicial em http://$host:$port/api/auth/pair...")
                val url = URL("http://$host:$port/api/auth/pair")
                val payload = JSONObject().apply {
                    put("fixedCode", request.fixedCode)
                    put("deviceId", request.deviceId)
                    put("deviceName", request.deviceName)
                    put("clientVersion", request.clientVersion)
                }
                val responseText = executePost(url, payload.toString(), clientId = request.deviceId)
                val json = JSONObject(responseText)
                val success = json.optBoolean("success", false)
                val token = json.optString("deviceToken").takeIf { it.isNotEmpty() }
                val deviceId = json.optString("deviceId", request.deviceId)
                val hostName = json.optString("hostDeviceName", "")
                val message = json.optString("message", "")
                val errorCode = json.optString("errorCode").takeIf { it.isNotEmpty() }

                val response = PairingResponse(
                    success = success,
                    deviceToken = token,
                    deviceId = deviceId,
                    hostDeviceName = hostName,
                    message = message,
                    errorCode = errorCode
                )
                mainHandler.post { callback(Result.success(response)) }
            } catch (e: Exception) {
                Log.e(TAG, "[AUTH] Falha ao parear dispositivo: ${e.message}", e)
                mainHandler.post { callback(Result.failure(e)) }
            }
        }
    }

    /**
     * Verifica a validade do token de autorização persistente com o Agente Windows (/api/auth/verify).
     */
    fun verifyDeviceToken(
        host: String,
        port: Int,
        deviceId: String,
        deviceToken: String,
        callback: (Result<DeviceAuthVerification>) -> Unit
    ) {
        executor.execute {
            try {
                Log.i(TAG, "[AUTH] Verificando token persistente em http://$host:$port/api/auth/verify...")
                val url = URL("http://$host:$port/api/auth/verify")
                val responseText = executeGet(url, clientId = deviceId, deviceToken = deviceToken)
                val json = JSONObject(responseText)
                val success = json.optBoolean("success", false)
                val data = json.optJSONObject("data")
                val authorized = data?.optBoolean("authorized", success) ?: success
                val isRevoked = json.optString("errorCode") == "DEVICE_REVOKED" || data?.optBoolean("revoked", false) == true

                val verification = DeviceAuthVerification(
                    isAuthorized = authorized,
                    isRevoked = isRevoked,
                    errorCode = json.optString("errorCode").takeIf { it.isNotEmpty() },
                    errorMessage = json.optString("errorMessage", json.optString("message", ""))
                )
                mainHandler.post { callback(Result.success(verification)) }
            } catch (e: Exception) {
                val isRevoked = e.message?.contains("DEVICE_REVOKED", ignoreCase = true) == true
                if (isRevoked) {
                    val verification = DeviceAuthVerification(
                        isAuthorized = false,
                        isRevoked = true,
                        errorCode = "DEVICE_REVOKED",
                        errorMessage = "A autorização deste dispositivo foi revogada pelo computador."
                    )
                    mainHandler.post { callback(Result.success(verification)) }
                } else {
                    Log.e(TAG, "[AUTH] Falha ao verificar token: ${e.message}", e)
                    mainHandler.post { callback(Result.failure(e)) }
                }
            }
        }
    }

    /**
     * Revoga a autorização deste ou de outro dispositivo (/api/auth/revoke).
     */
    fun revokeDevice(
        host: String,
        port: Int,
        deviceId: String,
        deviceToken: String,
        targetDeviceId: String? = null,
        revokeAll: Boolean = false,
        callback: (Result<Boolean>) -> Unit
    ) {
        executor.execute {
            try {
                val url = URL("http://$host:$port/api/auth/revoke")
                val payload = JSONObject().apply {
                    if (!targetDeviceId.isNullOrBlank()) put("deviceId", targetDeviceId)
                    put("revokeAll", revokeAll)
                }
                val responseText = executePost(url, payload.toString(), clientId = deviceId, deviceToken = deviceToken)
                val json = JSONObject(responseText)
                val success = json.optBoolean("success", false) ||
                        json.optJSONObject("data")?.optBoolean("success", false) == true
                mainHandler.post { callback(Result.success(success)) }
            } catch (e: Exception) {
                mainHandler.post { callback(Result.failure(e)) }
            }
        }
    }

    /**
     * Consulta dispositivos autorizados no Windows (/api/auth/devices).
     */
    fun getAuthorizedDevices(
        host: String,
        port: Int,
        deviceId: String,
        deviceToken: String,
        callback: (Result<List<AuthorizedDeviceItem>>) -> Unit
    ) {
        executor.execute {
            try {
                val url = URL("http://$host:$port/api/auth/devices")
                val responseText = executeGet(url, clientId = deviceId, deviceToken = deviceToken)
                val json = JSONObject(responseText)
                val arr = json.optJSONArray("devices") ?: json.optJSONArray("data") ?: JSONArray()
                val list = mutableListOf<AuthorizedDeviceItem>()
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    list.add(
                        AuthorizedDeviceItem(
                            deviceId = item.optString("deviceId"),
                            deviceName = item.optString("deviceName"),
                            createdAt = item.optString("createdAt"),
                            lastSeenAt = item.optString("lastSeenAt"),
                            isRevoked = item.optBoolean("isRevoked", false)
                        )
                    )
                }
                mainHandler.post { callback(Result.success(list)) }
            } catch (e: Exception) {
                mainHandler.post { callback(Result.failure(e)) }
            }
        }
    }

    /**
     * Obtem a lista de janelas e programas abertos na sessao ativa do Windows (/api/windows).
     * Suporta tanto formato direto de array JSON quanto envelope tipado { success: true, data: [...] }.
     */
    fun getOpenWindows(host: String, port: Int, accessPin: String? = null, clientId: String? = null, deviceToken: String? = null, callback: (Result<List<WindowItem>>) -> Unit) {
        executor.execute {
            try {
                Log.i(TAG, "[CONNECT_FLOW] Preparando requisição de rede: GET http://$host:$port/api/windows")
                val url = URL("http://$host:$port/api/windows")
                Log.i(TAG, "[CONNECT_NETWORK] Tentativa de conexão iniciada para http://$host:$port/api/windows")
                val responseText = executeGet(url, accessPin, clientId, deviceToken)
                val trimmed = responseText.trim()
                val list = mutableListOf<WindowItem>()

                if (trimmed.startsWith("[")) {
                    val jsonArray = JSONArray(trimmed)
                    for (i in 0 until jsonArray.length()) {
                        list.add(WindowItem.fromJson(jsonArray.getJSONObject(i)))
                    }
                } else {
                    val root = JSONObject(trimmed)
                    if (root.has("success") && !root.optBoolean("success", true)) {
                        val msg = root.optString("errorMessage", "Erro ao obter janelas.")
                        throw Exception(msg)
                    }
                    val jsonArray = root.optJSONArray("data")
                    if (jsonArray != null) {
                        for (i in 0 until jsonArray.length()) {
                            list.add(WindowItem.fromJson(jsonArray.getJSONObject(i)))
                        }
                    }
                }

                Log.i(TAG, "[CONNECT_NETWORK] Resposta recebida de janelas: ${list.size} janelas abertas detectadas.")
                mainHandler.post { callback(Result.success(list)) }
            } catch (e: Exception) {
                Log.e(TAG, "[CONNECT_NETWORK] Falha ao obter lista de janelas de $host:$port: ${e.message}", e)
                mainHandler.post { callback(Result.failure(e)) }
            }
        }
    }

    /**
     * Envia comando para trazer a janela ao foco no Windows (/api/command com acao activate-window).
     */
    fun activateWindow(host: String, port: Int, windowId: String, accessPin: String? = null, clientId: String? = null, deviceToken: String? = null, callback: (Result<Boolean>) -> Unit) {
        executor.execute {
            try {
                Log.i(TAG, "[CONNECT_FLOW] Preparando requisição de rede: POST http://$host:$port/api/command (activate-window)")
                val url = URL("http://$host:$port/api/command")
                val payload = JSONObject().apply {
                    put("action", "activate-window")
                    put("targetWindowId", windowId)
                    put("windowId", windowId)
                    if (!accessPin.isNullOrBlank()) put("accessPin", accessPin)
                    if (!clientId.isNullOrBlank()) put("clientId", clientId)
                }
                Log.i(TAG, "[CONNECT_NETWORK] Enviando comando de ativação para janela $windowId em $host:$port")
                val responseText = executePost(url, payload.toString(), accessPin, clientId, deviceToken)
                val json = JSONObject(responseText)
                val success = json.optBoolean("success", false) ||
                        json.optJSONObject("data")?.optBoolean("activated", false) == true
                Log.i(TAG, "[CONNECT_NETWORK] Resposta recebida da ativação da janela $windowId: sucesso=$success")
                mainHandler.post { callback(Result.success(success)) }
            } catch (e: Exception) {
                Log.e(TAG, "[CONNECT_NETWORK] Falha ao ativar janela $windowId em $host:$port: ${e.message}", e)
                mainHandler.post { callback(Result.failure(e)) }
            }
        }
    }

    /**
     * Envia comando para restaurar janela minimizada no Windows (/api/command com acao restore-window).
     */
    fun restoreWindow(host: String, port: Int, windowId: String, accessPin: String? = null, clientId: String? = null, deviceToken: String? = null, callback: (Result<Boolean>) -> Unit) {
        executor.execute {
            try {
                Log.i(TAG, "[STREAM] Enviando comando para restaurar janela $windowId em $host:$port...")
                val url = URL("http://$host:$port/api/command")
                val payload = JSONObject().apply {
                    put("action", "restore-window")
                    put("targetWindowId", windowId)
                    put("windowId", windowId)
                    if (!accessPin.isNullOrBlank()) put("accessPin", accessPin)
                    if (!clientId.isNullOrBlank()) put("clientId", clientId)
                }
                val responseText = executePost(url, payload.toString(), accessPin, clientId, deviceToken)
                val json = JSONObject(responseText)
                val success = json.optBoolean("success", false) ||
                        json.optJSONObject("data")?.optBoolean("restored", false) == true
                Log.i(TAG, "[STREAM] Resposta da restauração da janela $windowId: sucesso=$success")
                mainHandler.post { callback(Result.success(success)) }
            } catch (e: Exception) {
                Log.e(TAG, "[STREAM] Falha ao restaurar janela $windowId em $host:$port: ${e.message}", e)
                mainHandler.post { callback(Result.failure(e)) }
            }
        }
    }

    /**
     * Envia comando para redimensionar a janela no Windows para o tamanho de referência ideal (382 × 706).
     * Comunica-se com o endpoint /api/window/resize ou /api/command com a ação resize-window.
     */
    fun resizeWindow(
        host: String,
        port: Int,
        windowId: String,
        targetWidth: Int = 382,
        targetHeight: Int = 706,
        accessPin: String? = null,
        clientId: String? = null,
        deviceToken: String? = null,
        callback: (Result<Boolean>) -> Unit
    ) {
        executor.execute {
            try {
                Log.i(TAG, "[RESIZE] Solicitando redimensionamento real da janela $windowId para ${targetWidth}x${targetHeight}...")
                val url = URL("http://$host:$port/api/command")
                val payload = JSONObject().apply {
                    put("action", "resize-window")
                    put("targetWindowId", windowId)
                    put("windowId", windowId)
                    put("width", targetWidth)
                    put("height", targetHeight)
                    if (!accessPin.isNullOrBlank()) put("accessPin", accessPin)
                    if (!clientId.isNullOrBlank()) put("clientId", clientId)
                }
                val responseText = executePost(url, payload.toString(), accessPin, clientId, deviceToken)
                val json = JSONObject(responseText)
                val success = json.optBoolean("success", false) ||
                        json.optJSONObject("data")?.optBoolean("success", false) == true
                Log.i(TAG, "[RESIZE] Resposta do redimensionamento da janela $windowId: sucesso=$success")
                mainHandler.post { callback(Result.success(success)) }
            } catch (e: Exception) {
                Log.e(TAG, "[RESIZE] Falha ao redimensionar janela $windowId em $host:$port: ${e.message}", e)
                mainHandler.post { callback(Result.failure(e)) }
            }
        }
    }

    /**
     * Envia comando de entrada de mouse remoto (/api/input ou /api/command).
     */
    fun sendMouseInput(
        host: String,
        port: Int,
        windowId: String,
        mouseAction: String,
        button: String = "left",
        normalizedX: Double? = null,
        normalizedY: Double? = null,
        deltaY: Int? = null,
        accessPin: String? = null,
        clientId: String? = null,
        deviceToken: String? = null,
        callback: ((Result<Boolean>) -> Unit)? = null
    ) {
        executor.execute {
            try {
                val url = URL("http://$host:$port/api/input")
                val payload = JSONObject().apply {
                    put("action", "mouse-input")
                    put("targetWindowId", windowId)
                    put("mouseAction", mouseAction)
                    put("button", button)
                    if (normalizedX != null) put("normalizedX", normalizedX)
                    if (normalizedY != null) put("normalizedY", normalizedY)
                    if (deltaY != null) put("deltaY", deltaY)
                    if (!accessPin.isNullOrBlank()) put("accessPin", accessPin)
                    if (!clientId.isNullOrBlank()) put("clientId", clientId)
                }
                val responseText = executePost(url, payload.toString(), accessPin, clientId, deviceToken)
                val json = JSONObject(responseText)
                val success = json.optBoolean("success", false) ||
                        json.optJSONObject("data")?.optBoolean("success", false) == true
                mainHandler.post { callback?.invoke(Result.success(success)) }
            } catch (e: Exception) {
                mainHandler.post { callback?.invoke(Result.failure(e)) }
            }
        }
    }

    /**
     * Envia sequência de texto Unicode digitada pelo teclado virtual (/api/input).
     */
    fun sendTextInput(
        host: String,
        port: Int,
        windowId: String,
        text: String,
        accessPin: String? = null,
        clientId: String? = null,
        deviceToken: String? = null,
        callback: ((Result<Boolean>) -> Unit)? = null
    ) {
        executor.execute {
            try {
                val url = URL("http://$host:$port/api/input")
                val payload = JSONObject().apply {
                    put("action", "text-input")
                    put("targetWindowId", windowId)
                    put("text", text)
                    if (!accessPin.isNullOrBlank()) put("accessPin", accessPin)
                    if (!clientId.isNullOrBlank()) put("clientId", clientId)
                }
                val responseText = executePost(url, payload.toString(), accessPin, clientId, deviceToken)
                val json = JSONObject(responseText)
                val success = json.optBoolean("success", false) ||
                        json.optJSONObject("data")?.optBoolean("success", false) == true
                mainHandler.post { callback?.invoke(Result.success(success)) }
            } catch (e: Exception) {
                mainHandler.post { callback?.invoke(Result.failure(e)) }
            }
        }
    }

    /**
     * Envia comando de tecla especial ou modificador (/api/input).
     */
    fun sendKeyEvent(
        host: String,
        port: Int,
        windowId: String,
        key: String,
        modifiers: List<String>? = null,
        isKeyDown: Boolean? = null,
        accessPin: String? = null,
        clientId: String? = null,
        deviceToken: String? = null,
        callback: ((Result<Boolean>) -> Unit)? = null
    ) {
        executor.execute {
            try {
                val url = URL("http://$host:$port/api/input")
                val payload = JSONObject().apply {
                    put("action", "key-event")
                    put("targetWindowId", windowId)
                    put("key", key)
                    if (modifiers != null && modifiers.isNotEmpty()) {
                        put("modifiers", JSONArray(modifiers))
                    }
                    if (isKeyDown != null) put("isKeyDown", isKeyDown)
                    if (!accessPin.isNullOrBlank()) put("accessPin", accessPin)
                    if (!clientId.isNullOrBlank()) put("clientId", clientId)
                }
                val responseText = executePost(url, payload.toString(), accessPin, clientId, deviceToken)
                val json = JSONObject(responseText)
                val success = json.optBoolean("success", false) ||
                        json.optJSONObject("data")?.optBoolean("success", false) == true
                mainHandler.post { callback?.invoke(Result.success(success)) }
            } catch (e: Exception) {
                mainHandler.post { callback?.invoke(Result.failure(e)) }
            }
        }
    }

    /**
     * Envia comando de rolagem vertical (/api/input).
     */
    fun sendScroll(
        host: String,
        port: Int,
        windowId: String,
        deltaY: Int,
        normalizedX: Double? = null,
        normalizedY: Double? = null,
        accessPin: String? = null,
        clientId: String? = null,
        deviceToken: String? = null,
        callback: ((Result<Boolean>) -> Unit)? = null
    ) {
        sendMouseInput(
            host = host,
            port = port,
            windowId = windowId,
            mouseAction = "wheel",
            button = "middle",
            normalizedX = normalizedX,
            normalizedY = normalizedY,
            deltaY = deltaY,
            accessPin = accessPin,
            clientId = clientId,
            callback = callback
        )
    }

    /**
     * Solicita liberação forçada de botões e teclas retidos (/api/input).
     */
    fun releaseInputs(
        host: String,
        port: Int,
        accessPin: String? = null,
        clientId: String? = null,
        deviceToken: String? = null,
        callback: ((Result<Boolean>) -> Unit)? = null
    ) {
        executor.execute {
            try {
                val url = URL("http://$host:$port/api/input")
                val payload = JSONObject().apply {
                    put("action", "release-inputs")
                    if (!accessPin.isNullOrBlank()) put("accessPin", accessPin)
                    if (!clientId.isNullOrBlank()) put("clientId", clientId)
                }
                val responseText = executePost(url, payload.toString(), accessPin, clientId, deviceToken)
                val json = JSONObject(responseText)
                val success = json.optBoolean("success", false) ||
                        json.optJSONObject("data")?.optBoolean("success", false) == true
                mainHandler.post { callback?.invoke(Result.success(success)) }
            } catch (e: Exception) {
                mainHandler.post { callback?.invoke(Result.failure(e)) }
            }
        }
    }

    /**
     * Inicia transmissão contínua de quadros da janela remota via HTTP Multipart stream (/api/stream).
     * Decodifica os quadros BMP em tempo real e entrega Bitmaps prontos na UI thread.
     */
    fun startWindowStream(
        host: String,
        port: Int,
        windowId: String,
        accessPin: String? = null,
        clientId: String? = null,
        deviceToken: String? = null,
        fps: Int = 15,
        onFrame: (Bitmap, Int, Int) -> Unit,
        onMinimized: () -> Unit,
        onClosed: () -> Unit,
        onError: (Throwable) -> Unit
    ): LiveStreamSession {
        val isCancelled = AtomicBoolean(false)
        var activeConnection: HttpURLConnection? = null

        val session = LiveStreamSession(isCancelled) {
            try {
                activeConnection?.disconnect()
            } catch (_: Exception) {}
        }

        executor.execute {
            var inputStream: InputStream? = null
            try {
                val url = URL("http://$host:$port/api/stream?windowId=$windowId&fps=$fps")
                Log.i(TAG, "[STREAM] Iniciando fluxo de vídeo da janela $windowId em $url...")

                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = 0 // Stream persistente contínuo
                    requestMethod = "GET"
                    setRequestProperty("Accept", "multipart/x-mixed-replace")
                    if (!accessPin.isNullOrBlank()) setRequestProperty("X-Access-Pin", accessPin)
                    if (!clientId.isNullOrBlank()) setRequestProperty("X-Client-Id", clientId)
                    if (!deviceToken.isNullOrBlank()) setRequestProperty("X-Device-Token", deviceToken)
                }
                activeConnection = conn
                conn.connect()

                val code = conn.responseCode
                if (code !in 200..299) {
                    throw Exception("Servidor retornou código HTTP $code ao abrir transmissão da janela.")
                }

                inputStream = BufferedInputStream(conn.inputStream, 65536)
                readMultipartStream(inputStream, isCancelled, onFrame, onMinimized, onClosed, onError)
            } catch (e: Exception) {
                if (!isCancelled.get()) {
                    Log.e(TAG, "[STREAM] Exceção durante a transmissão de vídeo: ${e.message}", e)
                    mainHandler.post { onError(e) }
                }
            } finally {
                try {
                    inputStream?.close()
                } catch (_: Exception) {}
                try {
                    activeConnection?.disconnect()
                } catch (_: Exception) {}
            }
        }

        return session
    }

    private fun readMultipartStream(
        stream: InputStream,
        isCancelled: AtomicBoolean,
        onFrame: (Bitmap, Int, Int) -> Unit,
        onMinimized: () -> Unit,
        onClosed: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val headerBuffer = StringBuilder()

        while (!isCancelled.get()) {
            headerBuffer.setLength(0)
            var contentLength = -1
            var isMinimized = false
            var isClosed = false
            var width = 0
            var height = 0

            // 1. Lê os cabeçalhos do frame até a linha em branco
            while (!isCancelled.get()) {
                val line = readLine(stream) ?: return
                if (line.isEmpty() || line == "\r") {
                    if (headerBuffer.isNotEmpty()) break
                } else {
                    headerBuffer.append(line).append("\n")
                    val lower = line.lowercase()
                    if (lower.startsWith("content-length:")) {
                        contentLength = line.substringAfter(":").trim().toIntOrNull() ?: -1
                    } else if (lower.startsWith("x-window-minimized:")) {
                        isMinimized = line.substringAfter(":").trim().toBoolean()
                    } else if (lower.startsWith("x-window-closed:")) {
                        isClosed = line.substringAfter(":").trim().toBoolean()
                    } else if (lower.startsWith("x-window-width:")) {
                        width = line.substringAfter(":").trim().toIntOrNull() ?: 0
                    } else if (lower.startsWith("x-window-height:")) {
                        height = line.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                }
            }

            if (isCancelled.get()) break

            if (isClosed) {
                Log.i(TAG, "[STREAM] Aviso de encerramento de janela recebido do Windows.")
                mainHandler.post { onClosed() }
                break
            }

            if (isMinimized) {
                mainHandler.post { onMinimized() }
                continue
            }

            // 2. Lê os bytes do payload da imagem BMP
            if (contentLength > 0) {
                val frameBytes = ByteArray(contentLength)
                var bytesRead = 0
                while (bytesRead < contentLength && !isCancelled.get()) {
                    val count = stream.read(frameBytes, bytesRead, contentLength - bytesRead)
                    if (count < 0) return // Conexão encerrada pelo host
                    bytesRead += count
                }

                if (bytesRead == contentLength && !isCancelled.get()) {
                    val bitmap = BitmapFactory.decodeByteArray(frameBytes, 0, frameBytes.size)
                    if (bitmap != null) {
                        mainHandler.post { onFrame(bitmap, width, height) }
                    }
                }
            }
        }
    }

    private fun readLine(stream: InputStream): String? {
        val sb = StringBuilder()
        var b: Int
        while (stream.read().also { b = it } != -1) {
            val c = b.toChar()
            if (c == '\n') break
            if (c != '\r') sb.append(c)
        }
        return if (b == -1 && sb.isEmpty()) null else sb.toString()
    }

    /**
     * Captura um único quadro estático da janela indicada (/api/window/frame).
     */
    fun fetchWindowFrame(
        host: String,
        port: Int,
        windowId: String,
        accessPin: String? = null,
        clientId: String? = null,
        deviceToken: String? = null,
        callback: (Result<Bitmap>) -> Unit
    ) {
        executor.execute {
            try {
                val url = URL("http://$host:$port/api/window/frame?windowId=$windowId")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    requestMethod = "GET"
                    if (!accessPin.isNullOrBlank()) setRequestProperty("X-Access-Pin", accessPin)
                    if (!clientId.isNullOrBlank()) setRequestProperty("X-Client-Id", clientId)
                    if (!deviceToken.isNullOrBlank()) setRequestProperty("X-Device-Token", deviceToken)
                }
                conn.connect()
                val code = conn.responseCode
                if (code !in 200..299) {
                    throw Exception("Servidor retornou código HTTP $code ao capturar quadro.")
                }
                val bytes = conn.inputStream.use { it.readBytes() }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw Exception("Não foi possível decodificar o quadro BMP retornado.")
                mainHandler.post { callback(Result.success(bitmap)) }
            } catch (e: Exception) {
                mainHandler.post { callback(Result.failure(e)) }
            }
        }
    }

    /**
     * Executa bateria de testes diagnósticos de conectividade (TCP puro -> /health -> /api/status).
     */
    fun runConnectivityDiagnostic(host: String, port: Int, callback: (DiagnosticReport) -> Unit) {
        executor.execute {
            val steps = mutableListOf<DiagnosticStep>()
            var isTcp = false
            var isHttp = false
            var summary = ""
            var solution = ""

            // 1. Teste de Handshake TCP puro (camada de transporte)
            val tcpSocket = Socket()
            val tcpStart = System.currentTimeMillis()
            try {
                tcpSocket.connect(InetSocketAddress(host, port), 4000)
                val tcpElapsed = System.currentTimeMillis() - tcpStart
                tcpSocket.close()
                isTcp = true
                steps.add(DiagnosticStep("Handshake TCP (Porta $port)", true, tcpElapsed, "Conexão TCP estabelecida em ${tcpElapsed}ms. A porta está aberta e acessível."))
            } catch (e: SocketTimeoutException) {
                val tcpElapsed = System.currentTimeMillis() - tcpStart
                isTcp = false
                steps.add(DiagnosticStep("Handshake TCP (Porta $port)", false, tcpElapsed, "TIMEOUT (4000ms): Pacote SYN enviado, mas o PC não respondeu."))
                summary = "Falha no Handshake TCP: Os pacotes estão sendo bloqueados antes de alcançar o Agente."
                solution = "Causa Provável: Firewall do Windows ou Perfil de Rede.\n\nNo Windows PowerShell (como Administrador), execute:\nNew-NetFirewallRule -DisplayName 'RemoteDesk 5050' -Direction Inbound -LocalPort $port -Protocol TCP -Action Allow -Profile Private\n\nCertifique-se também de que a rede Wi-Fi do PC está definida como 'Rede Privada' (Get-NetConnectionProfile)."
            } catch (e: ConnectException) {
                val tcpElapsed = System.currentTimeMillis() - tcpStart
                isTcp = false
                steps.add(DiagnosticStep("Handshake TCP (Porta $port)", false, tcpElapsed, "CONEXÃO RECUSADA: O IP $host respondeu, mas a porta $port está fechada."))
                summary = "Porta Fechada: O Agente Windows não está ouvindo na porta $port."
                solution = "Inicie o Agente Windows no computador com:\ndotnet run --project windows-agent\\src\\RemoteDesk.Agent"
            } catch (e: Exception) {
                val tcpElapsed = System.currentTimeMillis() - tcpStart
                isTcp = false
                steps.add(DiagnosticStep("Handshake TCP (Porta $port)", false, tcpElapsed, "ERRO: ${e.javaClass.simpleName} - ${e.message}"))
                summary = "Falha de Roteamento: Não foi possível alcançar o IP $host."
                solution = "Verifique se o smartphone e o PC estão conectados ao mesmo roteador Wi-Fi."
            }

            // 2. Teste HTTP /health (camada de aplicação)
            if (isTcp) {
                val httpStart = System.currentTimeMillis()
                try {
                    val url = URL("http://$host:$port/health")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 3000
                        readTimeout = 3000
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/json")
                    }
                    val code = conn.responseCode
                    val httpElapsed = System.currentTimeMillis() - httpStart
                    conn.disconnect()
                    if (code in 200..299) {
                        isHttp = true
                        steps.add(DiagnosticStep("Serviço HTTP (/health)", true, httpElapsed, "Resposta HTTP $code recebida em ${httpElapsed}ms."))
                    } else {
                        steps.add(DiagnosticStep("Serviço HTTP (/health)", false, httpElapsed, "Servidor HTTP retornou código de erro $code."))
                        summary = "Servidor HTTP ativo, porém retornou erro $code."
                    }
                } catch (e: Exception) {
                    val httpElapsed = System.currentTimeMillis() - httpStart
                    steps.add(DiagnosticStep("Serviço HTTP (/health)", false, httpElapsed, "Exceção HTTP: ${e.message}"))
                    summary = "Socket TCP aberto, porém a requisição HTTP falhou."
                }
            }

            // 3. Teste API de Status (/api/status)
            if (isHttp) {
                val statusStart = System.currentTimeMillis()
                try {
                    val url = URL("http://$host:$port/api/status")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 3000
                        readTimeout = 3000
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/json")
                    }
                    val code = conn.responseCode
                    val raw = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).readText()
                    val statusElapsed = System.currentTimeMillis() - statusStart
                    conn.disconnect()
                    val json = JSONObject(raw)
                    val data = if (json.has("data") && !json.isNull("data") && json.optJSONObject("data") != null) json.getJSONObject("data") else json
                    val devName = data.optString("deviceName", "PC")
                    val pairCode = data.optString("friendlyPairingCode", "")
                    steps.add(DiagnosticStep("API de Status (/api/status)", true, statusElapsed, "Dispositivo: $devName | Código de Pareamento no PC: $pairCode"))
                    summary = "Comunicação 100% Funcional! O Android e o Windows estão se comunicando perfeitamente."
                    solution = "Digite o código '$pairCode' no campo acima e toque em 'Conectar ao Computador'."
                } catch (e: Exception) {
                    val statusElapsed = System.currentTimeMillis() - statusStart
                    steps.add(DiagnosticStep("API de Status (/api/status)", false, statusElapsed, "Falha ao obter status: ${e.message}"))
                }
            }

            val report = DiagnosticReport(
                host = host,
                port = port,
                steps = steps,
                isTcpReachable = isTcp,
                isHttpReachable = isHttp,
                summary = summary,
                solution = solution
            )

            mainHandler.post { callback(report) }
        }
    }

    private fun executeGet(url: URL, accessPin: String? = null, clientId: String? = null, deviceToken: String? = null): String {
        Log.d(TAG, "[CONNECT_NETWORK] Conectando via HttpURLConnection em $url")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("ngrok-skip-browser-warning", "true")
            setRequestProperty("User-Agent", "RemoteDesk-Android/1.0")
            if (!accessPin.isNullOrBlank()) setRequestProperty("X-Access-Pin", accessPin)
            if (!clientId.isNullOrBlank()) setRequestProperty("X-Client-Id", clientId)
            if (!deviceToken.isNullOrBlank()) setRequestProperty("X-Device-Token", deviceToken)
        }

        val startConnect = System.currentTimeMillis()
        try {
            conn.connect()
        } catch (e: SocketTimeoutException) {
            val elapsed = System.currentTimeMillis() - startConnect
            conn.disconnect()
            throw SocketTimeoutException("Tempo limite atingido durante o handshake TCP (${elapsed}ms) ao conectar com $url. O PC não respondeu ao pacote SYN. Causa provável: O Firewall do Windows está bloqueando a porta ${url.port} para a rede local ou o perfil Wi-Fi do PC está como 'Público'.")
        }

        val code = try {
            conn.responseCode
        } catch (e: SocketTimeoutException) {
            conn.disconnect()
            throw SocketTimeoutException("Conexão TCP estabelecida com $url, mas o servidor demorou mais de ${READ_TIMEOUT_MS}ms para responder.")
        }

        if (code !in 200..299) {
            val errBody = try {
                val errStream = conn.errorStream
                if (errStream != null) {
                    val raw = BufferedReader(InputStreamReader(errStream, "UTF-8")).readText()
                    val obj = JSONObject(raw)
                    obj.optString("errorMessage", obj.optString("message", raw))
                } else null
            } catch (_: Exception) {
                null
            }
            conn.disconnect()
            val desc = if (!errBody.isNullOrBlank()) " ($errBody)" else ""
            Log.w(TAG, "[CONNECT_NETWORK] Servidor retornou código HTTP $code$desc")
            throw Exception("Falha HTTP $code na requisicao GET $url$desc")
        }

        val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
        val response = reader.readText()
        reader.close()
        conn.disconnect()
        Log.d(TAG, "[CONNECT_NETWORK] Requisição GET $url concluída com sucesso ($code, ${response.length} bytes)")
        return response
    }

    private fun executePost(url: URL, body: String, accessPin: String? = null, clientId: String? = null, deviceToken: String? = null): String {
        Log.d(TAG, "[CONNECT_NETWORK] Conectando via HttpURLConnection em $url (POST)")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("ngrok-skip-browser-warning", "true")
            setRequestProperty("User-Agent", "RemoteDesk-Android/1.0")
            if (!accessPin.isNullOrBlank()) setRequestProperty("X-Access-Pin", accessPin)
            if (!clientId.isNullOrBlank()) setRequestProperty("X-Client-Id", clientId)
            if (!deviceToken.isNullOrBlank()) setRequestProperty("X-Device-Token", deviceToken)
        }

        val startConnect = System.currentTimeMillis()
        try {
            conn.connect()
        } catch (e: SocketTimeoutException) {
            val elapsed = System.currentTimeMillis() - startConnect
            conn.disconnect()
            throw SocketTimeoutException("Tempo limite atingido durante o handshake TCP (${elapsed}ms) ao conectar com $url. O PC não respondeu ao pacote SYN. Causa provável: O Firewall do Windows está bloqueando a porta ${url.port}.")
        }

        val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
        writer.write(body)
        writer.flush()
        writer.close()

        val code = try {
            conn.responseCode
        } catch (e: SocketTimeoutException) {
            conn.disconnect()
            throw SocketTimeoutException("Conexão TCP estabelecida com $url, mas o servidor demorou mais de ${READ_TIMEOUT_MS}ms para responder.")
        }

        if (code !in 200..299) {
            val errBody = try {
                val errStream = conn.errorStream
                if (errStream != null) {
                    val raw = BufferedReader(InputStreamReader(errStream, "UTF-8")).readText()
                    val obj = JSONObject(raw)
                    obj.optString("errorMessage", obj.optString("message", raw))
                } else null
            } catch (_: Exception) {
                null
            }
            conn.disconnect()
            val desc = if (!errBody.isNullOrBlank()) " ($errBody)" else ""
            Log.w(TAG, "[CONNECT_NETWORK] Servidor retornou código HTTP $code$desc")
            throw Exception("Falha HTTP $code na requisicao POST $url$desc")
        }

        val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
        val response = reader.readText()
        reader.close()
        conn.disconnect()
        Log.d(TAG, "[CONNECT_NETWORK] Requisição POST $url concluída com sucesso ($code, ${response.length} bytes)")
        return response
    }
}

package com.remotedesk.app.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import com.remotedesk.app.models.PairingResponse
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Cliente de sinalização WebSockets e negociação WebRTC para acesso remoto pela Internet (Etapa 8).
 * Suporta pareamento remoto por PIN, troca de SDP Offer/Answer, candidatos ICE,
 * credenciais dinâmicas de STUN/TURN e canal de dados de alta performance com fallback relay.
 */
class SignalingClient(
    private val signalingBaseUrl: String,
    private val clientDeviceId: String,
    private val clientDeviceName: String
) {
    companion object {
        private const val TAG = "SignalingClient"
    }

    private val executor = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isRunning = AtomicBoolean(false)

    interface SignalingListener {
        fun onConnected()
        fun onRegistered(deviceId: String)
        fun onPairingResult(response: PairingResponse)
        fun onSdpAnswer(sdp: String)
        fun onIceCandidate(candidate: String, sdpMid: String?, sdpMLineIndex: Int?)
        fun onRelayDataReceived(payload: String)
        fun onFrameReceived(bitmap: Bitmap, width: Int, height: Int)
        fun onError(errorCode: String, message: String)
        fun onDisconnected()
    }

    private var listener: SignalingListener? = null
    private var webSocketConnection: java.net.Socket? = null
    private var outputWriter: java.io.BufferedWriter? = null

    fun setListener(listener: SignalingListener?) {
        this.listener = listener
    }

    /**
     * Consulta servidores ICE (STUN e TURN) dinâmicos disponibilizados pelo servidor de sinalização.
     */
    fun fetchIceServers(callback: (List<String>) -> Unit) {
        executor.execute {
            try {
                val cleanUrl = signalingBaseUrl.trimEnd('/') + "/api/signaling/ice-servers"
                val conn = URL(cleanUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                conn.setRequestProperty("ngrok-skip-browser-warning", "true")
                conn.setRequestProperty("User-Agent", "RemoteDesk-Android/1.0")

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val json = reader.readText()
                    reader.close()

                    val root = JSONObject(json)
                    val servers = mutableListOf<String>()
                    if (root.optBoolean("success", false)) {
                        val array = root.optJSONArray("iceServers") ?: JSONArray()
                        for (i in 0 until array.length()) {
                            val item = array.getJSONObject(i)
                            val urls = item.optJSONArray("urls") ?: JSONArray()
                            for (j in 0 until urls.length()) {
                                servers.add(urls.getString(j))
                            }
                        }
                    }
                    mainHandler.post { callback(servers) }
                    return@execute
                }
            } catch (ex: Exception) {
                Log.w(TAG, "[ICE] Falha ao consultar servidores ICE: ${ex.message}")
            }
            val defaultStun1 = "stun:stun" + ".l.google.com:19302"
            val defaultStun2 = "stun:stun1" + ".l.google.com:19302"
            mainHandler.post {
                callback(listOf(defaultStun1, defaultStun2))
            }
        }
    }

    /**
     * Inicia conexão com o servidor de sinalização e registra o cliente Android.
     */
    fun connect() {
        if (isRunning.get()) return
        isRunning.set(true)

        executor.execute {
            try {
                val cleanUrl = signalingBaseUrl.trimEnd('/')
                val uri = URI(cleanUrl)
                val isTls = cleanUrl.startsWith("https://", ignoreCase = true) || cleanUrl.startsWith("wss://", ignoreCase = true)
                val host = uri.host ?: "10.0.2.2"
                val port = if (uri.port > 0) uri.port else (if (isTls) 443 else 5000)

                Log.i(TAG, "[SINALIZAÇÃO] Conectando ao backend de sinalização em $host:$port (TLS: $isTls)...")
                val socket: java.net.Socket = if (isTls) {
                    val sslSocket = javax.net.ssl.SSLSocketFactory.getDefault().createSocket() as javax.net.ssl.SSLSocket
                    val sslParams = sslSocket.sslParameters
                    sslParams.serverNames = listOf(javax.net.ssl.SNIHostName(host))
                    sslSocket.sslParameters = sslParams
                    sslSocket.connect(java.net.InetSocketAddress(host, port), 8000)
                    sslSocket.startHandshake()
                    sslSocket
                } else {
                    val plainSocket = java.net.Socket()
                    plainSocket.connect(java.net.InetSocketAddress(host, port), 8000)
                    plainSocket
                }
                webSocketConnection = socket

                val out = java.io.BufferedWriter(java.io.OutputStreamWriter(socket.getOutputStream(), "UTF-8"))
                outputWriter = out
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))

                // Handshake HTTP upgrade para WebSocket com bypass de aviso do ngrok
                val path = if (uri.path.isNullOrEmpty() || uri.path == "/") "/ws/signaling" else uri.path
                out.write("GET $path HTTP/1.1\r\n")
                out.write("Host: $host\r\n")
                out.write("Upgrade: websocket\r\n")
                out.write("Connection: Upgrade\r\n")
                out.write("Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n")
                out.write("Sec-WebSocket-Version: 13\r\n")
                out.write("ngrok-skip-browser-warning: true\r\n")
                out.write("User-Agent: RemoteDesk-Android/1.0\r\n\r\n")
                out.flush()

                // Consome os cabeçalhos de resposta HTTP até a linha em branco
                var headerLine = reader.readLine()
                var isUpgraded = false
                while (headerLine != null && headerLine.isNotEmpty()) {
                    if (headerLine.contains("101") || headerLine.contains("Upgrade")) {
                        isUpgraded = true
                    }
                    headerLine = reader.readLine()
                }

                mainHandler.post { listener?.onConnected() }

                // Registra o cliente imediatamente
                registerClient()

                // Loop de leitura de mensagens de sinalização
                while (isRunning.get() && !socket.isClosed) {
                    val line = reader.readLine() ?: break
                    if (line.startsWith("{") && line.endsWith("}")) {
                        handleIncomingJson(line)
                    }
                }
            } catch (ex: Exception) {
                Log.w(TAG, "[SINALIZAÇÃO] Exceção na conexão: ${ex.message}")
                mainHandler.post { listener?.onError("CONN_FAILED", ex.message ?: "Falha na conexão de sinalização.") }
            } finally {
                disconnect()
            }
        }
    }

    private fun registerClient() {
        val json = JSONObject().apply {
            put("type", "register")
            put("role", "client")
            put("sourceDeviceId", clientDeviceId)
            put("deviceName", clientDeviceName)
        }
        sendRaw(json.toString())
    }

    /**
     * Envia solicitação remota de pareamento com o PIN do proprietário pela Internet.
     */
    fun sendPairRequest(targetHostDeviceId: String, pin: String) {
        val json = JSONObject().apply {
            put("type", "pair-request")
            put("targetDeviceId", targetHostDeviceId)
            put("sourceDeviceId", clientDeviceId)
            put("deviceName", clientDeviceName)
            put("pin", pin)
        }
        sendRaw(json.toString())
    }

    /**
     * Envia oferta SDP WebRTC para o computador Windows.
     */
    fun sendOffer(targetHostDeviceId: String, sdp: String, token: String?) {
        val json = JSONObject().apply {
            put("type", "offer")
            put("targetDeviceId", targetHostDeviceId)
            put("sourceDeviceId", clientDeviceId)
            put("sdp", sdp)
            put("sdpType", "offer")
            put("token", token)
        }
        sendRaw(json.toString())
    }

    /**
     * Envia candidato ICE para o computador remoto.
     */
    fun sendIceCandidate(targetHostDeviceId: String, candidate: String, sdpMid: String?, sdpMLineIndex: Int?) {
        val json = JSONObject().apply {
            put("type", "ice-candidate")
            put("targetDeviceId", targetHostDeviceId)
            put("sourceDeviceId", clientDeviceId)
            put("candidate", candidate)
            put("sdpMid", sdpMid)
            put("sdpMLineIndex", sdpMLineIndex ?: 0)
        }
        sendRaw(json.toString())
    }

    /**
     * Envia comando ou dados via canal de relay / contingência (WebSockets Relay).
     */
    fun sendRelayData(targetHostDeviceId: String, payload: String, token: String?) {
        val json = JSONObject().apply {
            put("type", "relay-data")
            put("targetDeviceId", targetHostDeviceId)
            put("sourceDeviceId", clientDeviceId)
            put("token", token)
            put("payload", payload)
        }
        sendRaw(json.toString())
    }

    private fun handleIncomingJson(jsonStr: String) {
        try {
            val root = JSONObject(jsonStr)
            val type = root.optString("type", "")

            when (type) {
                "registered" -> {
                    val id = root.optString("sourceDeviceId", clientDeviceId)
                    mainHandler.post { listener?.onRegistered(id) }
                }
                "pair-response" -> {
                    val success = root.optBoolean("success", false)
                    val token = root.optString("token", "")
                    val hostName = root.optString("deviceName", "Windows Host")
                    val msg = root.optString("message", "")
                    val errCode = root.optString("errorCode", "")

                    val res = PairingResponse(
                        success = success,
                        deviceToken = if (token.isNotBlank()) token else null,
                        deviceId = clientDeviceId,
                        hostDeviceName = hostName,
                        message = msg,
                        errorCode = if (errCode.isNotBlank()) errCode else null
                    )
                    mainHandler.post { listener?.onPairingResult(res) }
                }
                "answer" -> {
                    val sdp = root.optString("sdp", "")
                    mainHandler.post { listener?.onSdpAnswer(sdp) }
                }
                "ice-candidate" -> {
                    val cand = root.optString("candidate", "")
                    val mid = root.optString("sdpMid", "")
                    val idx = root.optInt("sdpMLineIndex", 0)
                    mainHandler.post { listener?.onIceCandidate(cand, mid, idx) }
                }
                "relay-data" -> {
                    val payload = root.optString("payload", "")
                    // Verifica se o payload é um quadro de vídeo
                    if (payload.contains("\"type\":\"frame\"")) {
                        try {
                            val fObj = JSONObject(payload)
                            val b64 = fObj.optString("data", "")
                            val width = fObj.optInt("width", 0)
                            val height = fObj.optInt("height", 0)
                            if (b64.isNotEmpty()) {
                                val bytes = Base64.decode(b64, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (bitmap != null) {
                                    mainHandler.post { listener?.onFrameReceived(bitmap, width, height) }
                                    return
                                }
                            }
                        } catch (ex: Exception) {
                            Log.w(TAG, "[FRAME] Erro ao decodificar frame: ${ex.message}")
                        }
                    }
                    mainHandler.post { listener?.onRelayDataReceived(payload) }
                }
                "error" -> {
                    val errCode = root.optString("errorCode", "ERROR")
                    val message = root.optString("message", "Erro na sinalização.")
                    mainHandler.post { listener?.onError(errCode, message) }
                }
            }
        } catch (ex: Exception) {
            Log.w(TAG, "[SINALIZAÇÃO] Erro ao analisar mensagem: ${ex.message}")
        }
    }

    private fun sendRaw(data: String) {
        executor.execute {
            try {
                outputWriter?.apply {
                    write(data)
                    write("\r\n")
                    flush()
                }
            } catch (ex: Exception) {
                Log.w(TAG, "[SINALIZAÇÃO] Falha ao enviar dados brutos: ${ex.message}")
            }
        }
    }

    fun disconnect() {
        if (!isRunning.getAndSet(false)) return
        try {
            webSocketConnection?.close()
        } catch (ignored: Exception) {}
        webSocketConnection = null
        outputWriter = null
        mainHandler.post { listener?.onDisconnected() }
    }
}

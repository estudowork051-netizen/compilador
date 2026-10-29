package com.remotedesk.app

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.util.UUID
import com.remotedesk.app.models.PairingRequest
import com.remotedesk.app.models.PairingResponse
import com.remotedesk.app.models.DeviceAuthVerification
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.remotedesk.app.databinding.ActivityMainBinding
import com.remotedesk.app.models.AgentStatusItem
import com.remotedesk.app.models.WindowItem
import com.remotedesk.app.network.LiveStreamSession
import com.remotedesk.app.network.RemoteDeskClient
import com.remotedesk.app.ui.WindowGridAdapter
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * MainActivity do RemoteDesk.
 * Gerencia a conexao bidirecional com o agente Windows, exibe a grade de janelas abertas
 * e realiza a captura e transmissao contínua de tela em tempo real adaptada à tela vertical (Etapa 5/12).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val client = RemoteDeskClient()
    private lateinit var windowGridAdapter: WindowGridAdapter

    private var currentHost: String = DEFAULT_HOST
    private var currentPort: Int = DEFAULT_PORT
    private var currentAccessPin: String = ""
    private var isFormattingCode = false
    private var isConnected = false

    // Estado da transmissão ao vivo de janela (Etapa 5)
    private var liveStreamSession: LiveStreamSession? = null
    private var currentStreamingWindow: WindowItem? = null
    private var frameCount: Int = 0
    private var lastFpsCalculationTime: Long = 0L
    private var currentFps: Int = 0

    // Modos de escala e zoom para a visualização vertical (Etapa 5)
    private enum class ScaleMode {
        FIT,     // Ajuste completo da janela à tela vertical (padrão)
        READING  // Modo leitura ampliado (1.75x) com navegação panorâmica
    }

    private var currentScaleMode: ScaleMode = ScaleMode.FIT
    private var currentScaleFactor: Float = 1.0f
    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    private var posX: Float = 0f
    private var posY: Float = 0f
    private var activePointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var lastFrameWidth: Int = 0
    private var lastFrameHeight: Int = 0

    // Modos de controle remoto de mouse e teclado (Etapa 6)
    enum class RemoteMouseMode {
        CLICK,        // Toque = clique esquerdo
        RIGHT_CLICK,  // Toque = clique direito
        DRAG,         // Arrastar com botão esquerdo pressionado
        SCROLL        // Gesto vertical de rolagem da roda do mouse
    }

    private var currentMouseMode: RemoteMouseMode = RemoteMouseMode.CLICK
    private var isCtrlActive: Boolean = false
    private var isAltActive: Boolean = false
    private var isShiftActive: Boolean = false
    private var lastDragMoveTime: Long = 0L

    // Autenticação Pessoal, Pareamento Persistente e Reconexão (Etapa 7)
    private var deviceId: String = ""
    private var deviceName: String = "Android Client"
    private var deviceToken: String? = null
    private var isPaired: Boolean = false
    private var pairedDeviceName: String = "Windows Host"

    private val autoReconnectHandler = Handler(Looper.getMainLooper())
    private var autoReconnectRunnable: Runnable? = null
    private var reconnectAttempt = 0
    private var isUserInitiatedDisconnect = false
    private var isReconnecting = false

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }
    private val secureStorage by lazy { com.remotedesk.app.utils.SecureStorage(this) }

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "RemoteDeskPrefs"
        private const val KEY_HOST = "saved_host"
        private const val KEY_PORT = "saved_port"
        private const val KEY_PIN = "saved_pin"
        private const val KEY_SIGNALING_URL = "saved_signaling_url"
        private const val DEFAULT_HOST = "192.168.3.5"
        private const val DEFAULT_PORT = 5050

        // Área de visualização remota padronizada de referência (Etapa 5.1): 382 x 706 dp
        const val REF_VIEWPORT_WIDTH_DP = 382f
        const val REF_VIEWPORT_HEIGHT_DP = 706f
        const val REF_VIEWPORT_ASPECT = REF_VIEWPORT_WIDTH_DP / REF_VIEWPORT_HEIGHT_DP // ~0.541076f

        // Dimensão de referência do Bloco de Notas para calibração de leitura confortável
        const val NOTEPAD_REF_WIDTH = 640f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Bloqueio estrito da orientação vertical (Portrait)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSplashScreen()
        loadSavedNetworkPreferences()
        setupPairingCodeFormatter()
        setupServerSettingsToggle()
        setupDiagnosticButton()
        setupRecyclerView()
        setupConnectButton()
        setupUnpairButton()
        setupCancelReconnectButton()
        setupConnectedControls()
        setupViewportGestures()
        setupRemoteControlInputs()
    }

    /**
     * Apresenta a tela de abertura (Splash Screen) Cyber-Zombie / by HOD e realiza transicao suave.
     */
    private fun setupSplashScreen() {
        binding.layoutSplashOverlay.alpha = 1f
        binding.layoutSplashOverlay.visibility = View.VISIBLE
        binding.layoutSplashOverlay.postDelayed({
            binding.layoutSplashOverlay.animate()
                .alpha(0f)
                .setDuration(400L)
                .withEndAction {
                    binding.layoutSplashOverlay.visibility = View.GONE
                }
                .start()
        }, 500L)
    }

    /**
     * Carrega configuracoes de rede persistidas (ou usa defaults adequados para Wi-Fi local).
     */
    private fun loadSavedNetworkPreferences() {
        deviceId = secureStorage.getDeviceId().ifEmpty {
            prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
                prefs.edit().putString("device_id", it).apply()
            }
        }
        deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim().ifEmpty { "Android Client" }
        deviceToken = secureStorage.getDeviceToken() ?: prefs.getString("device_token", null)
        isPaired = (secureStorage.isPaired() || prefs.getBoolean("is_paired", false)) && !deviceToken.isNullOrBlank()
        pairedDeviceName = secureStorage.getPairedDeviceName().ifEmpty { prefs.getString("paired_device_name", "Windows Host") ?: "Windows Host" }

        var savedHost = prefs.getString(KEY_HOST, DEFAULT_HOST) ?: DEFAULT_HOST
        if (savedHost.isBlank() || savedHost == "10.0.2.2") {
            savedHost = DEFAULT_HOST
        }
        val savedPort = prefs.getInt(KEY_PORT, DEFAULT_PORT)
        val savedPin = prefs.getString(KEY_PIN, "") ?: ""

        currentHost = savedHost
        currentPort = savedPort
        currentAccessPin = savedPin

        binding.etHost.setText(savedHost)
        binding.etPort.setText(savedPort.toString())
        if (savedPin.isNotEmpty()) {
            binding.etAccessPin.setText(savedPin)
        }
        val savedSignaling = prefs.getString(KEY_SIGNALING_URL, "") ?: ""
        if (savedSignaling.isNotEmpty()) {
            binding.etSignalingUrl.setText(savedSignaling)
        }

        updatePairingUi()
        Log.d(TAG, "[CONNECT_UI] Configurações carregadas: Host=$currentHost, DeviceId=$deviceId, Paired=$isPaired")
    }

    /**
     * Atualiza os componentes de interface dependentes do status de pareamento persistente (Etapa 7).
     */
    private fun updatePairingUi() {
        if (isPaired && !deviceToken.isNullOrBlank()) {
            binding.layoutPairedStatus.visibility = View.VISIBLE
            binding.tvPairedHostInfo.text = "Pareado com: $pairedDeviceName"
            binding.tilPairingCode.visibility = View.GONE
            binding.btnConnect.text = getString(R.string.btn_auto_connect)
        } else {
            binding.layoutPairedStatus.visibility = View.GONE
            binding.tilPairingCode.visibility = View.VISIBLE
            binding.btnConnect.text = getString(R.string.btn_connect)
        }
    }

    /**
     * Configura o botão de desparear dispositivo persistente (Etapa 7).
     */
    private fun setupUnpairButton() {
        binding.btnUnpairDevice.setOnClickListener {
            val tokenToRevoke = deviceToken
            if (!tokenToRevoke.isNullOrBlank()) {
                client.revokeDevice(currentHost, currentPort, deviceId, tokenToRevoke, targetDeviceId = deviceId) { /* notificação assíncrona ao PC */ }
            }
            secureStorage.clearAll()
            prefs.edit()
                .remove("device_token")
                .putBoolean("is_paired", false)
                .remove("paired_device_name")
                .apply()
            isPaired = false
            deviceToken = null
            pairedDeviceName = ""
            updatePairingUi()
            Toast.makeText(this@MainActivity, "Pareamento removido com sucesso.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Configura o botão de cancelamento manual da reconexão automática (Etapa 7).
     */
    private fun setupCancelReconnectButton() {
        binding.btnCancelReconnect.setOnClickListener {
            isUserInitiatedDisconnect = true
            cancelAutoReconnect()
            disconnect()
        }
    }

    /**
     * Persiste as configuracoes confirmadas de rede.
     */
    private fun saveNetworkPreferences(host: String, port: Int, pin: String, signalingUrl: String = "") {
        prefs.edit()
            .putString(KEY_HOST, host)
            .putInt(KEY_PORT, port)
            .putString(KEY_PIN, pin)
            .putString(KEY_SIGNALING_URL, signalingUrl)
            .apply()
    }

    /**
     * Configura o formatador automatico do codigo de 9 digitos (ex: 492 813 605).
     */
    private fun setupPairingCodeFormatter() {
        binding.etPairingCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormattingCode) return

                val text = s?.toString() ?: ""
                val clean = text.replace(" ", "")

                // Se o texto for puramente numérico, aplica espaçamento em blocos de 3
                if (clean.isNotEmpty() && clean.all { it.isDigit() }) {
                    val rawDigits = if (clean.length > 9) clean.substring(0, 9) else clean
                    val formatted = buildString {
                        for (i in rawDigits.indices) {
                            if (i == 3 || i == 6) {
                                append(' ')
                            }
                            append(rawDigits[i])
                        }
                    }

                    if (formatted != text) {
                        isFormattingCode = true
                        binding.etPairingCode.setText(formatted)
                        binding.etPairingCode.setSelection(formatted.length)
                        isFormattingCode = false
                    }
                }

                if (text.isNotBlank()) {
                    binding.tilPairingCode.error = null
                }
            }
        })
    }

    /**
     * Alterna a visibilidade dos parametros avancados de rede (Host, Porta, PIN).
     */
    private fun setupServerSettingsToggle() {
        binding.tvToggleServerSettings.setOnClickListener {
            val isVisible = binding.layoutServerSettings.visibility == View.VISIBLE
            binding.layoutServerSettings.visibility = if (isVisible) View.GONE else View.VISIBLE
            binding.tvToggleServerSettings.text = if (isVisible) {
                getString(R.string.advanced_settings_toggle)
            } else {
                "${getString(R.string.advanced_settings_toggle)} ▲"
            }
        }
    }

    /**
     * Configura o botão de diagnóstico integrado de conectividade.
     */
    private fun setupDiagnosticButton() {
        binding.btnDiagnoseConnection.setOnClickListener {
            val rawHost = binding.etHost.text?.toString()?.trim()
            val rawPort = binding.etPort.text?.toString()?.trim()
            val host = if (!rawHost.isNullOrBlank() && rawHost != "10.0.2.2") rawHost else DEFAULT_HOST
            val port = rawPort?.toIntOrNull() ?: DEFAULT_PORT

            binding.btnDiagnoseConnection.isEnabled = false
            binding.tvDiagnosticOutput.visibility = View.VISIBLE
            binding.tvDiagnosticOutput.text = "⏳ Testando conectividade com $host:$port...\n1. Testando conexão TCP pura..."

            client.runConnectivityDiagnostic(host, port) { report ->
                binding.btnDiagnoseConnection.isEnabled = true
                val sb = StringBuilder()
                sb.append("📋 DIAGNÓSTICO DE REDE ($host:$port)\n\n")
                for (step in report.steps) {
                    val icon = if (step.success) "✅" else "❌"
                    sb.append("$icon ${step.title}: ${step.message}\n\n")
                }
                sb.append("📌 RESUMO: ${report.summary}\n")
                if (report.solution.isNotEmpty()) {
                    sb.append("\n💡 SOLUÇÃO:\n${report.solution}")
                }
                binding.tvDiagnosticOutput.text = sb.toString()
            }
        }
    }

    /**
     * Inicializa a RecyclerView configurada como Grade Visual de Icones com 2 colunas.
     */
    private fun setupRecyclerView() {
        windowGridAdapter = WindowGridAdapter { windowItem ->
            displaySelectedWindowDetails(windowItem)
        }

        binding.rvWindowsGrid.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = windowGridAdapter
            setHasFixedSize(true)
        }
    }

    /**
     * Configura o botao Conectar, iniciando a comunicacao HTTP com o Agente Windows.
     */
    private fun setupConnectButton() {
        binding.btnConnect.setOnClickListener {
            Log.i(TAG, "[CONNECT_UI] Toque no botão de conexão recebido")
            isUserInitiatedDisconnect = false

            // Sanitizacao e extracao do endereco do Agente Windows
            val rawHost = binding.etHost.text?.toString()?.trim()
            val rawPort = binding.etPort.text?.toString()?.trim()
            val rawPin = binding.etAccessPin.text?.toString()?.trim() ?: ""

            val isHttps = rawHost?.startsWith("https://", ignoreCase = true) == true || rawHost?.contains("ngrok", ignoreCase = true) == true
            var host = if (!rawHost.isNullOrBlank() && rawHost != "10.0.2.2") rawHost else DEFAULT_HOST
            host = host.removePrefix("http://").removePrefix("https://").trimEnd('/')
            var port = rawPort?.toIntOrNull() ?: (if (isHttps) 443 else DEFAULT_PORT)

            if (host.contains(":")) {
                val parts = host.split(":")
                host = parts[0]
                val p = parts[1].toIntOrNull()
                if (p != null) port = p
            } else if (isHttps && (rawPort.isNullOrBlank() || rawPort == "5050")) {
                port = 443
            }

            val rawSignaling = binding.etSignalingUrl.text?.toString()?.trim() ?: ""

            currentHost = host
            currentPort = port
            currentAccessPin = rawPin

            saveNetworkPreferences(currentHost, currentPort, currentAccessPin, rawSignaling)

            // Caso 1: Dispositivo já pareado anteriormente com token persistente (Conexão Automática Etapa 7)
            if (isPaired && !deviceToken.isNullOrBlank()) {
                Log.i(TAG, "[AUTH] Dispositivo já autorizado com token. Iniciando conexão automática direta...")
                performConnect(autoReconnecting = false)
                return@setOnClickListener
            }

            // Caso 2: Primeiro pareamento via código fixo do proprietário
            val codeInput = binding.etPairingCode.text?.toString()?.trim()?.replace(" ", "") ?: ""
            if (codeInput.isBlank()) {
                binding.tilPairingCode.error = getString(R.string.err_empty_code)
                binding.etPairingCode.requestFocus()
                return@setOnClickListener
            }

            binding.tilPairingCode.error = null
            setConnectingState(true)

            val pairReq = PairingRequest(
                fixedCode = codeInput,
                deviceId = deviceId,
                deviceName = deviceName
            )

            Log.i(TAG, "[AUTH] Enviando solicitação de pareamento com código fixo para $currentHost:$currentPort...")
            client.pairDevice(currentHost, currentPort, pairReq) { pairRes ->
                pairRes.onSuccess { res ->
                    if (res.success && !res.deviceToken.isNullOrBlank()) {
                        Log.i(TAG, "[AUTH] Pareamento bem-sucedido! Salvando token persistente e autorizando sessão...")
                        deviceToken = res.deviceToken
                        isPaired = true
                        pairedDeviceName = res.hostDeviceName.ifEmpty { "Windows Host" }

                        secureStorage.saveDeviceToken(deviceToken)
                        secureStorage.setPaired(true)
                        secureStorage.setPairedDeviceName(pairedDeviceName)

                        prefs.edit()
                            .putString("device_token", deviceToken)
                            .putBoolean("is_paired", true)
                            .putString("paired_device_name", pairedDeviceName)
                            .putString("paired_host", currentHost)
                            .putInt("paired_port", currentPort)
                            .apply()

                        updatePairingUi()
                        Toast.makeText(this@MainActivity, "Dispositivo pareado e autorizado com sucesso!", Toast.LENGTH_SHORT).show()

                        performConnect(autoReconnecting = false)
                    } else {
                        setConnectingState(false)
                        val err = res.errorCode ?: "AUTH_FAILED"
                        val msg = if (err == "INVALID_PAIRING_CODE") {
                            "PIN incorreto. Verifique o PIN de 6 dígitos no console do Windows Agent ou execute 'RemoteDesk.Agent --pin' no PC."
                        } else {
                            res.message.ifEmpty { "PIN incorreto ou rejeitado pelo PC." }
                        }
                        if (err == "RATE_LIMITED") {
                            Toast.makeText(this@MainActivity, getString(R.string.err_rate_limited), Toast.LENGTH_LONG).show()
                        } else {
                            binding.tilPairingCode.error = msg
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                }.onFailure { ex ->
                    setConnectingState(false)
                    onConnectionFailure(ex)
                }
            }
        }
    }

    /**
     * Executa a tentativa de conexão (ou reconexão automática) com o Agente Windows usando o token persistente (Etapa 7).
     */
    private fun performConnect(autoReconnecting: Boolean = false, resumeWindow: WindowItem? = null) {
        if (!autoReconnecting) {
            setConnectingState(true)
        }

        client.getStatus(currentHost, currentPort, currentAccessPin, clientId = deviceId, deviceToken = deviceToken) { result ->
            if (!autoReconnecting) {
                setConnectingState(false)
            }

            result.onSuccess { status ->
                Log.i(TAG, "[CONNECT_NETWORK] Resposta de status recebida com sucesso de http://$currentHost:$currentPort")
                reconnectAttempt = 0
                isReconnecting = false
                cancelAutoReconnect()
                onConnectionSuccess(status)
                if (resumeWindow != null) {
                    startLiveStream(resumeWindow)
                }
            }.onFailure { ex ->
                Log.e(TAG, "[CONNECT_NETWORK] Falha na conexão: ${ex.message}", ex)
                if (ex.message?.contains("DEVICE_REVOKED") == true) {
                    handleDeviceRevoked()
                } else if (autoReconnecting) {
                    if (reconnectAttempt < 5 && !isUserInitiatedDisconnect) {
                        scheduleNextAutoReconnect(resumeWindow)
                    } else {
                        cancelAutoReconnect()
                        onConnectionFailure(ex)
                    }
                } else {
                    onConnectionFailure(ex)
                }
            }
        }
    }

    /**
     * Inicia a sequência de reconexão automática resiliente com backoff exponencial (Etapa 7).
     */
    private fun startAutoReconnect(streamingWindow: WindowItem? = null) {
        if (isUserInitiatedDisconnect || isReconnecting || reconnectAttempt >= 5) return
        isReconnecting = true
        scheduleNextAutoReconnect(streamingWindow)
    }

    private fun scheduleNextAutoReconnect(streamingWindow: WindowItem? = null) {
        val delayMs = when (reconnectAttempt) {
            0 -> 2000L
            1 -> 4000L
            2 -> 8000L
            3 -> 16000L
            else -> 30000L
        }
        reconnectAttempt++

        binding.layoutReconnectingBanner.visibility = View.VISIBLE
        binding.tvReconnectingMessage.text = "Conexão perdida. Reconectando em ${delayMs / 1000}s (tentativa $reconnectAttempt de 5)..."

        autoReconnectRunnable?.let { autoReconnectHandler.removeCallbacks(it) }
        autoReconnectRunnable = Runnable {
            if (!isUserInitiatedDisconnect) {
                binding.tvReconnectingMessage.text = "Tentando reconectar (tentativa $reconnectAttempt de 5)..."
                performConnect(autoReconnecting = true, resumeWindow = streamingWindow)
            }
        }
        autoReconnectHandler.postDelayed(autoReconnectRunnable!!, delayMs)
    }

    /**
     * Cancela e redefine qualquer tentativa ativa de reconexão automática.
     */
    private fun cancelAutoReconnect() {
        autoReconnectRunnable?.let { autoReconnectHandler.removeCallbacks(it) }
        autoReconnectRunnable = null
        isReconnecting = false
        reconnectAttempt = 0
        binding.layoutReconnectingBanner.visibility = View.GONE
    }

    /**
     * Trata o evento em que a autorização do dispositivo foi revogada pelo computador (Etapa 7).
     */
    private fun handleDeviceRevoked() {
        cancelAutoReconnect()
        secureStorage.clearAll()
        prefs.edit()
            .remove("device_token")
            .putBoolean("is_paired", false)
            .remove("paired_device_name")
            .apply()
        isPaired = false
        deviceToken = null
        pairedDeviceName = ""
        disconnect()
        updatePairingUi()
        Toast.makeText(this@MainActivity, getString(R.string.err_device_revoked), Toast.LENGTH_LONG).show()
    }

    /**
     * Configura botoes de controle da sessao conectada (Atualizar, Desconectar, Foco).
     */
    private fun setupConnectedControls() {
        binding.btnRefresh.setOnClickListener {
            loadOpenWindows()
        }

        binding.btnEmptyRefresh.setOnClickListener {
            loadOpenWindows()
        }

        binding.btnRetry.setOnClickListener {
            loadOpenWindows()
        }

        binding.btnDisconnect.setOnClickListener {
            disconnect()
        }

        binding.btnStartStream.setOnClickListener {
            val selected = windowGridAdapter.getSelectedItem()
            if (selected != null) {
                startLiveStream(selected)
            } else {
                Toast.makeText(this@MainActivity, "Selecione uma janela na grade para transmitir.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBringToFront.setOnClickListener {
            val selected = windowGridAdapter.getSelectedItem()
            if (selected != null) {
                client.activateWindow(currentHost, currentPort, selected.windowId, currentAccessPin, clientId = deviceId, deviceToken = deviceToken) { res ->
                    res.onSuccess {
                        Toast.makeText(this@MainActivity, "Janela trazida ao foco no Windows!", Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(this@MainActivity, "Não foi possível focar a janela: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Controles da tela dedicada de transmissão ao vivo (Etapa 5)
        binding.btnLiveBack.setOnClickListener {
            stopLiveStream()
        }

        binding.btnLiveStop.setOnClickListener {
            stopLiveStream()
        }

        binding.btnLiveRestore.setOnClickListener {
            currentStreamingWindow?.let { win ->
                binding.layoutLiveLoading.visibility = View.VISIBLE
                binding.tvLiveLoadingMsg.text = "Restaurando janela no Windows..."
                binding.layoutLiveMinimized.visibility = View.GONE
                client.restoreWindow(currentHost, currentPort, win.windowId, currentAccessPin, clientId = deviceId, deviceToken = deviceToken) { res ->
                    res.onSuccess {
                        Toast.makeText(this@MainActivity, "Janela restaurada no PC!", Toast.LENGTH_SHORT).show()
                    }.onFailure { ex ->
                        Toast.makeText(this@MainActivity, "Falha ao restaurar: ${ex.message}", Toast.LENGTH_SHORT).show()
                        binding.layoutLiveMinimized.visibility = View.VISIBLE
                        binding.layoutLiveLoading.visibility = View.GONE
                    }
                }
            }
        }

        binding.btnLiveReturnClosed.setOnClickListener {
            stopLiveStream()
            loadOpenWindows()
        }

        binding.btnLiveRetry.setOnClickListener {
            currentStreamingWindow?.let { win ->
                startLiveStream(win)
            }
        }

        binding.btnScaleMode.setOnClickListener {
            toggleScaleMode()
        }

        binding.btnResetScale.setOnClickListener {
            resetLiveScale()
        }
    }

    private fun setConnectingState(connecting: Boolean) {
        binding.btnConnect.isEnabled = !connecting
        if (connecting) {
            binding.btnConnect.text = getString(R.string.status_connecting)
            binding.layoutConnectingIndicator.visibility = View.VISIBLE
            binding.pbConnecting.visibility = View.VISIBLE
            binding.tvConnectingMessage.visibility = View.VISIBLE
            binding.tvConnectingMessage.text = "Conectando a http://$currentHost:$currentPort..."
            binding.tvConnectingMessage.setTextColor(ContextCompat.getColor(this, R.color.primary))

            binding.tvConnectionStatus.text = "Conectando a $currentHost:$currentPort..."
            binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.status_orange))
        } else {
            binding.btnConnect.text = getString(R.string.btn_connect)
            binding.pbConnecting.visibility = View.GONE
        }
    }

    private fun onConnectionSuccess(status: AgentStatusItem) {
        isConnected = true
        binding.layoutConnectingIndicator.visibility = View.GONE
        binding.layoutDisconnected.visibility = View.GONE
        binding.layoutConnected.visibility = View.VISIBLE

        binding.tvConnectedDevice.text = "${status.deviceName} (${status.osVersion})"
        binding.tvConnectedStatus.text = "Conectado • Pareamento: ${status.friendlyPairingCode}"

        Toast.makeText(this@MainActivity, "Conectado com sucesso ao ${status.deviceName}!", Toast.LENGTH_SHORT).show()

        loadOpenWindows()
    }

    private fun onConnectionFailure(ex: Throwable) {
        binding.tvConnectionStatus.text = "Falha na conexão"
        binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.status_red))

        val hint = when {
            ex is ConnectException -> {
                "Conexão recusada em $currentHost:$currentPort. O computador foi alcançado, mas o Agente Windows não está ouvindo na porta $currentPort."
            }
            ex is SocketTimeoutException -> {
                "Tempo limite esgotado ao conectar com $currentHost:$currentPort.\n\nO PC não respondeu ao pedido de conexão. Causa provável: O Firewall do Windows está bloqueando a porta $currentPort ou o perfil da rede Wi-Fi está como 'Público'.\n\nNo Windows PowerShell (Admin), execute:\nNew-NetFirewallRule -DisplayName 'RemoteDesk 5050' -Direction Inbound -LocalPort $currentPort -Protocol TCP -Action Allow -Profile Private"
            }
            ex.message?.contains("401") == true || ex.message?.contains("UNAUTHORIZED") == true -> {
                "PIN ou credenciais não autorizadas pelo computador.\n\nPara parear seu celular:\n1. Verifique o PIN de 6 dígitos exibido no console do Windows Agent.\n2. Ou no computador execute: RemoteDesk.Agent --pin\n3. Insira o PIN no campo 'PIN de Pareamento' e conecte."
            }
            currentHost == "10.0.2.2" -> {
                "O endereço 10.0.2.2 é exclusivo para emuladores. No celular físico na rede Wi-Fi, informe o endereço IP do PC (ex: $DEFAULT_HOST)."
            }
            else -> {
                "Não foi possível conectar ao computador ($currentHost:$currentPort): ${ex.message}."
            }
        }

        binding.layoutConnectingIndicator.visibility = View.VISIBLE
        binding.pbConnecting.visibility = View.GONE
        binding.tvConnectingMessage.visibility = View.VISIBLE
        binding.tvConnectingMessage.text = hint
        binding.tvConnectingMessage.setTextColor(ContextCompat.getColor(this, R.color.status_red))

        Toast.makeText(this@MainActivity, "Falha na conexão: consulte as instruções na tela.", Toast.LENGTH_LONG).show()
    }

    /**
     * Obtem a lista de janelas e programas abertos no Windows e renderiza na Grade Visual de Icones.
     */
    private fun loadOpenWindows() {
        binding.layoutLoading.visibility = View.VISIBLE
        binding.layoutError.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        binding.rvWindowsGrid.visibility = View.GONE
        binding.cardSelectedWindow.visibility = View.GONE

        client.getOpenWindows(currentHost, currentPort, currentAccessPin, clientId = deviceId, deviceToken = deviceToken) { result ->
            binding.layoutLoading.visibility = View.GONE
            result.onSuccess { windows ->
                if (windows.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.tvWindowsHeader.text = getString(R.string.title_open_windows, 0)
                } else {
                    binding.layoutEmpty.visibility = View.GONE
                    binding.rvWindowsGrid.visibility = View.VISIBLE
                    binding.tvWindowsHeader.text = getString(R.string.title_open_windows, windows.size)
                    windowGridAdapter.updateItems(windows)
                }
            }.onFailure { ex ->
                binding.layoutError.visibility = View.VISIBLE
                binding.tvErrorMessage.text = "Erro ao buscar janelas: ${ex.message}"
            }
        }
    }

    /**
     * Exibe o card de detalhes do programa selecionado na grade visual.
     */
    private fun displaySelectedWindowDetails(item: WindowItem) {
        binding.cardSelectedWindow.visibility = View.VISIBLE
        binding.tvSelectedTitle.text = if (item.title.isNotBlank()) item.title else item.processName
        binding.tvSelectedDetails.text = getString(
            R.string.selected_window_details,
            item.processName,
            item.processId,
            item.width,
            item.height
        )
        binding.tvSelectedHandle.text = getString(
            R.string.selected_window_handle,
            item.windowId,
            item.state
        )

        // Carrega icone real em Base64 ou icone categorizado
        var iconLoaded = false
        if (!item.iconBase64.isNullOrBlank()) {
            try {
                val bytes = Base64.decode(item.iconBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    binding.ivSelectedIcon.setImageBitmap(bitmap)
                    iconLoaded = true
                }
            } catch (_: Exception) {
                iconLoaded = false
            }
        }

        if (!iconLoaded) {
            val resId = when (item.iconType.lowercase()) {
                "browser" -> R.drawable.ic_app_browser
                "editor" -> R.drawable.ic_app_editor
                "terminal" -> R.drawable.ic_app_terminal
                "file_manager" -> R.drawable.ic_app_files
                "media" -> R.drawable.ic_app_media
                "calculator" -> R.drawable.ic_app_calc
                "settings" -> R.drawable.ic_app_settings
                else -> R.drawable.ic_app_generic
            }
            binding.ivSelectedIcon.setImageResource(resId)
        }
    }

    /**
     * Inicia a transmissão contínua de quadros da janela selecionada no Windows (Etapa 5.1).
     * O layout adapta qualquer janela (pequena ou grande) proporcionalmente à tela vertical do celular.
     */
    private fun startLiveStream(windowItem: WindowItem) {
        currentStreamingWindow = windowItem
        frameCount = 0
        lastFpsCalculationTime = System.currentTimeMillis()
        currentFps = 0
        lastFrameWidth = if (windowItem.width > 0) windowItem.width else 0
        lastFrameHeight = if (windowItem.height > 0) windowItem.height else 0
        resetLiveScale()

        // Transiciona visualização: esconde o container rolável e exibe o viewer dedicado vertical
        binding.scrollMainContainer.visibility = View.GONE
        binding.layoutLiveViewer.visibility = View.VISIBLE

        binding.layoutLiveLoading.visibility = View.VISIBLE
        binding.tvLiveLoadingMsg.text = getString(R.string.live_connecting)
        binding.layoutLiveMinimized.visibility = View.GONE
        binding.layoutLiveClosed.visibility = View.GONE
        binding.layoutLiveError.visibility = View.GONE
        binding.ivLiveFrame.setImageDrawable(null)

        val title = if (windowItem.title.isNotBlank()) windowItem.title else windowItem.processName
        binding.tvLiveTitle.text = title
        binding.tvLiveSubtitle.text = "Processo: ${windowItem.processName} • PID: ${windowItem.processId} • ID: ${windowItem.windowId}"
        // Solicita ajuste real das dimensões da janela no Windows para a referência de 382x706
        client.resizeWindow(
            host = currentHost,
            port = currentPort,
            windowId = windowItem.windowId,
            targetWidth = 382,
            targetHeight = 706,
            accessPin = currentAccessPin,
            clientId = deviceId,
            deviceToken = deviceToken
        ) { /* stream também auto-ajusta no início */ }

        liveStreamSession?.stop()
        liveStreamSession = client.startWindowStream(
            host = currentHost,
            port = currentPort,
            windowId = windowItem.windowId,
            accessPin = currentAccessPin,
            clientId = deviceId,
            deviceToken = deviceToken,
            fps = 15,
            onFrame = { bitmap, width, height ->
                binding.layoutLiveLoading.visibility = View.GONE
                binding.layoutLiveMinimized.visibility = View.GONE
                binding.layoutLiveClosed.visibility = View.GONE
                binding.layoutLiveError.visibility = View.GONE

                binding.ivLiveFrame.setImageBitmap(bitmap)

                val resW = if (width > 0) width else bitmap.width
                val resH = if (height > 0) height else bitmap.height

                // Se a janela for redimensionada no PC durante o streaming, readapta a escala dinamicamente sem travar
                if (lastFrameWidth != resW || lastFrameHeight != resH) {
                    onWindowDimensionsChanged(resW, resH)
                }

                frameCount++
                val now = System.currentTimeMillis()
                val elapsed = now - lastFpsCalculationTime
                if (elapsed >= 1000L) {
                    currentFps = ((frameCount * 1000L) / elapsed).toInt()
                    frameCount = 0
                    lastFpsCalculationTime = now
                }

                updateStatsLabel(resW, resH)
            },
            onMinimized = {
                binding.layoutLiveLoading.visibility = View.GONE
                binding.layoutLiveMinimized.visibility = View.VISIBLE
                binding.tvLiveStats.text = "Janela pausada: minimizada no PC"
            },
            onClosed = {
                liveStreamSession?.stop()
                liveStreamSession = null
                binding.layoutLiveLoading.visibility = View.GONE
                binding.layoutLiveMinimized.visibility = View.GONE
                binding.layoutLiveClosed.visibility = View.VISIBLE
                binding.tvLiveStats.text = "Janela encerrada no computador"
            },
            onError = { ex ->
                binding.layoutLiveLoading.visibility = View.GONE
                binding.layoutLiveError.visibility = View.VISIBLE
                binding.tvLiveErrorMessage.text = "Falha na transmissão da janela: ${ex.message}"
                binding.tvLiveStats.text = "Erro no stream"

                if (ex.message?.contains("DEVICE_REVOKED") == true || ex.message?.contains("401") == true) {
                    handleDeviceRevoked()
                } else if (!isUserInitiatedDisconnect && isConnected) {
                    startAutoReconnect(windowItem)
                }
            }
        )
    }

    /**
     * Encerra a transmissão ao vivo e retorna para a grade de ícones.
     */
    private fun stopLiveStream() {
        // Libera com segurança qualquer botão do mouse ou tecla retida no Windows
        client.releaseInputs(currentHost, currentPort, currentAccessPin, clientId = deviceId, deviceToken = deviceToken)
        liveStreamSession?.stop()
        liveStreamSession = null
        binding.ivLiveFrame.setImageDrawable(null)
        resetLiveScale()
        binding.layoutLiveViewer.visibility = View.GONE
        binding.scrollMainContainer.visibility = View.VISIBLE
        if (isConnected) {
            binding.layoutConnected.visibility = View.VISIBLE
        }
    }

    /**
     * Mapeia com precisão as coordenadas de toque no celular para coordenadas normalizadas (0.0 a 1.0)
     * da janela real capturada no Windows, considerando a transformação fitCenter, zoom e margens letterbox.
     * Retorna null se o toque ocorreu fora da área visual útil da janela.
     */
    fun mapTouchToNormalizedCoordinates(touchX: Float, touchY: Float): Pair<Double, Double>? {
        val viewW = binding.flLiveViewport.width.toFloat()
        val viewH = binding.flLiveViewport.height.toFloat()
        if (viewW <= 0f || viewH <= 0f) return null

        val frameW = if (lastFrameWidth > 0) lastFrameWidth.toFloat() else REF_VIEWPORT_WIDTH_DP
        val frameH = if (lastFrameHeight > 0) lastFrameHeight.toFloat() else REF_VIEWPORT_HEIGHT_DP

        val aspect = frameW / frameH
        val viewAspect = viewW / viewH

        val fitW: Float
        val fitH: Float
        if (aspect > viewAspect) {
            fitW = viewW
            fitH = viewW / aspect
        } else {
            fitH = viewH
            fitW = viewH * aspect
        }

        val currentW = fitW * currentScaleFactor
        val currentH = fitH * currentScaleFactor

        val baseLeft = (viewW - currentW) / 2f + posX
        val baseTop = (viewH - currentH) / 2f + posY
        val baseRight = baseLeft + currentW
        val baseBottom = baseTop + currentH

        // Rejeita toques que caírem fora da imagem renderizada (espaço de preenchimento/letterbox)
        if (touchX < baseLeft || touchX > baseRight || touchY < baseTop || touchY > baseBottom) {
            return null
        }

        val normX = ((touchX - baseLeft) / currentW).coerceIn(0f, 1f).toDouble()
        val normY = ((touchY - baseTop) / currentH).coerceIn(0f, 1f).toDouble()
        return Pair(normX, normY)
    }

    /**
     * Configura detecção de gestos (pinça para zoom, toque duplo, arrasto panorâmico e controle de mouse).
     */
    private fun setupViewportGestures() {
        binding.flLiveViewport.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val newW = right - left
            val newH = bottom - top
            val oldW = oldRight - oldLeft
            val oldH = oldBottom - oldTop
            if (newW > 0 && newH > 0 && (newW != oldW || newH != oldH)) {
                clampPanCoordinates()
                applyLiveScaleTransform()
                updateScaleInfo()
            }
        }

        val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                currentScaleFactor *= detector.scaleFactor
                currentScaleFactor = currentScaleFactor.coerceIn(1.0f, 3.5f)

                if (currentScaleFactor <= 1.05f) {
                    currentScaleFactor = 1.0f
                    posX = 0f
                    posY = 0f
                    setModeFit()
                } else {
                    currentScaleMode = ScaleMode.READING
                    binding.btnScaleMode.text = getString(R.string.btn_scale_fit)
                    clampPanCoordinates()
                }
                applyLiveScaleTransform()
                updateScaleInfo()
                return true
            }
        }

        val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val coords = mapTouchToNormalizedCoordinates(e.x, e.y) ?: return false
                val btn = if (currentMouseMode == RemoteMouseMode.RIGHT_CLICK) "right" else "left"
                currentStreamingWindow?.let { win ->
                    client.sendMouseInput(
                        host = currentHost,
                        port = currentPort,
                        windowId = win.windowId,
                        mouseAction = "click",
                        button = btn,
                        normalizedX = coords.first,
                        normalizedY = coords.second,
                        accessPin = currentAccessPin
                    )
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val coords = mapTouchToNormalizedCoordinates(e.x, e.y)
                if (coords != null && currentMouseMode == RemoteMouseMode.CLICK) {
                    currentStreamingWindow?.let { win ->
                        client.sendMouseInput(
                            host = currentHost,
                            port = currentPort,
                            windowId = win.windowId,
                            mouseAction = "double-click",
                            button = "left",
                            normalizedX = coords.first,
                            normalizedY = coords.second,
                            accessPin = currentAccessPin
                        )
                    }
                    return true
                }
                toggleScaleMode()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                val coords = mapTouchToNormalizedCoordinates(e.x, e.y) ?: return
                currentStreamingWindow?.let { win ->
                    client.sendMouseInput(
                        host = currentHost,
                        port = currentPort,
                        windowId = win.windowId,
                        mouseAction = "click",
                        button = "right",
                        normalizedX = coords.first,
                        normalizedY = coords.second,
                        accessPin = currentAccessPin
                    )
                }
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (currentMouseMode == RemoteMouseMode.SCROLL && e1 != null) {
                    val delta = if (distanceY > 0) 1 else -1
                    val coords = mapTouchToNormalizedCoordinates(e2.x, e2.y)
                    currentStreamingWindow?.let { win ->
                        client.sendScroll(
                            host = currentHost,
                            port = currentPort,
                            windowId = win.windowId,
                            deltaY = delta,
                            normalizedX = coords?.first,
                            normalizedY = coords?.second,
                            accessPin = currentAccessPin
                        )
                    }
                    return true
                }
                return false
            }
        }

        val scaleDetector = ScaleGestureDetector(this, scaleListener)
        val gestureDetector = GestureDetector(this, gestureListener)

        binding.flLiveViewport.setOnTouchListener { _, event ->
            // Se houver 2 ou mais dedos, delega ao gesto de pinça e pan
            if (event.pointerCount >= 2) {
                scaleDetector.onTouchEvent(event)
                when (event.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY
                        posX += dx
                        posY += dy
                        clampPanCoordinates()
                        applyLiveScaleTransform()
                        lastTouchX = event.x
                        lastTouchY = event.y
                    }
                }
                return@setOnTouchListener true
            }

            // Gesto com 1 dedo: controle remoto ou pan no modo leitura
            gestureDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    activePointerId = event.getPointerId(0)
                    lastTouchX = event.x
                    lastTouchY = event.y

                    if (currentMouseMode == RemoteMouseMode.DRAG) {
                        val coords = mapTouchToNormalizedCoordinates(event.x, event.y)
                        if (coords != null) {
                            currentStreamingWindow?.let { win ->
                                client.sendMouseInput(
                                    host = currentHost,
                                    port = currentPort,
                                    windowId = win.windowId,
                                    mouseAction = "down",
                                    button = "left",
                                    normalizedX = coords.first,
                                    normalizedY = coords.second,
                                    accessPin = currentAccessPin
                                )
                            }
                        }
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    val pointerIndex = event.findPointerIndex(activePointerId)
                    if (pointerIndex != -1) {
                        val x = event.getX(pointerIndex)
                        val y = event.getY(pointerIndex)

                        if (currentMouseMode == RemoteMouseMode.DRAG) {
                            val now = System.currentTimeMillis()
                            if (now - lastDragMoveTime >= 20L) {
                                val coords = mapTouchToNormalizedCoordinates(x, y)
                                if (coords != null) {
                                    currentStreamingWindow?.let { win ->
                                        client.sendMouseInput(
                                            host = currentHost,
                                            port = currentPort,
                                            windowId = win.windowId,
                                            mouseAction = "move",
                                            button = "left",
                                            normalizedX = coords.first,
                                            normalizedY = coords.second,
                                            accessPin = currentAccessPin
                                        )
                                    }
                                }
                                lastDragMoveTime = now
                            }
                        } else if (currentMouseMode == RemoteMouseMode.SCROLL) {
                            val dy = y - lastTouchY
                            if (Math.abs(dy) >= 15f) {
                                val deltaY = if (dy > 0) 1 else -1
                                val coords = mapTouchToNormalizedCoordinates(x, y)
                                currentStreamingWindow?.let { win ->
                                    client.sendScroll(
                                        host = currentHost,
                                        port = currentPort,
                                        windowId = win.windowId,
                                        deltaY = deltaY,
                                        normalizedX = coords?.first,
                                        normalizedY = coords?.second,
                                        accessPin = currentAccessPin
                                    )
                                }
                                lastTouchY = y
                            }
                        } else if (currentScaleFactor > 1.0f) {
                            val dx = x - lastTouchX
                            val dy = y - lastTouchY
                            posX += dx
                            posY += dy
                            clampPanCoordinates()
                            applyLiveScaleTransform()
                            lastTouchX = x
                            lastTouchY = y
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (currentMouseMode == RemoteMouseMode.DRAG) {
                        val coords = mapTouchToNormalizedCoordinates(event.x, event.y)
                        currentStreamingWindow?.let { win ->
                            client.sendMouseInput(
                                host = currentHost,
                                port = currentPort,
                                windowId = win.windowId,
                                mouseAction = "up",
                                button = "left",
                                normalizedX = coords?.first,
                                normalizedY = coords?.second,
                                accessPin = currentAccessPin
                            )
                        }
                    }
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                }
                MotionEvent.ACTION_CANCEL -> {
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                    client.releaseInputs(currentHost, currentPort, currentAccessPin, clientId = deviceId, deviceToken = deviceToken)
                }
            }
            true
        }
    }

    /**
     * Configura controles da barra de controle remoto (mouse, teclado virtual e teclas especiais).
     */
    private fun setupRemoteControlInputs() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // Teclado Virtual
        binding.btnToggleKeyboard.setOnClickListener {
            binding.etHiddenKeyboardInput.requestFocus()
            imm.showSoftInput(binding.etHiddenKeyboardInput, InputMethodManager.SHOW_IMPLICIT)
        }

        binding.etHiddenKeyboardInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val str = s?.toString() ?: ""
                if (str.isNotEmpty()) {
                    currentStreamingWindow?.let { win ->
                        client.sendTextInput(
                            host = currentHost,
                            port = currentPort,
                            windowId = win.windowId,
                            text = str,
                            accessPin = currentAccessPin
                        )
                    }
                    binding.etHiddenKeyboardInput.setText("")
                }
            }
        })

        binding.etHiddenKeyboardInput.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                val keyName = when (keyCode) {
                    KeyEvent.KEYCODE_ENTER -> "Enter"
                    KeyEvent.KEYCODE_DEL -> "Backspace"
                    KeyEvent.KEYCODE_TAB -> "Tab"
                    KeyEvent.KEYCODE_FORWARD_DEL -> "Delete"
                    KeyEvent.KEYCODE_ESCAPE -> "Escape"
                    KeyEvent.KEYCODE_DPAD_UP -> "Up"
                    KeyEvent.KEYCODE_DPAD_DOWN -> "Down"
                    KeyEvent.KEYCODE_DPAD_LEFT -> "Left"
                    KeyEvent.KEYCODE_DPAD_RIGHT -> "Right"
                    else -> null
                }
                if (keyName != null) {
                    currentStreamingWindow?.let { win ->
                        client.sendKeyEvent(
                            host = currentHost,
                            port = currentPort,
                            windowId = win.windowId,
                            key = keyName,
                            modifiers = getActiveModifiers(),
                            accessPin = currentAccessPin
                        )
                    }
                    return@setOnKeyListener true
                }
            }
            false
        }

        // Barra de Teclas Especiais
        binding.btnToggleSpecialKeys.setOnClickListener {
            val isVisible = binding.layoutSpecialKeysBar.visibility == View.VISIBLE
            binding.layoutSpecialKeysBar.visibility = if (isVisible) View.GONE else View.VISIBLE
            val primaryColor = ContextCompat.getColor(this, R.color.primary)
            binding.btnToggleSpecialKeys.strokeColor = ColorStateList.valueOf(if (isVisible) Color.parseColor("#444444") else primaryColor)
            binding.btnToggleSpecialKeys.setTextColor(if (isVisible) Color.parseColor("#CCCCCC") else primaryColor)
        }

        binding.btnKeyEsc.setOnClickListener { sendSpecialKey("Escape") }
        binding.btnKeyTab.setOnClickListener { sendSpecialKey("Tab") }
        binding.btnKeyEnter.setOnClickListener { sendSpecialKey("Enter") }
        binding.btnKeyBackspace.setOnClickListener { sendSpecialKey("Backspace") }
        binding.btnKeyDelete.setOnClickListener { sendSpecialKey("Delete") }
        binding.btnKeyUp.setOnClickListener { sendSpecialKey("Up") }
        binding.btnKeyDown.setOnClickListener { sendSpecialKey("Down") }
        binding.btnKeyLeft.setOnClickListener { sendSpecialKey("Left") }
        binding.btnKeyRight.setOnClickListener { sendSpecialKey("Right") }
        binding.btnKeyHome.setOnClickListener { sendSpecialKey("Home") }
        binding.btnKeyEnd.setOnClickListener { sendSpecialKey("End") }

        binding.btnKeyCtrl.setOnClickListener {
            isCtrlActive = !isCtrlActive
            updateModifierButton(binding.btnKeyCtrl, isCtrlActive)
        }
        binding.btnKeyAlt.setOnClickListener {
            isAltActive = !isAltActive
            updateModifierButton(binding.btnKeyAlt, isAltActive)
        }
        binding.btnKeyShift.setOnClickListener {
            isShiftActive = !isShiftActive
            updateModifierButton(binding.btnKeyShift, isShiftActive)
        }

        // Modos de Mouse
        binding.btnMouseModeLeft.setOnClickListener { updateMouseModeUI(RemoteMouseMode.CLICK) }
        binding.btnMouseModeRight.setOnClickListener { updateMouseModeUI(RemoteMouseMode.RIGHT_CLICK) }
        binding.btnMouseModeDrag.setOnClickListener { updateMouseModeUI(RemoteMouseMode.DRAG) }
        binding.btnMouseModeScroll.setOnClickListener { updateMouseModeUI(RemoteMouseMode.SCROLL) }
    }

    private fun updateModifierButton(btn: com.google.android.material.button.MaterialButton, active: Boolean) {
        val primaryColor = ContextCompat.getColor(this, R.color.primary)
        btn.strokeColor = ColorStateList.valueOf(if (active) primaryColor else Color.parseColor("#444444"))
        btn.setTextColor(if (active) primaryColor else Color.WHITE)
    }

    private fun updateMouseModeUI(mode: RemoteMouseMode) {
        currentMouseMode = mode
        val primaryColor = ContextCompat.getColor(this, R.color.primary)
        val inactiveColor = Color.parseColor("#CCCCCC")
        val defaultBorder = ColorStateList.valueOf(Color.parseColor("#444444"))
        val activeBorder = ColorStateList.valueOf(primaryColor)

        binding.btnMouseModeLeft.strokeColor = if (mode == RemoteMouseMode.CLICK) activeBorder else defaultBorder
        binding.btnMouseModeLeft.setTextColor(if (mode == RemoteMouseMode.CLICK) primaryColor else inactiveColor)

        binding.btnMouseModeRight.strokeColor = if (mode == RemoteMouseMode.RIGHT_CLICK) activeBorder else defaultBorder
        binding.btnMouseModeRight.setTextColor(if (mode == RemoteMouseMode.RIGHT_CLICK) primaryColor else inactiveColor)

        binding.btnMouseModeDrag.strokeColor = if (mode == RemoteMouseMode.DRAG) activeBorder else defaultBorder
        binding.btnMouseModeDrag.setTextColor(if (mode == RemoteMouseMode.DRAG) primaryColor else inactiveColor)

        binding.btnMouseModeScroll.strokeColor = if (mode == RemoteMouseMode.SCROLL) activeBorder else defaultBorder
        binding.btnMouseModeScroll.setTextColor(if (mode == RemoteMouseMode.SCROLL) primaryColor else inactiveColor)
    }

    private fun getActiveModifiers(): List<String>? {
        val list = mutableListOf<String>()
        if (isCtrlActive) list.add("Ctrl")
        if (isAltActive) list.add("Alt")
        if (isShiftActive) list.add("Shift")
        return if (list.isEmpty()) null else list
    }

    private fun sendSpecialKey(keyName: String) {
        currentStreamingWindow?.let { win ->
            client.sendKeyEvent(
                host = currentHost,
                port = currentPort,
                windowId = win.windowId,
                key = keyName,
                modifiers = getActiveModifiers(),
                accessPin = currentAccessPin
            )
        }
    }

    /**
     * Readapta a escala dinamicamente quando a janela muda de tamanho ou no primeiro quadro recebido.
     */
    private fun onWindowDimensionsChanged(newW: Int, newH: Int) {
        lastFrameWidth = newW
        lastFrameHeight = newH

        if (currentScaleMode == ScaleMode.READING) {
            currentScaleFactor = calculateOptimalReadingScale(newW, newH)
        } else {
            currentScaleFactor = 1.0f
        }
        clampPanCoordinates()
        applyLiveScaleTransform()
        updateScaleInfo()
    }

    /**
     * Calcula dinamicamente o fator de zoom ideal para o Modo Leitura conforme a resolução da janela,
     * padronizado na área de referência de 382 x 706 e calibrado com base no padrão de leitura do Bloco de Notas.
     *
     * Para janelas pequenas (ex: Bloco de Notas <= 640px de largura):
     *   A escala de ajuste 100% Fit já aproveita a área útil de 382x706 e entrega leitura confortável nativa.
     * Para janelas grandes (ex: navegadores, 1080p, 1440p, 4K):
     *   Calcula o fator de aumento que equaliza o tamanho físico dos textos ao padrão confortável do Bloco de Notas
     *   na área de referência de 382 x 706: (frameW / 640).coerceIn(1.5f, 2.75f).
     */
    private fun calculateOptimalReadingScale(frameW: Int, frameH: Int): Float {
        val density = resources.displayMetrics.density
        val refWidthPx = REF_VIEWPORT_WIDTH_DP * density
        val refHeightPx = REF_VIEWPORT_HEIGHT_DP * density

        val vpWidth = binding.flLiveViewport.width.toFloat().takeIf { it > 0f } ?: refWidthPx
        val vpHeight = binding.flLiveViewport.height.toFloat().takeIf { it > 0f } ?: refHeightPx

        if (frameW <= 0 || frameH <= 0) return 1.75f

        val windowAspect = frameW.toFloat() / frameH.toFloat().coerceAtLeast(1f)
        val vpAspect = vpWidth / vpHeight.coerceAtLeast(1f)

        val renderedW = if (windowAspect > vpAspect) vpWidth else vpHeight * windowAspect
        val pixelRatio = (renderedW / frameW.toFloat().coerceAtLeast(1f)).coerceAtLeast(0.05f)

        // Calibração proporcional ao padrão de leitura validado do Bloco de Notas
        val targetZoom = (frameW.toFloat() / NOTEPAD_REF_WIDTH).coerceAtLeast(1.0f)
        return if (pixelRatio < 0.85f) {
            targetZoom.coerceIn(1.5f, 2.75f)
        } else {
            1.5f
        }
    }

    /**
     * Limita as coordenadas de arrasto para que o usuário não role para além dos limites do quadro visível
     * dentro da área de referência de visualização (382 x 706 dp).
     */
    private fun clampPanCoordinates() {
        val density = resources.displayMetrics.density
        val refWidthPx = REF_VIEWPORT_WIDTH_DP * density
        val refHeightPx = REF_VIEWPORT_HEIGHT_DP * density

        val vpWidth = binding.flLiveViewport.width.toFloat().takeIf { it > 0f } ?: refWidthPx
        val vpHeight = binding.flLiveViewport.height.toFloat().takeIf { it > 0f } ?: refHeightPx

        if (lastFrameWidth <= 0 || lastFrameHeight <= 0 || currentScaleFactor <= 1.02f) {
            posX = 0f
            posY = 0f
            return
        }

        val windowAspect = lastFrameWidth.toFloat() / lastFrameHeight.toFloat().coerceAtLeast(1f)
        val vpAspect = vpWidth / vpHeight.coerceAtLeast(1f)

        val renderedW = if (windowAspect > vpAspect) vpWidth else vpHeight * windowAspect
        val renderedH = if (windowAspect > vpAspect) vpWidth / windowAspect else vpHeight

        val scaledW = renderedW * currentScaleFactor
        val scaledH = renderedH * currentScaleFactor

        val maxPanX = if (scaledW > vpWidth) (scaledW - vpWidth) / 2f else 0f
        val maxPanY = if (scaledH > vpHeight) (scaledH - vpHeight) / 2f else 0f

        posX = posX.coerceIn(-maxPanX, maxPanX)
        posY = posY.coerceIn(-maxPanY, maxPanY)
    }

    /**
     * Aplica os fatores atuais de zoom e translação na ImageView de transmissão.
     */
    private fun applyLiveScaleTransform() {
        binding.ivLiveFrame.scaleX = currentScaleFactor
        binding.ivLiveFrame.scaleY = currentScaleFactor
        binding.ivLiveFrame.translationX = posX
        binding.ivLiveFrame.translationY = posY
    }

    /**
     * Redefine o zoom para a escala padrão 1:1 Fit (visualização completa da janela sem cortes).
     */
    private fun resetLiveScale() {
        currentScaleMode = ScaleMode.FIT
        currentScaleFactor = 1.0f
        posX = 0f
        posY = 0f
        clampPanCoordinates()
        applyLiveScaleTransform()
        binding.btnScaleMode.text = getString(R.string.btn_scale_reading)
        updateScaleInfo()
    }

    /**
     * Alterna entre o Modo Ajuste Completo (Fit 382x706) e o Modo Leitura calibrado pelo Bloco de Notas.
     */
    private fun toggleScaleMode() {
        if (currentScaleMode == ScaleMode.FIT) {
            currentScaleMode = ScaleMode.READING
            currentScaleFactor = calculateOptimalReadingScale(lastFrameWidth, lastFrameHeight)
            posX = 0f
            posY = 0f
            clampPanCoordinates()
            applyLiveScaleTransform()
            binding.btnScaleMode.text = getString(R.string.btn_scale_fit)
        } else {
            resetLiveScale()
        }
        updateScaleInfo()
    }

    private fun setModeFit() {
        currentScaleMode = ScaleMode.FIT
        currentScaleFactor = 1.0f
        posX = 0f
        posY = 0f
        clampPanCoordinates()
        applyLiveScaleTransform()
        binding.btnScaleMode.text = getString(R.string.btn_scale_reading)
        updateScaleInfo()
    }

    private fun updateScaleInfo() {
        val scalePercent = (currentScaleFactor * 100).toInt()
        val windowCategory = when {
            lastFrameWidth <= 0 -> ""
            lastFrameWidth <= 800 && lastFrameHeight <= 600 -> "Janela Pequena (Padrão Bloco de Notas)"
            lastFrameWidth >= 1280 || lastFrameHeight >= 720 -> "Janela Grande (Adaptada 382x706)"
            else -> "Janela Média (Adaptada 382x706)"
        }
        val modeName = if (currentScaleMode == ScaleMode.FIT) {
            "Ajuste Completo (Fit 100%)"
        } else {
            "Modo Leitura Bloco de Notas ($scalePercent%)"
        }
        val categoryPrefix = if (windowCategory.isNotBlank()) "$windowCategory • " else ""
        binding.tvLiveScaleInfo.text = "$categoryPrefix$modeName • Toque duplo para alternar • Pinça para zoom"
    }

    private fun updateStatsLabel(resW: Int, resH: Int) {
        val scaleInfo = if (currentScaleFactor > 1.05f) {
            " • Zoom ${(currentScaleFactor * 100).toInt()}%"
        } else {
            " • 100% Fit"
        }
        binding.tvLiveStats.text = "Janela: ${resW}x${resH} • Área Ref: 382x706 • $currentFps FPS$scaleInfo"
    }

    override fun onBackPressed() {
        if (binding.layoutLiveViewer.visibility == View.VISIBLE) {
            stopLiveStream()
        } else {
            super.onBackPressed()
        }
    }

    override fun onStop() {
        super.onStop()
        liveStreamSession?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        client.releaseInputs(currentHost, currentPort, currentAccessPin, clientId = deviceId, deviceToken = deviceToken)
        liveStreamSession?.stop()
    }

    /**
     * Encerra a visualizacao conectada e retorna a tela inicial de conexao.
     */
    private fun disconnect() {
        isUserInitiatedDisconnect = true
        cancelAutoReconnect()
        stopLiveStream()
        isConnected = false
        binding.scrollMainContainer.visibility = View.VISIBLE
        binding.layoutConnectingIndicator.visibility = View.GONE
        binding.layoutConnected.visibility = View.GONE
        binding.layoutLiveViewer.visibility = View.GONE
        binding.layoutDisconnected.visibility = View.VISIBLE
        binding.tvConnectionStatus.text = getString(R.string.status_ready)
        binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.status_green))
        windowGridAdapter.updateItems(emptyList())
        Toast.makeText(this@MainActivity, "Desconectado do computador.", Toast.LENGTH_SHORT).show()
    }
}

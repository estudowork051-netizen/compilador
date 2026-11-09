package com.lifeorganizer.app.core.diagnostic

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Centralized diagnostic and crash recovery manager for LifeFlow.
 * Persists timestamped lifecycle events and unhandled exceptions to local storage,
 * detects consecutive crashes to activate Recovery Mode, and provides copy/share
 * capabilities directly within the app without requiring ADB or external tools.
 */
object LifeFlowDiagnosticManager {

    private const val TAG = "LifeFlowDiagnostic"
    private const val LOG_FILE_NAME = "lifeflow_diagnostics.log"
    private const val PREFS_NAME = "lifeflow_diagnostic_recovery"
    private const val KEY_CRASH_COUNT = "consecutive_crash_count"
    private const val KEY_STARTUP_SUCCESS = "last_startup_successful"
    private const val MAX_LOG_SIZE_BYTES = 128 * 1024 // 128 KB
    private const val ROTATED_LOG_SIZE_BYTES = 64 * 1024 // 64 KB

    private val writeExecutor = Executors.newSingleThreadExecutor()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Volatile
    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null

    @Volatile
    var isRecoveryModeActive: Boolean = false
        private set

    /**
     * Initializes the diagnostic manager during Application onCreate.
     */
    fun initialize(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Evaluate startup resilience and crash count
        val sharedPrefs = prefs
        if (sharedPrefs != null) {
            val wasLastStartupSuccessful = sharedPrefs.getBoolean(KEY_STARTUP_SUCCESS, true)
            var crashCount = sharedPrefs.getInt(KEY_CRASH_COUNT, 0)

            if (!wasLastStartupSuccessful) {
                crashCount += 1
                sharedPrefs.edit().putInt(KEY_CRASH_COUNT, crashCount).apply()
            }

            // If 2 or more consecutive startup attempts failed before stabilization, activate Recovery Mode
            if (crashCount >= 2) {
                isRecoveryModeActive = true
            }

            // Tentative startup: mark as not yet stabilized until steady state is reached
            sharedPrefs.edit().putBoolean(KEY_STARTUP_SUCCESS, false).apply()
        }

        // Setup uncaught exception handler
        setupUncaughtExceptionHandler()

        logEvent("APP_START", "App starting up. RecoveryMode=$isRecoveryModeActive, SDK=${Build.VERSION.SDK_INT}, Model=${Build.MANUFACTURER} ${Build.MODEL}")
    }

    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTrace = sw.toString()

                val fatalMessage = "FATAL_EXCEPTION [thread=${thread.name}] ${throwable.javaClass.name}: ${throwable.message}\n$stackTrace"
                Log.e(TAG, fatalMessage)

                // Synchronously flush fatal exception to disk before process death
                appendLogSynchronous("FATAL_EXCEPTION", fatalMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write fatal exception to log file: ${e.message}")
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    /**
     * Appends an event log entry with timestamp.
     */
    fun logEvent(event: String, details: String = "") {
        val timestamp = synchronized(dateFormat) { dateFormat.format(Date()) }
        val entry = if (details.isNotBlank()) "[$timestamp] $event: $details" else "[$timestamp] $event"
        Log.d(TAG, entry)

        writeExecutor.execute {
            writeLogEntry(entry)
        }
    }

    /**
     * Logs a non-fatal caught exception or failure.
     */
    fun logError(event: String, throwable: Throwable? = null, details: String = "") {
        val timestamp = synchronized(dateFormat) { dateFormat.format(Date()) }
        val errorMsg = buildString {
            append("[$timestamp] $event")
            if (details.isNotBlank()) append(": $details")
            if (throwable != null) {
                append(" | ${throwable.javaClass.simpleName}: ${throwable.message}")
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                append("\n").append(sw.toString().take(1500))
            }
        }
        Log.e(TAG, errorMsg)

        writeExecutor.execute {
            writeLogEntry(errorMsg)
        }
    }

    /**
     * Marks the application startup as healthy and stabilized.
     * Resets the crash counter.
     */
    fun markStartupStabilized() {
        logEvent("STARTUP_STABILIZED", "App reached steady state successfully.")
        prefs?.edit()
            ?.putBoolean(KEY_STARTUP_SUCCESS, true)
            ?.putInt(KEY_CRASH_COUNT, 0)
            ?.apply()
        isRecoveryModeActive = false
    }

    /**
     * Resets Recovery Mode manually to allow normal launch attempt.
     */
    fun resetRecoveryMode() {
        logEvent("RECOVERY_MODE_RESET", "User requested normal launch retry.")
        prefs?.edit()
            ?.putBoolean(KEY_STARTUP_SUCCESS, true)
            ?.putInt(KEY_CRASH_COUNT, 0)
            ?.apply()
        isRecoveryModeActive = false
    }

    /**
     * Forces activation of Recovery Mode for testing or user request.
     */
    fun forceRecoveryMode() {
        isRecoveryModeActive = true
    }

    private fun getLogFile(): File? {
        val ctx = appContext ?: return null
        return File(ctx.filesDir, LOG_FILE_NAME)
    }

    private fun writeLogEntry(entry: String) {
        val file = getLogFile() ?: return
        try {
            // Check file rotation
            if (file.exists() && file.length() > MAX_LOG_SIZE_BYTES) {
                rotateLogFile(file)
            }
            FileOutputStream(file, true).bufferedWriter().use { writer ->
                writer.write(entry)
                writer.newLine()
                writer.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "writeLogEntry error: ${e.message}")
        }
    }

    private fun appendLogSynchronous(event: String, message: String) {
        val file = getLogFile() ?: return
        val timestamp = synchronized(dateFormat) { dateFormat.format(Date()) }
        val entry = "[$timestamp] $event\n$message"
        try {
            FileOutputStream(file, true).bufferedWriter().use { writer ->
                writer.write(entry)
                writer.newLine()
                writer.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "appendLogSynchronous error: ${e.message}")
        }
    }

    private fun rotateLogFile(file: File) {
        try {
            val content = file.readText()
            val keepContent = if (content.length > ROTATED_LOG_SIZE_BYTES) {
                content.substring(content.length - ROTATED_LOG_SIZE_BYTES)
            } else {
                content
            }
            file.writeText("[LOG_ROTATED]\n$keepContent")
        } catch (e: Exception) {
            Log.e(TAG, "rotateLogFile error: ${e.message}")
        }
    }

    /**
     * Reads all recorded diagnostic logs as plain text.
     */
    fun readDiagnosticLogs(): String {
        val file = getLogFile() ?: return "Nenhum arquivo de log disponível."
        return try {
            if (!file.exists()) "Nenhum log gravado ainda."
            else file.readText()
        } catch (e: Exception) {
            "Erro ao ler logs de diagnóstico: ${e.message}"
        }
    }

    /**
     * Generates a complete structured diagnostic report including device info and logs.
     */
    fun generateDiagnosticReport(): String {
        val ctx = appContext
        val versionName = try {
            ctx?.packageManager?.getPackageInfo(ctx.packageName, 0)?.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
        val crashCount = prefs?.getInt(KEY_CRASH_COUNT, 0) ?: 0
        val isHealthy = prefs?.getBoolean(KEY_STARTUP_SUCCESS, false) ?: false

        val header = buildString {
            appendLine("=== LIFEFLOW DIAGNOSTIC REPORT ===")
            appendLine("Timestamp: ${synchronized(dateFormat) { dateFormat.format(Date()) }}")
            appendLine("App Version: $versionName")
            appendLine("Android OS: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Recovery Mode Active: $isRecoveryModeActive")
            appendLine("Consecutive Crash Count: $crashCount")
            appendLine("Last Startup Healthy: $isHealthy")
            appendLine("----------------------------------")
            appendLine("HISTÓRICO DE EVENTOS RECENTES:")
        }

        val logs = readDiagnosticLogs()
        return "$header\n$logs\n=== END OF REPORT ==="
    }

    /**
     * Copies the full diagnostic report to the system clipboard.
     */
    fun copyReportToClipboard(context: Context): Boolean {
        return try {
            val report = generateDiagnosticReport()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("LifeFlow Relatório de Diagnóstico", report)
            clipboard.setPrimaryClip(clip)
            logEvent("DIAGNOSTIC_REPORT_COPIED", "Report successfully copied to clipboard.")
            true
        } catch (e: Exception) {
            logError("CLIPBOARD_COPY_FAILED", e)
            false
        }
    }

    /**
     * Opens the native Android share sheet with the diagnostic report.
     */
    fun shareReport(context: Context) {
        try {
            val report = generateDiagnosticReport()
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "LifeFlow - Relatório de Diagnóstico")
                putExtra(Intent.EXTRA_TEXT, report)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(sendIntent, "Compartilhar Diagnóstico LifeFlow").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
            logEvent("DIAGNOSTIC_REPORT_SHARED", "Report share intent launched.")
        } catch (e: Exception) {
            logError("SHARE_INTENT_FAILED", e)
        }
    }

    /**
     * Clears all stored logs (except the clear event itself).
     */
    fun clearLogs() {
        val file = getLogFile()
        try {
            file?.delete()
            logEvent("LOGS_CLEARED", "Diagnostic log history cleared.")
        } catch (e: Exception) {
            Log.e(TAG, "clearLogs error: ${e.message}")
        }
    }
}

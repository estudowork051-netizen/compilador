package com.remotedesk.app.models

import org.json.JSONObject

/**
 * Representa as informações de status do Agente Windows conectado.
 */
data class AgentStatusItem(
    val deviceId: String,
    val deviceName: String,
    val osVersion: String,
    val isReady: Boolean,
    val friendlyPairingCode: String,
    val openWindowsCount: Int,
    val activeWindowId: String?,
    val activeWindowTitle: String?,
    val timestamp: String
) {
    companion object {
        fun fromJson(json: JSONObject): AgentStatusItem {
            val target = if (json.has("data") && !json.isNull("data") && json.optJSONObject("data") != null) {
                json.getJSONObject("data")
            } else {
                json
            }
            return AgentStatusItem(
                deviceId = target.optString("deviceId", ""),
                deviceName = target.optString("deviceName", "Windows Host"),
                osVersion = target.optString("osVersion", "Windows"),
                isReady = target.optBoolean("isReady", true),
                friendlyPairingCode = target.optString("friendlyPairingCode", ""),
                openWindowsCount = target.optInt("openWindowsCount", 0),
                activeWindowId = if (target.isNull("activeWindowId")) null else target.optString("activeWindowId"),
                activeWindowTitle = if (target.isNull("activeWindowTitle")) null else target.optString("activeWindowTitle"),
                timestamp = target.optString("timestamp", "")
            )
        }
    }
}

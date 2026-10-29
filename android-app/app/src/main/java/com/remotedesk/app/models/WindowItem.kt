package com.remotedesk.app.models

import org.json.JSONObject

/**
 * Representa uma janela/aplicativo aberto detectado na sessão ativa do computador Windows.
 * Inclui metadados do processo, handle, dimensões e categoria visual de ícone.
 */
data class WindowItem(
    val windowId: String,
    val title: String,
    val processName: String,
    val processId: Int,
    val className: String,
    val state: String,
    val isVisible: Boolean,
    val isMinimized: Boolean,
    val isActive: Boolean,
    val iconType: String,
    val iconBase64: String?,
    val width: Int,
    val height: Int,
    val x: Int,
    val y: Int,
    var isSelected: Boolean = false
) {
    companion object {
        fun fromJson(json: JSONObject): WindowItem {
            return WindowItem(
                windowId = json.optString("windowId", ""),
                title = json.optString("title", "[Aplicativo sem título]"),
                processName = json.optString("processName", "desconhecido"),
                processId = json.optInt("processId", 0),
                className = json.optString("className", ""),
                state = json.optString("state", "Normal"),
                isVisible = json.optBoolean("isVisible", true),
                isMinimized = json.optBoolean("isMinimized", false),
                isActive = json.optBoolean("isActive", false),
                iconType = json.optString("iconType", "generic"),
                iconBase64 = if (json.isNull("iconBase64")) null else json.optString("iconBase64"),
                width = json.optInt("width", 0),
                height = json.optInt("height", 0),
                x = json.optInt("x", 0),
                y = json.optInt("y", 0),
                isSelected = false
            )
        }
    }
}

package com.remotedesk.app.models

/**
 * Modelos de dados para o subsistema de autenticação pessoal e pareamento persistente (Etapa 7).
 */

data class PairingRequest(
    val fixedCode: String,
    val deviceId: String,
    val deviceName: String,
    val clientVersion: String = "1.0.0"
)

data class PairingResponse(
    val success: Boolean,
    val deviceToken: String? = null,
    val deviceId: String = "",
    val hostDeviceName: String = "",
    val message: String = "",
    val errorCode: String? = null
)

data class DeviceAuthVerification(
    val isAuthorized: Boolean,
    val isRevoked: Boolean = false,
    val errorCode: String? = null,
    val errorMessage: String? = null
)

data class AuthorizedDeviceItem(
    val deviceId: String,
    val deviceName: String,
    val createdAt: String,
    val lastSeenAt: String,
    val isRevoked: Boolean
)

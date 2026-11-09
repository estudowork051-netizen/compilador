package com.lifeorganizer.app.domain.model

enum class ConfidenceLevel(val displayName: String) {
    INSUFFICIENT_DATA("Histórico insuficiente"),
    LOW("Baixa confiança"),
    MODERATE("Confiança moderada"),
    HIGH("Confiança elevada")
}

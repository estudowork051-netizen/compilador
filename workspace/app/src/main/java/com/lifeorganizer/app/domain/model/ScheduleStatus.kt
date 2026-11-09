package com.lifeorganizer.app.domain.model

/**
 * Execution states for scheduled activities.
 * Conforms to Step 3 specification:
 * Pendente, Em andamento, Concluída, Adiada, Não realizada, Cancelada.
 */
enum class ScheduleStatus(val displayName: String, val colorHex: String) {
    PENDING("Pendente", "#9CA3AF"),
    IN_PROGRESS("Em andamento", "#3B82F6"),
    COMPLETED("Concluída", "#10B981"),
    POSTPONED("Adiada", "#F59E0B"),
    NOT_DONE("Não realizada", "#EF4444"),
    CANCELLED("Cancelada", "#6B7280");

    val isFinished: Boolean
        get() = this == COMPLETED || this == NOT_DONE || this == CANCELLED

    companion object {
        fun fromString(value: String?): ScheduleStatus {
            if (value.isNullOrBlank()) return PENDING
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: PENDING
        }
    }
}

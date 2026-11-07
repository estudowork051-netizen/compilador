package com.lifeorganizer.app.domain.model

enum class ActivityPriority(val displayName: String) {
    LOW("Baixa"),
    MEDIUM("Média"),
    HIGH("Alta");

    companion object {
        fun fromString(value: String?): ActivityPriority {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}

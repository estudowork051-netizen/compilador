package com.lifeorganizer.app.domain.model

enum class ActivityType(val displayName: String) {
    FLEXIBLE("Atividade flexível"),
    FIXED("Compromisso fixo"),
    HABIT("Meta semanal");

    companion object {
        fun fromString(value: String?): ActivityType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: FLEXIBLE
        }
    }
}

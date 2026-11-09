package com.lifeorganizer.app.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromString(value: String?): ThemeMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
        }
    }
}

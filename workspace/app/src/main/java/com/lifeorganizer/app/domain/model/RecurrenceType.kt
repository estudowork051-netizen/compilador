package com.lifeorganizer.app.domain.model

enum class RecurrenceType(val displayName: String) {
    ONCE("Uma única vez"),
    DAILY("Todos os dias"),
    WEEKDAYS("Dias úteis (Seg a Sex)"),
    WEEKENDS("Finais de semana (Sáb e Dom)"),
    SPECIFIC_DAYS("Dias específicos");

    companion object {
        fun fromString(value: String?): RecurrenceType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DAILY
        }
    }
}

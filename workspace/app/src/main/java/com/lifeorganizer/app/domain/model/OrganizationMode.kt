package com.lifeorganizer.app.domain.model

enum class OrganizationMode {
    MANUAL,
    AUTOMATIC;

    companion object {
        fun fromString(value: String?): OrganizationMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MANUAL
        }
    }
}

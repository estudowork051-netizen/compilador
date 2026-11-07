package com.lifeorganizer.app.domain.model

enum class AppCategoryClassification(val displayName: String, val colorHex: String) {
    STUDY_TOOL("Ferramenta de estudo", "#3B82F6"),
    WORK("Trabalho", "#8B5CF6"),
    LEISURE("Lazer", "#EC4899"),
    DISTRACTION("Possível distração", "#F97316"),
    UNCLASSIFIED("Não classificado", "#6B7280");

    fun isDistraction(): Boolean = this == DISTRACTION
    fun isProductive(): Boolean = this == STUDY_TOOL || this == WORK

    companion object {
        fun fromString(value: String?): AppCategoryClassification {
            return when (value?.uppercase()) {
                "STUDY_TOOL" -> STUDY_TOOL
                "WORK", "PRODUCTIVE" -> WORK
                "LEISURE", "HEALTH_WELLNESS" -> LEISURE
                "DISTRACTION" -> DISTRACTION
                "UNCLASSIFIED", "NEUTRAL" -> UNCLASSIFIED
                else -> entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNCLASSIFIED
            }
        }
    }
}

data class AppClassificationModel(
    val packageName: String,
    val appName: String,
    val classification: AppCategoryClassification = AppCategoryClassification.UNCLASSIFIED,
    val updatedAt: Long = System.currentTimeMillis()
)

data class AppUsageStatModel(
    val packageName: String,
    val appName: String,
    val totalTimeInForegroundSeconds: Long,
    val classification: AppCategoryClassification = AppCategoryClassification.UNCLASSIFIED,
    val windowStartMillis: Long = 0L,
    val windowEndMillis: Long = 0L
) {
    val durationFormatted: String
        get() {
            val minutes = totalTimeInForegroundSeconds / 60
            val hours = minutes / 60
            val remMin = minutes % 60
            return if (hours > 0) "${hours}h ${remMin}m" else "${minutes}m"
        }

    fun isDistraction(): Boolean = classification == AppCategoryClassification.DISTRACTION

    fun isProductive(): Boolean = classification == AppCategoryClassification.STUDY_TOOL ||
            classification == AppCategoryClassification.WORK
}

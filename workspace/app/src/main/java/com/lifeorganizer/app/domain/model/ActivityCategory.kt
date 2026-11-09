package com.lifeorganizer.app.domain.model

data class ActivityCategory(
    val name: String,
    val colorHex: String,
    val iconName: String
) {
    val displayName: String
        get() = name

    companion object {
        val CASA = ActivityCategory("Casa", "#55D6BE", "home")
        val ESTUDOS = ActivityCategory("Estudos", "#8B9CFF", "school")
        val SAUDE = ActivityCategory("Saúde", "#4EBA6F", "fitness_center")
        val TRABALHO = ActivityCategory("Trabalho", "#FFA726", "work")
        val LAZER = ActivityCategory("Lazer", "#AB8BFF", "sports_esports")
        val PESSOAL = ActivityCategory("Pessoal", "#FF7597", "person")
        val REST = ActivityCategory("Descanso", "#4DD0E1", "hotel")
        val OUTRO = ActivityCategory("Outro", "#AAB3C5", "more_horiz")
        val OUTROS = OUTRO

        // English & semantic compatibility aliases
        val STUDIES = ESTUDOS
        val STUDY = ESTUDOS
        val WORK = TRABALHO
        val LEISURE = LAZER
        val HOME = CASA
        val CHORES = CASA
        val HEALTH = SAUDE
        val PERSONAL = PESSOAL

        val ALL_CATEGORIES = listOf(CASA, ESTUDOS, SAUDE, TRABALHO, LAZER, PESSOAL, REST, OUTRO)

        fun fromDisplayName(displayName: String?): ActivityCategory {
            if (displayName.isNullOrBlank()) return OUTRO
            return ALL_CATEGORIES.firstOrNull {
                it.name.equals(displayName, ignoreCase = true) ||
                it.name.equals(displayName.replace("Saude", "Saúde"), ignoreCase = true)
            } ?: when (displayName.uppercase()) {
                "STUDIES", "STUDY", "ESTUDO", "ESTUDOS" -> ESTUDOS
                "WORK", "TRABALHO" -> TRABALHO
                "LEISURE", "LAZER" -> LAZER
                "HOME", "CASA", "CHORES" -> CASA
                "HEALTH", "SAUDE", "SAÚDE" -> SAUDE
                "REST", "DESCANSO" -> REST
                "PERSONAL", "PESSOAL" -> PESSOAL
                else -> ActivityCategory(displayName, "#8B9CFF", "label")
            }
        }

        fun getColorHexForCategory(categoryName: String): String {
            return ALL_CATEGORIES.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }?.colorHex ?: "#8B9CFF"
        }
    }
}

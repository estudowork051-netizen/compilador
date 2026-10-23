package com.paciencia.solitaire.scenery

/**
 * Os cinco cenários visuais desbloqueáveis ao longo dos 50+ níveis de jogo.
 */
enum class SceneryType(
    val id: String,
    val title: String,
    val minLevel: Int,
    val description: String
) {
    ENCHANTED_FOREST(
        id = "enchanted_forest",
        title = "Floresta Encantada",
        minLevel = 1,
        description = "Floresta verde com atmosfera mágica, raios de luz e folhas flutuantes."
    ),
    FROZEN_MOUNTAINS(
        id = "frozen_mountains",
        title = "Montanhas Geladas",
        minLevel = 11,
        description = "Picos cobertos de neve, névoa suave e flocos caindo serenamente."
    ),
    GOLDEN_DESERT(
        id = "golden_desert",
        title = "Deserto Dourado",
        minLevel = 21,
        description = "Dunas douradas ao entardecer com brisa suave e partículas de areia."
    ),
    MEDIEVAL_KINGDOM(
        id = "medieval_kingdom",
        title = "Reino Medieval",
        minLevel = 31,
        description = "Castelo sob céu crepuscular com tochas acesas e faíscas brilhantes."
    ),
    COSMIC_NIGHT(
        id = "cosmic_night",
        title = "Noite Cósmica",
        minLevel = 41,
        description = "Nebulosa estelar com poeira luminosa e constelações cintilantes."
    );

    fun isUnlocked(highestLevel: Int): Boolean {
        return highestLevel >= minLevel
    }

    companion object {
        fun fromId(id: String): SceneryType {
            return entries.firstOrNull { it.id == id } ?: ENCHANTED_FOREST
        }

        fun getSceneryForLevel(level: Int): SceneryType {
            return when {
                level >= COSMIC_NIGHT.minLevel -> COSMIC_NIGHT
                level >= MEDIEVAL_KINGDOM.minLevel -> MEDIEVAL_KINGDOM
                level >= GOLDEN_DESERT.minLevel -> GOLDEN_DESERT
                level >= FROZEN_MOUNTAINS.minLevel -> FROZEN_MOUNTAINS
                else -> ENCHANTED_FOREST
            }
        }
    }
}

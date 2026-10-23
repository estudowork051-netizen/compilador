package com.paciencia.solitaire.customization

/**
 * Modelo de avatar do jogador com categoria temática.
 */
data class Avatar(
    val id: String,
    val name: String,
    val collection: String = "Coleção de Heróis",
    val description: String,
    val price: Int,
    val title: String,
    val iconEmoji: String,
    val borderColor: Int,
    val bgColor: Int,
    val isDefaultUnlocked: Boolean = false
) {
    companion object {
        val ADVENTURER = Avatar(
            id = "avatar_adventurer",
            name = "Aventureiro",
            collection = "Desbravadores",
            description = "Explorador destemido em busca de novos horizontes",
            price = 0,
            title = "O Desbravador",
            iconEmoji = "🤠",
            borderColor = 0xFFFBBF24.toInt(),
            bgColor = 0xFF78350F.toInt(),
            isDefaultUnlocked = true
        )

        val WIZARD = Avatar(
            id = "avatar_wizard",
            name = "Mago",
            collection = "Mestres Arcanos",
            description = "Mestre das artes arcanas e dos segredos ancestrais",
            price = 40,
            title = "O Conjurador",
            iconEmoji = "🧙",
            borderColor = 0xFFA78BFA.toInt(),
            bgColor = 0xFF3B0764.toInt()
        )

        val KNIGHT = Avatar(
            id = "avatar_knight",
            name = "Cavaleiro",
            collection = "Reino Medieval",
            description = "Defensor leal munido de armadura e honra",
            price = 40,
            title = "O Guardião",
            iconEmoji = "🛡️",
            borderColor = 0xFF94A3B8.toInt(),
            bgColor = 0xFF1E293B.toInt()
        )

        val FOREST_GUARDIAN = Avatar(
            id = "avatar_forest_guardian",
            name = "Guardião da Floresta",
            collection = "Natureza",
            description = "Protetor das copas místicas e dos seres selvagens",
            price = 70,
            title = "O Protetor Verde",
            iconEmoji = "🏹",
            borderColor = 0xFF34D399.toInt(),
            bgColor = 0xFF064E3B.toInt()
        )

        val ICE_SPIRIT = Avatar(
            id = "avatar_ice_spirit",
            name = "Espírito do Gelo",
            collection = "Gelo",
            description = "Entidade das geleiras eternas e nevascas polares",
            price = 70,
            title = "O Andarilho Gelado",
            iconEmoji = "❄️",
            borderColor = 0xFF38BDF8.toInt(),
            bgColor = 0xFF0C4A6E.toInt()
        )

        val DESERT_WANDERER = Avatar(
            id = "avatar_desert_wanderer",
            name = "Viajante do Deserto",
            collection = "Relíquias Douradas",
            description = "Nômade resiliente que conhece os mistérios das dunas",
            price = 90,
            title = "O Andarilho Solar",
            iconEmoji = "🐪",
            borderColor = 0xFFF59E0B.toInt(),
            bgColor = 0xFF451A03.toInt()
        )

        val MEDIEVAL_KING = Avatar(
            id = "avatar_medieval_king",
            name = "Rei Medieval",
            collection = "Reino Medieval",
            description = "Monarca sábio regente de reinos prósperos",
            price = 120,
            title = "Sua Majestade",
            iconEmoji = "👑",
            borderColor = 0xFFEAB308.toInt(),
            bgColor = 0xFF713F12.toInt()
        )

        val COSMIC_EXPLORER = Avatar(
            id = "avatar_cosmic_explorer",
            name = "Explorador Cósmico",
            collection = "Cósmico",
            description = "Navegador interestelar que viaja pelas constelações",
            price = 150,
            title = "O Viajante Estelar",
            iconEmoji = "🚀",
            borderColor = 0xFFE879F9.toInt(),
            bgColor = 0xFF3B0764.toInt()
        )

        val ALL_AVATARS = listOf(
            ADVENTURER,
            WIZARD,
            KNIGHT,
            FOREST_GUARDIAN,
            ICE_SPIRIT,
            DESERT_WANDERER,
            MEDIEVAL_KING,
            COSMIC_EXPLORER
        )

        fun getById(id: String): Avatar {
            return ALL_AVATARS.find { it.id == id } ?: ADVENTURER
        }
    }
}

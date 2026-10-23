package com.paciencia.solitaire.customization

/**
 * Arquétipo visual para renderização vetorial e artística detalhada do Avatar.
 */
enum class AvatarArchetype {
    ADVENTURER,       // Explorador com chapéu de aba, penas douradas e broche
    ARCANE_MAGE,      // Mago arcano com capuz místico, olhos de energia e runa frontal
    MEDIEVAL_KNIGHT,  // Cavaleiro medieval com elmo de placas, visor e pluma prateada
    FOREST_RANGER,    // Explorador da floresta com capuz de folhas e marcas tribais
    ICE_KNIGHT,       // Cavaleiro de gelo com diadema de cristal e olhos glaciais
    SHADOW_LORD,      // Senhor das sombras com chifres espectrais e máscara abissal
    COSMIC_GUARDIAN,  // Guardião cósmico com halo estelar e constelação orbital
    GOLDEN_ALCHEMIST, // Alquimista dourado com monóculo e anéis de transmutação
    MYSTIC_CREATURE   // Criatura mágica draconiana com chifres e escamas arcanas
}

/**
 * Modelo de avatar do jogador com categoria temática e renderização artística autêntica.
 */
data class Avatar(
    val id: String,
    val name: String,
    val collection: String = "Coleção de Heróis",
    val description: String,
    val price: Int,
    val title: String,
    val archetype: AvatarArchetype,
    val primaryColor: Int,
    val secondaryColor: Int,
    val borderColor: Int,
    val bgColor: Int,
    val isDefaultUnlocked: Boolean = false
) {
    // Compatibilidade com código legado
    val iconEmoji: String get() = when(archetype) {
        AvatarArchetype.ADVENTURER -> "🤠"
        AvatarArchetype.ARCANE_MAGE -> "🧙"
        AvatarArchetype.MEDIEVAL_KNIGHT -> "🛡️"
        AvatarArchetype.FOREST_RANGER -> "🏹"
        AvatarArchetype.ICE_KNIGHT -> "❄️"
        AvatarArchetype.SHADOW_LORD -> "🔮"
        AvatarArchetype.COSMIC_GUARDIAN -> "⭐"
        AvatarArchetype.GOLDEN_ALCHEMIST -> "⚗️"
        AvatarArchetype.MYSTIC_CREATURE -> "🐉"
    }

    companion object {
        val ADVENTURER = Avatar(
            id = "avatar_adventurer",
            name = "Explorador Destemido",
            collection = "Desbravadores",
            description = "Aventureiro audaz que descobre relíquias e mapas ancestrais",
            price = 0,
            title = "O Desbravador",
            archetype = AvatarArchetype.ADVENTURER,
            primaryColor = 0xFFD97706.toInt(),
            secondaryColor = 0xFF78350F.toInt(),
            borderColor = 0xFFFBBF24.toInt(),
            bgColor = 0xFF2E1005.toInt(),
            isDefaultUnlocked = true
        )

        val WIZARD = Avatar(
            id = "avatar_wizard",
            name = "Mago Arcano",
            collection = "Mestres Arcanos",
            description = "Conjurador de feitiços astrais com olhos flamejantes de éter",
            price = 30,
            title = "Mestre do Éter",
            archetype = AvatarArchetype.ARCANE_MAGE,
            primaryColor = 0xFF6366F1.toInt(),
            secondaryColor = 0xFF312E81.toInt(),
            borderColor = 0xFF818CF8.toInt(),
            bgColor = 0xFF0F172A.toInt()
        )

        val KNIGHT = Avatar(
            id = "avatar_knight",
            name = "Guardião Medieval",
            collection = "Reino Medieval",
            description = "Defensor blindado da Cidadela com elmo de aço nobre e honra",
            price = 50,
            title = "Defensor da Cidadela",
            archetype = AvatarArchetype.MEDIEVAL_KNIGHT,
            primaryColor = 0xFF94A3B8.toInt(),
            secondaryColor = 0xFF334155.toInt(),
            borderColor = 0xFFE2E8F0.toInt(),
            bgColor = 0xFF0F172A.toInt()
        )

        val FOREST_GUARDIAN = Avatar(
            id = "avatar_forest_guardian",
            name = "Explorador da Floresta",
            collection = "Natureza",
            description = "Sentinela silvestre mimetizado nas copas das árvores ancestrais",
            price = 70,
            title = "Sentinela Silvestre",
            archetype = AvatarArchetype.FOREST_RANGER,
            primaryColor = 0xFF10B981.toInt(),
            secondaryColor = 0xFF064E3B.toInt(),
            borderColor = 0xFF34D399.toInt(),
            bgColor = 0xFF022C22.toInt()
        )

        val ICE_SPIRIT = Avatar(
            id = "avatar_ice_spirit",
            name = "Cavaleiro de Gelo",
            collection = "Gelo",
            description = "Guerreiro forjado nas tempestades e geleiras polares eternas",
            price = 80,
            title = "Lâmina Polar",
            archetype = AvatarArchetype.ICE_KNIGHT,
            primaryColor = 0xFF0EA5E9.toInt(),
            secondaryColor = 0xFF0369A1.toInt(),
            borderColor = 0xFF38BDF8.toInt(),
            bgColor = 0xFF082F49.toInt()
        )

        val SHADOW_LORD = Avatar(
            id = "avatar_shadow_lord",
            name = "Senhor das Sombras",
            collection = "Sombras & Mistério",
            description = "Soberano do vazio abissal com máscara sombria e chifres de névoa",
            price = 100,
            title = "Arauto do Abismo",
            archetype = AvatarArchetype.SHADOW_LORD,
            primaryColor = 0xFFA855F7.toInt(),
            secondaryColor = 0xFF581C87.toInt(),
            borderColor = 0xFFC084FC.toInt(),
            bgColor = 0xFF180B26.toInt()
        )

        val COSMIC_GUARDIAN = Avatar(
            id = "avatar_cosmic_explorer",
            name = "Guardião Cósmico",
            collection = "Cósmico",
            description = "Vigia celestial coroado com órbitas de luz e poeira de estrelas",
            price = 120,
            title = "Astrônomo Estelar",
            archetype = AvatarArchetype.COSMIC_GUARDIAN,
            primaryColor = 0xFFEC4899.toInt(),
            secondaryColor = 0xFF831843.toInt(),
            borderColor = 0xFFF472B6.toInt(),
            bgColor = 0xFF240417.toInt()
        )

        val GOLDEN_ALCHEMIST = Avatar(
            id = "avatar_desert_wanderer",
            name = "Alquimista Dourado",
            collection = "Relíquias Douradas",
            description = "Mestre da transmutação que decifra segredos de ouro e fogo solar",
            price = 140,
            title = "Mestre da Transmutação",
            archetype = AvatarArchetype.GOLDEN_ALCHEMIST,
            primaryColor = 0xFFF59E0B.toInt(),
            secondaryColor = 0xFF78350F.toInt(),
            borderColor = 0xFFFBBF24.toInt(),
            bgColor = 0xFF351A05.toInt()
        )

        val MYSTIC_CREATURE = Avatar(
            id = "avatar_medieval_king",
            name = "Criatura Mágica",
            collection = "Místicos",
            description = "Espírito draconiano lendário abençoado com sabedoria milenar",
            price = 150,
            title = "Espírito Draconiano",
            archetype = AvatarArchetype.MYSTIC_CREATURE,
            primaryColor = 0xFF14B8A6.toInt(),
            secondaryColor = 0xFF0F766E.toInt(),
            borderColor = 0xFF2DD4BF.toInt(),
            bgColor = 0xFF042F2C.toInt()
        )

        val ALL_AVATARS = listOf(
            ADVENTURER,
            WIZARD,
            KNIGHT,
            FOREST_GUARDIAN,
            ICE_SPIRIT,
            SHADOW_LORD,
            COSMIC_GUARDIAN,
            GOLDEN_ALCHEMIST,
            MYSTIC_CREATURE
        )

        fun getById(id: String): Avatar {
            return ALL_AVATARS.find { it.id == id } ?: ADVENTURER
        }
    }
}

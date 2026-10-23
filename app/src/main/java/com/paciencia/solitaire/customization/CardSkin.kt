package com.paciencia.solitaire.customization

/**
 * Padrão de renderização do verso e ornamentos da carta.
 */
enum class SkinPattern {
    CLASSIC,
    GOLDEN,
    ARCANE,
    SHADOW,
    NATURE,
    ICE,
    COSMIC,
    ROYAL
}

/**
 * Modelo de skin de personalização de cartas.
 */
data class CardSkin(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val pattern: SkinPattern,
    val primaryColor: Int,
    val secondaryColor: Int,
    val accentColor: Int,
    val isDefaultUnlocked: Boolean = false
) {
    companion object {
        val CLASSIC = CardSkin(
            id = "skin_classic",
            name = "Clássica",
            description = "Aparência clássica e atemporal de baralho",
            price = 0,
            pattern = SkinPattern.CLASSIC,
            primaryColor = 0xFF1E3A8A.toInt(),
            secondaryColor = 0xFF0F172A.toInt(),
            accentColor = 0xFF60A5FA.toInt(),
            isDefaultUnlocked = true
        )

        val GOLDEN = CardSkin(
            id = "skin_golden",
            name = "Dourada",
            description = "Elegantes filigranas douradas com brilho nobre",
            price = 30,
            pattern = SkinPattern.GOLDEN,
            primaryColor = 0xFF78350F.toInt(),
            secondaryColor = 0xFF451A03.toInt(),
            accentColor = 0xFFFBBF24.toInt()
        )

        val ARCANE = CardSkin(
            id = "skin_arcane",
            name = "Arcana",
            description = "Símbolos mágicos e runas de poder antigo",
            price = 60,
            pattern = SkinPattern.ARCANE,
            primaryColor = 0xFF4C1D95.toInt(),
            secondaryColor = 0xFF2E1065.toInt(),
            accentColor = 0xFFA78BFA.toInt()
        )

        val SHADOW = CardSkin(
            id = "skin_shadow",
            name = "Sombria",
            description = "Acabamento escuro sofisticado com toques carmesim",
            price = 60,
            pattern = SkinPattern.SHADOW,
            primaryColor = 0xFF18181B.toInt(),
            secondaryColor = 0xFF09090B.toInt(),
            accentColor = 0xFFE11D48.toInt()
        )

        val NATURE = CardSkin(
            id = "skin_nature",
            name = "Natureza",
            description = "Ramos esmeralda e folhagens místicas",
            price = 100,
            pattern = SkinPattern.NATURE,
            primaryColor = 0xFF064E3B.toInt(),
            secondaryColor = 0xFF022C22.toInt(),
            accentColor = 0xFF34D399.toInt()
        )

        val ICE = CardSkin(
            id = "skin_ice",
            name = "Gelo",
            description = "Cristais gélidos e fractais azuis reluzentes",
            price = 100,
            pattern = SkinPattern.ICE,
            primaryColor = 0xFF0C4A6E.toInt(),
            secondaryColor = 0xFF082F49.toInt(),
            accentColor = 0xFF38BDF8.toInt()
        )

        val COSMIC = CardSkin(
            id = "skin_cosmic",
            name = "Cósmica",
            description = "Nebulosas profundas com constelações estelares",
            price = 150,
            pattern = SkinPattern.COSMIC,
            primaryColor = 0xFF311042.toInt(),
            secondaryColor = 0xFF150524.toInt(),
            accentColor = 0xFFE879F9.toInt()
        )

        val ROYAL = CardSkin(
            id = "skin_royal",
            name = "Real",
            description = "Ornamento de coroa imperial com detalhes em ouro",
            price = 150,
            pattern = SkinPattern.ROYAL,
            primaryColor = 0xFF831843.toInt(),
            secondaryColor = 0xFF500724.toInt(),
            accentColor = 0xFFF59E0B.toInt()
        )

        val ALL_SKINS = listOf(
            CLASSIC,
            GOLDEN,
            ARCANE,
            SHADOW,
            NATURE,
            ICE,
            COSMIC,
            ROYAL
        )

        fun getById(id: String): CardSkin {
            return ALL_SKINS.find { it.id == id } ?: CLASSIC
        }
    }
}

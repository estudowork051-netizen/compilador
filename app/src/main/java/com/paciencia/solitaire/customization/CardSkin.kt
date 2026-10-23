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
    FLAME,
    COSMIC,
    ROYAL
}

/**
 * Tipo exclusivo de celebração de vitória executada conforme a skin equipada.
 */
enum class VictoryCelebrationType {
    CLASSIC_FIREWORKS, // Cascata festiva de safira e prata
    GOLDEN_MAJESTY,    // Chuva majestosa de ouro e anéis de luz solar
    ARCANE_SURGE,      // Onda de choque mágica e runas astrais azul-cobalto
    ABYSSAL_SHADOW,    // Névoa escura e pulso sombrio violeta/carmesim
    LIVING_FOREST,     // Vórtice de folhas e expansão de energia esmeralda
    GLACIAL_BURST,     // Geada cristalina e fragmentos de luz fria
    ANCESTRAL_FLAME,   // Erupção de chamas ancestrais e brasas incandescentes
    COSMIC_SUPERNOVA,  // Supernova violeta e chuva de estrelas cadentes
    ROYAL_TRIUMPH      // Emblemas imperiais reluzentes e fanfarra majestosa
}

/**
 * Modelo de skin de personalização de cartas com coleções temáticas e celebrações exclusivas.
 */
data class CardSkin(
    val id: String,
    val name: String,
    val collection: String = "Coleção Clássica",
    val description: String,
    val price: Int,
    val pattern: SkinPattern,
    val primaryColor: Int,
    val secondaryColor: Int,
    val accentColor: Int,
    val isDefaultUnlocked: Boolean = false,
    val trailColor: Int = accentColor,
    val celebrationType: VictoryCelebrationType = VictoryCelebrationType.CLASSIC_FIREWORKS,
    val effectSummary: String = "Efeito de celebração temático exclusivo"
) {
    companion object {
        val CLASSIC = CardSkin(
            id = "skin_classic",
            name = "Clássica Nobre",
            collection = "Tradição",
            description = "Aparência atemporal com filigranas de safira e prata",
            price = 0,
            pattern = SkinPattern.CLASSIC,
            primaryColor = 0xFF1E3A8A.toInt(),
            secondaryColor = 0xFF0F172A.toInt(),
            accentColor = 0xFF60A5FA.toInt(),
            isDefaultUnlocked = true,
            trailColor = 0xFF93C5FD.toInt(),
            celebrationType = VictoryCelebrationType.CLASSIC_FIREWORKS,
            effectSummary = "Fogos de artifício nobres e chuva de prata estelar"
        )

        val ARCANE = CardSkin(
            id = "skin_arcane",
            name = "Arcana Azul",
            collection = "Arcano",
            description = "Runas astrais luminescentes e ondas de choque azul-cobalto",
            price = 30,
            pattern = SkinPattern.ARCANE,
            primaryColor = 0xFF1E1B4B.toInt(),
            secondaryColor = 0xFF0F172A.toInt(),
            accentColor = 0xFF38BDF8.toInt(),
            trailColor = 0xFF60A5FA.toInt(),
            celebrationType = VictoryCelebrationType.ARCANE_SURGE,
            effectSummary = "Onda de energia mágica azul, relâmpagos e runas astrais"
        )

        val GOLDEN = CardSkin(
            id = "skin_golden",
            name = "Relíquia Dourada",
            collection = "Relíquias Douradas",
            description = "Moldura metálica dourada com filigranas e ouro 24k",
            price = 50,
            pattern = SkinPattern.GOLDEN,
            primaryColor = 0xFF78350F.toInt(),
            secondaryColor = 0xFF451A03.toInt(),
            accentColor = 0xFFFBBF24.toInt(),
            trailColor = 0xFFFDE047.toInt(),
            celebrationType = VictoryCelebrationType.GOLDEN_MAJESTY,
            effectSummary = "Chuva majestosa de moedas de ouro, raios solares e anéis de luz"
        )

        val NATURE = CardSkin(
            id = "skin_nature",
            name = "Floresta Viva",
            collection = "Natureza",
            description = "Símbolos naturais, vinhas élficas e energia vegetal esmeralda",
            price = 80,
            pattern = SkinPattern.NATURE,
            primaryColor = 0xFF064E3B.toInt(),
            secondaryColor = 0xFF022C22.toInt(),
            accentColor = 0xFF34D399.toInt(),
            trailColor = 0xFF4ADE80.toInt(),
            celebrationType = VictoryCelebrationType.LIVING_FOREST,
            effectSummary = "Vórtice de folhas mágicas élficas e expansão de luz verde"
        )

        val ICE = CardSkin(
            id = "skin_ice",
            name = "Cristal Glacial",
            collection = "Gelo",
            description = "Molduras de gelo cristalino e fractais polares de geada",
            price = 80,
            pattern = SkinPattern.ICE,
            primaryColor = 0xFF0C4A6E.toInt(),
            secondaryColor = 0xFF082F49.toInt(),
            accentColor = 0xFF38BDF8.toInt(),
            trailColor = 0xFFBAE6FD.toInt(),
            celebrationType = VictoryCelebrationType.GLACIAL_BURST,
            effectSummary = "Explosão de cristais glaciais e onda de luz polar cintilante"
        )

        val FLAME = CardSkin(
            id = "skin_flame",
            name = "Chamas Ancestrais",
            collection = "Fogo Ancestral",
            description = "Obsidiana vulcânica com brasas vivas e dragão de fogo",
            price = 100,
            pattern = SkinPattern.FLAME,
            primaryColor = 0xFF431407.toInt(),
            secondaryColor = 0xFF1C0A00.toInt(),
            accentColor = 0xFFFB923C.toInt(),
            trailColor = 0xFFF97316.toInt(),
            celebrationType = VictoryCelebrationType.ANCESTRAL_FLAME,
            effectSummary = "Erupção de fogo ancestral, brasas vivas e dragões flamejantes"
        )

        val COSMIC = CardSkin(
            id = "skin_cosmic",
            name = "Energia Cósmica",
            collection = "Cósmico",
            description = "Nebulosas violetas, astrolábio astral e constelações estelares",
            price = 120,
            pattern = SkinPattern.COSMIC,
            primaryColor = 0xFF311042.toInt(),
            secondaryColor = 0xFF150524.toInt(),
            accentColor = 0xFFE879F9.toInt(),
            trailColor = 0xFFA855F7.toInt(),
            celebrationType = VictoryCelebrationType.COSMIC_SUPERNOVA,
            effectSummary = "Supernova cósmica, pulso astral e chuva de estrelas cadentes"
        )

        val SHADOW = CardSkin(
            id = "skin_shadow",
            name = "Sombra Abissal",
            collection = "Sombras & Mistério",
            description = "Armadura gótica de obsidiana com névoa e olhos abissais",
            price = 120,
            pattern = SkinPattern.SHADOW,
            primaryColor = 0xFF18181B.toInt(),
            secondaryColor = 0xFF09090B.toInt(),
            accentColor = 0xFFC084FC.toInt(),
            trailColor = 0xFFE11D48.toInt(),
            celebrationType = VictoryCelebrationType.ABYSSAL_SHADOW,
            effectSummary = "Névoa abissal escura, tentáculos espectrais e pulso de sombra"
        )

        val ROYAL = CardSkin(
            id = "skin_royal",
            name = "Guardião Real",
            collection = "Reino Medieval",
            description = "Veludo imperial, espadas cruzadas e brasão de realeza",
            price = 150,
            pattern = SkinPattern.ROYAL,
            primaryColor = 0xFF831843.toInt(),
            secondaryColor = 0xFF500724.toInt(),
            accentColor = 0xFFF59E0B.toInt(),
            trailColor = 0xFFFBBF24.toInt(),
            celebrationType = VictoryCelebrationType.ROYAL_TRIUMPH,
            effectSummary = "Estandartes dourados, brasão imperial e chuva de glória da corte"
        )

        val ALL_SKINS = listOf(
            CLASSIC,
            ARCANE,
            GOLDEN,
            NATURE,
            ICE,
            FLAME,
            COSMIC,
            SHADOW,
            ROYAL
        )

        fun getById(id: String): CardSkin {
            return ALL_SKINS.find { it.id == id } ?: CLASSIC
        }
    }
}

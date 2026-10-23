package com.paciencia.solitaire.scenery

import com.paciencia.solitaire.scenery.themes.CosmicNightTheme
import com.paciencia.solitaire.scenery.themes.EnchantedForestTheme
import com.paciencia.solitaire.scenery.themes.FrozenMountainsTheme
import com.paciencia.solitaire.scenery.themes.GoldenDesertTheme
import com.paciencia.solitaire.scenery.themes.MedievalKingdomTheme

/**
 * Gerenciador central de cenários e temas visuais.
 */
object SceneryManager {

    private val themes = mapOf(
        SceneryType.ENCHANTED_FOREST to EnchantedForestTheme(),
        SceneryType.FROZEN_MOUNTAINS to FrozenMountainsTheme(),
        SceneryType.GOLDEN_DESERT to GoldenDesertTheme(),
        SceneryType.MEDIEVAL_KINGDOM to MedievalKingdomTheme(),
        SceneryType.COSMIC_NIGHT to CosmicNightTheme()
    )

    fun getTheme(type: SceneryType): SceneryTheme {
        return themes[type] ?: themes.getValue(SceneryType.ENCHANTED_FOREST)
    }
}

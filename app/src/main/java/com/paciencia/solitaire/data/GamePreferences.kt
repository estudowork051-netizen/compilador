package com.paciencia.solitaire.data

import android.content.Context
import android.content.SharedPreferences
import com.paciencia.solitaire.scenery.SceneryType

/**
 * Gerencia a persistência de configurações, preferências e progresso do jogador em SharedPreferences.
 */
class GamePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var highestLevelUnlocked: Int
        get() = prefs.getInt(KEY_HIGHEST_LEVEL, 1)
        set(value) {
            if (value > highestLevelUnlocked) {
                prefs.edit().putInt(KEY_HIGHEST_LEVEL, value).apply()
            }
        }

    var currentLevel: Int
        get() = prefs.getInt(KEY_CURRENT_LEVEL, 1)
        set(value) = prefs.edit().putInt(KEY_CURRENT_LEVEL, value).apply()

    var selectedScenery: SceneryType
        get() {
            val id = prefs.getString(KEY_SELECTED_SCENERY, SceneryType.ENCHANTED_FOREST.id)
                ?: SceneryType.ENCHANTED_FOREST.id
            return SceneryType.fromId(id)
        }
        set(value) = prefs.edit().putString(KEY_SELECTED_SCENERY, value.id).apply()

    var isParticlesEnabled: Boolean
        get() = prefs.getBoolean(KEY_PARTICLES_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_PARTICLES_ENABLED, value).apply()

    var isHapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, value).apply()

    var isPowerSaveMode: Boolean
        get() = prefs.getBoolean(KEY_POWER_SAVE, false)
        set(value) = prefs.edit().putBoolean(KEY_POWER_SAVE, value).apply()

    var isAutoFinishEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_FINISH, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_FINISH, value).apply()

    /**
     * Registra vitória em um nível:
     * - Desbloqueia o próximo nível.
     * - Atualiza o cenário padrão se um novo cenário for liberado.
     * @return Cenário recém desbloqueado, se houver.
     */
    fun onLevelCompleted(level: Int): SceneryType? {
        val nextLevel = level + 1
        if (nextLevel > highestLevelUnlocked) {
            highestLevelUnlocked = nextLevel
        }
        currentLevel = nextLevel

        // Verifica se este novo nível destravou um novo cenário
        for (scenery in SceneryType.entries) {
            if (scenery.minLevel == nextLevel) {
                // Auto-seleciona o novo cenário desbloqueado
                selectedScenery = scenery
                return scenery
            }
        }
        return null
    }

    companion object {
        private const val PREFS_NAME = "paciencia_solitaire_prefs"
        private const val KEY_HIGHEST_LEVEL = "highest_level"
        private const val KEY_CURRENT_LEVEL = "current_level"
        private const val KEY_SELECTED_SCENERY = "selected_scenery"
        private const val KEY_PARTICLES_ENABLED = "particles_enabled"
        private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
        private const val KEY_POWER_SAVE = "power_save_mode"
        private const val KEY_AUTO_FINISH = "auto_finish_enabled"
    }
}

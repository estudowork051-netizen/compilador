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

    // --- SISTEMA DE MOEDAS E ECONOMIA ---
    var coinsBalance: Int
        get() = prefs.getInt(KEY_COINS_BALANCE, 0)
        private set(value) = prefs.edit().putInt(KEY_COINS_BALANCE, value.coerceAtLeast(0)).apply()

    /**
     * Adiciona moedas ao saldo persistente e retorna o novo total.
     */
    fun addCoins(amount: Int): Int {
        if (amount <= 0) return coinsBalance
        val updated = coinsBalance + amount
        coinsBalance = updated
        return updated
    }

    /**
     * Desconta moedas se houver saldo suficiente. Retorna true se a transação teve sucesso.
     */
    fun spendCoins(amount: Int): Boolean {
        if (amount <= 0) return true
        if (coinsBalance >= amount) {
            coinsBalance -= amount
            return true
        }
        return false
    }

    fun hasEnoughCoins(amount: Int): Boolean = coinsBalance >= amount

    /**
     * Verifica se a vitória deste nível específico já foi premiada para evitar premiação duplicada.
     */
    fun isLevelVictoryRewarded(level: Int): Boolean {
        val rewardedSet = prefs.getStringSet(KEY_REWARDED_LEVELS, emptySet()) ?: emptySet()
        return rewardedSet.contains(level.toString())
    }

    /**
     * Marca o nível como premiado.
     */
    fun markLevelVictoryRewarded(level: Int) {
        val currentSet = prefs.getStringSet(KEY_REWARDED_LEVELS, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(level.toString())
        prefs.edit().putStringSet(KEY_REWARDED_LEVELS, currentSet).apply()
    }

    // --- SISTEMA DE SKINS ---
    var equippedSkinId: String
        get() = prefs.getString(KEY_EQUIPPED_SKIN, "skin_classic") ?: "skin_classic"
        set(value) = prefs.edit().putString(KEY_EQUIPPED_SKIN, value).apply()

    fun getUnlockedSkinIds(): Set<String> {
        val defaultSet = setOf("skin_classic")
        val saved = prefs.getStringSet(KEY_UNLOCKED_SKINS, null)
        return if (saved != null) saved + defaultSet else defaultSet
    }

    fun isSkinUnlocked(skinId: String): Boolean {
        if (skinId == "skin_classic") return true
        return getUnlockedSkinIds().contains(skinId)
    }

    fun unlockSkin(skinId: String) {
        val set = getUnlockedSkinIds().toMutableSet()
        set.add(skinId)
        prefs.edit().putStringSet(KEY_UNLOCKED_SKINS, set).apply()
    }

    // --- SISTEMA DE AVATARES ---
    var equippedAvatarId: String
        get() = prefs.getString(KEY_EQUIPPED_AVATAR, "avatar_adventurer") ?: "avatar_adventurer"
        set(value) = prefs.edit().putString(KEY_EQUIPPED_AVATAR, value).apply()

    fun getUnlockedAvatarIds(): Set<String> {
        val defaultSet = setOf("avatar_adventurer")
        val saved = prefs.getStringSet(KEY_UNLOCKED_AVATARS, null)
        return if (saved != null) saved + defaultSet else defaultSet
    }

    fun isAvatarUnlocked(avatarId: String): Boolean {
        if (avatarId == "avatar_adventurer") return true
        return getUnlockedAvatarIds().contains(avatarId)
    }

    fun unlockAvatar(avatarId: String) {
        val set = getUnlockedAvatarIds().toMutableSet()
        set.add(avatarId)
        prefs.edit().putStringSet(KEY_UNLOCKED_AVATARS, set).apply()
    }

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

        private const val KEY_COINS_BALANCE = "coins_balance"
        private const val KEY_REWARDED_LEVELS = "rewarded_levels_set"
        private const val KEY_EQUIPPED_SKIN = "equipped_skin_id"
        private const val KEY_UNLOCKED_SKINS = "unlocked_skins_set"
        private const val KEY_EQUIPPED_AVATAR = "equipped_avatar_id"
        private const val KEY_UNLOCKED_AVATARS = "unlocked_avatars_set"
    }
}

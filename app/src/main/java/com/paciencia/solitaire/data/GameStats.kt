package com.paciencia.solitaire.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Registra estatísticas acumuladas do jogador.
 */
class GameStats(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(STATS_PREFS_NAME, Context.MODE_PRIVATE)

    var gamesPlayed: Int
        get() = prefs.getInt(KEY_GAMES_PLAYED, 0)
        private set(value) = prefs.edit().putInt(KEY_GAMES_PLAYED, value).apply()

    var gamesWon: Int
        get() = prefs.getInt(KEY_GAMES_WON, 0)
        private set(value) = prefs.edit().putInt(KEY_GAMES_WON, value).apply()

    var bestTimeSeconds: Int
        get() = prefs.getInt(KEY_BEST_TIME, Int.MAX_VALUE)
        private set(value) = prefs.edit().putInt(KEY_BEST_TIME, value).apply()

    var fewestMoves: Int
        get() = prefs.getInt(KEY_FEWEST_MOVES, Int.MAX_VALUE)
        private set(value) = prefs.edit().putInt(KEY_FEWEST_MOVES, value).apply()

    fun recordGameStarted() {
        gamesPlayed++
    }

    fun recordGameWon(elapsedSeconds: Int, moves: Int) {
        gamesWon++
        if (elapsedSeconds < bestTimeSeconds) {
            bestTimeSeconds = elapsedSeconds
        }
        if (moves < fewestMoves) {
            fewestMoves = moves
        }
    }

    companion object {
        private const val STATS_PREFS_NAME = "paciencia_solitaire_stats"
        private const val KEY_GAMES_PLAYED = "games_played"
        private const val KEY_GAMES_WON = "games_won"
        private const val KEY_BEST_TIME = "best_time"
        private const val KEY_FEWEST_MOVES = "fewest_moves"
    }
}

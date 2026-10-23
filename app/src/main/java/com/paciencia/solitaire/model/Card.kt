package com.paciencia.solitaire.model

/**
 * Representa uma carta individual de baralho.
 */
data class Card(
    val id: Int,
    val suit: Suit,
    val rank: Rank,
    var isFaceUp: Boolean = false
) {
    // Coordenadas visuais interpoladas na tela durante animação ou repouso
    var currentX: Float = 0f
    var currentY: Float = 0f

    val isRed: Boolean
        get() = suit.isRed

    val isBlack: Boolean
        get() = suit.isBlack

    /**
     * Clona a carta com seu estado atual para histórico e serialização.
     */
    fun clone(): Card {
        val copy = Card(id, suit, rank, isFaceUp)
        copy.currentX = currentX
        copy.currentY = currentY
        return copy
    }

    override fun toString(): String {
        return "${rank.label}${suit.symbol}${if (isFaceUp) "↑" else "↓"}"
    }
}

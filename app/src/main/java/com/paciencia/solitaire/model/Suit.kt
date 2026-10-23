package com.paciencia.solitaire.model

/**
 * Representa os quatro naipes clássicos do baralho de Paciência.
 */
enum class Suit(val isRed: Boolean, val symbol: String, val displayName: String) {
    HEARTS(true, "♥", "Copas"),
    DIAMONDS(true, "♦", "Ouros"),
    CLUBS(false, "♣", "Paus"),
    SPADES(false, "♠", "Espadas");

    val isBlack: Boolean
        get() = !isRed
}

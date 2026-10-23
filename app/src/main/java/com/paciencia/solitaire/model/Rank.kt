package com.paciencia.solitaire.model

/**
 * Representa os 13 valores das cartas, do Ás (1) ao Rei (13).
 */
enum class Rank(val value: Int, val label: String) {
    ACE(1, "A"),
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K");

    /**
     * Retorna se esta carta é exatamente 1 valor abaixo da outra (para colunas de tableau).
     * Exemplo: Dama (12) é um abaixo de Rei (13).
     */
    fun isOneBelow(other: Rank): Boolean {
        return this.value == other.value - 1
    }

    /**
     * Retorna se esta carta é exatamente 1 valor acima da outra (para fundações).
     * Exemplo: Dois (2) é um acima de Ás (1).
     */
    fun isOneAbove(other: Rank): Boolean {
        return this.value == other.value + 1
    }

    companion object {
        fun fromValue(value: Int): Rank {
            return entries.firstOrNull { it.value == value } ?: ACE
        }
    }
}

package com.paciencia.solitaire.model

/**
 * Identifica cada uma das pilhas ou áreas de cartas na mesa do Klondike Solitaire.
 */
sealed class PileType {
    object Stock : PileType() {
        override fun toString(): String = "Stock"
    }

    object Waste : PileType() {
        override fun toString(): String = "Waste"
    }

    data class Foundation(val index: Int) : PileType() {
        init {
            require(index in 0..3) { "Índice de fundação inválido: $index" }
        }
        override fun toString(): String = "Foundation_$index"
    }

    data class Tableau(val index: Int) : PileType() {
        init {
            require(index in 0..6) { "Índice de coluna inválido: $index" }
        }
        override fun toString(): String = "Tableau_$index"
    }

    companion object {
        fun fromString(str: String): PileType {
            return when {
                str == "Stock" -> Stock
                str == "Waste" -> Waste
                str.startsWith("Foundation_") -> {
                    val idx = str.removePrefix("Foundation_").toIntOrNull() ?: 0
                    Foundation(idx)
                }
                str.startsWith("Tableau_") -> {
                    val idx = str.removePrefix("Tableau_").toIntOrNull() ?: 0
                    Tableau(idx)
                }
                else -> Stock
            }
        }
    }
}

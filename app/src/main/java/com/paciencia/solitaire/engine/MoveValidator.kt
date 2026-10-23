package com.paciencia.solitaire.engine

import com.paciencia.solitaire.model.Card
import com.paciencia.solitaire.model.CardPile
import com.paciencia.solitaire.model.PileType
import com.paciencia.solitaire.model.Rank

/**
 * Validador estrito das regras clássicas do Klondike Solitaire.
 */
object MoveValidator {

    /**
     * Valida se uma sequência de cartas pode ser movida para uma coluna de tableau.
     */
    fun canMoveToTableau(cardsToMove: List<Card>, destinationPile: CardPile): Boolean {
        if (cardsToMove.isEmpty()) return false
        val firstCard = cardsToMove.first()

        // Regra 1: Se a coluna estiver vazia, apenas um Rei (King) pode iniciar a coluna
        if (destinationPile.isEmpty) {
            return firstCard.rank == Rank.KING
        }

        val topCard = destinationPile.top() ?: return false
        // A carta receptora deve estar virada para cima
        if (!topCard.isFaceUp) return false

        // Regra 2: Alternância estrita de cores (Vermelho sobre Preto, Preto sobre Vermelho)
        val alternatingColors = firstCard.isRed != topCard.isRed

        // Regra 3: Ordem decrescente exata (ex.: 8 sobre 9, Valete sobre Dama)
        val isDescending = firstCard.rank.isOneBelow(topCard.rank)

        return alternatingColors && isDescending
    }

    /**
     * Valida se uma carta pode ser movida para uma das quatro fundações.
     */
    fun canMoveToFoundation(cardToMove: Card, foundationPile: CardPile): Boolean {
        // Regra 1: Se a fundação estiver vazia, apenas um Ás (Ace) pode ser colocado
        if (foundationPile.isEmpty) {
            return cardToMove.rank == Rank.ACE
        }

        val topCard = foundationPile.top() ?: return false

        // Regra 2: Mesmo naipe obrigatório
        val sameSuit = cardToMove.suit == topCard.suit

        // Regra 3: Ordem ascendente exata (ex.: 2 sobre Ás, 3 sobre 2, até o Rei)
        val isAscending = cardToMove.rank.isOneAbove(topCard.rank)

        return sameSuit && isAscending
    }

    /**
     * Valida um movimento genérico entre quaisquer dois tipos de pilhas.
     */
    fun isValidMove(cardsToMove: List<Card>, fromType: PileType, toPile: CardPile): Boolean {
        if (cardsToMove.isEmpty()) return false

        return when (toPile.type) {
            is PileType.Tableau -> canMoveToTableau(cardsToMove, toPile)
            is PileType.Foundation -> {
                // Para a fundação, apenas 1 carta pode ser movida por vez
                if (cardsToMove.size == 1) {
                    canMoveToFoundation(cardsToMove.first(), toPile)
                } else {
                    false
                }
            }
            PileType.Stock, PileType.Waste -> false
        }
    }
}

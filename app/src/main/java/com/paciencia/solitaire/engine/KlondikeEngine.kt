package com.paciencia.solitaire.engine

import com.paciencia.solitaire.model.Card
import com.paciencia.solitaire.model.CardPile
import com.paciencia.solitaire.model.MoveAction
import com.paciencia.solitaire.model.PileType
import com.paciencia.solitaire.model.Rank
import java.util.ArrayDeque

/**
 * Motor central de regras e estado do Klondike Solitaire.
 */
class KlondikeEngine {

    val stockPile = CardPile(PileType.Stock)
    val wastePile = CardPile(PileType.Waste)
    val foundationPiles = Array(4) { CardPile(PileType.Foundation(it)) }
    val tableauPiles = Array(7) { CardPile(PileType.Tableau(it)) }

    private val undoStack = ArrayDeque<MoveAction>()

    var moveCount: Int = 0
        private set

    var currentLevel: Int = 1
        private set

    val isWon: Boolean
        get() = foundationPiles.all { it.size == 13 }

    /**
     * Inicia uma nova partida para o nível especificado.
     */
    fun startNewGame(level: Int = 1) {
        currentLevel = level
        moveCount = 0
        undoStack.clear()

        // Limpa todas as pilhas
        stockPile.clear()
        wastePile.clear()
        foundationPiles.forEach { it.clear() }
        tableauPiles.forEach { it.clear() }

        // Cria e embaralha o baralho para o nível
        val deck = DeckGenerator.createShuffledDeckForLevel(level)
        var cardIndex = 0

        // Distribui as cartas no formato clássico triangular de 7 colunas:
        // Coluna 0: 1 carta (1 para cima)
        // Coluna 1: 2 cartas (1 para baixo, 1 para cima)
        // ...
        // Coluna 6: 7 cartas (6 para baixo, 1 para cima)
        for (col in 0 until 7) {
            for (row in 0..col) {
                val card = deck[cardIndex++]
                card.isFaceUp = (row == col) // Apenas a última carta de cada coluna fica virada para cima
                tableauPiles[col].push(card)
            }
        }

        // As cartas restantes (24 cartas) vão para o monte de compra (Stock), todas viradas para baixo
        while (cardIndex < deck.size) {
            val card = deck[cardIndex++]
            card.isFaceUp = false
            stockPile.push(card)
        }
    }

    /**
     * Compra uma carta do monte de compra (Stock) e coloca no descarte (Waste).
     * Se o Stock estiver vazio, recicla o Waste de volta para o Stock.
     */
    fun drawOrRecycleStock(): Boolean {
        if (stockPile.isNotEmpty) {
            val card = stockPile.pop() ?: return false
            card.isFaceUp = true
            wastePile.push(card)
            undoStack.push(MoveAction.StockDrawn(card))
            moveCount++
            return true
        } else if (wastePile.isNotEmpty) {
            // Recicla o Waste de volta para o Stock
            val recycledCards = mutableListOf<Card>()
            while (wastePile.isNotEmpty) {
                val card = wastePile.pop() ?: break
                card.isFaceUp = false
                stockPile.push(card)
                recycledCards.add(card)
            }
            undoStack.push(MoveAction.WasteRecycled(recycledCards))
            moveCount++
            return true
        }
        return false
    }

    /**
     * Move uma sequência de cartas de uma pilha para outra se o movimento for válido.
     */
    fun moveCards(fromType: PileType, toType: PileType, count: Int): Boolean {
        if (count <= 0) return false
        val fromPile = getPile(fromType)
        val toPile = getPile(toType)

        if (fromPile.size < count) return false

        val cardsToMove = fromPile.cardsFrom(fromPile.size - count)
        if (!MoveValidator.isValidMove(cardsToMove, fromType, toPile)) {
            return false
        }

        // Executa a movimentação
        val popped = fromPile.pop(count)
        toPile.pushAll(popped)

        // Se a carta que ficou no topo da pilha de origem estiver virada para baixo, revela-a
        var flippedCardBelow = false
        val newTop = fromPile.top()
        if (newTop != null && !newTop.isFaceUp) {
            newTop.isFaceUp = true
            flippedCardBelow = true
        }

        undoStack.push(MoveAction.CardsMoved(fromType, toType, popped, flippedCardBelow))
        moveCount++
        return true
    }

    /**
     * Tenta movimentar inteligentemente uma carta ao ser tocada (Tap-to-Move).
     * Ordem de prioridade:
     * 1. Fundação apropriada (se for apenas a carta do topo)
     * 2. Colunas de tableau válidas
     */
    fun autoMove(card: Card, fromType: PileType): Boolean {
        val fromPile = getPile(fromType)
        val cardIndex = fromPile.indexOf(card)
        if (cardIndex == -1 || !card.isFaceUp) return false

        val count = fromPile.size - cardIndex
        val cardsToMove = fromPile.cardsFrom(cardIndex)

        // Se for apenas uma carta, tenta primeiro as 4 fundações
        if (count == 1) {
            for (foundation in foundationPiles) {
                if (MoveValidator.canMoveToFoundation(card, foundation)) {
                    return moveCards(fromType, foundation.type, 1)
                }
            }
        }

        // Tenta colunas de Tableau
        for (tableau in tableauPiles) {
            if (tableau.type != fromType && MoveValidator.canMoveToTableau(cardsToMove, tableau)) {
                return moveCards(fromType, tableau.type, count)
            }
        }

        return false
    }

    /**
     * Desfaz o último movimento realizado.
     */
    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        val lastAction = undoStack.pop()

        when (lastAction) {
            is MoveAction.CardsMoved -> {
                val fromPile = getPile(lastAction.from)
                val toPile = getPile(lastAction.to)

                // Se uma carta abaixo havia sido virada para cima, volta a virá-la para baixo
                if (lastAction.flippedCardBelow) {
                    fromPile.top()?.isFaceUp = false
                }

                // Devolve as cartas movidas para a pilha de origem
                val returnedCards = toPile.pop(lastAction.cards.size)
                fromPile.pushAll(returnedCards)
            }
            is MoveAction.StockDrawn -> {
                val card = wastePile.pop()
                if (card != null) {
                    card.isFaceUp = false
                    stockPile.push(card)
                }
            }
            is MoveAction.WasteRecycled -> {
                // Devolve as cartas do Stock de volta ao Waste
                while (stockPile.isNotEmpty) {
                    val card = stockPile.pop() ?: break
                    card.isFaceUp = true
                    wastePile.push(card)
                }
            }
        }

        if (moveCount > 0) moveCount--
        return true
    }

    /**
     * Verifica e executa auto-finalização segura se todas as cartas do tableau estiverem reveladas
     * e o estoque e descarte estiverem vazios.
     */
    fun canAutoComplete(): Boolean {
        if (stockPile.isNotEmpty || wastePile.isNotEmpty) return false
        return tableauPiles.all { pile -> pile.cards.all { it.isFaceUp } }
    }

    /**
     * Executa 1 passo de auto-finalização (move a menor carta viável para a fundação).
     * Retorna true se um movimento foi executado.
     */
    fun stepAutoComplete(): Boolean {
        for (tableau in tableauPiles) {
            val top = tableau.top() ?: continue
            for (foundation in foundationPiles) {
                if (MoveValidator.canMoveToFoundation(top, foundation)) {
                    return moveCards(tableau.type, foundation.type, 1)
                }
            }
        }
        return false
    }

    fun getPile(type: PileType): CardPile {
        return when (type) {
            is PileType.Stock -> stockPile
            is PileType.Waste -> wastePile
            is PileType.Foundation -> foundationPiles[type.index]
            is PileType.Tableau -> tableauPiles[type.index]
        }
    }
}

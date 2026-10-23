package com.paciencia.solitaire.model

/**
 * Registra uma ação executada pelo jogador para suportar a funcionalidade de Desfazer (Undo).
 */
sealed class MoveAction {
    /**
     * Movimentação de uma ou mais cartas entre pilhas.
     * @param from Pilha de origem.
     * @param to Pilha de destino.
     * @param cards Lista de cartas movidas (preservando ordem).
     * @param flippedCardBelow Se o movimento resultou em revelar a carta que estava logo abaixo na pilha de origem.
     */
    data class CardsMoved(
        val from: PileType,
        val to: PileType,
        val cards: List<Card>,
        val flippedCardBelow: Boolean
    ) : MoveAction()

    /**
     * Compra de uma carta do estoque para o descarte.
     */
    data class StockDrawn(
        val card: Card
    ) : MoveAction()

    /**
     * Reciclagem de todas as cartas do descarte de volta para o monte de compras.
     */
    data class WasteRecycled(
        val cards: List<Card>
    ) : MoveAction()
}

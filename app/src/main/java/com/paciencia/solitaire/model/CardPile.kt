package com.paciencia.solitaire.model

/**
 * Representa uma pilha de cartas em uma posição da mesa de jogo.
 */
class CardPile(val type: PileType) {
    private val cardList = mutableListOf<Card>()

    val cards: List<Card>
        get() = cardList

    val size: Int
        get() = cardList.size

    val isEmpty: Boolean
        get() = cardList.isEmpty()

    val isNotEmpty: Boolean
        get() = cardList.isNotEmpty()

    fun top(): Card? = cardList.lastOrNull()

    fun push(card: Card) {
        cardList.add(card)
    }

    fun pushAll(cardsToAdd: List<Card>) {
        cardList.addAll(cardsToAdd)
    }

    fun pop(): Card? {
        return if (cardList.isNotEmpty()) cardList.removeAt(cardList.size - 1) else null
    }

    fun pop(count: Int): List<Card> {
        if (count <= 0 || cardList.isEmpty()) return emptyList()
        val safeCount = count.coerceAtMost(cardList.size)
        val startIndex = cardList.size - safeCount
        val sub = ArrayList(cardList.subList(startIndex, cardList.size))
        for (i in 0 until safeCount) {
            cardList.removeAt(cardList.size - 1)
        }
        return sub
    }

    operator fun get(index: Int): Card = cardList[index]

    fun indexOf(card: Card): Int = cardList.indexOf(card)

    fun cardsFrom(index: Int): List<Card> {
        if (index !in 0 until cardList.size) return emptyList()
        return cardList.subList(index, cardList.size)
    }

    fun clear() {
        cardList.clear()
    }

    fun clone(): CardPile {
        val newPile = CardPile(type)
        for (card in cardList) {
            newPile.push(card.clone())
        }
        return newPile
    }
}

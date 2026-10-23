package com.paciencia.solitaire.engine

import com.paciencia.solitaire.model.Card
import com.paciencia.solitaire.model.Rank
import com.paciencia.solitaire.model.Suit
import java.util.Random

/**
 * Gerador e distribuidor de baralhos de 52 cartas sem coringas.
 */
object DeckGenerator {

    /**
     * Cria um baralho padrão completo de 52 cartas.
     */
    fun createFullDeck(): List<Card> {
        val deck = ArrayList<Card>(52)
        var id = 0
        for (suit in Suit.entries) {
            for (rank in Rank.entries) {
                deck.add(Card(id = id++, suit = suit, rank = rank, isFaceUp = false))
            }
        }
        return deck
    }

    /**
     * Gera um baralho embaralhado com base no número do nível.
     * Utiliza uma semente determinística (seed) para que cada nível possua uma configuração inicial única e reprodutível.
     */
    fun createShuffledDeckForLevel(level: Int): List<Card> {
        val deck = ArrayList(createFullDeck())
        val seed = 10007L * level + 99991L
        val random = Random(seed)

        // Algoritmo Fisher-Yates de embaralhamento
        for (i in deck.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = deck[i]
            deck[i] = deck[j]
            deck[j] = temp
        }

        return deck
    }
}

package com.paciencia.solitaire

import com.paciencia.solitaire.engine.DeckGenerator
import com.paciencia.solitaire.engine.KlondikeEngine
import com.paciencia.solitaire.engine.MoveValidator
import com.paciencia.solitaire.model.Card
import com.paciencia.solitaire.model.CardPile
import com.paciencia.solitaire.model.PileType
import com.paciencia.solitaire.model.Rank
import com.paciencia.solitaire.model.Suit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bateria abrangente de testes unitários para o Klondike Solitaire.
 * Valida a integridade estrita das 52 cartas, regras de movimentação,
 * ciclo do monte/descarte, desfazer jogadas e condição de vitória.
 */
class KlondikeEngineTest {

    @Test
    fun testDeckGeneration52CardsIntegrity() {
        val deck = DeckGenerator.createFullDeck()
        assertEquals("O baralho deve conter exatamente 52 cartas", 52, deck.size)

        // Verifica que não há IDs duplicados
        val uniqueIds = deck.map { it.id }.toSet()
        assertEquals("Todos os IDs de cartas devem ser únicos", 52, uniqueIds.size)

        // Verifica que cada um dos 4 naipes possui exatamente 13 cartas
        for (suit in Suit.entries) {
            val suitCards = deck.filter { it.suit == suit }
            assertEquals("Cada naipe deve conter exatamente 13 cartas", 13, suitCards.size)

            val ranksInSuit = suitCards.map { it.rank }.toSet()
            assertEquals("Cada naipe deve conter todos os 13 postos (Rank)", 13, ranksInSuit.size)
        }
    }

    @Test
    fun testInitialGameDistributionAndIntegrity() {
        val engine = KlondikeEngine()
        engine.startNewGame(level = 1)

        // Valida contagem das 7 colunas de Tableau (1 + 2 + 3 + 4 + 5 + 6 + 7 = 28)
        var totalTableauCards = 0
        for (col in 0 until 7) {
            val pile = engine.tableauPiles[col]
            val expectedCount = col + 1
            assertEquals("Coluna $col deve ter $expectedCount cartas", expectedCount, pile.size)
            totalTableauCards += pile.size

            // Todas as cartas abaixo devem ser face-down, apenas a do topo deve ser face-up
            val cards = pile.cards
            for (idx in 0 until cards.size - 1) {
                assertFalse("Carta interna na coluna $col índice $idx deve estar virada para baixo", cards[idx].isFaceUp)
            }
            assertTrue("Carta do topo na coluna $col deve estar virada para cima", cards.last().isFaceUp)
        }
        assertEquals("Total no tableau deve ser 28 cartas", 28, totalTableauCards)

        // Valida monte de compras (Stock)
        assertEquals("Stock inicial deve conter exatamente 24 cartas", 24, engine.stockPile.size)
        assertTrue("Todas as cartas do Stock devem iniciar viradas para baixo", engine.stockPile.cards.all { !it.isFaceUp })

        // Valida descarte e fundações iniciais
        assertTrue("Waste inicial deve estar vazio", engine.wastePile.isEmpty)
        for (i in 0 until 4) {
            assertTrue("Fundação $i inicial deve estar vazia", engine.foundationPiles[i].isEmpty)
        }

        // Valida a conservação estrita das 52 cartas no jogo
        val allCardsInGame = mutableListOf<Card>()
        allCardsInGame.addAll(engine.stockPile.cards)
        allCardsInGame.addAll(engine.wastePile.cards)
        engine.foundationPiles.forEach { allCardsInGame.addAll(it.cards) }
        engine.tableauPiles.forEach { allCardsInGame.addAll(it.cards) }

        assertEquals("Total de cartas em jogo deve ser exatamente 52", 52, allCardsInGame.size)
        assertEquals("Nenhuma carta pode ser duplicada", 52, allCardsInGame.map { it.id }.toSet().size)
    }

    @Test
    fun testStockDrawAndRecycleIntegrity() {
        val engine = KlondikeEngine()
        engine.startNewGame(level = 1)

        // Compra as 24 cartas do Stock uma a uma
        for (i in 1..24) {
            val drew = engine.drawOrRecycleStock()
            assertTrue("Deveria conseguir comprar a carta $i do estoque", drew)
            assertEquals("Waste deve conter $i cartas", i, engine.wastePile.size)
            assertEquals("Stock deve conter ${24 - i} cartas", 24 - i, engine.stockPile.size)
            assertTrue("Carta no topo do waste deve estar virada para cima", engine.wastePile.top()!!.isFaceUp)
        }

        // Agora o Stock está vazio e o Waste tem 24 cartas
        assertTrue("Stock deve estar vazio após 24 compras", engine.stockPile.isEmpty)
        assertEquals("Waste deve ter 24 cartas", 24, engine.wastePile.size)

        // 25ª ação: reciclagem do Waste de volta para o Stock
        val recycled = engine.drawOrRecycleStock()
        assertTrue("Reciclagem deve ter sucesso", recycled)
        assertEquals("Stock deve voltar a ter 24 cartas após reciclagem", 24, engine.stockPile.size)
        assertTrue("Waste deve estar vazio após reciclagem", engine.wastePile.isEmpty)
        assertTrue("Todas as cartas recicladas no Stock devem estar viradas para baixo", engine.stockPile.cards.all { !it.isFaceUp })
    }

    @Test
    fun testMoveValidatorTableauAlternatingColorsAndDescendingRank() {
        val destinationPile = CardPile(PileType.Tableau(0))
        val blackKing = Card(1, Suit.SPADES, Rank.KING, isFaceUp = true)
        val redQueen = Card(2, Suit.HEARTS, Rank.QUEEN, isFaceUp = true)
        val blackQueen = Card(3, Suit.CLUBS, Rank.QUEEN, isFaceUp = true)
        val redJack = Card(4, Suit.DIAMONDS, Rank.JACK, isFaceUp = true)

        destinationPile.push(blackKing)

        // Dama Vermelha sobre Rei Preto: Válido (cores alternadas, decrescente)
        assertTrue("Dama Vermelha sobre Rei Preto deve ser válido",
            MoveValidator.canMoveToTableau(listOf(redQueen), destinationPile))

        // Dama Preta sobre Rei Preto: Inválido (mesma cor)
        assertFalse("Dama Preta sobre Rei Preto deve ser inválido",
            MoveValidator.canMoveToTableau(listOf(blackQueen), destinationPile))

        // Valete Vermelho sobre Rei Preto: Inválido (salto de ordem)
        assertFalse("Valete Vermelho sobre Rei Preto deve ser inválido",
            MoveValidator.canMoveToTableau(listOf(redJack), destinationPile))

        // Coluna vazia
        val emptyPile = CardPile(PileType.Tableau(1))
        assertTrue("Rei deve poder iniciar coluna vazia",
            MoveValidator.canMoveToTableau(listOf(blackKing), emptyPile))
        assertFalse("Carta que não seja Rei não deve poder iniciar coluna vazia",
            MoveValidator.canMoveToTableau(listOf(redQueen), emptyPile))
    }

    @Test
    fun testMoveValidatorFoundationsAscendingRankSameSuit() {
        val foundation = CardPile(PileType.Foundation(0))
        val aceSpades = Card(10, Suit.SPADES, Rank.ACE, isFaceUp = true)
        val twoSpades = Card(11, Suit.SPADES, Rank.TWO, isFaceUp = true)
        val twoHearts = Card(12, Suit.HEARTS, Rank.TWO, isFaceUp = true)
        val threeSpades = Card(13, Suit.SPADES, Rank.THREE, isFaceUp = true)

        // Fundação vazia: apenas Ás
        assertTrue("Ás de Espadas deve ser aceito na fundação vazia",
            MoveValidator.canMoveToFoundation(aceSpades, foundation))
        assertFalse("2 de Espadas não pode entrar na fundação vazia",
            MoveValidator.canMoveToFoundation(twoSpades, foundation))

        // Adiciona o Ás
        foundation.push(aceSpades)

        // 2 de Espadas sobre Ás de Espadas: Válido
        assertTrue("2 de Espadas sobre Ás de Espadas deve ser válido",
            MoveValidator.canMoveToFoundation(twoSpades, foundation))

        // 2 de Copas sobre Ás de Espadas: Inválido (naipe diferente)
        assertFalse("2 de Copas sobre Ás de Espadas deve ser inválido",
            MoveValidator.canMoveToFoundation(twoHearts, foundation))

        // 3 de Espadas sobre Ás de Espadas: Inválido (salto)
        assertFalse("3 de Espadas sobre Ás de Espadas deve ser inválido",
            MoveValidator.canMoveToFoundation(threeSpades, foundation))

        // Movimento de múltiplas cartas para fundação: proibido
        assertFalse("Não é permitido mover múltiplas cartas para a fundação",
            MoveValidator.isValidMove(listOf(twoSpades, threeSpades), PileType.Tableau(0), foundation))
    }

    @Test
    fun testCardMovementAndUndoConservation() {
        val engine = KlondikeEngine()
        val tableau0 = engine.tableauPiles[0]
        val tableau1 = engine.tableauPiles[1]

        tableau0.clear()
        tableau1.clear()

        val blackKing = Card(1, Suit.SPADES, Rank.KING, isFaceUp = true)
        val redQueen = Card(2, Suit.HEARTS, Rank.QUEEN, isFaceUp = true)

        tableau0.push(blackKing)
        tableau1.push(redQueen)

        // Move a Dama Vermelha de tableau1 para tableau0
        val moved = engine.moveCards(tableau1.type, tableau0.type, 1)
        assertTrue("Movimento válido deve retornar true", moved)
        assertEquals("Tableau 0 deve ter 2 cartas", 2, tableau0.size)
        assertEquals("Tableau 1 deve ficar vazio", 0, tableau1.size)
        assertEquals("Carta do topo de Tableau 0 deve ser a Dama Vermelha", redQueen, tableau0.top())

        // Executa Desfazer (Undo)
        val undone = engine.undo()
        assertTrue("Desfazer deve ter sucesso", undone)
        assertEquals("Tableau 0 deve voltar a ter 1 carta", 1, tableau0.size)
        assertEquals("Tableau 1 deve voltar a ter 1 carta", 1, tableau1.size)
        assertEquals("Topo de Tableau 0 deve ser o Rei Preto", blackKing, tableau0.top())
        assertEquals("Topo de Tableau 1 deve ser a Dama Vermelha", redQueen, tableau1.top())
    }

    @Test
    fun testAutoMovePrioritizesFoundations() {
        val engine = KlondikeEngine()
        engine.startNewGame(level = 1)

        val aceHearts = Card(100, Suit.HEARTS, Rank.ACE, isFaceUp = true)
        engine.wastePile.push(aceHearts)

        val autoMoved = engine.autoMove(aceHearts, PileType.Waste)
        assertTrue("AutoMove de um Ás deve ter sucesso para as fundações", autoMoved)
        assertTrue("Ás de Copas deve estar no topo de uma das fundações",
            engine.foundationPiles.any { it.top() == aceHearts })
    }

    @Test
    fun testWinConditionDetection() {
        val engine = KlondikeEngine()
        assertFalse("Jogo não vencido inicialmente", engine.isWon)

        // Preenche as 4 fundações com 13 cartas cada
        for (i in 0 until 4) {
            val suit = Suit.entries[i]
            for (rank in Rank.entries) {
                engine.foundationPiles[i].push(Card(i * 13 + rank.ordinal, suit, rank, isFaceUp = true))
            }
            assertEquals("Fundação $i deve ter 13 cartas", 13, engine.foundationPiles[i].size)
        }

        assertTrue("isWon deve ser true quando todas as 4 fundações tiverem 13 cartas", engine.isWon)
    }

    @Test
    fun testCardPileSafeBounds() {
        val pile = CardPile(PileType.Tableau(0))
        pile.push(Card(1, Suit.SPADES, Rank.ACE, isFaceUp = true))

        // Solicita pop de 5 cartas quando a pilha só tem 1 carta
        // Não deve lançar exceção devido ao coerceAtMost
        val popped = pile.pop(5)
        assertEquals("Deve desempilhar com segurança apenas a quantidade disponível", 1, popped.size)
        assertTrue("Pilha deve ficar vazia", pile.isEmpty)

        // Pop em pilha vazia
        val emptyPopped = pile.pop(2)
        assertTrue("Pop em pilha vazia retorna lista vazia sem erro", emptyPopped.isEmpty())
    }
}

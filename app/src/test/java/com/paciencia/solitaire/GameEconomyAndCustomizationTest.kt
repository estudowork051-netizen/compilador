package com.paciencia.solitaire

import com.paciencia.solitaire.customization.Avatar
import com.paciencia.solitaire.customization.CardSkin
import com.paciencia.solitaire.customization.SkinPattern
import com.paciencia.solitaire.data.GameEconomy
import com.paciencia.solitaire.model.Card
import com.paciencia.solitaire.model.Rank
import com.paciencia.solitaire.model.Suit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para a economia de moedas, catálogo de skins, avatares
 * e validação de regras de conclusão de sequências de colunas.
 */
class GameEconomyAndCustomizationTest {

    @Test
    fun testEconomyConstants() {
        assertEquals("Vitória deve conceder exatamente 10 moedas", 10, GameEconomy.COINS_PER_LEVEL_VICTORY)
        assertEquals("Vídeo recompensado deve conceder exatamente 3 moedas", 3, GameEconomy.COINS_PER_REWARDED_AD)
    }

    @Test
    fun testCardSkinsCatalogIntegrity() {
        val skins = CardSkin.ALL_SKINS
        assertEquals("Devem existir exatamente 8 skins no catálogo inicial", 8, skins.size)

        // Todos os IDs devem ser únicos
        val uniqueIds = skins.map { it.id }.toSet()
        assertEquals("Cada skin deve ter ID único", skins.size, uniqueIds.size)

        // Skin Clássica deve ser gratuita e desbloqueada por padrão
        val classic = CardSkin.getById("skin_classic")
        assertEquals(0, classic.price)
        assertTrue(classic.isDefaultUnlocked)
        assertEquals(SkinPattern.CLASSIC, classic.pattern)

        // As demais skins devem ter preços maiores que zero
        for (skin in skins) {
            if (skin.id != "skin_classic") {
                assertTrue("Skin ${skin.name} deve ter preço positivo", skin.price > 0)
                assertFalse("Skin ${skin.name} não deve vir desbloqueada por padrão", skin.isDefaultUnlocked)
            }
        }
    }

    @Test
    fun testAvatarsCatalogIntegrity() {
        val avatars = Avatar.ALL_AVATARS
        assertEquals("Devem existir exatamente 8 avatares no catálogo", 8, avatars.size)

        // Todos os IDs devem ser únicos
        val uniqueIds = avatars.map { it.id }.toSet()
        assertEquals("Cada avatar deve ter ID único", avatars.size, uniqueIds.size)

        // Avatar Aventureiro deve ser gratuito e desbloqueado por padrão
        val adventurer = Avatar.getById("avatar_adventurer")
        assertEquals(0, adventurer.price)
        assertTrue(adventurer.isDefaultUnlocked)
        assertEquals("🤠", adventurer.iconEmoji)

        // Os demais avatares devem ter preços maiores que zero
        for (avatar in avatars) {
            if (avatar.id != "avatar_adventurer") {
                assertTrue("Avatar ${avatar.name} deve ter preço positivo", avatar.price > 0)
                assertFalse("Avatar ${avatar.name} não deve vir desbloqueado por padrão", avatar.isDefaultUnlocked)
            }
        }
    }

    @Test
    fun testEconomyBalanceOperations() {
        var balance = 0

        // Simula vitória na fase 1: +10 moedas
        balance += GameEconomy.COINS_PER_LEVEL_VICTORY
        assertEquals(10, balance)

        // Simula visualização de 2 vídeos recompensados: +3 cada
        balance += GameEconomy.COINS_PER_REWARDED_AD
        balance += GameEconomy.COINS_PER_REWARDED_AD
        assertEquals(16, balance)

        // Tentativa de compra de item de 30 moedas com saldo insuficiente (16)
        val canBuyDourada = balance >= CardSkin.GOLDEN.price
        assertFalse("Não deve permitir compra com saldo insuficiente", canBuyDourada)
        assertEquals(16, balance) // Saldo deve permanecer inalterado

        // Acumula mais moedas até permitir a compra
        balance += GameEconomy.COINS_PER_LEVEL_VICTORY * 2 // +20 = 36
        assertTrue("Deve permitir compra quando saldo >= preço", balance >= CardSkin.GOLDEN.price)

        // Desconta as moedas da compra
        balance -= CardSkin.GOLDEN.price
        assertEquals(6, balance) // 36 - 30 = 6
    }

    @Test
    fun testColumnSequenceCompletionLogic() {
        // Sequência completa: Rei de Espadas até Ás de Ouros/Copas
        val completedSeq = mutableListOf<Card>()
        val ranks = listOf(
            Rank.KING, Rank.QUEEN, Rank.JACK, Rank.TEN, Rank.NINE,
            Rank.EIGHT, Rank.SEVEN, Rank.SIX, Rank.FIVE, Rank.FOUR,
            Rank.THREE, Rank.TWO, Rank.ACE
        )
        for (i in ranks.indices) {
            val suit = if (i % 2 == 0) Suit.SPADES else Suit.HEARTS
            completedSeq.add(Card(i, suit, ranks[i], isFaceUp = true))
        }

        // Verifica a lógica de coluna completa
        var isValidRun = true
        for (i in 0 until completedSeq.size - 1) {
            if (!completedSeq[i + 1].rank.isOneBelow(completedSeq[i].rank) ||
                completedSeq[i + 1].isRed == completedSeq[i].isRed) {
                isValidRun = false
                break
            }
        }
        assertTrue("A sequência ordenada deve ser identificada como válida", isValidRun)
        assertEquals(Rank.ACE, completedSeq.last().rank)
    }
}

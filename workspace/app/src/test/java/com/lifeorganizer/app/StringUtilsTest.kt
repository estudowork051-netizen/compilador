package com.lifeorganizer.app

import com.lifeorganizer.app.core.utils.StringUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StringUtilsTest {

    @Test
    fun testRemoveAccents() {
        assertEquals("exercicio", StringUtils.removeAccents("exercício"))
        assertEquals("louca", StringUtils.removeAccents("louça"))
        assertEquals("Atencao", StringUtils.removeAccents("Atenção"))
        assertEquals("habito", StringUtils.removeAccents("hábito"))
        assertEquals("manha", StringUtils.removeAccents("manhã"))
        assertEquals("reuniao", StringUtils.removeAccents("reunião"))
    }

    @Test
    fun testFuzzyContains() {
        // Query without accents matching target with accents
        assertTrue(StringUtils.fuzzyContains("Fazer exercícios", "exercicio"))
        assertTrue(StringUtils.fuzzyContains("Lavar louça", "louca"))
        assertTrue(StringUtils.fuzzyContains("Reunião de equipe", "reuniao"))

        // Case insensitivity
        assertTrue(StringUtils.fuzzyContains("ESTUDAR KOTLIN", "estudar"))
        assertTrue(StringUtils.fuzzyContains("estudar kotlin", "ESTUDAR"))

        // Partial matching
        assertTrue(StringUtils.fuzzyContains("Lavar louça", "lav"))
        assertTrue(StringUtils.fuzzyContains("Limpar quarto", "quar"))

        // Non matching
        assertFalse(StringUtils.fuzzyContains("Cozinhar", "treinar"))
    }
}

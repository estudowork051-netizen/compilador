package com.lifeorganizer.app

import com.lifeorganizer.app.core.utils.ContextExtractor
import com.lifeorganizer.app.domain.model.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ContextExtractorTest {

    @Test
    fun testExtract_seriesWithSeasonAndEpisode() {
        val result = ContextExtractor.extract("Breaking Bad S02E05", "", "Lazer")
        assertNotNull(result)
        assertEquals(ContentType.SERIES, result!!.contentType)
        assertEquals("Breaking Bad", result.contentName)
        assertEquals("Temporada 2 Episódio 5", result.progressLabel)
        assertEquals(5, result.currentUnit)
    }

    @Test
    fun testExtract_seriesWithEpKeyword() {
        val result = ContextExtractor.extract("Assistir Stranger Things Ep. 4", "", "Lazer")
        assertNotNull(result)
        assertEquals(ContentType.SERIES, result!!.contentType)
        assertEquals("Stranger Things", result.contentName)
        assertEquals("Episódio 4", result.progressLabel)
        assertEquals(4, result.currentUnit)
    }

    @Test
    fun testExtract_bookWithChapter() {
        val result = ContextExtractor.extract("Ler O Senhor dos Anéis Cap. 12", "", "Lazer")
        assertNotNull(result)
        assertEquals(ContentType.BOOK, result!!.contentType)
        assertEquals("O Senhor dos Anéis", result.contentName)
        assertEquals("Capítulo 12", result.progressLabel)
        assertEquals(12, result.currentUnit)
    }

    @Test
    fun testExtract_studyWithModule() {
        val result = ContextExtractor.extract("Curso de Kotlin Módulo 3", "Avançando em corrotinas", "Estudos")
        assertNotNull(result)
        assertEquals(ContentType.STUDY_COURSE, result!!.contentType)
        assertEquals("Curso de Kotlin", result.contentName)
        assertEquals("Módulo 3", result.progressLabel)
        assertEquals(3, result.currentUnit)
    }

    @Test
    fun testExtract_workout() {
        val result = ContextExtractor.extract("Treino de Peito e Tríceps", "", "Saúde")
        assertNotNull(result)
        assertEquals(ContentType.WORKOUT, result!!.contentType)
        assertEquals("Peito e Tríceps", result.contentName)
    }

    @Test
    fun testExtract_chores() {
        val result = ContextExtractor.extract("Lavar louça da cozinha", "", "Casa")
        assertNotNull(result)
        assertEquals(ContentType.CHORE, result!!.contentType)
        assertEquals("Lavar a louça", result.contentName)
    }

    @Test
    fun testExtract_genericTaskReturnsNull() {
        val result = ContextExtractor.extract("Fazer algo qualquer", "", "Outros")
        assertNull(result)
    }
}

package com.lifeorganizer.app

import com.lifeorganizer.app.core.utils.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DateTimeUtilsTest {

    @Test
    fun testGreetingMorning() {
        assertEquals("Bom dia", DateTimeUtils.getGreeting(5))
        assertEquals("Bom dia", DateTimeUtils.getGreeting(8))
        assertEquals("Bom dia", DateTimeUtils.getGreeting(11))
    }

    @Test
    fun testGreetingAfternoon() {
        assertEquals("Boa tarde", DateTimeUtils.getGreeting(12))
        assertEquals("Boa tarde", DateTimeUtils.getGreeting(15))
        assertEquals("Boa tarde", DateTimeUtils.getGreeting(17))
    }

    @Test
    fun testGreetingNight() {
        assertEquals("Boa noite", DateTimeUtils.getGreeting(18))
        assertEquals("Boa noite", DateTimeUtils.getGreeting(23))
        assertEquals("Boa noite", DateTimeUtils.getGreeting(0))
        assertEquals("Boa noite", DateTimeUtils.getGreeting(4))
    }

    @Test
    fun testFormatCurrentDateInPortuguese() {
        // Thursday, Nov 6, 2025
        val date = LocalDate.of(2025, 11, 6)
        val formatted = DateTimeUtils.formatCurrentDate(date)
        assertTrue("Formatted date should contain 'novembro': $formatted", formatted.contains("novembro", ignoreCase = true))
        assertTrue("Formatted date should start with capital letter", formatted[0].isUpperCase())
    }

    @Test
    fun testFormatDisplayDateTodayTomorrowYesterday() {
        val today = LocalDate.of(2025, 11, 6)
        val tomorrow = today.plusDays(1)
        val yesterday = today.minusDays(1)

        val displayToday = DateTimeUtils.formatDisplayDate(today, referenceDate = today)
        assertTrue(displayToday.startsWith("Hoje"))

        val displayTomorrow = DateTimeUtils.formatDisplayDate(tomorrow, referenceDate = today)
        assertTrue(displayTomorrow.startsWith("Amanhã"))

        val displayYesterday = DateTimeUtils.formatDisplayDate(yesterday, referenceDate = today)
        assertTrue(displayYesterday.startsWith("Ontem"))
    }

    @Test
    fun testParseTimeValid() {
        val time = DateTimeUtils.parseTime("08:30")
        assertEquals(8, time.hour)
        assertEquals(30, time.minute)
    }

    @Test
    fun testParseTimeFallback() {
        val fallback = LocalTime.of(8, 0)
        val invalidTime = DateTimeUtils.parseTime("invalid", fallback)
        assertEquals(fallback, invalidTime)
    }

    @Test
    fun testFormatTime() {
        assertEquals("08:00", DateTimeUtils.formatTime(8, 0))
        assertEquals("23:45", DateTimeUtils.formatTime(23, 45))
        assertEquals("00:05", DateTimeUtils.formatTime(0, 5))
    }
}

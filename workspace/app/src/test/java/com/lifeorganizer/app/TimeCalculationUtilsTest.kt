package com.lifeorganizer.app

import com.lifeorganizer.app.core.utils.TimeCalculationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class TimeCalculationUtilsTest {

    @Test
    fun testAwakeCalculationSameDay() {
        // Wake 08:00, Sleep 23:00 -> 15 hours = 900 minutes
        val wake = LocalTime.of(8, 0)
        val sleep = LocalTime.of(23, 0)
        val minutes = TimeCalculationUtils.calculateAwakeDurationMinutes(wake, sleep)
        assertEquals(900L, minutes)
        assertEquals("15h", TimeCalculationUtils.formatMinutesToHoursAndMinutes(minutes))
    }

    @Test
    fun testAwakeCalculationMidnightCrossing() {
        // Wake 08:00, Sleep 00:00 -> 16 hours = 960 minutes
        val wake = LocalTime.of(8, 0)
        val sleep = LocalTime.of(0, 0)
        val minutes = TimeCalculationUtils.calculateAwakeDurationMinutes(wake, sleep)
        assertEquals(960L, minutes)
        assertEquals("16h", TimeCalculationUtils.formatMinutesToHoursAndMinutes(minutes))
    }

    @Test
    fun testAwakeCalculationPastMidnight() {
        // Wake 08:00, Sleep 01:00 -> 17 hours = 1020 minutes
        val wake = LocalTime.of(8, 0)
        val sleep = LocalTime.of(1, 0)
        val minutes = TimeCalculationUtils.calculateAwakeDurationMinutes(wake, sleep)
        assertEquals(1020L, minutes)
        assertEquals("17h", TimeCalculationUtils.formatMinutesToHoursAndMinutes(minutes))
    }

    @Test
    fun testAwakeCalculationNightShift() {
        // Night worker: Wake 22:00, Sleep 06:00 next morning -> 8 hours = 480 minutes
        val wake = LocalTime.of(22, 0)
        val sleep = LocalTime.of(6, 0)
        val minutes = TimeCalculationUtils.calculateAwakeDurationMinutes(wake, sleep)
        assertEquals(480L, minutes)
        assertEquals("8h", TimeCalculationUtils.formatMinutesToHoursAndMinutes(minutes))
    }

    @Test
    fun testAwakeCalculationWithMinutes() {
        // Wake 07:30, Sleep 23:45 -> 16 hours and 15 minutes = 975 minutes
        val wake = LocalTime.of(7, 30)
        val sleep = LocalTime.of(23, 45)
        val minutes = TimeCalculationUtils.calculateAwakeDurationMinutes(wake, sleep)
        assertEquals(975L, minutes)
        assertEquals("16h 15min", TimeCalculationUtils.formatMinutesToHoursAndMinutes(minutes))
    }

    @Test
    fun testTimeFormatValidation() {
        assertTrue(TimeCalculationUtils.isValidTimeFormat("08:00"))
        assertTrue(TimeCalculationUtils.isValidTimeFormat("23:59"))
        assertTrue(TimeCalculationUtils.isValidTimeFormat("00:00"))
        assertFalse(TimeCalculationUtils.isValidTimeFormat("24:00"))
        assertFalse(TimeCalculationUtils.isValidTimeFormat("08:60"))
        assertFalse(TimeCalculationUtils.isValidTimeFormat("invalid"))
        assertFalse(TimeCalculationUtils.isValidTimeFormat("8:0"))
    }
}

package com.lifeorganizer.app

import com.lifeorganizer.app.core.utils.ScheduleReorderEngine
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleReorderEngineTest {

    private fun createEntry(id: Long, title: String, startH: Int, startM: Int, duration: Int): ScheduleEntryModel {
        val startTotal = startH * 60 + startM
        val endTotal = startTotal + duration
        val activity = ActivityModel(
            id = id,
            title = title,
            category = "Geral",
            type = ActivityType.FLEXIBLE,
            estimatedDurationMinutes = duration
        )
        return ScheduleEntryModel(
            id = id,
            activityId = id,
            activity = activity,
            date = "2026-11-06",
            startHour = startH,
            startMinute = startM,
            endHour = (endTotal / 60) % 24,
            endMinute = endTotal % 60,
            targetDurationMinutes = duration,
            status = ScheduleStatus.PENDING
        )
    }

    private fun createFixed(id: Long, title: String, startH: Int, startM: Int, endH: Int, endM: Int): ActivityModel {
        return ActivityModel(
            id = id,
            title = title,
            category = "Trabalho",
            type = ActivityType.FIXED,
            startHour = startH,
            startMinute = startM,
            endHour = endH,
            endMinute = endM,
            startTime = "%02d:%02d".format(startH, startM),
            endTime = "%02d:%02d".format(endH, endM),
            estimatedDurationMinutes = (endH * 60 + endM) - (startH * 60 + startM)
        )
    }

    @Test
    fun reorderAndRecalculate_reordersCorrectlyAndRecalculatesTimes() {
        val task1 = createEntry(1, "Ler livro", 9, 0, 30)
        val task2 = createEntry(2, "Exercício", 9, 35, 45)
        val task3 = createEntry(3, "Organizar mesa", 10, 25, 20)

        val list = listOf(task1, task2, task3)

        // Move task3 (index 2) to first position (index 0)
        val result = ScheduleReorderEngine.reorderAndRecalculate(
            entries = list,
            fromIndex = 2,
            toIndex = 0,
            fixedCommitments = emptyList(),
            wakeUpTimeMinutes = 8 * 60,
            sleepTimeMinutes = 23 * 60
        )

        assertEquals(3, result.updatedEntries.size)
        // First should now be task3
        assertEquals("Organizar mesa", result.updatedEntries[0].activity?.title)
        assertEquals(9, result.updatedEntries[0].startHour)
        assertEquals(0, result.updatedEntries[0].startMinute)
        assertEquals(9, result.updatedEntries[0].endHour)
        assertEquals(20, result.updatedEntries[0].endMinute)

        // Second should be task1 (starts at 9:20 + 5 min buffer = 9:25)
        assertEquals("Ler livro", result.updatedEntries[1].activity?.title)
        assertEquals(9, result.updatedEntries[1].startHour)
        assertEquals(25, result.updatedEntries[1].startMinute)

        // Third should be task2 (starts at 9:55 + 5 min buffer = 10:00)
        assertEquals("Exercício", result.updatedEntries[2].activity?.title)
        assertEquals(10, result.updatedEntries[2].startHour)
        assertEquals(0, result.updatedEntries[2].startMinute)

        assertNull(result.conflictWarning)
    }

    @Test
    fun reorderAndRecalculate_avoidsFixedCommitmentCollisions() {
        val task1 = createEntry(1, "Estudo", 9, 0, 60)
        val task2 = createEntry(2, "Pesquisa", 10, 5, 60)
        val list = listOf(task1, task2)

        // Fixed meeting from 10:00 to 11:30
        val fixedMeeting = createFixed(100, "Reunião de Equipe", 10, 0, 11, 30)

        val result = ScheduleReorderEngine.reorderAndRecalculate(
            entries = list,
            fromIndex = 0,
            toIndex = 1,
            fixedCommitments = listOf(fixedMeeting),
            wakeUpTimeMinutes = 8 * 60,
            sleepTimeMinutes = 23 * 60
        )

        assertEquals(2, result.updatedEntries.size)
        // task2 is now first, runs 9:00 to 10:00
        val first = result.updatedEntries[0]
        assertEquals("Pesquisa", first.activity?.title)
        assertEquals(9, first.startHour)
        assertEquals(0, first.startMinute)

        // task1 is pushed after the 10:00-11:30 fixed meeting
        val second = result.updatedEntries[1]
        assertEquals("Estudo", second.activity?.title)
        assertTrue(
            "Task should start at or after meeting end time (11:30)",
            second.startTotalMinutes >= 11 * 60 + 30
        )
    }

    @Test
    fun reorderAndRecalculate_emitsWarningWhenExceedingSleepTime() {
        // Late task extending past 22:00 bedtime
        val task1 = createEntry(1, "Série", 21, 0, 90)
        val task2 = createEntry(2, "Estudo longo", 22, 35, 120)
        val list = listOf(task1, task2)

        val result = ScheduleReorderEngine.reorderAndRecalculate(
            entries = list,
            fromIndex = 0,
            toIndex = 1,
            fixedCommitments = emptyList(),
            wakeUpTimeMinutes = 8 * 60,
            sleepTimeMinutes = 22 * 60
        )

        assertNotNull(result.conflictWarning)
        assertTrue(result.conflictWarning!!.contains("termina após seu horário de dormir"))
    }

    @Test
    fun reorderAndRecalculate_preservesCompletedTasksTimes() {
        val completedTask = createEntry(1, "Café e Leitura", 8, 30, 30).copy(status = ScheduleStatus.COMPLETED)
        val pendingTask1 = createEntry(2, "Estudo", 9, 5, 45)
        val pendingTask2 = createEntry(3, "Alongamento", 9, 55, 15)

        val list = listOf(completedTask, pendingTask1, pendingTask2)

        // Move pendingTask2 to middle (from 2 to 1)
        val result = ScheduleReorderEngine.reorderAndRecalculate(
            entries = list,
            fromIndex = 2,
            toIndex = 1,
            fixedCommitments = emptyList(),
            wakeUpTimeMinutes = 8 * 60,
            sleepTimeMinutes = 23 * 60
        )

        assertEquals(3, result.updatedEntries.size)
        // Completed task preserved
        val first = result.updatedEntries[0]
        assertEquals("Café e Leitura", first.activity?.title)
        assertEquals(8, first.startHour)
        assertEquals(30, first.startMinute)
        assertEquals(9, first.endHour)
        assertEquals(0, first.endMinute)
        assertEquals(ScheduleStatus.COMPLETED, first.status)

        // Moved task starts after completed task buffer (9:05)
        val second = result.updatedEntries[1]
        assertEquals("Alongamento", second.activity?.title)
        assertEquals(9, second.startHour)
        assertEquals(5, second.startMinute)
    }
}

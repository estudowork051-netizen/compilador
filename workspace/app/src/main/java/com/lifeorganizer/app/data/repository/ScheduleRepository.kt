package com.lifeorganizer.app.data.repository

import com.lifeorganizer.app.data.local.dao.ExecutionRecordDao
import com.lifeorganizer.app.data.local.dao.ScheduleEntryDao
import com.lifeorganizer.app.data.local.entity.ExecutionRecordEntity
import com.lifeorganizer.app.data.local.entity.ScheduleEntryEntity
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import com.lifeorganizer.app.data.local.dao.ActivityDao
import com.lifeorganizer.app.data.local.entity.toEntity

class ScheduleRepository(
    private val scheduleEntryDao: ScheduleEntryDao,
    private val executionRecordDao: ExecutionRecordDao,
    private val activityDao: ActivityDao? = null
) {

    fun getEntriesForDate(date: String): Flow<List<ScheduleEntryModel>> {
        return scheduleEntryDao.getEntriesForDate(date).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getEntriesForRange(startDate: String, endDate: String): Flow<List<ScheduleEntryModel>> {
        return scheduleEntryDao.getEntriesForRange(startDate, endDate).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getUncompletedEntries(): Flow<List<ScheduleEntryModel>> {
        return scheduleEntryDao.getUncompletedEntries().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getEntryById(id: Long): ScheduleEntryModel? {
        return scheduleEntryDao.getEntryById(id)?.toDomain()
    }

    suspend fun scheduleActivity(
        activity: ActivityModel,
        date: String,
        startHour: Int,
        startMinute: Int,
        durationMinutes: Int,
        notes: String = ""
    ): Long {
        val totalStart = startHour * 60 + startMinute
        val totalEnd = totalStart + durationMinutes
        val endHour = (totalEnd / 60) % 24
        val endMinute = totalEnd % 60

        // Ensure activity has a persistent row with a unique ID
        val effectiveActivityId = if (activity.id <= 0L || (activityDao != null && activityDao.getActivityById(activity.id) == null)) {
            if (activityDao != null) {
                activityDao.insertActivity(activity.copy(id = 0L).toEntity())
            } else {
                activity.id
            }
        } else {
            activity.id
        }

        val entity = ScheduleEntryEntity(
            activityId = effectiveActivityId,
            date = date,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            targetDurationMinutes = durationMinutes,
            status = ScheduleStatus.PENDING.name,
            notes = notes
        )
        return scheduleEntryDao.insertEntry(entity)
    }

    suspend fun insertEntry(entry: ScheduleEntryModel): Long {
        return scheduleEntryDao.insertEntry(ScheduleEntryEntity.fromDomain(entry))
    }

    suspend fun updateEntry(entry: ScheduleEntryModel) {
        scheduleEntryDao.updateEntry(ScheduleEntryEntity.fromDomain(entry))
    }

    suspend fun updateEntries(entries: List<ScheduleEntryModel>) {
        scheduleEntryDao.updateEntries(entries.map { ScheduleEntryEntity.fromDomain(it) })
    }

    suspend fun deleteEntry(id: Long) {
        scheduleEntryDao.deleteEntryById(id)
    }

    suspend fun updateStatus(
        id: Long,
        newStatus: ScheduleStatus,
        actualDurationMinutes: Int? = null,
        notes: String = ""
    ) {
        val completedAt = if (newStatus == ScheduleStatus.COMPLETED) System.currentTimeMillis() else null
        scheduleEntryDao.updateStatus(
            id = id,
            newStatus = newStatus.name,
            actualDurationMinutes = actualDurationMinutes,
            completedAt = completedAt,
            notes = notes
        )

        // Record execution event for persistent analytics & behavior learning
        val entry = scheduleEntryDao.getEntryById(id)
        if (entry != null && newStatus.isFinished) {
            val record = ExecutionRecordEntity(
                scheduleEntryId = entry.entry.id,
                activityId = entry.entry.activityId,
                activityTitle = entry.activity?.title ?: "Atividade",
                category = entry.activity?.category ?: "Geral",
                plannedDate = entry.entry.date,
                plannedStartMinutes = entry.entry.startHour * 60 + entry.entry.startMinute,
                plannedDurationMinutes = entry.entry.targetDurationMinutes,
                actualCompletedAt = completedAt,
                actualDurationMinutes = actualDurationMinutes,
                finalStatus = newStatus.name,
                timestamp = System.currentTimeMillis(),
                notes = notes
            )
            executionRecordDao.insertRecord(record)
        }
    }

    suspend fun markCompleted(
        entryId: Long,
        actualDurationMinutes: Int? = null,
        notes: String = ""
    ) {
        val existing = scheduleEntryDao.getEntryById(entryId)
        if (existing != null && existing.entry.status == ScheduleStatus.COMPLETED.name) {
            // Already completed, avoid duplicate completion and records
            return
        }
        updateStatus(
            id = entryId,
            newStatus = ScheduleStatus.COMPLETED,
            actualDurationMinutes = actualDurationMinutes,
            notes = notes
        )
    }

    suspend fun postponeEntry(
        id: Long,
        newDate: String? = null,
        notes: String = if (newDate != null) "Adiada para $newDate" else "Adiado em 15 minutos"
    ) {
        if (newDate != null) {
            val existing = scheduleEntryDao.getEntryById(id)
            if (existing != null) {
                // Log postponement in history
                val record = ExecutionRecordEntity(
                    scheduleEntryId = existing.entry.id,
                    activityId = existing.entry.activityId,
                    activityTitle = existing.activity?.title ?: "Atividade",
                    category = existing.activity?.category ?: "Geral",
                    plannedDate = existing.entry.date,
                    plannedStartMinutes = existing.entry.startHour * 60 + existing.entry.startMinute,
                    plannedDurationMinutes = existing.entry.targetDurationMinutes,
                    actualCompletedAt = null,
                    actualDurationMinutes = null,
                    finalStatus = ScheduleStatus.POSTPONED.name,
                    timestamp = System.currentTimeMillis(),
                    notes = notes
                )
                executionRecordDao.insertRecord(record)
            }
            scheduleEntryDao.postponeEntry(id, newDate, ScheduleStatus.PENDING.name)
        } else {
            postponeEntryByMinutes(id, 15, notes)
        }
    }

    suspend fun postponeEntryByMinutes(
        id: Long,
        minutes: Int = 15,
        notes: String = "Adiado em $minutes minutos"
    ) {
        val existing = scheduleEntryDao.getEntryById(id) ?: return
        val currentStartMin = existing.entry.startHour * 60 + existing.entry.startMinute
        val newStartMin = currentStartMin + minutes
        val newStartHour = (newStartMin / 60) % 24
        val newStartMinute = newStartMin % 60
        val duration = existing.entry.targetDurationMinutes
        val newEndMin = newStartMin + duration
        val newEndHour = (newEndMin / 60) % 24
        val newEndMinute = newEndMin % 60

        val updatedEntry = existing.entry.copy(
            startHour = newStartHour,
            startMinute = newStartMinute,
            endHour = newEndHour,
            endMinute = newEndMinute,
            status = ScheduleStatus.PENDING.name
        )
        scheduleEntryDao.updateEntry(updatedEntry)

        val record = ExecutionRecordEntity(
            scheduleEntryId = existing.entry.id,
            activityId = existing.entry.activityId,
            activityTitle = existing.activity?.title ?: "Atividade",
            category = existing.activity?.category ?: "Geral",
            plannedDate = existing.entry.date,
            plannedStartMinutes = currentStartMin,
            plannedDurationMinutes = duration,
            actualCompletedAt = null,
            actualDurationMinutes = null,
            finalStatus = ScheduleStatus.POSTPONED.name,
            timestamp = System.currentTimeMillis(),
            notes = notes
        )
        executionRecordDao.insertRecord(record)
    }

    fun getExecutionHistory(): Flow<List<ExecutionRecordModel>> {
        return executionRecordDao.getAllRecords().map { list ->
            list.map { it.toDomain() }
        }
    }
}

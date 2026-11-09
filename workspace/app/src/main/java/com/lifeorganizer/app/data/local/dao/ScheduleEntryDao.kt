package com.lifeorganizer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.lifeorganizer.app.data.local.entity.ScheduleEntryEntity
import com.lifeorganizer.app.data.local.entity.ScheduleEntryWithActivity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleEntryDao {

    @Transaction
    @Query("SELECT * FROM schedule_entries WHERE date = :date ORDER BY startHour ASC, startMinute ASC")
    fun getEntriesForDate(date: String): Flow<List<ScheduleEntryWithActivity>>

    @Transaction
    @Query("SELECT * FROM schedule_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, startHour ASC")
    fun getEntriesForRange(startDate: String, endDate: String): Flow<List<ScheduleEntryWithActivity>>

    @Transaction
    @Query("SELECT * FROM schedule_entries WHERE status IN ('PENDING', 'IN_PROGRESS') ORDER BY date ASC, startHour ASC")
    fun getUncompletedEntries(): Flow<List<ScheduleEntryWithActivity>>

    @Transaction
    @Query("SELECT * FROM schedule_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): ScheduleEntryWithActivity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: ScheduleEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: ScheduleEntryEntity)

    @Update
    suspend fun updateEntries(entries: List<ScheduleEntryEntity>)

    @Delete
    suspend fun deleteEntry(entry: ScheduleEntryEntity)

    @Query("DELETE FROM schedule_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("""
        UPDATE schedule_entries 
        SET status = :newStatus,
            actualDurationMinutes = :actualDurationMinutes,
            completedAt = :completedAt,
            notes = CASE WHEN :notes != '' THEN :notes ELSE notes END
        WHERE id = :id
    """)
    suspend fun updateStatus(
        id: Long,
        newStatus: String,
        actualDurationMinutes: Int? = null,
        completedAt: Long? = null,
        notes: String = ""
    )

    @Query("UPDATE schedule_entries SET date = :newDate, status = :newStatus, postponedToDate = :newDate WHERE id = :id")
    suspend fun postponeEntry(id: Long, newDate: String, newStatus: String = "PENDING")
}

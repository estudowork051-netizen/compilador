package com.lifeorganizer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifeorganizer.app.data.local.entity.ExecutionRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutionRecordDao {

    @Query("SELECT * FROM execution_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<ExecutionRecordEntity>>

    @Query("SELECT * FROM execution_records ORDER BY timestamp DESC")
    suspend fun getAllRecordsList(): List<ExecutionRecordEntity>

    @Query("SELECT * FROM execution_records WHERE activityId = :activityId ORDER BY timestamp DESC")
    fun getRecordsForActivity(activityId: Long): Flow<List<ExecutionRecordEntity>>

    @Query("SELECT * FROM execution_records WHERE activityId = :activityId ORDER BY timestamp DESC")
    suspend fun getRecordsForActivityList(activityId: Long): List<ExecutionRecordEntity>

    @Query("SELECT * FROM execution_records WHERE plannedDate = :date ORDER BY timestamp DESC")
    fun getRecordsForDate(date: String): Flow<List<ExecutionRecordEntity>>

    @Query("SELECT * FROM execution_records WHERE finalStatus = :status ORDER BY timestamp DESC")
    fun getRecordsByStatus(status: String): Flow<List<ExecutionRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ExecutionRecordEntity): Long

    @Query("SELECT COUNT(*) FROM execution_records WHERE activityId = :activityId AND finalStatus = 'COMPLETED'")
    suspend fun countCompletedForActivity(activityId: Long): Int

    @Query("SELECT COUNT(*) FROM execution_records WHERE activityId = :activityId AND finalStatus = 'NOT_DONE'")
    suspend fun countNotDoneForActivity(activityId: Long): Int

    @Query("DELETE FROM execution_records")
    suspend fun clearAllRecords()
}

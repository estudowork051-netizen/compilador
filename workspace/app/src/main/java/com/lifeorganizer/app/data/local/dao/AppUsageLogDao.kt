package com.lifeorganizer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifeorganizer.app.data.local.entity.AppUsageLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageLogDao {

    @Query("SELECT * FROM app_usage_logs WHERE windowStartMillis >= :sinceMillis ORDER BY windowStartMillis DESC")
    fun getLogsSinceFlow(sinceMillis: Long): Flow<List<AppUsageLogEntity>>

    @Query("SELECT * FROM app_usage_logs WHERE activityId = :activityId ORDER BY durationSeconds DESC")
    suspend fun getLogsForActivityOnce(activityId: Long): List<AppUsageLogEntity>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM app_usage_logs WHERE windowStartMillis >= :sinceMillis")
    fun getTotalMonitoredSecondsSinceFlow(sinceMillis: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<AppUsageLogEntity>)

    @Query("DELETE FROM app_usage_logs")
    suspend fun clearAll()
}

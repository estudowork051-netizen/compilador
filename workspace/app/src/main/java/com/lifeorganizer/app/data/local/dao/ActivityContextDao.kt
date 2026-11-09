package com.lifeorganizer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifeorganizer.app.data.local.entity.ActivityContextEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityContextDao {

    @Query("SELECT * FROM activity_contexts WHERE activityId = :activityId LIMIT 1")
    fun getContextForActivity(activityId: Long): Flow<ActivityContextEntity?>

    @Query("SELECT * FROM activity_contexts WHERE activityId = :activityId LIMIT 1")
    suspend fun getContextForActivityOnce(activityId: Long): ActivityContextEntity?

    @Query("SELECT * FROM activity_contexts WHERE continuityStatus = 'IN_PROGRESS' ORDER BY lastSessionAt DESC")
    fun getAllActiveContexts(): Flow<List<ActivityContextEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(context: ActivityContextEntity): Long

    @Update
    suspend fun update(context: ActivityContextEntity)

    @Delete
    suspend fun delete(context: ActivityContextEntity)

    @Query("UPDATE activity_contexts SET currentProgress = :progress, currentUnitValue = :unitVal, lastSessionAt = :timestamp WHERE activityId = :activityId")
    suspend fun updateProgress(activityId: Long, progress: String, unitVal: Int?, timestamp: Long = System.currentTimeMillis())
}

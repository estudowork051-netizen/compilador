package com.lifeorganizer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifeorganizer.app.data.local.entity.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    @Query("SELECT * FROM activities ORDER BY updatedAt DESC")
    fun getAllActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE type = :type ORDER BY updatedAt DESC")
    fun getActivitiesByType(type: String): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE category = :category ORDER BY title ASC")
    fun getActivitiesByCategory(category: String): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getActivityById(id: Long): ActivityEntity?

    @Query("SELECT * FROM activities WHERE title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchActivities(query: String): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentActivities(limit: Int = 5): Flow<List<ActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityEntity>): List<Long>

    @Update
    suspend fun updateActivity(activity: ActivityEntity)

    @Delete
    suspend fun deleteActivity(activity: ActivityEntity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivityById(id: Long)
}

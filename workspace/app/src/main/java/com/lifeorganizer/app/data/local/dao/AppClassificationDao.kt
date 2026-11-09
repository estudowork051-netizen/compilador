package com.lifeorganizer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifeorganizer.app.data.local.entity.AppClassificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppClassificationDao {

    @Query("SELECT * FROM app_classifications ORDER BY appName ASC")
    fun getAllClassificationsFlow(): Flow<List<AppClassificationEntity>>

    @Query("SELECT * FROM app_classifications WHERE packageName = :packageName LIMIT 1")
    suspend fun getClassificationForPackage(packageName: String): AppClassificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassification(entity: AppClassificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassifications(entities: List<AppClassificationEntity>)

    @Query("DELETE FROM app_classifications")
    suspend fun clearAll()
}

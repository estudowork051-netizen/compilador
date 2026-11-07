package com.lifeorganizer.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lifeorganizer.app.domain.model.AppCategoryClassification
import com.lifeorganizer.app.domain.model.AppUsageStatModel

@Entity(
    tableName = "app_usage_logs",
    indices = [
        Index(value = ["activityId"]),
        Index(value = ["windowStartMillis"]),
        Index(value = ["packageName"])
    ]
)
data class AppUsageLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val activityId: Long? = null,
    val packageName: String,
    val appName: String,
    val durationSeconds: Long,
    val windowStartMillis: Long,
    val windowEndMillis: Long,
    val classification: String = AppCategoryClassification.UNCLASSIFIED.name
) {
    fun toDomain(): AppUsageStatModel {
        return AppUsageStatModel(
            packageName = packageName,
            appName = appName,
            totalTimeInForegroundSeconds = durationSeconds,
            classification = AppCategoryClassification.fromString(classification),
            windowStartMillis = windowStartMillis,
            windowEndMillis = windowEndMillis
        )
    }
}

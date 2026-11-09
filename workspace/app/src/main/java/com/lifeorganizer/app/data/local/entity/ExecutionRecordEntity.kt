package com.lifeorganizer.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.ScheduleStatus

@Entity(
    tableName = "execution_records",
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["activityId"]),
        Index(value = ["plannedDate"]),
        Index(value = ["finalStatus"])
    ]
)
data class ExecutionRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleEntryId: Long,
    val activityId: Long,
    val activityTitle: String,
    val category: String,
    val plannedDate: String, // "YYYY-MM-DD"
    val plannedStartMinutes: Int,
    val plannedDurationMinutes: Int,
    val actualCompletedAt: Long? = null,
    val actualDurationMinutes: Int? = null,
    val finalStatus: String, // COMPLETED, NOT_DONE, POSTPONED, CANCELLED
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    fun toDomain(): ExecutionRecordModel {
        return ExecutionRecordModel(
            id = id,
            scheduleEntryId = scheduleEntryId,
            activityId = activityId,
            activityTitle = activityTitle,
            category = ActivityCategory.fromDisplayName(category),
            plannedDate = plannedDate,
            plannedStartMinutes = plannedStartMinutes,
            plannedDurationMinutes = plannedDurationMinutes,
            actualCompletedAt = actualCompletedAt,
            actualDurationMinutes = actualDurationMinutes,
            finalStatus = ScheduleStatus.fromString(finalStatus),
            timestamp = timestamp,
            notes = notes
        )
    }

    companion object {
        fun fromDomain(model: ExecutionRecordModel): ExecutionRecordEntity {
            return ExecutionRecordEntity(
                id = model.id,
                scheduleEntryId = model.scheduleEntryId,
                activityId = model.activityId,
                activityTitle = model.activityTitle,
                category = model.category.displayName,
                plannedDate = model.plannedDate,
                plannedStartMinutes = model.plannedStartMinutes,
                plannedDurationMinutes = model.plannedDurationMinutes,
                actualCompletedAt = model.actualCompletedAt,
                actualDurationMinutes = model.actualDurationMinutes,
                finalStatus = model.finalStatus.name,
                timestamp = model.timestamp,
                notes = model.notes
            )
        }
    }
}

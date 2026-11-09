package com.lifeorganizer.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.FocusSessionModel

@Entity(
    tableName = "focus_sessions",
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
        Index(value = ["scheduleEntryId"]),
        Index(value = ["isActive"])
    ]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val scheduleEntryId: Long? = null,
    val activityId: Long,
    val activityTitle: String,
    val category: String,
    val plannedDurationMinutes: Int,
    val startTimeMillis: Long,
    val elapsedSecondsBeforePause: Long = 0L,
    val pausedAtMillis: Long? = null,
    val isPaused: Boolean = false,
    val isActive: Boolean = true,
    val completedAtMillis: Long? = null,
    val notes: String = ""
) {
    fun toDomain(): FocusSessionModel {
        return FocusSessionModel(
            id = id,
            scheduleEntryId = scheduleEntryId,
            activityId = activityId,
            activityTitle = activityTitle,
            category = ActivityCategory.fromDisplayName(category),
            plannedDurationMinutes = plannedDurationMinutes,
            startTimeMillis = startTimeMillis,
            elapsedSecondsBeforePause = elapsedSecondsBeforePause,
            pausedAtMillis = pausedAtMillis,
            isPaused = isPaused,
            isActive = isActive,
            completedAtMillis = completedAtMillis,
            notes = notes
        )
    }

    companion object {
        fun fromDomain(model: FocusSessionModel): FocusSessionEntity {
            return FocusSessionEntity(
                id = model.id,
                scheduleEntryId = model.scheduleEntryId,
                activityId = model.activityId,
                activityTitle = model.activityTitle,
                category = model.category.displayName,
                plannedDurationMinutes = model.plannedDurationMinutes,
                startTimeMillis = model.startTimeMillis,
                elapsedSecondsBeforePause = model.elapsedSecondsBeforePause,
                pausedAtMillis = model.pausedAtMillis,
                isPaused = model.isPaused,
                isActive = model.isActive,
                completedAtMillis = model.completedAtMillis,
                notes = model.notes
            )
        }
    }
}

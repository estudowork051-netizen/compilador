package com.lifeorganizer.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus

@Entity(
    tableName = "schedule_entries",
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
        Index(value = ["date"]),
        Index(value = ["startAt"]),
        Index(value = ["status"])
    ]
)
data class ScheduleEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val activityId: Long,
    val date: String = "", // "YYYY-MM-DD"
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 10,
    val endMinute: Int = 0,
    val targetDurationMinutes: Int = 60,
    val actualDurationMinutes: Int? = null,
    val status: String = "PENDING", // PENDING, IN_PROGRESS, COMPLETED, POSTPONED, NOT_DONE, CANCELLED
    val completedAt: Long? = null,
    val notes: String = "",
    val postponedToDate: String? = null,
    val startAt: Long = 0L,
    val endAt: Long = 0L
) {
    fun toDomain(activity: ActivityEntity? = null): ScheduleEntryModel {
        val cat = activity?.let { ActivityCategory.fromDisplayName(it.category) } ?: ActivityCategory.OUTROS
        return ScheduleEntryModel(
            id = id,
            activityId = activityId,
            activity = activity?.toDomain(),
            activityTitle = activity?.title ?: "Atividade",
            category = cat,
            colorHex = activity?.colorHex ?: "#8B9CFF",
            date = date,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            targetDurationMinutes = targetDurationMinutes,
            actualDurationMinutes = actualDurationMinutes,
            status = ScheduleStatus.fromString(status),
            completedAt = completedAt,
            notes = notes,
            postponedToDate = postponedToDate,
            startAt = startAt,
            endAt = endAt
        )
    }

    companion object {
        fun fromDomain(model: ScheduleEntryModel): ScheduleEntryEntity {
            return ScheduleEntryEntity(
                id = model.id,
                activityId = model.activityId,
                date = model.date,
                startHour = model.startHour,
                startMinute = model.startMinute,
                endHour = model.endHour,
                endMinute = model.endMinute,
                targetDurationMinutes = model.targetDurationMinutes,
                actualDurationMinutes = model.actualDurationMinutes,
                status = model.status.name,
                completedAt = model.completedAt,
                notes = model.notes,
                postponedToDate = model.postponedToDate,
                startAt = model.startAt,
                endAt = model.endAt
            )
        }
    }
}

data class ScheduleEntryWithActivity(
    @Embedded
    val entry: ScheduleEntryEntity,

    @Relation(
        parentColumn = "activityId",
        entityColumn = "id"
    )
    val activity: ActivityEntity?
) {
    fun toDomain(): ScheduleEntryModel = entry.toDomain(activity)
}

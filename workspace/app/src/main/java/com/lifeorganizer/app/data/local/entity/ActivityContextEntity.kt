package com.lifeorganizer.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lifeorganizer.app.domain.model.ActivityContextModel
import com.lifeorganizer.app.domain.model.ContentType
import com.lifeorganizer.app.domain.model.ContinuityStatus

@Entity(
    tableName = "activity_contexts",
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
        Index(value = ["continuityStatus"])
    ]
)
data class ActivityContextEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val activityId: Long,
    val contentType: String = "GENERAL",
    val contentName: String,
    val currentProgress: String = "",
    val unitType: String = "",
    val currentUnitValue: Int? = null,
    val continuityStatus: String = "IN_PROGRESS",
    val lastSessionAt: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    fun toDomain(): ActivityContextModel {
        return ActivityContextModel(
            id = id,
            activityId = activityId,
            contentType = ContentType.fromString(contentType),
            contentName = contentName,
            currentProgress = currentProgress,
            unitType = unitType,
            currentUnitValue = currentUnitValue,
            continuityStatus = ContinuityStatus.fromString(continuityStatus),
            lastSessionAt = lastSessionAt,
            notes = notes
        )
    }

    companion object {
        fun fromDomain(model: ActivityContextModel): ActivityContextEntity {
            return ActivityContextEntity(
                id = model.id,
                activityId = model.activityId,
                contentType = model.contentType.name,
                contentName = model.contentName,
                currentProgress = model.currentProgress,
                unitType = model.unitType,
                currentUnitValue = model.currentUnitValue,
                continuityStatus = model.continuityStatus.name,
                lastSessionAt = model.lastSessionAt,
                notes = model.notes
            )
        }
    }
}

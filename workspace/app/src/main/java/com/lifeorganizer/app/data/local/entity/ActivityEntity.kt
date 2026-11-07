package com.lifeorganizer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityPriority
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.RecurrenceType

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String,
    val estimatedDurationMinutes: Int,
    val type: String = "FLEXIBLE",
    val defaultCount: Int = 1,
    val priority: String = "MEDIUM",
    val colorHex: String = "#8B9CFF",
    val iconName: String = "default",
    val description: String = "",
    val startHour: Int? = null,
    val startMinute: Int? = null,
    val endHour: Int? = null,
    val endMinute: Int? = null,
    val recurrenceType: String? = null,
    val recurrenceDays: String? = null,
    val weeklyTargetCount: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun ActivityEntity.toDomain(): ActivityModel {
    val daysList = recurrenceDays?.split(",")
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?: emptyList()

    return ActivityModel(
        id = id,
        title = title,
        category = category,
        estimatedDurationMinutes = estimatedDurationMinutes,
        type = ActivityType.fromString(type),
        defaultCount = defaultCount,
        priority = ActivityPriority.fromString(priority),
        colorHex = colorHex,
        iconName = iconName,
        description = description,
        startHour = startHour,
        startMinute = startMinute,
        endHour = endHour,
        endMinute = endMinute,
        recurrenceType = recurrenceType?.let { RecurrenceType.fromString(it) },
        recurrenceDays = daysList,
        weeklyTargetCount = weeklyTargetCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ActivityModel.toEntity(): ActivityEntity {
    val daysStr = if (recurrenceDays.isNotEmpty()) {
        recurrenceDays.joinToString(",")
    } else {
        null
    }

    return ActivityEntity(
        id = id,
        title = title.trim(),
        category = category.trim(),
        estimatedDurationMinutes = estimatedDurationMinutes.coerceAtLeast(1),
        type = type.name,
        defaultCount = defaultCount.coerceAtLeast(1),
        priority = priority.name,
        colorHex = colorHex,
        iconName = iconName,
        description = description.trim(),
        startHour = startHour,
        startMinute = startMinute,
        endHour = endHour,
        endMinute = endMinute,
        recurrenceType = recurrenceType?.name,
        recurrenceDays = daysStr,
        weeklyTargetCount = weeklyTargetCount,
        createdAt = if (createdAt > 0) createdAt else System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

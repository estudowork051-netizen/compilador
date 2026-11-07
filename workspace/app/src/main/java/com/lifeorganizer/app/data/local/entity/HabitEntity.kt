package com.lifeorganizer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetCount: Int,
    val period: String = "DAILY", // DAILY, WEEKLY
    val createdAt: Long = System.currentTimeMillis()
)

package com.lifeorganizer.app.data.local.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun dateToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }

    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }
}

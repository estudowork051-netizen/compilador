package com.lifeorganizer.app.core.utils

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {

    private val PT_BR = Locale("pt", "BR")

    fun getGreeting(hour: Int = LocalTime.now().hour): String {
        return when (hour) {
            in 5..11 -> "Bom dia"
            in 12..17 -> "Boa tarde"
            else -> "Boa noite"
        }
    }

    fun formatCurrentDate(date: LocalDate = LocalDate.now()): String {
        val formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT_BR)
        val formatted = date.format(formatter)
        return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(PT_BR) else it.toString() }
    }

    fun formatDayOfWeek(date: LocalDate = LocalDate.now()): String {
        val formatter = DateTimeFormatter.ofPattern("EEEE", PT_BR)
        val formatted = date.format(formatter)
        return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(PT_BR) else it.toString() }
    }

    fun formatDisplayDate(date: LocalDate, referenceDate: LocalDate = LocalDate.now()): String {
        return when (date) {
            referenceDate -> {
                val dayAndMonth = date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", PT_BR))
                "Hoje, $dayAndMonth"
            }
            referenceDate.plusDays(1) -> {
                val dayAndMonth = date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", PT_BR))
                "Amanhã, $dayAndMonth"
            }
            referenceDate.minusDays(1) -> {
                val dayAndMonth = date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", PT_BR))
                "Ontem, $dayAndMonth"
            }
            else -> {
                val formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMM", PT_BR)
                val formatted = date.format(formatter)
                formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(PT_BR) else it.toString() }
            }
        }
    }

    fun parseTime(timeStr: String, fallback: LocalTime = LocalTime.of(8, 0)): LocalTime {
        return try {
            val parts = timeStr.trim().split(":")
            if (parts.size == 2) {
                val hour = parts[0].toInt().coerceIn(0, 23)
                val minute = parts[1].toInt().coerceIn(0, 59)
                LocalTime.of(hour, minute)
            } else {
                fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }

    fun formatTime(hour: Int, minute: Int): String {
        return String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    fun formatTime(time: LocalTime): String {
        return formatTime(time.hour, time.minute)
    }
}

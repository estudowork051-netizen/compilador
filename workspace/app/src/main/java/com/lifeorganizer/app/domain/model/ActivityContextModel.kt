package com.lifeorganizer.app.domain.model

/**
 * Encapsulates contextual memory for an activity (Step 4 Section 6).
 * Preserves specific knowledge such as series name, current episode, study subject,
 * book chapter, or workout focus without forcing users to remember where they stopped.
 */
data class ActivityContextModel(
    val id: Long = 0,
    val activityId: Long = 0L,
    val contentType: ContentType = ContentType.GENERAL,
    val contentName: String, // e.g. "Naruto", "Banco de Dados", "Clean Code"
    val currentProgress: String = "", // e.g. "Episódio 35", "Capítulo 4", "Página 120"
    val unitType: String = "", // "episódio", "capítulo", "página", "módulo"
    val currentUnitValue: Int? = null, // 35, 4, 120
    val continuityStatus: ContinuityStatus = ContinuityStatus.IN_PROGRESS,
    val lastSessionAt: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    constructor(
        activityId: Long,
        contentType: ContentType,
        contentName: String,
        progressLabel: String = "",
        currentUnit: Int? = null,
        continuityStatus: ContinuityStatus = ContinuityStatus.IN_PROGRESS,
        id: Long = 0,
        unitType: String = "",
        lastSessionAt: Long = System.currentTimeMillis(),
        notes: String = ""
    ) : this(
        id = id,
        activityId = activityId,
        contentType = contentType,
        contentName = contentName,
        currentProgress = progressLabel,
        unitType = unitType,
        currentUnitValue = currentUnit,
        continuityStatus = continuityStatus,
        lastSessionAt = lastSessionAt,
        notes = notes
    )

    val progressLabel: String
        get() = when {
            currentProgress.isNotBlank() -> currentProgress
            currentUnitValue != null && unitType.isNotBlank() -> "${unitType.replaceFirstChar { it.uppercase() }} $currentUnitValue"
            else -> ""
        }

    val currentUnit: Int?
        get() = currentUnitValue

    fun nextUnitProgress(): ActivityContextModel {
        val nextVal = (currentUnitValue ?: 0) + 1
        val nextProg = if (unitType.isNotBlank()) "${unitType.replaceFirstChar { it.uppercase() }} $nextVal" else "$nextVal"
        return copy(
            currentUnitValue = nextVal,
            currentProgress = nextProg,
            lastSessionAt = System.currentTimeMillis()
        )
    }

    fun advance(): ActivityContextModel = nextUnitProgress()
}

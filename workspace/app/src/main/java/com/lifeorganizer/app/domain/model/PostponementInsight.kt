package com.lifeorganizer.app.domain.model

data class PostponementInsight(
    val activityId: Long,
    val activityTitle: String,
    val postponedCount: Int,
    val totalScheduledCount: Int,
    val postponementRate: Float,
    val suggestionMessage: String
) {
    val postponementPercentage: Int get() = (postponementRate * 100).toInt()
}

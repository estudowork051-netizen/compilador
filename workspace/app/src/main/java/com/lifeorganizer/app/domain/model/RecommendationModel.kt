package com.lifeorganizer.app.domain.model

enum class RecommendationType {
    CONTINUITY,           // "Quer continuar Naruto do episódio 35?"
    PENDING_URGENT,       // "Você tem uma revisão de Banco de Dados pendente."
    FREE_TIME_FIT,        // "Você tem 1h30 livre antes do trabalho. Que tal estudar?"
    CATEGORY_BALANCE,     // "Hoje você já aproveitou bastante seu lazer. Que tal dedicar um tempinho aos estudos?"
    REST,                 // "Seu dia foi intenso. Que tal reservar esse tempo para descansar?"
    BEST_TIME_SLOT,       // "Você costuma concluir seus estudos com mais frequência às 10h. Quer tentar esse horário?"
    LEARNED_DURATION,     // "Você costuma terminar essa atividade em cerca de 25 minutos. Quer atualizar a duração sugerida?"
    POSTPONED_ADJUSTMENT, // "Percebi que estudar às 8h nem sempre está funcionando. Quer experimentar um horário diferente?"
    // Aliases from step 4 for compatibility
    RESUME_CONTENT,
    REST_BREAK,
    FILL_GAP,
    LEISURE_BALANCE,
    PENDING_TASK
}

data class RecommendationModel(
    val id: String,
    val type: RecommendationType,
    val title: String,
    val message: String,
    val activity: ActivityModel? = null,
    val suggestedStartHour: Int? = null,
    val suggestedStartMinute: Int? = null,
    val suggestedDurationMinutes: Int = 30,
    val reason: String = "",
    val isGeneric: Boolean = false,
    val priorityScore: Int = 0,
    val confidence: ConfidenceLevel = ConfidenceLevel.INSUFFICIENT_DATA,
    val sampleCount: Int = 0,
    val feedbackAction: FeedbackAction? = null
) {
    val subtitle: String get() = message
    val suggestedActivity: ActivityModel? get() = activity

    val actionLabel: String
        get() = when (type) {
            RecommendationType.LEARNED_DURATION -> "Atualizar duração"
            RecommendationType.BEST_TIME_SLOT -> "Reservar horário"
            RecommendationType.CONTINUITY, RecommendationType.RESUME_CONTENT -> "Continuar agora"
            RecommendationType.REST, RecommendationType.REST_BREAK -> "Reservar pausa"
            else -> "Adicionar à rotina"
        }

    val suggestedTimeRangeFormatted: String?
        get() {
            if (suggestedStartHour == null || suggestedStartMinute == null) return null
            val startMin = suggestedStartHour * 60 + suggestedStartMinute
            val endMin = startMin + suggestedDurationMinutes
            val endH = (endMin / 60) % 24
            val endM = endMin % 60
            return "%02d:%02d às %02d:%02d".format(suggestedStartHour, suggestedStartMinute, endH, endM)
        }
}

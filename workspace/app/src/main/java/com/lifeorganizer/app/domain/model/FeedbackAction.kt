package com.lifeorganizer.app.domain.model

enum class FeedbackAction(val displayName: String) {
    ACCEPTED("Aceito"),
    DISMISSED("Ignorado"),
    POSTPONED("Adiado"),
    NOT_INTERESTED("Sem interesse")
}

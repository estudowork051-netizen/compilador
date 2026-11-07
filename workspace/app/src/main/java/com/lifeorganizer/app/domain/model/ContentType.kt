package com.lifeorganizer.app.domain.model

enum class ContentType(val displayName: String) {
    SERIES("Série / Anime"),
    BOOK("Livro / Leitura"),
    STUDY("Estudo / Matéria"),
    STUDY_COURSE("Curso / Módulo"),
    WORKOUT("Treino / Exercício"),
    HOME_CHORE("Casa / Cômodo"),
    CHORE("Tarefa Doméstica"),
    PROJECT("Projeto / Trabalho"),
    GENERAL("Geral");

    companion object {
        fun fromString(value: String?): ContentType {
            if (value.isNullOrBlank()) return GENERAL
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: GENERAL
        }
    }
}

enum class ContinuityStatus(val displayName: String) {
    IN_PROGRESS("Em andamento"),
    PAUSED("Pausado"),
    COMPLETED("Concluído");

    companion object {
        fun fromString(value: String?): ContinuityStatus {
            if (value.isNullOrBlank()) return IN_PROGRESS
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: IN_PROGRESS
        }
    }
}

enum class BalancePreference(val displayName: String, val description: String) {
    BALANCED("Equilibrar atividades", "Sugere alternar entre responsabilidades, estudos e lazer"),
    STUDY_FOCUS("Priorizar estudos", "Recomenda avançar em matérias e leituras sempre que houver tempo livre"),
    REST_FOCUS("Mais tempo para descanso", "Prioriza janelas de pausa após tarefas intensas"),
    NO_PREFERENCE("Sem recomendações de equilíbrio", "Decidir sem sugestões de proporção entre categorias");

    companion object {
        fun fromString(value: String?): BalancePreference {
            if (value.isNullOrBlank()) return BALANCED
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: BALANCED
        }
    }
}

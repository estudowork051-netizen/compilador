package com.lifeorganizer.app.domain.model

import com.lifeorganizer.app.core.utils.StringUtils

data class PredefinedActivity(
    val title: String,
    val category: String,
    val suggestedDurationMinutes: Int,
    val type: ActivityType = ActivityType.FLEXIBLE
) {
    val durationMinutes: Int
        get() = suggestedDurationMinutes

    companion object {
        val LIBRARY = listOf(
            // Casa
            PredefinedActivity("Lavar louça", "Casa", 20),
            PredefinedActivity("Limpar quarto", "Casa", 30),
            PredefinedActivity("Cozinhar", "Casa", 60),
            PredefinedActivity("Lavar roupa", "Casa", 40),

            // Estudos
            PredefinedActivity("Estudar", "Estudos", 90),
            PredefinedActivity("Ler", "Estudos", 30),
            PredefinedActivity("Fazer exercícios", "Estudos", 60),
            PredefinedActivity("Revisar conteúdo", "Estudos", 45),

            // Saúde
            PredefinedActivity("Treinar", "Saúde", 90, ActivityType.HABIT),
            PredefinedActivity("Caminhar", "Saúde", 45, ActivityType.HABIT),
            PredefinedActivity("Alongar", "Saúde", 15),
            PredefinedActivity("Meditar", "Saúde", 15),

            // Trabalho
            PredefinedActivity("Trabalhar", "Trabalho", 240, ActivityType.FIXED),
            PredefinedActivity("Reunião", "Trabalho", 60, ActivityType.FIXED),
            PredefinedActivity("Organizar tarefas", "Trabalho", 30),

            // Lazer
            PredefinedActivity("Jogar", "Lazer", 60),
            PredefinedActivity("Assistir filme", "Lazer", 120),
            PredefinedActivity("Assistir série", "Lazer", 45),
            PredefinedActivity("Descansar", "Lazer", 30),

            // Pessoal
            PredefinedActivity("Fazer compras", "Pessoal", 60),
            PredefinedActivity("Resolver pendências", "Pessoal", 45),
            PredefinedActivity("Compromisso pessoal", "Pessoal", 60, ActivityType.FIXED)
        )

        fun getAll(): List<PredefinedActivity> = LIBRARY

        fun search(query: String): List<PredefinedActivity> {
            if (query.isBlank()) return LIBRARY.take(8)
            return LIBRARY.filter {
                StringUtils.fuzzyContains(it.title, query) ||
                        StringUtils.fuzzyContains(it.category, query)
            }
        }
    }
}

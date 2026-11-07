package com.lifeorganizer.app.core.utils

import com.lifeorganizer.app.domain.model.ActivityContextModel
import com.lifeorganizer.app.domain.model.ContentType

data class ContextSuggestion(
    val contentType: ContentType,
    val contentName: String,
    val currentProgress: String = "",
    val unitType: String = "",
    val unitValue: Int? = null,
    val confirmationPrompt: String = "",
    val currentUnitValue: Int? = unitValue,
    val progressLabel: String = currentProgress,
    val currentUnit: Int? = unitValue
)

/**
 * Lightweight, offline, deterministic regex & keyword extractor for activity descriptions.
 * Conforms to Step 4 Section 7:
 * Identifies contextual metadata (series, books, study subjects, workout types)
 * and generates polite, non-intrusive confirmation prompts.
 */
object ContextExtractor {

    // Regex for Series / Anime with Season and Episode (e.g. Breaking Bad S02E05)
    private val seriesSeasonRegex = Regex(
        """(?:assistir|ver|s[eé]rie|anime)?\s*(.+?)\s*[-:,]?\s*[sS](\d+)\s*[eE](\d+)""",
        RegexOption.IGNORE_CASE
    )

    // Regex for Series / Anime episodes (e.g. Stranger Things Ep. 4 or Naruto Episódio 42)
    private val seriesEpisodeRegex = Regex(
        """(?:assistir|ver|s[eé]rie|anime)?\s*(.+?)\s*[-:,]?\s*(?:epis[oó]dio|ep\.?)\s*(\d+)""",
        RegexOption.IGNORE_CASE
    )

    // Regex for Reading / Books (e.g. Ler O Senhor dos Anéis Cap. 12)
    private val bookRegex = Regex(
        """(?:ler|leitura|livro)\s*(?:de\s+)?(.+?)\s*[-:,]?\s*(?:cap[ií]tulo|cap\.?|p[aá]gina|p[aá]g\.?)\s*(\d+)""",
        RegexOption.IGNORE_CASE
    )

    // Regex for Study subjects & chapters / modules (e.g. Curso de Kotlin Módulo 3)
    private val studyRegex = Regex(
        """(Curso\s+de\s+.+?|.+?)\s*[-:,]?\s*(m[oó]dulo|cap[ií]tulo|cap\.?|aula)\s*(\d+)""",
        RegexOption.IGNORE_CASE
    )

    // Workout focus keywords
    private val workoutKeywords = mapOf(
        "peito e tríceps" to "Peito e Tríceps",
        "peito" to "Peito e Tríceps",
        "costa" to "Costas e Bíceps",
        "perna" to "Treino de Pernas",
        "ombro" to "Ombros",
        "cardio" to "Cardio / Esteira",
        "superior" to "Membros Superiores",
        "inferior" to "Membros Inferiores",
        "corrida" to "Corrida",
        "natação" to "Natação",
        "alongamento" to "Alongamento / Mobilidade"
    )

    // Home chore room keywords
    private val choreKeywords = mapOf(
        "cozinha" to "Limpar a cozinha",
        "quarto" to "Organizar o quarto",
        "banheiro" to "Limpar o banheiro",
        "sala" to "Organizar a sala",
        "lavanderia" to "Lavar roupas"
    )

    fun extract(
        activityTitle: String = "",
        notes: String = "",
        category: String = ""
    ): ActivityContextModel? {
        val fullText = "$activityTitle $notes".trim()
        if (fullText.isBlank()) return null

        // 1. Series with Season and Episode (e.g. Breaking Bad S02E05)
        val sMatch = seriesSeasonRegex.find(fullText)
        if (sMatch != null) {
            val rawName = sMatch.groupValues[1].trim()
            val clean = cleanName(rawName)
            val season = sMatch.groupValues[2].toIntOrNull()
            val epNum = sMatch.groupValues[3].toIntOrNull()
            if (clean.isNotBlank() && epNum != null) {
                val label = if (season != null) "Temporada $season Episódio $epNum" else "Episódio $epNum"
                return ActivityContextModel(
                    contentType = ContentType.SERIES,
                    contentName = clean,
                    currentProgress = label,
                    unitType = "episódio",
                    currentUnitValue = epNum
                )
            }
        }

        // 2. Series with Episode (e.g. Stranger Things Ep. 4 or Assistir Naruto Episódio 42)
        val epMatch = seriesEpisodeRegex.find(fullText)
        if (epMatch != null) {
            val rawName = epMatch.groupValues[1].trim()
            val clean = cleanName(rawName)
            val epNum = epMatch.groupValues[2].toIntOrNull()
            if (clean.isNotBlank() && epNum != null) {
                return ActivityContextModel(
                    contentType = ContentType.SERIES,
                    contentName = clean,
                    currentProgress = "Episódio $epNum",
                    unitType = "episódio",
                    currentUnitValue = epNum
                )
            }
        }

        // 3. Books (e.g. Ler O Senhor dos Anéis Cap. 12)
        val bookMatch = bookRegex.find(fullText)
        if (bookMatch != null) {
            val rawName = bookMatch.groupValues[1].trim()
            val clean = cleanName(rawName)
            val unitNum = bookMatch.groupValues[2].toIntOrNull()
            val isPage = fullText.contains("pág", ignoreCase = true) || fullText.contains("página", ignoreCase = true)
            val unitName = if (isPage) "Página" else "Capítulo"
            if (clean.isNotBlank() && unitNum != null) {
                return ActivityContextModel(
                    contentType = ContentType.BOOK,
                    contentName = clean,
                    currentProgress = "$unitName $unitNum",
                    unitType = unitName.lowercase(),
                    currentUnitValue = unitNum
                )
            }
        }

        // 4. Study Course / Modules (e.g. Curso de Kotlin Módulo 3)
        val studyMatch = studyRegex.find(fullText)
        if (studyMatch != null && (fullText.contains("curso", ignoreCase = true) ||
                    fullText.contains("estud", ignoreCase = true) ||
                    fullText.contains("módulo", ignoreCase = true) ||
                    fullText.contains("modulo", ignoreCase = true))) {
            val rawName = studyMatch.groupValues[1].trim()
            val clean = cleanName(rawName, keepCurso = true)
            val unitLabelRaw = studyMatch.groupValues[2]
            val unitLabel = if (unitLabelRaw.contains("modulo", ignoreCase = true) || unitLabelRaw.contains("módulo", ignoreCase = true)) {
                "Módulo"
            } else {
                unitLabelRaw.replaceFirstChar { it.uppercase() }
            }
            val unitNum = studyMatch.groupValues[3].toIntOrNull()
            if (clean.isNotBlank() && unitNum != null) {
                return ActivityContextModel(
                    contentType = ContentType.STUDY_COURSE,
                    contentName = clean,
                    currentProgress = "$unitLabel $unitNum",
                    unitType = unitLabel.lowercase(),
                    currentUnitValue = unitNum
                )
            }
        }

        // 5. Chores (Special handling for dish washing & room keywords)
        if (fullText.contains("louça", ignoreCase = true) || fullText.contains("louca", ignoreCase = true)) {
            return ActivityContextModel(
                contentType = ContentType.CHORE,
                contentName = "Lavar a louça",
                currentProgress = "Lavar a louça",
                unitType = "etapa",
                currentUnitValue = 1
            )
        }
        for ((keyword, choreName) in choreKeywords) {
            if (fullText.contains(keyword, ignoreCase = true) &&
                (category.equals("Casa", ignoreCase = true) || category.equals("HOME", ignoreCase = true))) {
                return ActivityContextModel(
                    contentType = ContentType.CHORE,
                    contentName = choreName,
                    currentProgress = choreName,
                    unitType = "etapa",
                    currentUnitValue = 1
                )
            }
        }

        // 6. Workout Focus
        for ((keyword, focus) in workoutKeywords) {
            if (fullText.contains(keyword, ignoreCase = true)) {
                return ActivityContextModel(
                    contentType = ContentType.WORKOUT,
                    contentName = focus,
                    currentProgress = focus,
                    unitType = "sessão",
                    currentUnitValue = 1
                )
            }
        }

        return null
    }

    fun extractContext(
        activityTitle: String = "",
        notes: String = "",
        category: String = ""
    ): ContextSuggestion? {
        val model = extract(activityTitle, notes, category) ?: return null
        return ContextSuggestion(
            contentType = model.contentType,
            contentName = model.contentName,
            currentProgress = model.currentProgress,
            unitType = model.unitType,
            unitValue = model.currentUnitValue,
            confirmationPrompt = "Identifiquei ${model.contentName} (${model.progressLabel}). Deseja salvar para futuras sugestões de continuidade?"
        )
    }

    private fun cleanName(raw: String, keepCurso: Boolean = false): String {
        if (keepCurso && raw.startsWith("curso de", ignoreCase = true)) {
            return raw.trim()
        }
        return raw.replace(
            Regex("""^(assistir|ver|estudar|revisar|ler|livro|s[eé]rie|anime|treino\s+de|treino|de)\s+""", RegexOption.IGNORE_CASE),
            ""
        ).trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

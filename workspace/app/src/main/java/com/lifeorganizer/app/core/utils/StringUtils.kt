package com.lifeorganizer.app.core.utils

import java.text.Normalizer
import java.util.regex.Pattern

object StringUtils {

    private val DIACRITICS_PATTERN: Pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")

    /**
     * Removes diacritical marks (accents, tildes, cedillas) from a string,
     * converting e.g. "exercício" -> "exercicio", "louça" -> "louca".
     */
    fun removeAccents(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return DIACRITICS_PATTERN.matcher(normalized).replaceAll("")
    }

    /**
     * Checks if a target string contains a query string, ignoring case and diacritics.
     */
    fun fuzzyContains(target: String, query: String): Boolean {
        if (query.isBlank()) return true
        val cleanTarget = removeAccents(target).trim().lowercase()
        val cleanQuery = removeAccents(query).trim().lowercase()
        return cleanTarget.contains(cleanQuery)
    }
}

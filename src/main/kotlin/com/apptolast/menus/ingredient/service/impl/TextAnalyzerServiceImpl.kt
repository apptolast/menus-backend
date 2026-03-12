package com.apptolast.menus.ingredient.service.impl

import com.apptolast.menus.allergen.model.enum.AllergenType
import com.apptolast.menus.ingredient.service.DetectedAllergen
import com.apptolast.menus.ingredient.service.TextAnalysisResult
import com.apptolast.menus.ingredient.service.TextAnalyzerService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TextAnalyzerServiceImpl : TextAnalyzerService {

    private val log = LoggerFactory.getLogger(TextAnalyzerServiceImpl::class.java)

    companion object {
        private val TRACES_PATTERNS = listOf(
            Regex("""(?i)puede\s+contener\s+trazas\s+de"""),
            Regex("""(?i)may\s+contain\s+traces?\s+of"""),
            Regex("""(?i)trazas\s*:"""),
            Regex("""(?i)traces\s*:"""),
            Regex("""(?i)puede\s+contener"""),
            Regex("""(?i)may\s+contain"""),
            Regex("""(?i)posibles\s+trazas"""),
            Regex("""(?i)elaborado\s+en\s+.*que\s+.*tambi[eé]n""")
        )
    }

    override fun analyzeText(text: String): TextAnalysisResult {
        log.info("Analyzing text for allergen detection ({} characters)", text.length)

        val normalizedText = text.lowercase()
        val tracesStartIndex = findTracesSection(normalizedText)
        val detected = mutableMapOf<String, DetectedAllergen>()

        for (allergenType in AllergenType.entries) {
            for (keyword in allergenType.keywords) {
                val pattern = Regex("""(?i)\b${Regex.escape(keyword)}\b""")
                val matchResult = pattern.find(normalizedText) ?: continue

                val matchIndex = matchResult.range.first
                val level = if (tracesStartIndex != null && matchIndex >= tracesStartIndex) {
                    "MAY_CONTAIN"
                } else {
                    "CONTAINS"
                }

                val existing = detected[allergenType.name]
                if (existing == null || shouldUpgrade(existing.level, level)) {
                    detected[allergenType.name] = DetectedAllergen(
                        code = allergenType.name,
                        level = level,
                        matchedKeyword = keyword
                    )
                }
            }
        }

        val result = detected.values.sortedBy { it.code }
        log.info("Detected {} allergens in text", result.size)
        return TextAnalysisResult(
            detectedAllergens = result,
            rawText = text
        )
    }

    private fun findTracesSection(normalizedText: String): Int? {
        for (pattern in TRACES_PATTERNS) {
            val match = pattern.find(normalizedText)
            if (match != null) {
                return match.range.first
            }
        }
        return null
    }

    /**
     * Returns true if we should upgrade: CONTAINS takes priority over MAY_CONTAIN.
     * We only upgrade from MAY_CONTAIN to CONTAINS, never downgrade.
     */
    private fun shouldUpgrade(currentLevel: String, newLevel: String): Boolean {
        return currentLevel == "MAY_CONTAIN" && newLevel == "CONTAINS"
    }
}

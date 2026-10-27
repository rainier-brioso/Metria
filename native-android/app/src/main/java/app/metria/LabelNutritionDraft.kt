package app.metria

data class LabelNutritionDraft(
    val caloriesKcal: Double?,
    val proteinGrams: Double?,
    val carbohydrateGrams: Double?,
    val totalFatGrams: Double?,
)

/** Redacted OCR text and image positions; these values remain transient. */
data class OcrTextElement(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int)

data class OcrTextLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val elements: List<OcrTextElement> = emptyList(),
) {
    val centerY: Int get() = (top + bottom) / 2
    val height: Int get() = (bottom - top).coerceAtLeast(1)
}

/** Deterministic extraction from local OCR text; it never treats OCR as exact. */
object LabelNutritionParser {
    private val carbohydrateLabel = Regex("\\b(?:total\\s+carb(?:ohydrates?)?|carbohydrates?|carbohidratos?)\\b", RegexOption.IGNORE_CASE)
    private val fatLabel = Regex("(?<!saturated )(?<!sat )(?<!trans )\\b(?:total\\s+fat|fat|grasas?\\s+totales?)\\b", RegexOption.IGNORE_CASE)
    private val carbohydrate = Regex(
        "\\b(?:total\\s+carb(?:ohydrates?)?|carbohydrates?|carbohidratos?)\\b[^\\d]{0,20}(\\d{1,3}(?:[.,]\\d{1,2})?)\\s*g\\b",
        RegexOption.IGNORE_CASE,
    )
    private val totalFat = Regex(
        "(?<!saturated )(?<!sat )(?<!trans )\\b(?:total\\s+fat|fat|grasas?\\s+totales?)\\b[^\\d]{0,20}(\\d{1,3}(?:[.,]\\d{1,2})?)\\s*g\\b",
        RegexOption.IGNORE_CASE,
    )
    private val caloriesLabelSafe = Regex("\\b(?:calories?|calor[i\\u00ed]as?|energ[i\\u00ed]a)\\b(?!\\s+(?:from\\s+fat|de\\s+grasas?))", RegexOption.IGNORE_CASE)
    private val proteinLabelSafe = Regex("\\b(?:protein(?:a|as)?|prote[i\\u00ed]nas?)\\b", RegexOption.IGNORE_CASE)
    private val caloriesSafe = Regex(
        "\\b(?:calories?|calor[i\\u00ed]as?|energ[i\\u00ed]a)\\b(?!\\s+(?:from\\s+fat|de\\s+grasas?))[^\\d]{0,20}(\\d{1,4}(?:[.,]\\d{1,2})?)\\s*(?:kcal|cal)?",
        RegexOption.IGNORE_CASE,
    )
    private val proteinSafe = Regex(
        "\\b(?:protein(?:a|as)?|prote[i\\u00ed]nas?)\\b[^\\d]{0,20}(\\d{1,3}(?:[.,]\\d{1,2})?)\\s*g\\b",
        RegexOption.IGNORE_CASE,
    )

    fun extract(text: String, lines: List<OcrTextLine> = emptyList()): LabelNutritionDraft? {
        // When image coordinates exist, use them for every nutrient. Flattened
        // OCR is ordered by blocks/columns and can silently mix serving columns.
        // If a row cannot be resolved spatially, leave that field for review.
        val spatial = lines.isNotEmpty()
        val draft = LabelNutritionDraft(
            caloriesKcal = if (spatial) spatialValue(lines, caloriesLabelSafe, null) else caloriesSafe.valueFrom(text),
            proteinGrams = if (spatial) spatialValue(lines, proteinLabelSafe, "g") else proteinSafe.valueFrom(text),
            carbohydrateGrams = if (spatial) spatialValue(lines, carbohydrateLabel, "g") else carbohydrate.valueFrom(text),
            totalFatGrams = if (spatial) spatialValue(lines, fatLabel, "g") else totalFat.valueFrom(text),
        )
        return draft.takeIf {
            it.caloriesKcal != null || it.proteinGrams != null ||
                it.carbohydrateGrams != null || it.totalFatGrams != null
        }
    }

    private fun Regex.valueFrom(text: String): Double? {
        if (this === caloriesSafe && Regex("\\b(?:kJ|kilojoules?)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            return null
        }
        // A plain text-only row with two candidate amounts has no usable column
        // identity. Abstain for that row instead of silently taking column 1.
        val labels = listOf(caloriesLabelSafe, proteinLabelSafe, carbohydrateLabel, fatLabel)
        val numericToken = Regex("(?<!\\d)\\d+(?:[.,]\\d+)?")
        if (text.lineSequence().any { row ->
                val labelMatch = labels.firstNotNullOfOrNull { it.find(row) } ?: return@any false
                numericToken.findAll(row.substring(labelMatch.range.last + 1)).take(2).count() > 1
            }) return null
        val matches = findAll(text).take(2).toList()
        if (matches.size != 1) return null
        return matches.first()
        .groupValues
        .getOrNull(1)
        ?.replace(',', '.')
        ?.toDoubleOrNull()
        ?.takeIf { it >= 0.0 }
    }

    /**
     * Match each label to values on the same visual row. For a two-column
     * table, consistently choose the left-most value after the label; do not
     * claim that this is always the per-serving column. Never fill a missing
     * spatial value from flattened OCR, where column order is ambiguous.
     */
    private fun spatialValue(lines: List<OcrTextLine>, label: Regex, unit: String?): Double? {
        val rows = nutrientRows.mapNotNull { (rowLabel, rowUnit) ->
            lines.filter { rowLabel.containsMatchIn(it.text) }
                .minByOrNull(OcrTextLine::top)?.let { NutrientRow(rowLabel, rowUnit, it) }
        }
        val row = rows.firstOrNull { it.label == label } ?: return null
        val candidates = candidatesFor(row, lines, rows)
        if (candidates.isEmpty()) return null

        // Merged label/value text has no trustworthy value x. It is safe only
        // if other rows provide no evidence of multiple columns.
        val candidatesByRow = rows.associateWith { candidatesFor(it, lines, rows) }
        val allCandidates = candidatesByRow.values.flatten()
        val positioned = candidates.filter { it.left != null }
        if (positioned.isNotEmpty()) {
            // Infer the left-value region from rows that visibly contain more
            // than one value. Keep the evidence row-local: values on the same
            // row must never be clustered together, even when columns are
            // narrow or the OCR image is scaled.
            val pairedBoundariesByRow = candidatesByRow.mapNotNull { (candidateRow, rowCandidates) ->
                val values = rowCandidates.filter { it.left != null }.sortedBy(Candidate::centerX)
                if (values.size < 2) null else candidateRow to (values[0].centerX + values[1].centerX) / 2.0
            }
            if (pairedBoundariesByRow.isNotEmpty()) {
                val pairedMacroBoundaries = pairedBoundariesByRow
                    .filter { (candidateRow, _) -> candidateRow.label != caloriesLabelSafe }
                    .map { it.second }
                val boundaries = if (pairedMacroBoundaries.size >= 2) pairedMacroBoundaries else
                    pairedBoundariesByRow.map { it.second }
                val lowerBoundary = boundaries.minOrNull()!!
                // Values on or close to the observed split are ambiguous. A
                // clear value left of every split is accepted; values to its
                // right cannot backfill a missing left-column value.
                return positioned.filter { it.centerX < lowerBoundary }
                    .minByOrNull { it.centerX }?.value
            }

            // With no paired row there is no direct evidence for the split.
            // Keep one narrow left-aligned region; distant singleton values
            // must not form a synthetic median column across rows.
            // Compare the left edges here: differently sized numbers in a
            // left-aligned column naturally have different centers.
            val leftmostEdge = allCandidates.mapNotNull(Candidate::left).min()
            val maxOffset = maxOf(1, minOf(row.line.height, positioned.minOf(Candidate::height)) / 2)
            return positioned.filter { it.left!! - leftmostEdge <= maxOffset }
                .minByOrNull { it.left!! }?.value
        }
        if (allCandidates.any { it.left != null }) return null
        return candidates.singleOrNull()?.value
    }

    private data class NutrientRow(val label: Regex, val unit: String?, val line: OcrTextLine)
    private data class Candidate(val left: Int?, val right: Int?, val height: Int, val value: Double) {
        val centerX: Int get() = if (left == null || right == null) 0 else (left + right) / 2
    }

    private val nutrientRows: List<Pair<Regex, String?>>
        get() = listOf(caloriesLabelSafe to null, proteinLabelSafe to "g", carbohydrateLabel to "g", fatLabel to "g")

    private fun candidatesFor(row: NutrientRow, lines: List<OcrTextLine>, rows: List<NutrientRow>): List<Candidate> {
        val match = row.label.find(row.line.text) ?: return emptyList()
        val elements = elementValues(row.line, match.range.last + 1, row.unit)
        if (elements.isNotEmpty()) return elements

        if (row.line.elements.isEmpty()) {
            val inline = amountFrom(row.line.text.substring(match.range.last + 1), row.unit)
            if (inline != null) return listOf(Candidate(null, null, row.line.height, inline))
        }

        val tolerance = maxOf(4, row.line.height / 2)
        return lines.asSequence()
            .filter { candidate -> candidate !== row.line && candidate.left >= row.line.right - 4 &&
                kotlin.math.abs(candidate.centerY - row.line.centerY) <= tolerance && nearestRow(candidate, rows) === row &&
                !rows.any { other -> other.line !== row.line && other.label.containsMatchIn(candidate.text) }
            }
            .sortedBy(OcrTextLine::left)
            .flatMap { candidate ->
                val positionedValues = elementValues(candidate, 0, row.unit)
                if (positionedValues.isNotEmpty()) positionedValues
                else amountFrom(candidate.text, row.unit)?.let {
                    listOf(Candidate(candidate.left, candidate.right, candidate.height, it))
                }.orEmpty()
            }
            .toList()
    }

    private fun elementValues(line: OcrTextLine, afterIndex: Int, unit: String?): List<Candidate> {
        if (line.elements.isEmpty()) return emptyList()
        var searchFrom = 0
        val tokens = line.elements.sortedBy(OcrTextElement::left).map { element ->
            val index = line.text.indexOf(element.text, searchFrom).let { if (it < 0) searchFrom else it }
            searchFrom = index + element.text.length
            index to element
        }.filter { (index, _) -> index >= afterIndex }
        return buildList {
            tokens.forEachIndexed { i, (index, element) ->
                if (index < afterIndex) return@forEachIndexed
                val next = tokens.getOrNull(i + 1)?.second
                // OCR may split the number from its unit or percent sign.
                // Attach that suffix before parsing so kJ/% cannot become kcal.
                val suffix = next?.text?.trim()?.takeIf {
                    it.matches(Regex("(?:%|kJ|kilojoules?|kcal|cal|g)", RegexOption.IGNORE_CASE))
                }
                val value = if (suffix != null) "${element.text} $suffix" else element.text
                amountFrom(value, unit)?.let { add(Candidate(element.left, element.right,
                    (element.bottom - element.top).coerceAtLeast(1), it)) }
            }
        }
    }

    private fun nearestRow(candidate: OcrTextLine, rows: List<NutrientRow>): NutrientRow? {
        val nearest = rows.sortedBy { kotlin.math.abs(it.line.centerY - candidate.centerY) }
        val first = nearest.firstOrNull() ?: return null
        val firstDistance = kotlin.math.abs(first.line.centerY - candidate.centerY)
        if (nearest.getOrNull(1)?.let { kotlin.math.abs(it.line.centerY - candidate.centerY) == firstDistance } == true) return null
        return first
    }

    private fun amountFrom(value: String, unit: String?): Double? {
        val pattern = if (unit == null) {
            "\\s*(\\d{1,4}(?:[.,]\\d{1,2})?|O(?:[.,]\\d{1,2})?)\\s*(?:kcal|cal)?\\s*"
        } else {
            "\\s*(\\d{1,4}(?:[.,]\\d{1,2})?|O(?:[.,]\\d{1,2})?)\\s*${Regex.escape(unit)}\\s*"
        }
        val match = (if (unit == null && Regex("\\b(?:kJ|kilojoules?)\\b", RegexOption.IGNORE_CASE).containsMatchIn(value)) null
            else Regex(pattern, RegexOption.IGNORE_CASE).matchEntire(value)) ?: return null
        return match.groupValues[1].replace('O', '0').replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0.0 }
    }
}

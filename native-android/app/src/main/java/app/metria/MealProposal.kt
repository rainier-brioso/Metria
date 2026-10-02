package app.metria

import org.json.JSONArray
import org.json.JSONObject

data class GramRange(val min: Int, val max: Int)

data class MealProposalItem(
    val name: String,
    val description: String,
    val visiblePortion: String,
    val estimatedGramsRange: GramRange,
    val preparation: String,
    val confidence: String,
    /** Seen in the photo, or an uncertain component of a recognisable dish. */
    val evidence: String = "seen",
    /** Optional dish/drink label supplied by the nested proposal normalizer. */
    val groupLabel: String? = null,
    val role: String = "main_food",
)

data class MealProposal(
    val items: List<MealProposalItem>,
    val assumptions: List<String>,
    val questions: List<String>,
    val overallConfidence: String,
    val omittedItemCount: Int = 0,
    val partialCategory: MealProposalOmissionCategory? = null,
)

/** Closed, non-sensitive reasons a usable proposal omitted one or more foods. */
enum class MealProposalOmissionCategory(val auditCode: String) {
    INVALID_GROUP("invalid_group"),
    INVALID_ITEM("invalid_item"),
    INVALID_RANGE("invalid_range"),
    TOO_MANY_ITEMS("too_many_items"),
    MULTIPLE("multiple_omissions"),
}

enum class MealProposalRejectionCategory(val auditCode: String, val diagnosticLabel: String) {
    INVALID_JSON("invalid_json", "response was not valid JSON"),
    INVALID_ROOT_SHAPE("invalid_root_shape", "proposal root did not match the expected shape"),
    INVALID_GROUP("invalid_group", "a grouped dish did not match the expected shape"),
    INVALID_ITEM("invalid_item", "an item did not match the expected shape"),
    INVALID_RANGE("invalid_range", "an item portion range was invalid"),
}

data class MealProposalParseResult(
    val proposal: MealProposal?,
    val rejectionCategory: MealProposalRejectionCategory?,
)

/** Rejects any model shape outside the explicit M0 display-only proposal contract. */
object MealProposalParser {
    private val confidences = setOf("low", "medium", "high")
    private val proposalKeys = setOf("kind", "items", "assumptions", "questions", "overall_confidence")
    private val itemKeys = setOf(
        "name", "description", "visible_portion", "estimated_grams_range", "preparation", "confidence", "role", "evidence"
    )
    private val itemKeysWithGroup = itemKeys + "group_label"
    private val groupedItemKeys = setOf("name", "kind", "ingredients")
    private val groupedItemKeysWithConfidence = groupedItemKeys + "confidence"
    // A visible sauce can be proposed as its own group. Keep its explicit
    // label and validate every ingredient; do not silently drop the group.
    private val groupKinds = setOf("dish", "drink", "other", "sauce")
    private val ingredientRoles = setOf("main_food", "side", "drink", "sauce_oil", "dry_seasoning")
    // Vision models often use ordinary nutrition labels rather than the compact
    // app vocabulary. These are safe semantic aliases, not extra user-visible
    // roles: the review still presents the normalized role for confirmation.
    private val roleAliases = mapOf(
        "main_dish_component" to "main_food",
        "main_dish" to "main_food",
        "base" to "main_food",
        "protein" to "main_food",
        "carbohydrate" to "main_food",
        "starch" to "main_food",
        "side_dish" to "side",
        "vegetable" to "side",
        "vegetables" to "side",
        "fruit" to "side",
        "sauce" to "sauce_oil",
        "dressing" to "sauce_oil",
        "oil" to "sauce_oil",
        "fat" to "sauce_oil",
        "fat/moisture" to "sauce_oil",
        "seasoning" to "dry_seasoning",
        "herbs" to "dry_seasoning",
        "spices" to "dry_seasoning",
    )
    private val rangeKeys = setOf("min", "max")

    fun parse(raw: String): MealProposal? = parseWithDiagnostic(raw).proposal

    /**
     * Returns only a fixed rejection category alongside a valid proposal. The
     * category is safe for local diagnostics: it never includes model text,
     * field values, parser messages, or exception details.
     */
    fun parseWithDiagnostic(raw: String): MealProposalParseResult {
        val normalized = unwrapSingleJsonFence(raw)
        val source = runCatching { JSONObject(normalized) }.getOrNull()
        if (source == null) {
            val isValidArray = runCatching { JSONArray(normalized) }.isSuccess
            val isValidScalar = normalized.trim().let {
                it == "true" || it == "false" || it == "null" ||
                    it.matches(Regex("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?"))
            }
            return MealProposalParseResult(
                null,
                if (isValidArray || isValidScalar) MealProposalRejectionCategory.INVALID_ROOT_SHAPE
                else MealProposalRejectionCategory.INVALID_JSON,
            )
        }

        val root = try {
            canonicalObject(source, mapOf("overallConfidence" to "overall_confidence"))
                .also {
                    require(keys(it) == proposalKeys)
                    require(it.getString("kind") == "meal_proposal")
                }
        } catch (_: Exception) {
            return MealProposalParseResult(null, MealProposalRejectionCategory.INVALID_ROOT_SHAPE)
        }

        val rawItems = try {
            root.get("items").asArray("items")
        } catch (_: Exception) {
            return MealProposalParseResult(null, MealProposalRejectionCategory.INVALID_ROOT_SHAPE)
        }
        val parsedItems = mutableListOf<MealProposalItem>()
        val omissionCategories = linkedSetOf<MealProposalOmissionCategory>()
        var omittedCount = 0
        for (index in 0 until rawItems.length()) {
            val item = rawItems.optJSONObject(index)
            if (item == null) {
                omittedCount += 1
                omissionCategories += MealProposalOmissionCategory.INVALID_ITEM
                continue
            }
            val group = if (item.has("ingredients")) {
                try {
                    validateGroup(item)
                } catch (_: Exception) {
                    omittedCount += item.optJSONArray("ingredients")?.length()?.coerceAtLeast(1) ?: 1
                    omissionCategories += MealProposalOmissionCategory.INVALID_GROUP
                    continue
                }
            } else null
            val ingredients: List<JSONObject?> = if (group == null) listOf(item) else {
                val array = group.second
                List(array.length()) { array.optJSONObject(it) }
            }
            for (ingredient in ingredients) {
                if (ingredient == null) {
                    omittedCount += 1
                    omissionCategories += MealProposalOmissionCategory.INVALID_ITEM
                    continue
                }
                val parsed = try {
                    val normalized = normalizeIngredient(ingredient)
                    if (group != null) normalized.put("group_label", group.first)
                    JSONArray().put(normalized).parseItems().single()
                } catch (_: InvalidRangeException) {
                    omittedCount += 1
                    omissionCategories += MealProposalOmissionCategory.INVALID_RANGE
                    continue
                } catch (_: Exception) {
                    omittedCount += 1
                    omissionCategories += MealProposalOmissionCategory.INVALID_ITEM
                    continue
                }
                if (parsedItems.size < 12) parsedItems += parsed
                else {
                    omittedCount += 1
                    omissionCategories += MealProposalOmissionCategory.TOO_MANY_ITEMS
                }
            }
        }
        if (parsedItems.isEmpty()) {
            val soleCategory = omissionCategories.singleOrNull()
            val rejection = when (soleCategory) {
                MealProposalOmissionCategory.INVALID_GROUP -> MealProposalRejectionCategory.INVALID_GROUP
                MealProposalOmissionCategory.INVALID_RANGE -> MealProposalRejectionCategory.INVALID_RANGE
                else -> MealProposalRejectionCategory.INVALID_ITEM
            }
            return MealProposalParseResult(null, rejection)
        }
        val proposal = try {
            root.put("assumptions", root.get("assumptions").asStringArray("assumptions").withoutLinks().take(1))
            root.put("questions", root.get("questions").asStringArray("questions").take(1))
            root.put("overall_confidence", normalizedConfidence(root.getString("overall_confidence")))
            val assumptions = root.getJSONArray("assumptions").parseStrings()
            val questions = root.getJSONArray("questions").parseStrings()
            val partialCategory = when {
                omissionCategories.isEmpty() -> null
                omissionCategories.size == 1 -> omissionCategories.single()
                else -> MealProposalOmissionCategory.MULTIPLE
            }
            MealProposal(
                parsedItems,
                assumptions,
                questions,
                if (omittedCount > 0) "low" else root.getString("overall_confidence"),
                omittedCount,
                partialCategory,
            )
        } catch (_: Exception) {
            return MealProposalParseResult(null, MealProposalRejectionCategory.INVALID_ROOT_SHAPE)
        }
        return MealProposalParseResult(proposal, null)
    }

    internal fun parseOrThrow(raw: String): MealProposal = parseWithDiagnostic(raw).proposal
        ?: throw IllegalArgumentException("Meal proposal rejected.")

    /** Normalize only explicit lossless variations; unknown fields still fail closed. */
    private fun validateGroup(source: JSONObject): Pair<String, JSONArray> {
        val grouped = canonicalObject(source, emptyMap())
        require(keys(grouped) == groupedItemKeys || keys(grouped) == groupedItemKeysWithConfidence)
        val groupLabel = grouped.getString("name").boundedText(120)
        val kind = grouped.getString("kind").trim().lowercase()
        require((if (kind == "side") "dish" else kind) in groupKinds)
        val ingredients = grouped.get("ingredients").asArray("ingredients")
        require(ingredients.length() > 0)
        return groupLabel to ingredients
    }

    private fun normalizeIngredient(source: JSONObject): JSONObject {
        val item = canonicalObject(source, mapOf(
            "visiblePortion" to "visible_portion",
            "estimatedGramsRange" to "estimated_grams_range",
        ))
        val range = try {
            item.get("estimated_grams_range").asRangeObject().also {
                it.put("min", wholeNumber(it.get("min")))
                it.put("max", wholeNumber(it.get("max")))
            }
        } catch (_: Exception) {
            throw InvalidRangeException()
        }
        item.put("estimated_grams_range", range)
        if (!item.has("description")) item.put("description", "")
        if (!item.has("role")) item.put("role", "main_food")
        if (!item.has("evidence")) item.put("evidence", "seen")
        item.put("role", roleAliases[item.getString("role").trim().lowercase()] ?: item.getString("role").trim().lowercase())
        item.put("evidence", when (item.getString("evidence").trim().lowercase()) {
            "visible", "observed", "visible_portion" -> "seen"
            "inferred", "possible", "hidden", "likely_inside" -> "possible_inside"
            else -> item.getString("evidence").trim().lowercase()
        })
        item.put("confidence", normalizedConfidence(item.getString("confidence")))
        return item
    }

    private fun canonicalObject(source: JSONObject, aliases: Map<String, String>): JSONObject {
        val result = JSONObject()
        val iterator = source.keys()
        while (iterator.hasNext()) {
            val sourceKey = iterator.next()
            val key = aliases[sourceKey] ?: sourceKey
            require(!result.has(key)) { "Duplicate canonical key." }
            result.put(key, source.get(sourceKey))
        }
        return result
    }

    private fun Any.asArray(field: String): JSONArray = when (this) {
        is JSONArray -> this
        is JSONObject -> JSONArray().put(this)
        else -> throw IllegalArgumentException("$field must be an array or object.")
    }

    private fun Any.asStringArray(field: String): JSONArray = when (this) {
        is JSONArray -> this
        is String -> JSONArray().put(this)
        else -> throw IllegalArgumentException("$field must be a string or array.")
    }

    private fun Any.asRangeObject(): JSONObject = when (this) {
        is JSONObject -> canonicalObject(this, emptyMap())
        is JSONArray -> {
            require(length() == 2) { "Range arrays must contain exactly min and max." }
            JSONObject().put("min", get(0)).put("max", get(1))
        }
        else -> throw IllegalArgumentException("Range must be an object or two-value array.")
    }

    private fun wholeNumber(value: Any): Int = when (value) {
        is Number -> value.toInt().also { require(value.toDouble() == it.toDouble()) }
        is String -> value.trim().toInt()
        else -> throw IllegalArgumentException("Range value must be a whole number.")
    }

    private fun normalizedConfidence(value: String): String = value.trim().lowercase().also {
        require(it in confidences)
    }

    private class InvalidRangeException : IllegalArgumentException()

    private fun JSONArray.parseItems(): List<MealProposalItem> = buildList {
        require(length() in 1..12)
        for (index in 0 until length()) {
            val item = getJSONObject(index)
            require(keys(item) == itemKeys || keys(item) == itemKeysWithGroup)
            val name = item.getString("name").boundedText(120)
            val description = item.getString("description").trim().also { require(it.length <= 280) }
            val visiblePortion = item.getString("visible_portion").boundedText(120)
            val preparation = item.getString("preparation").trim().also { require(it.length <= 120) }
            val confidence = normalizedConfidence(item.getString("confidence"))
            val role = item.getString("role").trim().lowercase().also { require(it in ingredientRoles) }
            val evidence = item.getString("evidence").trim().lowercase().also { require(it in setOf("seen", "possible_inside")) }
            val (min, max) = try {
                val range = item.getJSONObject("estimated_grams_range")
                require(keys(range) == rangeKeys)
                val min = range.getInt("min")
                val max = range.getInt("max")
                require(min in 1..2_000 && max in min..2_000)
                min to max
            } catch (_: Exception) {
                throw InvalidRangeException()
            }
            val groupLabel = item.optString("group_label").takeIf(String::isNotEmpty)?.boundedText(120)
            add(MealProposalItem(name, description, visiblePortion, GramRange(min, max), preparation, confidence, evidence, groupLabel, role))
        }
    }

    private fun JSONArray.parseStrings(): List<String> = buildList {
        require(length() <= 12)
        for (index in 0 until length()) add(getString(index).boundedText(280))
    }

    private fun JSONArray.withoutLinks(): JSONArray = JSONArray().also { safe ->
        for (index in 0 until length()) {
            val value = getString(index).trim()
            if (!value.contains(Regex("https?://", RegexOption.IGNORE_CASE))) safe.put(value)
        }
    }

    private fun JSONArray.take(maxItems: Int): JSONArray = JSONArray().also { limited ->
        for (index in 0 until minOf(length(), maxItems)) limited.put(get(index))
    }

    private fun keys(json: JSONObject): Set<String> = buildSet {
        val iterator = json.keys()
        while (iterator.hasNext()) add(iterator.next())
    }

    private fun String.boundedText(maxLength: Int): String {
        val trimmed = trim()
        require(trimmed.isNotBlank() && trimmed.length <= maxLength)
        return trimmed
    }

    /**
     * Models commonly wrap an otherwise valid response in one Markdown JSON
     * fence. This normalizes only that exact transport wrapper; it never
     * accepts arbitrary prose around a JSON object.
     */
    private fun unwrapSingleJsonFence(raw: String): String {
        // A model occasionally inserts an unquoted search URL into assumptions.
        // Delete only that non-content transport error; never interpret it as an assumption.
        val trimmed = raw.trim().replace(
            Regex("\\\"assumptions\\\"\\s*:\\s*\\[\\s*https?://[^\\]]*]", RegexOption.IGNORE_CASE),
            "\"assumptions\": []",
        )
        val match = Regex("^```(?:json)?\\s*(\\{[\\s\\S]*\\})\\s*```$").matchEntire(trimmed)
        return match?.groupValues?.get(1) ?: trimmed
    }
}

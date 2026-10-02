package app.metria

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Version-1 JSON codec for the optional encrypted/archive correction-memory collection. */
object MealCorrectionMemoryCodec {
    const val PAYLOAD_FIELD = "mealCorrectionMemories"

    fun encode(memories: List<MealCorrectionMemory>): JSONArray {
        require(memories.size <= MealCorrectionMemoryPolicy.MAX_RECORDS)
        require(memories.all(MealCorrectionMemoryPolicy::valid))
        require(memories.map(MealCorrectionMemory::mealId).distinct().size == memories.size)
        return JSONArray().apply {
            memories.forEach { memory -> put(JSONObject()
                .put("mealId", memory.mealId)
                .put("recordedAt", memory.recordedAt.toString())
                .put("changes", JSONArray().apply {
                    memory.changes.forEach { change -> put(JSONObject()
                        .put("kind", change.kind.name)
                        .put("itemId", change.itemId)
                        .put("oldName", change.oldName)
                        .put("newName", change.newName)
                        .put("oldGroupId", change.oldGroupId)
                        .put("newGroupId", change.newGroupId)
                        .put("oldGroupName", change.oldGroupName)
                        .put("newGroupName", change.newGroupName)
                        .put("oldPortionGrams", change.oldPortionGrams)
                        .put("newPortionGrams", change.newPortionGrams)
                        .put("oldCatalogFoodId", change.oldCatalogFoodId)
                        .put("newCatalogFoodId", change.newCatalogFoodId)
                        .put("oldCatalogRelease", change.oldCatalogRelease)
                        .put("newCatalogRelease", change.newCatalogRelease))
                    }
                }))
            }
        }
    }

    /** Missing field is the legacy version-1 shape and defaults to empty. */
    fun decode(root: JSONObject, meals: List<MealEntry>): List<MealCorrectionMemory> {
        if (!root.has(PAYLOAD_FIELD)) return emptyList()
        val values = root.optJSONArray(PAYLOAD_FIELD)
            ?: throw IllegalArgumentException("Meal correction memories must be an array.")
        require(values.length() <= MealCorrectionMemoryPolicy.MAX_RECORDS) { "Too many meal correction memories." }
        val mealById = meals.associateBy(MealEntry::id)
        val decoded = List(values.length()) { index ->
            val saved = values.getJSONObject(index)
            val memory = MealCorrectionMemory(
                mealId = saved.getString("mealId"),
                recordedAt = Instant.parse(saved.getString("recordedAt")),
                changes = saved.getJSONArray("changes").let { changes ->
                    require(changes.length() <= MealCorrectionMemoryPolicy.MAX_CHANGES_PER_RECORD) { "Too many meal correction changes." }
                    List(changes.length()) { changeIndex -> changes.getJSONObject(changeIndex).let { change ->
                        MealCorrectionChange(
                            kind = MealCorrectionKind.valueOf(change.getString("kind")),
                            itemId = change.getString("itemId"),
                            oldName = change.nullableString("oldName"),
                            newName = change.nullableString("newName"),
                            oldGroupId = change.nullableString("oldGroupId"),
                            newGroupId = change.nullableString("newGroupId"),
                            oldGroupName = change.nullableString("oldGroupName"),
                            newGroupName = change.nullableString("newGroupName"),
                            oldPortionGrams = change.nullableInt("oldPortionGrams"),
                            newPortionGrams = change.nullableInt("newPortionGrams"),
                            oldCatalogFoodId = change.nullableLong("oldCatalogFoodId"),
                            newCatalogFoodId = change.nullableLong("newCatalogFoodId"),
                            oldCatalogRelease = change.nullableString("oldCatalogRelease"),
                            newCatalogRelease = change.nullableString("newCatalogRelease"),
                        )
                    } }
                },
            )
            require(MealCorrectionMemoryPolicy.valid(memory)) { "Invalid meal correction memory." }
            val linkedMeal = mealById[memory.mealId]
                ?: throw IllegalArgumentException("Meal correction memory has no matching meal.")
            require(linkedMeal.recordedAt == memory.recordedAt && linkedMeal.source in setOf("ai_estimate", "member_confirmed_catalog")) {
                "Meal correction memory does not match an eligible meal."
            }
            memory
        }
        require(decoded.map(MealCorrectionMemory::mealId).distinct().size == decoded.size) {
            "Duplicate meal correction memories."
        }
        return decoded
    }

    private fun JSONObject.nullableString(name: String): String? =
        if (!has(name) || isNull(name)) null else getString(name)

    private fun JSONObject.nullableInt(name: String): Int? = if (!has(name) || isNull(name)) null else {
        val number = get(name) as? Number ?: throw IllegalArgumentException("Expected integer field: $name")
        val longValue = number.toLong()
        require(number.toDouble() == longValue.toDouble() && longValue in Int.MIN_VALUE..Int.MAX_VALUE) {
            "Expected integer field: $name"
        }
        longValue.toInt()
    }

    private fun JSONObject.nullableLong(name: String): Long? = if (!has(name) || isNull(name)) null else {
        val number = get(name) as? Number ?: throw IllegalArgumentException("Expected integer field: $name")
        val longValue = number.toLong()
        require(number.toDouble() == longValue.toDouble()) { "Expected integer field: $name" }
        longValue
    }
}

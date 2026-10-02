package app.metria

import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MealCorrectionMemoryTest {
    private val timestamp = Instant.parse("2026-09-28T10:00:00Z")

    @Test fun `normalization retains only the field changed by each signal`() {
        val before = MealReviewSnapshot(listOf(item(name = "Rice", portion = 100)))
        val after = MealReviewSnapshot(listOf(item(name = "Cooked rice", portion = 150)))
        val changes = (MealCorrectionDiff.diff(before, after) as MealCorrectionDiffResult.Valid).changes
        val memory = MealCorrectionMemory("meal-1", timestamp, changes)

        val normalized = MealCorrectionMemoryPolicy.normalized(memory)

        assertNotNull(normalized)
        assertTrue(MealCorrectionMemoryPolicy.valid(normalized!!))
        val rename = normalized.changes.single { it.kind == MealCorrectionKind.RENAMED }
        assertEquals("Rice", rename.oldName)
        assertEquals("Cooked rice", rename.newName)
        assertNull(rename.oldGroupId)
        assertNull(rename.oldPortionGrams)
        val portion = normalized.changes.single { it.kind == MealCorrectionKind.PORTION_CHANGED }
        assertEquals(100, portion.oldPortionGrams)
        assertEquals(150, portion.newPortionGrams)
        assertNull(portion.oldName)
        assertNull(portion.newGroupId)
    }

    @Test fun `policy rejects malformed and unbounded memories`() {
        val valid = MealCorrectionMemory("meal-1", timestamp, listOf(
            MealCorrectionChange(MealCorrectionKind.RENAMED, "item-1", oldName = "Rice", newName = "Cooked rice"),
        ))

        assertTrue(MealCorrectionMemoryPolicy.valid(valid))
        assertFalse(MealCorrectionMemoryPolicy.valid(valid.copy(mealId = "bad id")))
        assertFalse(MealCorrectionMemoryPolicy.valid(valid.copy(changes = listOf(
            valid.changes.single().copy(oldPortionGrams = 100),
        ))))
        assertNull(MealCorrectionMemoryPolicy.normalized(valid.copy(changes = emptyList())))
        assertNull(MealCorrectionMemoryPolicy.normalized(valid.copy(changes = List(MealCorrectionMemoryPolicy.MAX_CHANGES_PER_RECORD + 1) {
            valid.changes.single().copy(itemId = "item-$it")
        })))
    }

    @Test fun `memory retention replaces by meal and stays within the record cap`() {
        val original = (0 until MealCorrectionMemoryPolicy.MAX_RECORDS).map { index ->
            memory("meal-$index", timestamp.plusSeconds(index.toLong()))
        }
        val replacement = memory("meal-50", timestamp.plusSeconds(500))
        val replaced = MealCorrectionMemoryPolicy.withRecord(original, replacement)
        val next = MealCorrectionMemoryPolicy.withRecord(replaced, memory("meal-new", timestamp.plusSeconds(600)))

        assertEquals(MealCorrectionMemoryPolicy.MAX_RECORDS, next.size)
        assertEquals(1, replaced.count { it.mealId == "meal-50" })
        assertEquals(replacement, replaced.last())
        assertFalse(next.any { it.mealId == "meal-0" })
    }

    @Test fun `codec reads legacy payload without optional field as empty`() {
        assertTrue(MealCorrectionMemoryCodec.decode(JSONObject().put("version", 1), listOf(meal())).isEmpty())
    }

    @Test fun `codec round trips optional fields through serialized version one payload`() {
        val memory = memory("meal-1", timestamp)
        val meal = meal()
        val root = JSONObject().put("version", 1)
            .put(MealCorrectionMemoryCodec.PAYLOAD_FIELD, MealCorrectionMemoryCodec.encode(listOf(memory)))
        val decoded = MealCorrectionMemoryCodec.decode(JSONObject(root.toString()), listOf(meal))

        assertEquals(listOf(memory), decoded)
        val encodedChange = root.getJSONArray(MealCorrectionMemoryCodec.PAYLOAD_FIELD)
            .getJSONObject(0).getJSONArray("changes").getJSONObject(0)
        assertFalse(encodedChange.has("oldPortionGrams"))
        assertNull(decoded.single().changes.single().oldPortionGrams)
    }

    @Test fun `codec rejects malformed memories and dangling meal links`() {
        val memory = memory("meal-1", timestamp)
        val meal = meal()
        val dangling = JSONObject().put("version", 1).put(
            MealCorrectionMemoryCodec.PAYLOAD_FIELD,
            MealCorrectionMemoryCodec.encode(listOf(memory.copy(mealId = "missing"))),
        )
        assertTrue(runCatching { MealCorrectionMemoryCodec.decode(dangling, listOf(meal)) }.isFailure)

        val malformedChange = JSONObject().put("kind", "RENAMED").put("itemId", "item-1")
            .put("oldName", "").put("newName", "Cooked rice")
        val malformed = JSONObject().put("mealId", "meal-1").put("recordedAt", timestamp.toString())
            .put("changes", JSONArray().put(malformedChange))
        val root = JSONObject().put("version", 1).put(
            MealCorrectionMemoryCodec.PAYLOAD_FIELD,
            JSONArray().put(malformed),
        )
        assertTrue(runCatching { MealCorrectionMemoryCodec.decode(root, listOf(meal)) }.isFailure)
    }

    private fun memory(id: String, at: Instant) = MealCorrectionMemory(
        id,
        at,
        listOf(MealCorrectionChange(MealCorrectionKind.RENAMED, "item-1", oldName = "Rice", newName = "Cooked rice")),
    )

    private fun item(name: String, portion: Int) = MealReviewItemSnapshot(
        itemId = "item-1",
        name = name,
        groupId = "group-1",
        groupName = "Lunch",
        portionGrams = portion,
        catalogFoodId = null,
        catalogRelease = null,
    )

    private fun meal() = MealEntry(
        id = "meal-1",
        recordedAt = timestamp,
        items = listOf(MealEntryItem("Rice", 100, null)),
        assumptions = emptyList(),
        confidence = "medium",
        source = "ai_estimate",
    )
}

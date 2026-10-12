package app.metria

import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionAssistantPolicyTest {
    private val today = LocalDate.of(2026, 10, 12)

    @Test
    fun `goal request keeps normalized member wording and host-selected date and metric`() {
        val result = CompanionAssistantPolicy.resolve(
            RequiredAgentTool("read_nutrition_summary", period = "today", metric = "protein"),
            "How much protein do I have left today?",
            selectedMealId = null,
            today = today,
        )

        assertEquals(CompanionAssistantPolicy.IntentKind.GOAL_REMAINING, result?.kind)
        assertEquals("how much protein do i have left today", result?.classifierText)
        assertEquals("goal_remaining", result?.classifierKey)
        assertEquals("protein", result?.metric)
        assertEquals(today, result?.requiredTool?.targetDate)
        assertNull(CompanionAssistantPolicy.resolve(
            RequiredAgentTool("read_nutrition_summary", period = "week", metric = "protein"),
            "How much protein do I have left today?", null, today,
        ))
        assertNull(CompanionAssistantPolicy.resolve(
            RequiredAgentTool("read_nutrition_summary", period = "today", metric = "protein"),
            "How much protein do I have left today?", UUID.randomUUID().toString(), today,
        ))
        assertNull(CompanionAssistantPolicy.resolve(null, "How much protein do I have left yesterday?", null, today))
    }

    @Test
    fun `saved recall and draft retain actual normalized member wording`() {
        val recall = CompanionAssistantPolicy.resolve(null, "Find my usual breakfast", null, today)
        val draft = CompanionAssistantPolicy.resolve(null, "I ate my usual breakfast", null, today)

        assertEquals(CompanionAssistantPolicy.IntentKind.SAVED_RECALL, recall?.kind)
        assertEquals("find my usual breakfast", recall?.classifierText)
        assertEquals("breakfast", recall?.query)
        assertEquals(CompanionAssistantPolicy.IntentKind.SAVED_DRAFT, draft?.kind)
        assertEquals("i ate my usual breakfast", draft?.classifierText)
        assertEquals("saved_draft", draft?.classifierKey)
        assertNull(CompanionAssistantPolicy.resolve(null, "I had my usual breakfast and save it", null, today))
    }

    @Test
    fun `saved dish phrasing extracts bounded exact names for draft and recall`() {
        val draftPhrases = listOf(
            "I had my saved lentil stew",
            "I had my saved dish lentil stew",
        )
        draftPhrases.forEach { phrase ->
            val resolution = requireNotNull(CompanionAssistantPolicy.resolve(null, phrase, null, today))
            assertEquals(CompanionAssistantPolicy.IntentKind.SAVED_DRAFT, resolution.kind)
            assertEquals("lentil stew", resolution.query)
            assertEquals("lentil stew", resolution.requiredTool?.query)
        }

        val recallPhrases = listOf(
            "Find my lentil stew dish",
            "Show my lentil stew dish",
            "Look up my lentil stew dish",
            "Find my saved lentil stew",
            "Find my saved dish lentil stew",
        )
        recallPhrases.forEach { phrase ->
            val resolution = requireNotNull(CompanionAssistantPolicy.resolve(null, phrase, null, today))
            assertEquals(CompanionAssistantPolicy.IntentKind.SAVED_RECALL, resolution.kind)
            assertEquals("lentil stew", resolution.query)
            assertEquals("lentil stew", resolution.requiredTool?.query)
        }

        val exactHost = RequiredAgentTool("search_saved_dishes", query = "lentil stew")
        assertEquals(
            "lentil stew",
            CompanionAssistantPolicy.resolve(exactHost, "I had my saved dish lentil stew", null, today)?.query,
        )
        assertNull(CompanionAssistantPolicy.resolve(
            exactHost.copy(query = "dish lentil stew"), "I had my saved dish lentil stew", null, today,
        ))
        listOf(
            "I had my saved dish lentil stew and log it",
            "I had my saved dish lentil stew yesterday",
            "Find my lentil stew dish and add it",
            "Find my lentil stew dish yesterday",
        ).forEach { phrase ->
            assertNull(phrase, CompanionAssistantPolicy.resolve(null, phrase, null, today))
        }
    }

    @Test
    fun `saved dish replies bind names and draft choices to audited search results`() {
        val query = "lentil stew"
        val single = savedDishResponse(query, listOf("Lentil stew"))
        val singleDraft = requireNotNull(CompanionAssistantPolicy.savedDishReply(single, query, draft = true))
        assertEquals(
            "I found the saved dish “Lentil stew”. Review its foods and portions before adding it as a meal. This is an unsaved draft.",
            singleDraft,
        )
        assertAuditedSavedDishReply(singleDraft, single)

        val multiple = savedDishResponse(query, listOf("Lentil stew", "Bean bake"))
        val multipleDraft = requireNotNull(CompanionAssistantPolicy.savedDishReply(multiple, query, draft = true))
        assertTrue(multipleDraft.contains("Lentil stew") && multipleDraft.contains("Bean bake"))
        assertTrue(multipleDraft.contains("Choose one to review"))
        assertTrue(multipleDraft.contains("cannot tell which is usual"))
        assertTrue(multipleDraft.contains("unsaved draft"))
        assertAuditedSavedDishReply(multipleDraft, multiple)

        val recall = requireNotNull(CompanionAssistantPolicy.savedDishReply(single, query, draft = false))
        assertAuditedSavedDishReply(recall, single)

        val noMatch = savedDishResponse(query, emptyList())
        val emptyRecall = requireNotNull(CompanionAssistantPolicy.savedDishReply(noMatch, query, draft = false))
        assertAuditedSavedDishReply(emptyRecall, noMatch)
        assertEquals(emptyRecall, CompanionAssistantPolicy.savedDishReply(noMatch, query, draft = true))

        assertNull(CompanionAssistantPolicy.savedDishReply(single + ("query" to "bean bake"), query, draft = true))
        assertNull(CompanionAssistantPolicy.savedDishReply(single + ("source" to "remote"), query, draft = true))
        assertNull(CompanionAssistantPolicy.savedDishReply(single + ("total_count" to 2), query, draft = true))
        assertNull(CompanionAssistantPolicy.savedDishReply(single + ("coverage" to "latest_records_only"), query, draft = true))
        val invalidName = single + ("matches" to listOf(mapOf(
            "name" to "", "ingredients" to listOf("rice"), "confirmation_count" to 1,
        )))
        assertNull(CompanionAssistantPolicy.savedDishReply(invalidName, query, draft = true))
        assertNull(MetriaAgentFactAuditor.audit(
            "I found the saved dish “Bean bake”.",
            MetriaAgentFactEvidence.from("search_saved_dishes", single),
        ).acceptedText)
    }

    private fun savedDishResponse(query: String, names: List<String>): Map<String, Any?> = mapOf(
        "source" to "metria_local_vault",
        "reply_language" to "English",
        "query" to query,
        "total_count" to names.size,
        "returned_count" to names.size,
        "omitted_count" to 0,
        "coverage" to "full",
        "matches" to names.map { name -> mapOf(
            "name" to name,
            "ingredients" to listOf("rice", "beans"),
            "confirmation_count" to 1,
        ) },
        "summary" to "Synthetic test result",
    )

    private fun assertAuditedSavedDishReply(reply: String, response: Map<String, Any?>) {
        assertEquals(
            reply,
            MetriaAgentFactAuditor.audit(
                reply, MetriaAgentFactEvidence.from("search_saved_dishes", response),
            ).acceptedText,
        )
    }

    @Test
    fun `only one exact dish name or alias is an exact draft match`() {
        val first = dish(name = "Oat bowl", aliases = listOf("my breakfast"))
        val second = dish(name = "Egg toast")

        val exact = CompanionAssistantPolicy.savedDishDraftChoice("my breakfast", listOf(PersonalDishMatch(first, 0.8, emptySet())))
        val possible = CompanionAssistantPolicy.savedDishDraftChoice("breakfast", listOf(PersonalDishMatch(second, 0.7, emptySet())))
        val ambiguous = CompanionAssistantPolicy.savedDishDraftChoice(
            "breakfast",
            listOf(PersonalDishMatch(first, 0.8, emptySet()), PersonalDishMatch(second, 0.7, emptySet())),
        )

        assertEquals(first.id, exact.exactCandidateId)
        assertNull(possible.exactCandidateId)
        assertEquals(1, possible.candidates.size)
        assertNull(ambiguous.exactCandidateId)
        assertEquals(2, ambiguous.candidates.size)
    }

    @Test
    fun `saved dish prefill preserves unknown portions and catalog release`() {
        val source = dish(name = "Usual breakfast", items = listOf(
            MealEntryItem(name = "Oats", grams = null, preparation = null, catalogFoodId = 123L, catalogRelease = "foundation-v1"),
            MealEntryItem(name = "Milk", grams = 180, preparation = null, catalogFoodId = 456L, catalogRelease = "foundation-v1"),
        ))

        val prefill = CompanionAssistantPolicy.savedDishPrefill(source)

        assertEquals(listOf(null, 180), prefill?.items?.map { it.grams })
        assertEquals(listOf("foundation-v1", "foundation-v1"), prefill?.items?.map { it.catalogRelease })
        assertEquals(listOf(123L, 456L), prefill?.items?.map { it.catalogFoodId })
    }

    @Test
    fun `navigation needs explicit open wording and does not steal meal reads`() {
        val history = CompanionAssistantPolicy.resolve(null, "Open my weight history", null, today)
        val todayMeals = MiniLmMealReadPolicy.resolveReadRequest(null, "Show my meals for today", null, today)

        assertEquals(CompanionAssistantPolicy.IntentKind.NAVIGATION, history?.kind)
        assertEquals(CompanionAssistantPolicy.Destination.HISTORY, history?.destination)
        assertNull(CompanionAssistantPolicy.resolve(null, "Show my meals for today", null, today))
        assertEquals("read_meals", todayMeals?.requiredTool?.name)
        assertFalse(todayMeals?.companion?.kind == CompanionAssistantPolicy.IntentKind.NAVIGATION)
        assertNull(CompanionAssistantPolicy.resolve(null, "Show privacy settings", null, today))
    }

    @Test
    fun `weight history spelling alias preserves the closed navigation boundaries`() {
        listOf("open my weight hystory", "Could you open my weight hystory, please?").forEach { phrase ->
            val resolution = requireNotNull(CompanionAssistantPolicy.resolve(null, phrase, null, today))
            assertEquals(CompanionAssistantPolicy.IntentKind.NAVIGATION, resolution.kind)
            assertEquals(CompanionAssistantPolicy.Destination.HISTORY, resolution.destination)
            assertEquals("open my weight history", resolution.classifierText)
            assertNull(resolution.requiredTool)
        }
        listOf(
            "open my weight hystory and log 80 kg",
            "open my weight hystory yesterday",
            "open someone else's weight hystory",
            "what should my weight hystory look like",
        ).forEach { phrase ->
            assertNull(phrase, CompanionAssistantPolicy.resolve(null, phrase, null, today))
        }
        assertNull(CompanionAssistantPolicy.resolve(null, "open my weight hystory", "selected-meal", today))
        assertNull(CompanionAssistantPolicy.resolve(
            RequiredAgentTool("read_weight"), "open my weight hystory", null, today,
        ))
        assertEquals("open my medical hystory", MiniLmReadTextNormalization.normalize("open my medical hystory"))
    }

    private fun dish(
        name: String,
        aliases: List<String> = emptyList(),
        items: List<MealEntryItem> = listOf(MealEntryItem("Oats", 50, preparation = null)),
    ): PersonalDish = PersonalDish(
        id = UUID.randomUUID().toString(),
        name = name,
        aliases = aliases,
        items = items,
    )
}

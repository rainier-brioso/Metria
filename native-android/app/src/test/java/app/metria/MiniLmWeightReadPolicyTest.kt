package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MiniLmWeightReadPolicyTest {
    private val today = LocalDate.parse("2026-10-12")

    @Test
    fun copiedLatestWightQuestionNormalizesAndRequiresItsOwnRead() {
        val question = "could you show me the latest wight, please?"
        val host = MetriaAgentGroundingPolicy.requiredTool(question, today = today)
        val resolved = MiniLmMealReadPolicy.resolveReadRequest(host, question, null, today)!!
        assertEquals("show me the latest weight", resolved.classifierText)
        assertEquals("latest_weight", resolved.requiredTool.metric)
        assertEquals(90, resolved.requiredTool.fromDaysAgo)
        assertTrue(resolved.allowMiniLm)
        assertNotNull(MiniLmMealReadPolicy.validatedFastReadReply(
            resolved.requiredTool, response(today, 90, listOf(today.toString() to 72.4)),
        ))
        listOf(
            "could you show me the latest height, please?",
            "could you show me the latest wight of my wife, please?",
            "could you show me the latest wight yesterday, please?",
            "could you show me the latest wight and meals, please?",
            "could you save the latest wight, please?",
        ).forEach { assertNull(it, MiniLmMealReadPolicy.resolveReadRequest(null, it, null, today)) }
    }

    @Test
    fun politeLatestWeightQuestionUsesTheRealHostRequirementAndAuditedRead() {
        val question = "Could you show me my latest weight, please?"
        val host = MetriaAgentGroundingPolicy.requiredTool(question, today = today)
        val resolved = MiniLmMealReadPolicy.resolveReadRequest(host, question, null, today)!!
        assertEquals("show me my latest weight", resolved.classifierText)
        assertEquals("read_weight", resolved.requiredTool.name)
        assertEquals("latest_weight", resolved.requiredTool.metric)
        assertNotNull(MiniLmMealReadPolicy.validatedFastReadReply(
            resolved.requiredTool, response(today, 90, listOf(today.toString() to 72.4)),
        ))
    }

    @Test
    fun acceptsOnlyClosedEnglishWeightReadPhrases() {
        listOf(
            "What's my latest weight?", "What is my most recent weight", "When did I last weigh myself?",
            "When was my last weigh-in", "Show my recent weigh-ins", "List my weigh-ins from the last 30 days",
            "when did I last weight myself?",
            "Show my resent wigh-ins", "Show my recent wigh-ins", "Show my resent weigh-ins",
        ).forEach { assertTrue(it, MiniLmWeightReadPolicy.isInScope(it)) }
        listOf(
            "When did I weigh 2 days ago", "Show my weight and meals", "How can I lose weight?",
            "What is my weight goal?", "Add my weight", "What's my weight on 2026-10-01?", "Show selected meal weight",
            "Tell me my weight and recommend a diet", "When was my last weigh-in? Also show calories",
            "What was my weight yesterday", "Show my weigh-ins last week", "What's my latest weight and protein?",
            "When did I last weight myself today", "When did I last weight myself and show meals",
            "Show my resent wigh-ins and meals", "Add my resent wigh-ins", "Show my resent wigh-ins yesterday",
        ).forEach { assertFalse(it, MiniLmWeightReadPolicy.isInScope(it)) }
        assertFalse(MiniLmWeightReadPolicy.isInScope("x".repeat(501)))
    }

    @Test
    fun recentSpellingAliasesKeepClassifierAndHostReadAligned() {
        val resolved = MiniLmMealReadPolicy.resolveReadRequest(null, "Show my resent wigh-ins", null, today)!!
        assertEquals("show my recent weigh-ins", resolved.classifierText)
        assertEquals("recent_weigh_ins", resolved.requiredTool.metric)
        assertEquals(29, resolved.requiredTool.fromDaysAgo)
        assertEquals(0, resolved.requiredTool.toDaysAgo)
        assertTrue(MiniLmMealReadPolicy.isInScope(resolved.classifierText, "read_weight"))
        assertNotNull(MiniLmMealReadPolicy.validatedFastReadReply(
            resolved.requiredTool, response(today, 29, listOf(today.toString() to 72.4)),
        ))
        val selectedRead = MiniLmMealReadPolicy.resolveReadRequest(null, "Show my resent wigh-ins", "selected", today)!!
        assertFalse(selectedRead.allowMiniLm)
        assertEquals("read_weight", MiniLmMealReadPolicy.gemmaFallbackRequiredTool(selectedRead, null)?.name)
    }

    @Test
    fun selectedContextDisablesWeightShortcutButKeepsFreshGemmaReadAndFixedReasons() {
        val question = "Could you show me my latest weight, please?"
        val resolved = MiniLmMealReadPolicy.resolveReadRequest(null, question, "selected", today)!!
        assertFalse(resolved.allowMiniLm)
        assertEquals(90, resolved.requiredTool.fromDaysAgo)
        assertEquals(null, MiniLmMealReadPolicy.gemmaFallbackRequiredTool(resolved, null)?.targetDate)
        assertEquals("scope_selected", MiniLmMealReadPolicy.scopeRejectionReason(question, "selected"))
        assertEquals("scope_host", MiniLmMealReadPolicy.scopeRejectionReason(question, null))
        assertEquals("scope_text", MiniLmMealReadPolicy.scopeRejectionReason("Show my weight and meals", null))
        assertEquals("scope_context", MiniLmMealReadPolicy.scopeRejectionReason("And protein?", null))
        assertNull(MiniLmMealReadPolicy.resolveReadRequest(
            RequiredAgentTool("read_meals", daysAgo = 0), question, "selected", today,
        ))
    }

    @Test
    fun requestsAreBoundedAndFreezeTheirAsOfDate() {
        val cases = listOf(
            "What's my latest weight" to (MiniLmWeightReadPolicy.Kind.LATEST to 90),
            "When did I last weigh myself" to (MiniLmWeightReadPolicy.Kind.LAST_WEIGHED to 90),
            "when did I last weight myself?" to (MiniLmWeightReadPolicy.Kind.LAST_WEIGHED to 90),
            "Show my recent weigh-ins" to (MiniLmWeightReadPolicy.Kind.RECENT to 29),
        )
        cases.forEach { (text, expected) ->
            val resolved = MiniLmWeightReadPolicy.resolve(null, text, null, today)!!
            assertEquals("read_weight", resolved.requiredTool.name)
            assertEquals(expected.first.metric, resolved.requiredTool.metric)
            assertEquals(expected.second, resolved.requiredTool.fromDaysAgo)
            assertEquals(0, resolved.requiredTool.toDaysAgo)
            assertEquals(today, resolved.requiredTool.targetDate)
            assertNotNull(MiniLmMealReadPolicy.validatedFastReadReply(
                resolved.requiredTool, response(today, expected.second, emptyList()),
            ))
        }
        val bounded = MiniLmMealReadPolicy.resolveReadRequest(
            null, "What's my latest weight", null, today,
        )!!
        assertEquals(null, MiniLmMealReadPolicy.gemmaFallbackRequiredTool(bounded, null)?.targetDate)
        val typoRead = MiniLmMealReadPolicy.resolveReadRequest(null, "when did I last weight myself?", null, today)!!
        assertEquals("last_weighed", typoRead.requiredTool.metric)
        assertEquals(90, MiniLmMealReadPolicy.gemmaFallbackRequiredTool(typoRead, null)?.fromDaysAgo)
        val oldHostRead = RequiredAgentTool("read_weight", fromDaysAgo = 4, toDaysAgo = 4, targetDate = today.minusDays(4))
        assertEquals(oldHostRead, MiniLmMealReadPolicy.gemmaFallbackRequiredTool(null, oldHostRead))
        assertNull(MiniLmWeightReadPolicy.resolve(null, "What's my latest weight", "selected", today))
        assertNull(MiniLmWeightReadPolicy.resolve(
            RequiredAgentTool("read_weight", fromDaysAgo = 3, toDaysAgo = 0),
            "What's my latest weight", null, today,
        ))
        assertNull(MiniLmWeightReadPolicy.resolve(
            RequiredAgentTool("read_meals", daysAgo = 0), "What's my latest weight", null, today,
        ))
    }

    @Test
    fun latestAndDateAnswersUseLatestRetainedRecordAndAuditSameDayRows() {
        val weights = listOf(
            "2026-10-10" to 72.15,
            "2026-10-12" to 72.35,
            "2026-10-12" to 72.45,
        )
        val latestCandidate = "Your latest saved weight in the read window was 72.45 kg, recorded on 2026-10-12 in your records. Read window: from 2026-07-14 through 2026-10-12 inclusive."
        assertEquals(
            latestCandidate,
            MetriaAgentFactAuditor.audit(
                latestCandidate, MetriaAgentFactEvidence.from("read_weight", response(today, 90, weights)),
            ).acceptedText,
        )
        for (phrase in listOf("What's my latest weight", "When did I last weigh myself", "when did I last weight myself?")) {
            val tool = MiniLmWeightReadPolicy.resolve(null, phrase, null, today)!!.requiredTool
            val result = MiniLmWeightReadPolicy.validatedReply(tool, response(today, 90, weights))
            assertNotNull(result)
            assertTrue(result!!, result.contains("2026-10-12"))
            if (phrase.startsWith("What's")) assertTrue(result, result.contains("72.45 kg"))
            assertTrue(result, result.contains("2026-07-14 through 2026-10-12 inclusive"))
        }
    }

    @Test
    fun recentReadDisclosesExactWindowAndOmissions() {
        val tool = MiniLmWeightReadPolicy.resolve(null, "Show my recent weigh-ins", null, today)!!.requiredTool
        val all = (0 until 27).map { today.minusDays((26 - it).toLong()).toString() to (70.0 + it / 10.0) }
        val visible = all.takeLast(24)
        val result = MiniLmWeightReadPolicy.validatedReply(tool, response(today, 29, visible, total = 27, omitted = 3))
        assertNotNull(result)
        assertTrue(result!!, result.contains("2026-09-13 through 2026-10-12 inclusive"))
        assertTrue(result, result.contains("27 weight entries were recorded in this window. 3 weight entries were omitted."))
        assertFalse(result, result.contains(all.first().first))
        val noRows = MiniLmWeightReadPolicy.validatedReply(tool, response(today, 29, emptyList()))
        assertTrue(noRows!!, noRows.contains("No weight entries were saved within the read window. Read window: from 2026-09-13 through 2026-10-12 inclusive"))
    }

    @Test
    fun rejectsMalformedWeightPayloadAndDateRollover() {
        val tool = MiniLmWeightReadPolicy.resolve(null, "What's my latest weight", null, today)!!.requiredTool
        val valid = response(today, 90, listOf(today.toString() to 72.4))
        listOf(
            valid + ("source" to "other"),
            valid + ("reply_language" to "Spanish"),
            valid + ("as_of" to today.plusDays(1).toString()),
            valid + ("from" to today.minusDays(89).toString()),
            valid + ("returned_count" to 2),
            valid + ("total_count" to 0),
            valid + ("coverage" to "latest_records_only"),
            valid + ("entries" to listOf(mapOf("date" to "not-a-date", "kilograms" to 72.4))),
            valid + ("entries" to listOf(mapOf("date" to today.plusDays(1).toString(), "kilograms" to 72.4))),
            valid + ("entries" to listOf(mapOf("date" to today.toString(), "kilograms" to Double.NaN))),
            valid + ("entries" to listOf(mapOf("date" to today.toString(), "kilograms" to 0.0))),
        ).forEach { assertNull(MiniLmWeightReadPolicy.validatedReply(tool, it)) }
        assertNull(MiniLmWeightReadPolicy.validatedReply(tool.copy(targetDate = today.minusDays(1)), valid))
        val unordered = response(today, 90, listOf(today.toString() to 72.0, today.minusDays(1).toString() to 71.9))
        assertNull(MiniLmWeightReadPolicy.validatedReply(tool, unordered))
    }

    private fun response(
        asOf: LocalDate,
        fromOffset: Int,
        entries: List<Pair<String, Double>>,
        total: Int = entries.size,
        omitted: Int = total - entries.size,
    ): Map<String, Any?> = mapOf(
        "source" to "metria_local_vault",
        "reply_language" to "English",
        "as_of" to asOf.toString(),
        "from" to asOf.minusDays(fromOffset.toLong()).toString(),
        "to" to asOf.toString(),
        "total_count" to total,
        "returned_count" to entries.size,
        "omitted_count" to omitted,
        "coverage" to if (omitted == 0) "full" else "latest_records_only",
        "entries" to entries.map { (date, kg) -> mapOf("date" to date, "kilograms" to kg) },
    )
}

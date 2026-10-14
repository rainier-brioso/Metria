package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MiniLmFlexibleReadPolicyTest {
    private val today = LocalDate.parse("2026-10-14")

    @Test
    fun composedLocalCorrectionsStayInsideDebugRouterScope() {
        val question = "give me my recored protein tot for today?"
        val host = MetriaAgentGroundingPolicy.requiredTool(question, today = today)
        val corrected = MiniLmSpellCorrectionPolicy.resolve(
            host, question, null, today, EnglishSpellChecker { _, _ ->
                error("This fully local correction should not call Android")
            },
        ) { true }

        assertEquals(SpellCheckReason.ACCEPTED, corrected.reason)
        assertEquals(
            "read_day_nutrition",
            corrected.resolvedRead?.classifierKey,
        )
        assertTrue(
            MiniLmMealReadPolicy.isMiniLmRouterInputInScope(
                requireNotNull(corrected.resolvedRead).classifierText,
                corrected.resolvedRead!!.classifierKey,
            ),
        )
    }

    @Test
    fun compositionalPhrasingResolvesWithRealHostGroundingAndKeepsOriginalClassifierText() {
        val cases = listOf(
            "Today, can I see my recorded meals?" to "read_meals",
            "What is in my food log for yesterday?" to "read_meals",
            "Give me my recorded protein total for today" to "read_day_nutrition",
            "For yesterday, how many calories did I log?" to "read_day_nutrition",
            "Tell me the protein content of my most recent meal" to "read_latest_meal",
            "What is the most recent weight I recorded?" to "read_weight",
            "On what date was my last weigh-in?" to "read_weight",
            "Can I see my recent weigh-ins?" to "read_weight",
        )
        cases.forEach { (question, expectedTool) ->
            val actualHostTool = MetriaAgentGroundingPolicy.requiredTool(question, today = today)
            val resolved = MiniLmMealReadPolicy.resolveReadRequest(actualHostTool, question, null, today)
            assertEquals(question, expectedTool, resolved?.requiredTool?.name)
            assertEquals(question, MiniLmReadTextNormalization.normalize(question), resolved?.classifierText)
            assertTrue(question, MiniLmMealReadPolicy.isMiniLmRouterInputInScope(
                requireNotNull(resolved).classifierText,
                MiniLmFlexibleReadPolicy.classifierKey(requireNotNull(MiniLmFlexibleReadPolicy.parse(question)).kind),
            ))
            val expectedDate = when {
                question.contains("yesterday", ignoreCase = true) -> today.minusDays(1)
                expectedTool == "read_meals" || question.contains("today", ignoreCase = true) -> today
                expectedTool == "read_weight" -> today
                else -> null
            }
            if (expectedDate != null) assertEquals(question, expectedDate, resolved?.requiredTool?.targetDate)
        }
    }

    @Test
    fun requestHeadsSubjectsAndDatePositionsComposeWithoutChangingTypedOperation() {
        listOf(
            "Today, can I see my recorded meals?" to LocalDate.parse("2026-10-14"),
            "For yesterday, can I see my recorded meals?" to LocalDate.parse("2026-10-13"),
            "List my saved foods for today" to LocalDate.parse("2026-10-14"),
            "List my saved foods from yesterday" to LocalDate.parse("2026-10-13"),
            "What are recorded meals today" to LocalDate.parse("2026-10-14"),
            "What are recorded meals yesterday" to LocalDate.parse("2026-10-13"),
        ).forEach { (question, date) ->
            val host = MetriaAgentGroundingPolicy.requiredTool(question, today = today)
            val resolved = MiniLmMealReadPolicy.resolveReadRequest(host, question, null, today)
            assertEquals(question, "read_meals", resolved?.requiredTool?.name)
            assertEquals(question, date, resolved?.requiredTool?.targetDate)
        }
    }

    @Test
    fun parserConsumesOrderedRolesAndRejectsMeaningChangesEvenWithKnownWords() {
        listOf(
            "what save my protein today",
            "record my protein total today",
            "show my meals today instead of yesterday",
            "show my last meal instead today",
            "what is my protein intake instead today",
            "show my latest weigh-in",
            "what is in my food log for yesterday, save",
            "show my meals and weight today",
            "What did my partner eat yesterday",
            "Find my saved breakfast today",
            "How many calories and how much protein today",
            "Show meals today or yesterday",
            "Show meals two days ago",
            "Should I eat more protein today",
            "Show my food log for breakfast today",
        ).forEach { assertNull(it, MiniLmFlexibleReadPolicy.parse(it)) }
    }

    @Test
    fun flexibleReadsRespectSelectedContextHostConflictsAndFrozenDate() {
        val question = "For yesterday, how many calories did I log?"
        val host = MetriaAgentGroundingPolicy.requiredTool(question, today = today)
        assertNull(MiniLmMealReadPolicy.resolveReadRequest(host, question, "selected-meal", today))
        assertNull(MiniLmMealReadPolicy.resolveReadRequest(
            RequiredAgentTool("read_day_nutrition", period = "historical_day", daysAgo = 1, metric = "protein"),
            question,
            null,
            today,
        ))
        val frozen = requireNotNull(MiniLmMealReadPolicy.resolveReadRequest(host, question, null, today))
        assertEquals(today.minusDays(1), frozen.requiredTool.targetDate)
        assertEquals(1, frozen.requiredTool.daysAgo)
        assertEquals("calories", frozen.requiredTool.metric)
        assertEquals("for yesterday, how many calories did i log", frozen.classifierText)
    }

    @Test
    fun recordedProteinTotalIgnoresSurroundingHistoryAndTrimsWhitespaceButDoesNotCorrectDateTypos() {
        val question = "Give me my recorded protein total for today"
        val contexts = listOf(
            emptyList<PrivateConversationMessage>() to null,
            listOf(
                PrivateConversationMessage("What's my calorie total today?", fromMember = true),
                PrivateConversationMessage("Your recorded calorie estimate is 420 kcal.", fromMember = false),
            ) to MiniLmMealReadPolicy.FollowUpScope(
                MiniLmMealReadPolicy.FollowUpOperation.TODAY_NUTRITION, today, metric = "calories",
            ),
            listOf(
                PrivateConversationMessage("How much protein was in my latest meal?", fromMember = true),
                PrivateConversationMessage("The latest meal had 18 g protein.", fromMember = false),
            ) to MiniLmMealReadPolicy.FollowUpScope(
                MiniLmMealReadPolicy.FollowUpOperation.LATEST_MEAL, today, metric = "protein",
            ),
            listOf(
                PrivateConversationMessage("What did I eat yesterday?", fromMember = true),
                PrivateConversationMessage("You recorded a meal yesterday.", fromMember = false),
            ) to MiniLmMealReadPolicy.FollowUpScope(
                MiniLmMealReadPolicy.FollowUpOperation.TODAY_MEALS, today, dayOffset = 1,
            ),
        )
        val whitespaceVariants = listOf(question, "$question  ", "  $question  ", question.replace("my ", "my   "))
        contexts.forEach { (history, scope) ->
            whitespaceVariants.forEach { submitted ->
                val host = MetriaAgentGroundingPolicy.requiredTool(submitted, history, today)
                val resolved = MiniLmMealReadPolicy.resolveReadRequest(host, submitted, null, today, scope)
                assertEquals(submitted, "read_day_nutrition", resolved?.requiredTool?.name)
                assertEquals(submitted, "protein", resolved?.requiredTool?.metric)
                assertEquals(submitted, 0, resolved?.requiredTool?.daysAgo)
                assertEquals(submitted, today, resolved?.requiredTool?.targetDate)
                assertEquals(submitted, "give me my recorded protein total for today", resolved?.classifierText)
            }

            val typoDate = "Give me my recorded protein total for tday"
            val typoHost = MetriaAgentGroundingPolicy.requiredTool(typoDate, history, today)
            assertNull("date-word typo must abstain: $history", MiniLmMealReadPolicy.resolveReadRequest(
                typoHost, typoDate, null, today, scope,
            ))
        }
    }

    @Test
    fun explicitProteinTotalDoesNotRouteWithAnySelectedMealContext() {
        val question = "Give me my recorded protein total for today"
        val host = MetriaAgentGroundingPolicy.requiredTool(question, today = today)
            ?: RequiredAgentTool("read_nutrition_summary", period = "today", metric = "protein")
        assertNull(MiniLmMealReadPolicy.resolveReadRequest(host, question, "selected-meal", today))
        assertNull(MiniLmMealReadPolicy.resolveReadRequest(
            host.copy(selectedMealId = "selected-meal"), question, null, today,
        ))
        assertNull(MiniLmMealReadPolicy.resolveReadRequest(
            host.copy(selectedMealQuestion = "What is in this meal?"), question, null, today,
        ))
        val selectedHost = MetriaAgentGroundingPolicy.requiredTool(
            question, today = today, selectedMealId = "selected-meal",
        )
        assertNull(MiniLmMealReadPolicy.resolveReadRequest(selectedHost, question, "selected-meal", today))
    }

    @Test
    fun sharedTypoRetryCanReachFlexibleGrammarWithoutChangingExactRoutes() {
        val typo = "Today, can I see my recoreded meals?"
        val typoHost = MetriaAgentGroundingPolicy.requiredTool(typo, today = today)
        val resolvedTypo = requireNotNull(MiniLmMealReadPolicy.resolveReadRequest(typoHost, typo, null, today))
        assertEquals("read_meals", resolvedTypo.requiredTool.name)
        assertEquals("today, can i see my recorded meals", resolvedTypo.classifierText)
        val exact = requireNotNull(MiniLmMealReadPolicy.resolveReadRequest(
            null,
            "Show my resent wigh-ins",
            null,
            today,
        ))
        assertEquals("read_weight", exact.requiredTool.name)
        assertEquals("show my recent weigh-ins", exact.classifierText)
    }

    @Test
    fun fullFlexibleReadMatchAloneBypassesLegacyWriteGuard() {
        assertFalse(MetriaAgentMigrationPolicy.useLegacyWriteRoute("What is in my food log for yesterday?"))
        assertFalse(MetriaAgentMigrationPolicy.useLegacyWriteRoute("How many calories did I log today?"))
        assertFalse(MetriaAgentMigrationPolicy.useLegacyWriteRoute("Today, how much protien did I log?"))
        assertTrue(MetriaAgentMigrationPolicy.useLegacyWriteRoute("What is in my food log for yesterday, save"))
        assertTrue(MetriaAgentMigrationPolicy.useLegacyWriteRoute("Record my protein total today"))
    }

    @Test
    fun weightReadKindsKeepDistinctMeaningAndReadWindows() {
        listOf(
            Triple("What is the most recent weight I recorded?", "latest_weight", 90),
            Triple("On what date was my last weigh-in?", "last_weighed", 90),
            Triple("When was the most recent weigh-in?", "last_weighed", 90),
            Triple("Can I see my recent weigh-ins?", "recent_weigh_ins", 29),
        ).forEach { (question, metric, window) ->
            val host = MetriaAgentGroundingPolicy.requiredTool(question, today = today)
            val required = requireNotNull(MiniLmMealReadPolicy.resolveReadRequest(host, question, null, today)).requiredTool
            assertEquals(question, metric, required.metric)
            assertEquals(question, window, required.fromDaysAgo)
            assertEquals(question, 0, required.toDaysAgo)
            assertEquals(question, today, required.targetDate)
            assertNull(question, MiniLmMealReadPolicy.resolveReadRequest(
                RequiredAgentTool("read_weight", fromDaysAgo = 7, toDaysAgo = 0), question, null, today,
            ))
        }
    }

    @Test
    fun scopeChecksRemainBoundedAndDoNotEraseUnknownOrNegatedContent() {
        listOf(
            " ".repeat(501) + "Today, how much protien did I log?",
            "Today, how much protien did I log?\u0000",
            "Today, do not show my recorded meals",
            "Today, can I see my recorded meals without dinner",
            "Today, can I see my recorded meals, please and save a snack",
            "Today, can I see my recorded meals yesterday",
            "Anything recorded today",
            "When my meals today",
        ).forEach { assertFalse(it, MiniLmMealReadPolicy.isFlexibleReadInScope(it)) }
        val recognized = "Today, can I see my recorded meals?"
        assertEquals("scope_selected", MiniLmMealReadPolicy.scopeRejectionReason(recognized, "selected"))
        assertEquals("scope_host", MiniLmMealReadPolicy.scopeRejectionReason(recognized, null))
        assertFalse(MiniLmMealReadPolicy.isMiniLmRouterInputInScope(recognized, "read_weight"))
    }
}

package app.metria

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MiniLmMealReadPolicyTest {
    @Test
    fun acceptsOnlyNarrowTodayAndYesterdayMealListRequests() {
        listOf(
            "Show the meals I recorded today",
            "List my saved meals for today",
            "What meals have I logged today",
            "What did I log for meals today",
            "What did I eat today",
            "Which meals are in my record for today",
            "Show me the food I saved today",
            "Show today's meals",
            "Which meals are recorded today",
            "Show the meals I recorded yesterday",
            "List my saved meals for yesterday",
            "What meals have I logged yesterday",
            "What did I eat yesterday",
            "Which meals are in my record for yesterday",
            "Show me all the food I saved yesterday",
            "Show my meals from yesterday",
            "Remind me what I ate today",
            "Could you remind me what I ate yesterday?",
        ).forEach { assertTrue(it, MiniLmMealReadPolicy.isInScope(it)) }
        listOf("What have I eaten today?", "Please remind me of today’s meals, please").forEach {
            assertTrue(it, MiniLmMealReadPolicy.isInScope(it))
        }
    }

    @Test
    fun acceptsNaturalSinglePurposeLatestMealNutritionQuestions() {
        val today = LocalDate.parse("2026-10-11")
        val accepted = listOf(
            "Could you tell me how many calories were in my last meal?",
            "How much protein was in my latest meal?",
            "Please show me the calories in my most recent meal",
            "What was the protein in my last meal?",
            "How many calories did my latest meal have?",
            "Calories in my last meal?",
            "Protein in my latest meal?",
            "How many calories in my last meal?",
            "Could you please tell me what’s the protein in my latest meal, please?",
        )
        accepted.forEach { text ->
            assertTrue(text, MiniLmMealReadPolicy.isInScope(text, "read_latest_meal"))
            val hostTool = MetriaAgentGroundingPolicy.requiredTool(text, today = today)
            val selected = MiniLmMealReadPolicy.requiredReadTool(hostTool, text, null, today)
            assertEquals(text, "read_latest_meal", selected?.name)
            assertEquals(today, selected?.targetDate)
        }
        assertTrue(
            MiniLmMealReadPolicy.requiredReadTool(null, accepted.first(), null, today)?.targetDate == today,
        )
    }

    @Test
    fun normalizedParaphrasesKeepClosedToolAndClassifierAligned() {
        val today = LocalDate.parse("2026-10-12")
        val cases = listOf(
            "What have I eaten today?" to "read_meals",
            "Please remind me of today’s meals, please" to "read_meals",
            "How many calories in my last meal?" to "read_latest_meal",
            "Could you please tell me what’s the protein in my latest meal, please?" to "read_latest_meal",
            "What’s today’s protein total?" to "read_day_nutrition",
            "Could you tell me my calorie total for today, please?" to "read_day_nutrition",
            "Ｃｏｕｌｄ　ｙｏｕ　ｓｈｏｗ　ｍｅ　ｍｙ　ｌａｔｅｓｔ　ｗｅｉｇｈｔ？" to "read_weight",
            "When was my most recent weigh in?" to "read_weight",
            "Let me see my recent weigh-ins." to "read_weight",
        )
        cases.forEach { (phrase, toolName) ->
            val resolved = MiniLmMealReadPolicy.resolveReadRequest(null, phrase, null, today)
            assertNotNull(phrase, resolved)
            assertEquals(phrase, toolName, resolved!!.requiredTool.name)
            assertTrue(phrase, MiniLmMealReadPolicy.isInScope(resolved.classifierText, toolName))
            assertEquals(phrase, MiniLmReadTextNormalization.normalize(phrase), resolved.classifierText)
            assertNotNull(phrase, MiniLmMealReadPolicy.validatedFastReadReply(
                resolved.requiredTool,
                validEmptyReply(resolved.requiredTool, today),
            ))
        }
        assertEquals(
            "read_meals",
            MiniLmMealReadPolicy.resolveReadRequest(null, "Please remind me of yesterday’s meals, please", null, today)
                ?.requiredTool?.name,
        )
        assertEquals(1, MiniLmMealReadPolicy.resolveReadRequest(
            null, "Please remind me of yesterday’s meals, please", null, today,
        )?.requiredTool?.daysAgo)
        assertEquals(
            "protein",
            MiniLmMealReadPolicy.resolveReadRequest(null, "What’s today’s protein total?", null, today)
                ?.requiredTool?.metric,
        )
    }

    private fun validEmptyReply(tool: RequiredAgentTool, today: LocalDate): Map<String, Any?> = when (tool.name) {
        "read_meals" -> {
            val offset = requireNotNull(tool.daysAgo)
            val target = today.minusDays(offset.toLong())
            mapOf(
                "source" to "metria_local_vault", "reply_language" to "English", "as_of" to today.toString(),
                "from" to target.toString(), "to" to target.toString(), "meal_slot" to null,
                "total_count" to 0, "returned_count" to 0, "omitted_count" to 0,
                "coverage" to "full", "meals" to emptyList<Any>(),
            )
        }
        "read_latest_meal" -> LatestMealNutritionResult.from(emptyList(), today, "English")
        "read_day_nutrition" -> HistoricalDayNutritionResult.from(
            emptyList(), today, 0, requireNotNull(tool.metric),
        ) + ("reply_language" to "English")
        "read_weight" -> {
            val from = today.minusDays(requireNotNull(tool.fromDaysAgo).toLong())
            mapOf(
                "source" to "metria_local_vault", "reply_language" to "English", "as_of" to today.toString(),
                "from" to from.toString(), "to" to today.toString(), "total_count" to 0,
                "returned_count" to 0, "omitted_count" to 0, "coverage" to "full", "entries" to emptyList<Any>(),
            )
        }
        else -> error("Unsupported fixture tool ${tool.name}")
    }

    @Test
    fun yesterdayNutritionFollowUpCarriesHostMetricThroughFrozenVaultResultValidation() {
        val today = LocalDate.parse("2026-10-12")
        val proteinGoal = requireNotNull(MiniLmMealReadPolicy.resolveReadRequest(
            null, "How much protein do I have left today?", null, today,
        ))
        val calorieGoal = requireNotNull(MiniLmMealReadPolicy.resolveReadRequest(
            null, "And calories?", null, today,
            MiniLmMealReadPolicy.FollowUpScope(
                operation = MiniLmMealReadPolicy.FollowUpOperation.GOAL_REMAINING,
                referenceDate = today,
                metric = proteinGoal.requiredTool.metric,
            ),
        ))
        assertEquals(CompanionAssistantPolicy.IntentKind.GOAL_REMAINING, calorieGoal.companion?.kind)
        val followUpScope = MiniLmMealReadPolicy.FollowUpScope(
            operation = MiniLmMealReadPolicy.FollowUpOperation.GOAL_REMAINING,
            referenceDate = today,
            metric = calorieGoal.companion?.metric,
        )
        val yesterdayRequired = RequiredAgentTool(
            "read_day_nutrition", period = "historical_day", daysAgo = 1,
            metric = calorieGoal.companion?.metric, targetDate = today.minusDays(1),
        )
        val resolved = requireNotNull(MiniLmMealReadPolicy.resolveReadRequest(
            yesterdayRequired, "And yesterday?", null, today, followUpScope,
        ))
        assertTrue(resolved.allowMiniLm)
        assertEquals("calories", resolved.requiredTool.metric)
        assertNull(MetriaAgentGroundingPolicy.historicalNutritionMetric("And yesterday?"))

        // Mirrors the trusted fields attached by ModelLifecycle to the native tool call.
        val nativeCall = PrivateCoachToolCall(
            intent = "read_day_nutrition", period = "historical_day", daysAgo = 1,
            hostReadDate = today, hostMetric = resolved.requiredTool.metric,
        )
        val requestedMetric = MiniLmMealReadPolicy.nativeNutritionMetric(nativeCall, "And yesterday?")
        assertEquals("calories", requestedMetric)
        val vaultResult = HistoricalDayNutritionResult.from(emptyList(), today, 1, requestedMetric!!) +
            ("reply_language" to "English")

        assertNotNull(MiniLmMealReadPolicy.validatedFastReadReply(resolved.requiredTool, vaultResult))
        assertNull(MiniLmMealReadPolicy.nativeNutritionMetric(
            nativeCall.copy(hostReadDate = null), "And yesterday?",
        ))
    }

    @Test
    fun todayOverviewUsesRealTextAndNativeHostDateWithoutStealingNarrowReads() {
        val today = LocalDate.parse("2026-10-12")
        val phrase = "How am I doing today?"
        val host = MetriaAgentGroundingPolicy.requiredTool(phrase, emptyList(), today)
        val resolved = requireNotNull(MiniLmMealReadPolicy.resolveReadRequest(host, phrase, null, today))

        assertTrue(resolved.allowMiniLm)
        assertEquals("read_progress_overview", resolved.requiredTool.name)
        assertEquals("today", resolved.requiredTool.period)
        assertEquals(today, resolved.requiredTool.targetDate)
        assertEquals("today_overview", resolved.classifierKey)
        assertEquals("how am i doing today", resolved.classifierText)
        assertTrue(MiniLmMealReadPolicy.isInScope(phrase, "today_overview"))

        val nativeCall = PrivateCoachToolCall(
            intent = resolved.requiredTool.name,
            period = resolved.requiredTool.period,
            hostReadDate = today,
            hostCompanionKind = resolved.companion?.kind,
        )
        assertEquals(today, nativeCall.hostReadDate)
        assertEquals(CompanionAssistantPolicy.IntentKind.TODAY_OVERVIEW, nativeCall.hostCompanionKind)

        val frozenOverview = MemberProgressOverview.from(
            entries = emptyList(),
            meals = emptyList(),
            targets = NutritionTargets(caloriesKcal = 2_000.0, proteinGrams = 100.0),
            today = requireNotNull(nativeCall.hostReadDate),
            period = "today",
        )
        val response = frozenOverview.toolResponse() + mapOf(
            "as_of" to nativeCall.hostReadDate.toString(), "reply_language" to "English",
        )
        assertEquals(today.toString(), response["from"])
        assertEquals(today.toString(), response["to"])
        val fastReply = TodayOverviewReply.from(resolved.requiredTool, response)
        assertNotNull(fastReply)
        assertTrue(requireNotNull(fastReply).contains("Your daily calorie goal is 2000 kcal."))
        assertTrue(fastReply.contains("Your daily protein goal is 100 g protein."))
        assertFalse(fastReply.contains("kg"))

        val narrowRead = MiniLmMealReadPolicy.resolveReadRequest(
            MetriaAgentGroundingPolicy.requiredTool("How many calories did I eat today?", emptyList(), today),
            "How many calories did I eat today?", null, today,
        )
        assertEquals("read_day_nutrition", narrowRead?.requiredTool?.name)
        assertNotEquals("today_overview", narrowRead?.classifierKey)
        assertFalse(MiniLmMealReadPolicy.isInScope("Should I eat more today?", "today_overview"))
        assertFalse(MiniLmMealReadPolicy.isInScope("How am I doing this week?", "today_overview"))
    }

    @Test
    fun oneUniqueRoutingTypoRetriesOnlyClosedReadGrammars() {
        val today = LocalDate.parse("2026-10-12")
        val accepted = listOf(
            "show me an oberview of my day today" to
                ("read_progress_overview" to "show me an overview of my day today"),
            "show my recoreded meals today" to
                ("read_meals" to "show my recorded meals today"),
            "what is my calorie totall for today" to
                ("read_day_nutrition" to "what is my calorie total for today"),
            "what were the calories in my lates meal" to
                ("read_latest_meal" to "what were the calories in my latest meal"),
            "what is my latest weightt" to
                ("read_weight" to "what is my latest weight"),
            "how much protien do i have left today" to
                ("read_nutrition_summary" to "how much protein do i have left today"),
        )
        accepted.forEach { (question, expected) ->
            val host = MetriaAgentGroundingPolicy.requiredTool(question, emptyList(), today)
            val resolved = requireNotNull(MiniLmMealReadPolicy.resolveReadRequest(host, question, null, today))
            assertTrue(question, resolved.allowMiniLm)
            assertEquals(question, expected.first, resolved.requiredTool.name)
            assertEquals(question, expected.second, resolved.classifierText)
        }

        val compatibleOverview = MiniLmMealReadPolicy.resolveReadRequest(
            RequiredAgentTool("read_progress_overview", period = "today"),
            "show me an oberview of my day today", null, today,
        )
        assertEquals("show me an overview of my day today", compatibleOverview?.classifierText)
        val incompatibleHosts = listOf(
            RequiredAgentTool("read_progress_overview", period = "week"),
            RequiredAgentTool("read_day_nutrition", period = "historical_day", daysAgo = 1, metric = "calories"),
            RequiredAgentTool("read_meals", daysAgo = 1, targetDate = today.minusDays(1)),
        )
        incompatibleHosts.forEach { host ->
            assertNull(host.toString(), MiniLmMealReadPolicy.resolveReadRequest(
                host, "show me an oberview of my day today", null, today,
            ))
        }

        listOf(
            "show my meals for yesterdya",
            "do not show my recoreded meals today",
            "show my recoreded meals today and log lunch",
            "show my caloriex today",
            "show my recoreded mealx today",
            "show my latest height",
            "show my meats today",
            "what are the calories in my lamest meal",
            "show my meals for sunday",
            "show Maria my latest weight",
            "show my deals today",
            "log my recoreded meals today",
        ).forEach { question ->
            assertNull(question, MiniLmMealReadPolicy.resolveReadRequest(null, question, null, today))
        }
        val savedDraftNameIsNeverRewritten = requireNotNull(MiniLmMealReadPolicy.resolveReadRequest(
            null, "I had my saved dish weightt", null, today,
        ))
        assertEquals("saved_draft", savedDraftNameIsNeverRewritten.classifierKey)
        assertEquals("i had my saved dish name", savedDraftNameIsNeverRewritten.classifierText)
        assertEquals("weightt", savedDraftNameIsNeverRewritten.requiredTool.query)
        assertEquals("weightt", savedDraftNameIsNeverRewritten.companion?.query)
        assertTrue(MiniLmMealReadPolicy.isMiniLmRouterInputInScope(
            savedDraftNameIsNeverRewritten.classifierText, "saved_draft",
        ))
        val postfixRecall = requireNotNull(MiniLmMealReadPolicy.resolveReadRequest(
            null, "Find my weightt dish", null, today,
        ))
        assertEquals("weightt", postfixRecall.requiredTool.query)
        assertTrue(MiniLmMealReadPolicy.isMiniLmRouterInputInScope(
            postfixRecall.classifierText, "saved_recall",
        ))
        assertNull(MiniLmMealReadPolicy.resolveReadRequest(
            null, "show me an oberview of my day today", "selected-meal", today,
        ))
        val explicitAfterScopedRead = MiniLmMealReadPolicy.resolveReadRequest(
            null, "what is my calorie totall for today", null, today,
            MiniLmMealReadPolicy.FollowUpScope(
                MiniLmMealReadPolicy.FollowUpOperation.TODAY_NUTRITION, today, metric = "protein",
            ),
        )
        assertEquals("calories", explicitAfterScopedRead?.requiredTool?.metric)
        assertEquals("what is my calorie total for today", explicitAfterScopedRead?.classifierText)
        val preservedYesterday = MiniLmMealReadPolicy.resolveReadRequest(
            null, "show my recoreded meals yesterday", null, today,
        )
        assertEquals(1, preservedYesterday?.requiredTool?.daysAgo)
        assertEquals(today.minusDays(1), preservedYesterday?.requiredTool?.targetDate)
        assertEquals("show my recorded meals yesterday", preservedYesterday?.classifierText)
        assertNull(MiniLmMealReadPolicy.resolveReadRequest(
            null, "And protien", null, today,
            MiniLmMealReadPolicy.FollowUpScope(
                MiniLmMealReadPolicy.FollowUpOperation.TODAY_NUTRITION, today, metric = "protein",
            ),
        ))
    }

    @Test
    fun normalizationDoesNotEraseQualifiersOrTurnMixedRequestsIntoReads() {
        val today = LocalDate.parse("2026-10-12")
        listOf(
            "Please show my weight and meals, please",
            "What is my calorie total tomorrow?",
            "What is my calorie goal today?",
            "Add my meal today, please",
            "Do not show my meals today",
            "Show my meals yesterday and my weight",
            "Show my meals from two days ago",
            "Show my meals and recommend dinner today",
            "Could you show me my selected meal?",
            "Show yesterday's meals and meals today",
        ).forEach { phrase ->
            assertFalse(phrase, MiniLmMealReadPolicy.isAnyDirectReadInScope(phrase))
            assertNull(phrase, MiniLmMealReadPolicy.resolveReadRequest(null, phrase, null, today))
        }
        val flexibleYesterdayProtein = requireNotNull(MiniLmMealReadPolicy.resolveReadRequest(
            null, "What is my protein total yesterday?", null, today,
        ))
        assertEquals("read_day_nutrition", flexibleYesterdayProtein.requiredTool.name)
        assertEquals("protein", flexibleYesterdayProtein.requiredTool.metric)
        assertEquals(1, flexibleYesterdayProtein.requiredTool.daysAgo)
    }

    @Test
    fun rejectsAdviceWritesMixedMetricsDatesAndSelectedMealContextForLatestRoute() {
        val today = LocalDate.parse("2026-10-11")
        listOf(
            "How much protein should my last meal have?",
            "How many calories were in my last meal and what should I eat next?",
            "How many calories were in my last meal and today?",
            "How many calories and carbs were in my last meal?",
            "How many calories were in my lunch yesterday?",
            "Log my latest meal and tell me how many calories were in it",
            "How many calories were in the meal I selected?",
        ).forEach { text ->
            assertFalse(text, MiniLmMealReadPolicy.isInScope(text, "read_latest_meal"))
            assertNull(MiniLmMealReadPolicy.requiredReadTool(null, text, null, today))
        }
        assertNull(MiniLmMealReadPolicy.requiredReadTool(
            RequiredAgentTool("read_meals", daysAgo = 0),
            "Could you tell me how many calories were in my last meal?", null, today,
        ))
        assertNull(MiniLmMealReadPolicy.requiredReadTool(
            RequiredAgentTool("read_latest_meal", metric = "calories"),
            "Could you tell me how many calories were in my last meal?", null, today,
        ))
        assertNull(MiniLmMealReadPolicy.requiredReadTool(
            null, "Could you tell me how many calories were in my last meal?", "selected-id", today,
        ))
    }

    @Test
    fun frozenLatestMealResultRequiresMatchingAsOfAndTheExistingReplyContract() {
        val today = LocalDate.parse("2026-10-11")
        val meal = MealEntry(
            recordedAt = today.atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant(),
            title = "Soup", items = emptyList(), assumptions = emptyList(), confidence = "high",
            source = "manual_entry", recordedTimeKnown = true,
            nutrition = MealNutrition(350.0, 14.0, null, null, estimateCoverage = "full"),
        )
        val result = LatestMealNutritionResult.from(listOf(meal), today, "English")
        val call = PrivateCoachToolCall("read_latest_meal", hostReadDate = today)
        assertNotNull(MiniLmMealReadPolicy.latestMealReplyForFrozenRead(call, result))
        assertNotNull(MiniLmMealReadPolicy.validatedFastReadReply(
            RequiredAgentTool("read_latest_meal", targetDate = today), result,
        ))
        assertNull(MiniLmMealReadPolicy.latestMealReplyForFrozenRead(call, result + ("as_of" to today.minusDays(1).toString())))
        assertNull(MiniLmMealReadPolicy.latestMealReplyForFrozenRead(call.copy(hostReadDate = null), result))
        assertNull(MiniLmMealReadPolicy.latestMealReplyForFrozenRead(call.copy(intent = "read_meals"), result))
        assertNull(MiniLmMealReadPolicy.latestMealReplyForFrozenRead(call.copy(daysAgo = 1), result))
        assertNull(MiniLmMealReadPolicy.latestMealReplyForFrozenRead(call, result + ("source" to "other")))
        assertNull(MiniLmMealReadPolicy.latestMealReplyForFrozenRead(call, result + ("period" to "today")))
        assertNull(MiniLmMealReadPolicy.validatedFastReadReply(
            RequiredAgentTool("read_meals", daysAgo = 0, targetDate = today), result,
        ))
    }

    @Test
    fun rejectsWritesAdviceOtherDatesAndMixedRequests() {
        listOf(
            "I just ate eggs and toast",
            "Log a chicken meal for today",
            "What should I eat tomorrow",
            "Show today's meals and suggest dinner",
            "Show yesterday's meals and suggest dinner",
            "How many calories did I eat today",
            "What did I eat this week",
            "What did I log today",
            "Help me make a meal plan",
            "Add the lunch I just had",
            "Show today's meals and my weight",
            "Show meals for 2026-10-11",
            "Do not show meals today",
            "Show meals today except breakfast",
            "What meals can you record today",
            "Show meals today and meals yesterday",
            "What did I eat yesterday and what should I eat today",
            "Show my meals from two days ago",
            "Show meals for 2026-10-10",
        ).forEach { assertFalse(it, MiniLmMealReadPolicy.isInScope(it)) }
    }

    @Test
    fun resolvesOffsetsAgainstHostToolAndRejectsConflictingHostScope() {
        val today = LocalDate.parse("2026-10-11")
        listOf("What did I eat today" to 0, "What did I eat yesterday" to 1).forEach { (text, offset) ->
            val hostTool = MetriaAgentGroundingPolicy.requiredTool(text, today = today)
            assertNotNull(text, hostTool)
            assertEquals("read_meals", hostTool?.name)
            assertEquals(offset, hostTool?.daysAgo)
            val resolved = MiniLmMealReadPolicy.requiredReadTool(hostTool, text, null, today)
            assertEquals(offset, resolved?.daysAgo)
            assertEquals(today.minusDays(offset.toLong()), resolved?.targetDate)
        }
        listOf("Show the meals I recorded today" to 0, "Show the meals I recorded yesterday" to 1).forEach { (text, offset) ->
            val resolved = MiniLmMealReadPolicy.requiredReadTool(null, text, null, today)
            assertEquals(offset, resolved?.daysAgo)
            assertEquals(today.minusDays(offset.toLong()), resolved?.targetDate)
        }
        val yesterday = "Show the meals I recorded yesterday"
        assertNull(MiniLmMealReadPolicy.requiredReadTool(RequiredAgentTool("read_weight"), yesterday, null, today))
        assertNull(MiniLmMealReadPolicy.requiredReadTool(RequiredAgentTool("read_meals", daysAgo = 0), yesterday, null, today))
        assertNull(MiniLmMealReadPolicy.requiredReadTool(
            RequiredAgentTool("read_meals", daysAgo = 1, targetDate = today), yesterday, null, today,
        ))
        assertNull(MiniLmMealReadPolicy.requiredReadTool(
            RequiredAgentTool("read_meals", daysAgo = 1, selectedMealId = "meal-id"), yesterday, null, today,
        ))
        assertNull(MiniLmMealReadPolicy.requiredReadTool(
            RequiredAgentTool("read_meals", daysAgo = 1, period = "yesterday"), yesterday, null, today,
        ))
        assertNull(MiniLmMealReadPolicy.requiredReadTool(
            RequiredAgentTool("read_meals", daysAgo = 1, query = "breakfast"), yesterday, null, today,
        ))
        assertNull(MiniLmMealReadPolicy.requiredReadTool(
            RequiredAgentTool("read_meals", daysAgo = 1, metric = "calories"), yesterday, null, today,
        ))
        assertNull(MiniLmMealReadPolicy.requiredReadTool(
            RequiredAgentTool("read_meals", daysAgo = 1, fromDaysAgo = 1, toDaysAgo = 1), yesterday, null, today,
        ))
        assertNull(MiniLmMealReadPolicy.requiredReadTool(
            RequiredAgentTool("read_meals", daysAgo = 1, selectedMealQuestion = yesterday), yesterday, null, today,
        ))
        assertNull(MiniLmMealReadPolicy.requiredReadTool(null, yesterday, "selected-meal", today))
        assertNull(MiniLmMealReadPolicy.requiredReadTool(null, "Show yesterday's meals and my weight", null, today))
    }

    @Test
    fun frozenReferenceDateAndOffsetMustMatchTheCompleteValidatedMealResponse() {
        val date = LocalDate.parse("2026-10-11")
        val targetDate = date.minusDays(1)
        val call = PrivateCoachToolCall(intent = "read_meals", daysAgo = 1, hostReadDate = date)
        val response = mapOf<String, Any?>(
            "source" to "metria_local_vault",
            "reply_language" to "English",
            "as_of" to date.toString(),
            "from" to targetDate.toString(),
            "to" to targetDate.toString(),
            "meal_slot" to null,
            "total_count" to 1,
            "returned_count" to 1,
            "omitted_count" to 0,
            "coverage" to "full",
            "meals" to listOf(mapOf("date" to targetDate.toString(), "title" to "Breakfast", "slot" to null)),
        )
        assertTrue(MiniLmMealReadPolicy.hasValidFrozenMealRead(call, response))
        val requiredTool = RequiredAgentTool("read_meals", daysAgo = 1, targetDate = targetDate)
        assertNotNull(MiniLmMealReadPolicy.validatedFastReadReply(requiredTool, response))
        assertNull(MiniLmMealReadPolicy.validatedFastReadReply(
            requiredTool, response + ("as_of" to targetDate.toString()),
        ))
        assertNull(MiniLmMealReadPolicy.validatedFastReadReply(requiredTool.copy(name = "read_latest_meal"), response))
        assertFalse(MiniLmMealReadPolicy.hasValidFrozenMealRead(call.copy(hostReadDate = null), response))
        assertFalse(MiniLmMealReadPolicy.hasValidFrozenMealRead(call.copy(hostReadDate = date.plusDays(1)), response))
        assertFalse(MiniLmMealReadPolicy.hasValidFrozenMealRead(call.copy(daysAgo = 0), response))
        assertFalse(MiniLmMealReadPolicy.hasValidFrozenMealRead(call.copy(daysAgo = 2), response))
        assertFalse(MiniLmMealReadPolicy.hasValidFrozenMealRead(call.copy(fromDaysAgo = 1), response))
        assertFalse(MiniLmMealReadPolicy.hasValidFrozenMealRead(call, response + ("from" to date.toString())))
        assertFalse(MiniLmMealReadPolicy.hasValidFrozenMealRead(call, response + ("as_of" to date.plusDays(1).toString())))
    }

    @Test
    fun yesterdayOffsetStillTargetsTheFrozenReferenceDayAcrossYearBoundary() {
        val referenceDate = LocalDate.parse("2026-01-01")
        val yesterday = referenceDate.minusDays(1)
        val call = PrivateCoachToolCall(intent = "read_meals", daysAgo = 1, hostReadDate = referenceDate)
        val response = mapOf<String, Any?>(
            "source" to "metria_local_vault", "reply_language" to "English",
            "as_of" to referenceDate.toString(), "from" to yesterday.toString(), "to" to yesterday.toString(),
            "meal_slot" to null, "total_count" to 0, "returned_count" to 0, "omitted_count" to 0,
            "coverage" to "full", "meals" to emptyList<Any>(),
        )
        assertTrue(MiniLmMealReadPolicy.hasValidFrozenMealRead(call, response))
        assertFalse(MiniLmMealReadPolicy.hasValidFrozenMealRead(
            call.copy(hostReadDate = referenceDate.plusDays(1)), response,
        ))
    }

    @Test
    fun todayOffsetStillValidatesTheFrozenReferenceDay() {
        val today = LocalDate.parse("2026-10-11")
        val call = PrivateCoachToolCall(intent = "read_meals", daysAgo = 0, hostReadDate = today)
        val response = mapOf<String, Any?>(
            "source" to "metria_local_vault", "reply_language" to "English",
            "as_of" to today.toString(), "from" to today.toString(), "to" to today.toString(),
            "meal_slot" to null, "total_count" to 0, "returned_count" to 0, "omitted_count" to 0,
            "coverage" to "full", "meals" to emptyList<Any>(),
        )
        assertTrue(MiniLmMealReadPolicy.hasValidFrozenMealRead(call, response))
        assertFalse(MiniLmMealReadPolicy.hasValidFrozenMealRead(call, response + ("as_of" to today.minusDays(1).toString())))
        assertFalse(MiniLmMealReadPolicy.hasValidFrozenMealRead(call, response + ("from" to today.minusDays(1).toString())))
    }

    @Test
    fun acceptsSingleMetricTodayRecordedNutritionRequestsAndMapsOnlyExactDayReads() {
        val today = LocalDate.parse("2026-10-12")
        val accepted = listOf(
            "How many calories did I eat today?" to "calories",
            "How much protein have I logged today?" to "protein",
            "Could you tell me how many calories I recorded today?" to "calories",
            "Show my recorded protein for today" to "protein",
            "What's my protein total today?" to "protein",
            "What's my calorie total today?" to "calories",
            "How much protein did I have today?" to "protein",
            "How many calories have I had today?" to "calories",
        )
        accepted.forEach { (text, metric) ->
            assertTrue(text, MiniLmMealReadPolicy.isTodayNutritionInScope(text))
            assertTrue(text, MiniLmMealReadPolicy.isInScope(text, "read_day_nutrition"))
            val selected = MiniLmMealReadPolicy.requiredReadTool(null, text, null, today)
            assertEquals(text, "read_day_nutrition", selected?.name)
            assertEquals(text, "historical_day", selected?.period)
            assertEquals(text, 0, selected?.daysAgo)
            assertEquals(text, metric, selected?.metric)
            assertEquals(text, today, selected?.targetDate)
            val grounded = MetriaAgentGroundingPolicy.requiredTool(text, today = today)
            if (grounded != null) {
                assertEquals(text, "read_nutrition_summary", grounded.name)
                assertEquals(text, "today", grounded.period)
                assertEquals(text, metric, grounded.metric)
                assertEquals(
                    text,
                    "read_day_nutrition",
                    MiniLmMealReadPolicy.requiredReadTool(grounded, text, null, today)?.name,
                )
            }
        }
        val calorieQuestion = accepted.first().first
        assertEquals(
            RequiredAgentTool("read_day_nutrition", period = "historical_day", daysAgo = 0, metric = "calories", targetDate = today),
            MiniLmMealReadPolicy.requiredReadTool(
                RequiredAgentTool("read_nutrition_summary", period = "today", metric = "calories"),
                calorieQuestion, null, today,
            ),
        )
        assertEquals(
            today,
            MiniLmMealReadPolicy.requiredReadTool(
                RequiredAgentTool("read_day_nutrition", period = "historical_day", daysAgo = 0, metric = "calories"),
                calorieQuestion, null, today,
            )?.targetDate,
        )
    }

    @Test
    fun directReadParaphrasesStayAheadOfLegacyWriteWordRoutingOnlyWhenFullyInScope() {
        listOf(
            "How much protein have I logged today?",
            "Remind me what I ate yesterday",
            "Calories in my last meal?",
        ).forEach { assertFalse(it, MetriaAgentMigrationPolicy.useLegacyWriteRoute(it)) }
        listOf(
            "Log a meal and tell me my protein total today",
            "Could you remind me what I ate today and add dinner",
            "Save my latest meal and show its calories",
        ).forEach { assertTrue(it, MetriaAgentMigrationPolicy.useLegacyWriteRoute(it)) }
    }

    @Test
    fun shortNutritionFollowUpsResolveOnlyAgainstMatchingSameDaySuccessfulReadScope() {
        val today = LocalDate.parse("2026-10-12")
        val nutritionScope = MiniLmMealReadPolicy.FollowUpScope(
            MiniLmMealReadPolicy.FollowUpOperation.TODAY_NUTRITION, today,
        )
        val latestScope = MiniLmMealReadPolicy.FollowUpScope(
            MiniLmMealReadPolicy.FollowUpOperation.LATEST_MEAL, today,
        )
        val todayProtein = MiniLmMealReadPolicy.resolveReadRequest(null, "And protein?", null, today, nutritionScope)
        assertEquals("read_day_nutrition", todayProtein?.requiredTool?.name)
        assertEquals("protein", todayProtein?.requiredTool?.metric)
        assertEquals("How much protein have I logged today", todayProtein?.classifierText)
        val latestCalories = MiniLmMealReadPolicy.resolveReadRequest(null, "And calories?", null, today, latestScope)
        assertEquals("read_latest_meal", latestCalories?.requiredTool?.name)
        assertEquals("How many calories were in my latest meal", latestCalories?.classifierText)
        assertNull(MiniLmMealReadPolicy.gemmaFallbackRequiredTool(latestCalories, null)?.targetDate)
        val gemmaTodayNutrition = MiniLmMealReadPolicy.gemmaFallbackRequiredTool(todayProtein, null)
        assertEquals(today, gemmaTodayNutrition?.targetDate)

        listOf("And protein?", "And calories?").forEach { text ->
            assertNull(MiniLmMealReadPolicy.resolveReadRequest(null, text, null, today, null))
            assertNull(MiniLmMealReadPolicy.resolveReadRequest(null, text, null, today.plusDays(1), nutritionScope))
            assertNull(MiniLmMealReadPolicy.resolveReadRequest(null, text, "selected-meal", today, nutritionScope))
        }
        listOf("And carbs?", "And protein with my goal?", "And protein and suggest dinner?").forEach { text ->
            assertNull(MiniLmMealReadPolicy.resolveReadRequest(null, text, null, today, nutritionScope))
        }
        val conflictingHost = MiniLmMealReadPolicy.resolveReadRequest(
            RequiredAgentTool("read_weight"), "And protein?", null, today, nutritionScope,
        )
        assertNotNull(conflictingHost)
        assertFalse(conflictingHost!!.allowMiniLm)
        assertEquals("read_day_nutrition", conflictingHost.requiredTool.name)
        assertEquals("read_day_nutrition", MiniLmMealReadPolicy.gemmaFallbackRequiredTool(conflictingHost, null)?.name)
        val conflictingDay = MiniLmMealReadPolicy.resolveReadRequest(
            RequiredAgentTool("read_day_nutrition", period = "historical_day", daysAgo = 1, metric = "protein"),
            "And protein?", null, today, nutritionScope,
        )
        assertNotNull(conflictingDay)
        assertFalse(conflictingDay!!.allowMiniLm)
        assertEquals(0, conflictingDay.requiredTool.daysAgo)
        assertEquals(today, conflictingDay.requiredTool.targetDate)
        assertEquals(
            conflictingDay.requiredTool,
            MiniLmMealReadPolicy.gemmaFallbackRequiredTool(conflictingDay, null),
        )
    }

    @Test
    fun goalRemainingFollowUpsReadFreshTodayOrYesterdayWithStrictHostScope() {
        val today = LocalDate.parse("2026-10-12")
        val scope = MiniLmMealReadPolicy.FollowUpScope(
            MiniLmMealReadPolicy.FollowUpOperation.GOAL_REMAINING,
            referenceDate = today,
            metric = "calories",
        )
        val shortGoalFollowUp = MiniLmMealReadPolicy.resolveReadRequest(
            RequiredAgentTool("read_nutrition_summary", period = "today", metric = "protein"),
            "And protein?", null, today, scope,
        )
        assertEquals("read_nutrition_summary", shortGoalFollowUp?.requiredTool?.name)
        assertEquals("protein", shortGoalFollowUp?.requiredTool?.metric)
        assertTrue(shortGoalFollowUp?.allowMiniLm == true)
        assertEquals(CompanionAssistantPolicy.IntentKind.GOAL_REMAINING, shortGoalFollowUp?.companion?.kind)

        val yesterday = MiniLmMealReadPolicy.resolveReadRequest(null, "And yesterday?", null, today, scope)
        assertEquals("read_day_nutrition", yesterday?.requiredTool?.name)
        assertEquals("historical_day", yesterday?.requiredTool?.period)
        assertEquals(1, yesterday?.requiredTool?.daysAgo)
        assertEquals(today.minusDays(1), yesterday?.requiredTool?.targetDate)
        assertNull(yesterday?.companion)
        assertEquals(today.minusDays(1), MiniLmMealReadPolicy.gemmaFallbackRequiredTool(yesterday, null)?.targetDate)

        val conflicting = MiniLmMealReadPolicy.resolveReadRequest(
            RequiredAgentTool("read_nutrition_summary", period = "week", metric = "protein"),
            "And protein?", null, today, scope,
        )
        assertFalse(conflicting?.allowMiniLm == true)
        val conflictingYesterday = MiniLmMealReadPolicy.resolveReadRequest(
            RequiredAgentTool("read_day_nutrition", period = "today", daysAgo = 1, metric = "calories"),
            "And yesterday?", null, today, scope,
        )
        assertFalse(conflictingYesterday?.allowMiniLm == true)
    }

    @Test
    fun rejectsNutritionAdviceMixedMetricsOtherDatesAndConflictingHostScopes() {
        val today = LocalDate.parse("2026-10-12")
        listOf(
            "How many calories did I eat yesterday?",
            "How much protein did I log two days ago?",
            "How many calories and protein did I record today?",
            "How many calories should I eat today?",
            "Show calories and suggest dinner today",
            "Compare my calories with my goal today",
            "Show my calories today and protein yesterday",
            "How many calories will I eat today?",
            "Show my calorie and protein totals today",
        ).forEach { text ->
            assertFalse(text, MiniLmMealReadPolicy.isTodayNutritionInScope(text))
            assertNull(text, MiniLmMealReadPolicy.requiredReadTool(null, text, null, today))
        }
        val text = "How many calories did I eat today?"
        listOf(
            RequiredAgentTool("read_day_nutrition", period = "historical_day", daysAgo = 1, metric = "calories"),
            RequiredAgentTool("read_day_nutrition", period = "historical_day", daysAgo = 0, metric = "protein"),
            RequiredAgentTool("read_day_nutrition", period = "today", daysAgo = 0, metric = "calories"),
            RequiredAgentTool("read_nutrition_summary", period = "week", metric = "calories"),
            RequiredAgentTool("read_nutrition_summary", period = "today", metric = null),
            RequiredAgentTool("read_nutrition_summary", period = "today", metric = "calories", query = "extra"),
        ).forEach { host ->
            assertNull(host.toString(), MiniLmMealReadPolicy.requiredReadTool(host, text, null, today))
        }
        assertNull(MiniLmMealReadPolicy.requiredReadTool(
            RequiredAgentTool("read_day_nutrition", period = "historical_day", daysAgo = 0, metric = "calories"),
            "What did I eat yesterday?", null, today,
        ))
        assertNull(MiniLmMealReadPolicy.requiredReadTool(
            RequiredAgentTool("read_nutrition_summary", period = "today", metric = "calories"),
            text, "selected-meal", today,
        ))
    }

    @Test
    fun fastTodayNutritionReplyReusesValidatedFullPartialMissingAndEmptyDayResults() {
        val today = LocalDate.parse("2026-10-12")
        val required = RequiredAgentTool(
            "read_day_nutrition", period = "historical_day", daysAgo = 0, metric = "calories", targetDate = today,
        )
        fun meal(label: String, nutrition: MealNutrition?) = MealEntry(
            recordedAt = today.atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant(),
            title = label, items = emptyList(), assumptions = emptyList(), confidence = "high",
            source = "manual_entry", nutrition = nutrition,
        )
        fun response(vararg meals: MealEntry) = HistoricalDayNutritionResult.from(
            meals.toList(), today, 0, "calories",
        ) + ("reply_language" to "English")

        val full = response(meal("Full", MealNutrition(420.0, 20.0, 10.0, 5.0)))
        val fullAnswer = MiniLmMealReadPolicy.todayNutritionReplyForFrozenRead(required, full)
        assertNotNull(fullAnswer)
        assertTrue(fullAnswer!!.contains("420 kcal"))
        assertTrue(fullAnswer.contains("Unrecorded meals are unknown"))

        val partial = response(meal("Partial", MealNutrition(125.0, 6.0, 2.0, 1.0, estimateCoverage = "partial")))
        val partialAnswer = MiniLmMealReadPolicy.todayNutritionReplyForFrozenRead(required, partial)
        assertNotNull(partialAnswer)
        assertTrue(partialAnswer!!.contains("125 kcal"))
        assertTrue(partialAnswer.contains("partial estimates"))

        val missing = response(meal("Missing", null))
        val missingAnswer = MiniLmMealReadPolicy.todayNutritionReplyForFrozenRead(required, missing)
        assertNotNull(missingAnswer)
        assertTrue(missingAnswer!!.contains("No calories estimate is available"))

        val empty = response()
        val emptyAnswer = MiniLmMealReadPolicy.todayNutritionReplyForFrozenRead(required, empty)
        assertNotNull(emptyAnswer)
        assertTrue(emptyAnswer!!.contains("No calories estimate is available"))

        val roundingEdge = response(
            meal("Complete", MealNutrition(0.5, 0.04, 0.04, 0.04)),
            meal("Partial", MealNutrition(0.5, 0.04, 0.04, 0.04, estimateCoverage = "partial")),
        )
        assertNotNull(MiniLmMealReadPolicy.todayNutritionReplyForFrozenRead(required, roundingEdge))
        val decimalRoundingEdge = response(
            meal("Complete", MealNutrition(1.0, 1.05, 1.0, 1.0)),
            meal("Partial", MealNutrition(1.0, 2.15, 1.0, 1.0, estimateCoverage = "partial")),
        )
        assertNotNull(MiniLmMealReadPolicy.todayNutritionReplyForFrozenRead(required, decimalRoundingEdge))
    }

    @Test
    fun fastTodayNutritionReplyFailsClosedOnWrongDateMetricLanguageCountsOrNutrition() {
        val today = LocalDate.parse("2026-10-12")
        val required = RequiredAgentTool(
            "read_day_nutrition", period = "historical_day", daysAgo = 0, metric = "calories", targetDate = today,
        )
        val meal = MealEntry(
            recordedAt = today.atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant(),
            title = "Meal", items = emptyList(), assumptions = emptyList(), confidence = "high",
            source = "manual_entry", nutrition = MealNutrition(420.0, 20.0, 10.0, 5.0),
        )
        val base = HistoricalDayNutritionResult.from(listOf(meal), today, 0, "calories") + ("reply_language" to "English")
        val nutrition = base["nutrition"] as Map<*, *>
        val counts = base["meals"] as Map<*, *>
        listOf(
            base + ("as_of" to today.plusDays(1).toString()),
            base + ("from" to today.minusDays(1).toString()),
            base + ("requested_metric" to "protein"),
            base + ("reply_language" to "Spanish"),
            base + ("interpretation" to "total_intake"),
            base + ("source" to "other"),
            base + ("meals" to (counts + ("missing_nutrition_count" to 1))),
            base + ("meals" to (counts + ("recorded_count" to Double.NaN))),
            base + ("nutrition" to (nutrition - "calories_kcal")),
            base + ("nutrition" to (nutrition + ("calories_kcal" to Double.POSITIVE_INFINITY))),
            base + ("nutrition" to (nutrition + ("calories_kcal" to 999.0))),
        ).forEach { response ->
            assertNull(MiniLmMealReadPolicy.todayNutritionReplyForFrozenRead(required, response))
        }
        assertNull(MiniLmMealReadPolicy.todayNutritionReplyForFrozenRead(
            required.copy(targetDate = null), base,
        ))
        assertNull(MiniLmMealReadPolicy.todayNutritionReplyForFrozenRead(
            required.copy(daysAgo = 1), base,
        ))
        assertNull(MiniLmMealReadPolicy.todayNutritionReplyForFrozenRead(
            required.copy(name = "read_meals"), base,
        ))
        assertNull(MiniLmMealReadPolicy.validatedFastReadReply(
            required.copy(name = "read_meals"), base,
        ))
    }
}

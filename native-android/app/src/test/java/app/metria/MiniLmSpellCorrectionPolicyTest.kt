package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MiniLmSpellCorrectionPolicyTest {
    private val today = LocalDate.parse("2026-10-14")

    @Test
    fun `normal-intent saved-dish fallback skips corrections that could recreate the probe`() {
        var providerCalls = 0
        val result = MiniLmSpellCorrectionPolicy.resolve(
            hostRequiredTool = null,
            originalText = "I had a bad night's sleep",
            selectedMealId = null,
            today = java.time.LocalDate.of(2026, 10, 16),
            checker = EnglishSpellChecker { _, _ ->
                providerCalls++
                error("Fallback must preserve the original turn for native intent selection")
            },
            isActive = { true },
            allowBareConsumptionProbe = false,
        )

        assertEquals(SpellCheckReason.NO_CANDIDATE, result.reason)
        assertEquals(0, providerCalls)
        assertNull(result.resolvedRead)
    }

    @Test
    fun rejectionReasonsSeparateUnusableProviderResultsWithoutExposingText() {
        val question = "Give me my recorded protein total for tday"
        val host = MetriaAgentGroundingPolicy.requiredTool(question, today = today)
        listOf(
            SpellSuggestion("today", 1, 4, true, false) to SpellCheckReason.SEGMENT_MISMATCH,
            SpellSuggestion("today", 0, 4, true, true) to SpellCheckReason.DICTIONARY_WORD,
            SpellSuggestion("today", 0, 4, false, false) to SpellCheckReason.UNMARKED_TYPO,
            SpellSuggestion("teddy", 0, 4, true, false) to SpellCheckReason.NO_MATCH,
        ).forEach { (suggestion, reason) ->
            val result = MiniLmSpellCorrectionPolicy.resolve(host, question, null, today,
                EnglishSpellChecker { _, _ -> SpellCheckResult(SpellCheckReason.ACCEPTED, listOf(suggestion)) },
            ) { true }
            assertEquals(reason, result.reason)
            assertNull(result.resolvedRead)
            assertTrue(result.providerAttempted)
        }
        listOf(SpellCheckReason.EMPTY_RESPONSE, SpellCheckReason.REQUEST_MISMATCH).forEach { reason ->
            val result = MiniLmSpellCorrectionPolicy.resolve(host, question, null, today,
                EnglishSpellChecker { _, _ -> SpellCheckResult(reason) },
            ) { true }
            assertEquals(reason, result.reason)
            assertNull(result.resolvedRead)
        }
    }

    @Test
    fun providerConfirmsOneDateTypoAndOnlyReceivesTheUnknownToken() {
        val text = "Give me my recorded protein total for tday"
        val host = MetriaAgentGroundingPolicy.requiredTool(text, today = today)
        var seenToken: String? = null
        val result = MiniLmSpellCorrectionPolicy.resolve(host, text, null, today, EnglishSpellChecker { token, _ ->
            seenToken = token
            SpellCheckResult(SpellCheckReason.ACCEPTED, listOf(
                SpellSuggestion("today", 0, token.length, looksLikeTypo = true, inDictionary = false),
            ))
        }) { true }

        assertEquals("tday", seenToken)
        assertEquals(SpellCheckReason.ACCEPTED, result.reason)
        assertTrue(result.providerAttempted)
        assertEquals("give me my recorded protein total for today", result.resolvedRead?.classifierText)
        assertEquals("read_day_nutrition", result.resolvedRead?.requiredTool?.name)
        assertEquals(today, result.resolvedRead?.requiredTool?.targetDate)
    }

    @Test
    fun bareSavedDishProbeNeverSendsItsOpaqueNameToTheSystemChecker() {
        val text = "I had san jacobo"
        val host = MetriaAgentGroundingPolicy.requiredTool(text, today = today)
        var checkerCalls = 0
        val result = MiniLmSpellCorrectionPolicy.resolve(host, text, null, today, EnglishSpellChecker { _, _ ->
            checkerCalls += 1
            error("Saved-dish names must remain opaque to the system checker")
        }) { true }

        assertEquals(SpellCheckReason.NO_CANDIDATE, result.reason)
        assertFalse(result.providerAttempted)
        assertEquals(0, checkerCalls)
    }

    @Test
    fun reportedTwoTyposUseAtMostThreeIsolatedProviderCallsAndResolveWholeRead() {
        val text = "Give me my recorded proteen total for tday"
        val host = MetriaAgentGroundingPolicy.requiredTool(text, today = today)
        val seen = mutableListOf<String>()
        val result = MiniLmSpellCorrectionPolicy.resolve(host, text, null, today, EnglishSpellChecker { token, _ ->
            seen += token
            val correction = when (token.lowercase()) {
                "proteen" -> "protein"
                "tday" -> "today"
                else -> error("Unexpected provider candidate")
            }
            SpellCheckResult(SpellCheckReason.ACCEPTED, listOf(
                SpellSuggestion(correction, 0, token.length, looksLikeTypo = true, inDictionary = false),
            ))
        }) { true }

        assertEquals(listOf("proteen", "tday"), seen)
        assertEquals(SpellCheckReason.ACCEPTED, result.reason)
        assertEquals("read_day_nutrition", result.resolvedRead?.requiredTool?.name)
        assertEquals("protein", result.resolvedRead?.requiredTool?.metric)
        assertEquals(today, result.resolvedRead?.requiredTool?.targetDate)
        assertEquals("give me my recorded protein total for today", result.resolvedRead?.classifierText)
    }

    @Test
    fun localCorrectionsComposeRecoredProteinAndContextualTotWithoutSystemService() {
        listOf(
            "give me my recored protein tot for today?" to "protein",
            "Give me my recored protien tot for today?" to "protein",
            "Give me my recored cal tot for today?" to "calories",
        ).forEach { (text, expectedMetric) ->
            var called = false
            val host = MetriaAgentGroundingPolicy.requiredTool(text, today = today)
            val result = MiniLmSpellCorrectionPolicy.resolve(host, text, null, today, EnglishSpellChecker { _, _ ->
                called = true
                SpellCheckResult(SpellCheckReason.RUNTIME_FAILURE)
            }) { true }

            assertFalse(called)
            assertFalse(result.providerAttempted)
            assertEquals(SpellCheckReason.ACCEPTED, result.reason)
            assertEquals("read_day_nutrition", result.resolvedRead?.requiredTool?.name)
            assertEquals(expectedMetric, result.resolvedRead?.requiredTool?.metric)
            assertEquals(today, result.resolvedRead?.requiredTool?.targetDate)
        }
    }

    @Test
    fun savedDishNaturalPhrasingAndFrameTyposUseBoundedLocalCorrectionOnly() {
        val cases = listOf(
            "Could you fnd my saved dish san jacobo?" to ("san jacobo" to CompanionAssistantPolicy.IntentKind.SAVED_RECALL),
            "I just had my ususal breakfast" to ("breakfast" to CompanionAssistantPolicy.IntentKind.SAVED_DRAFT),
            "I just ate my usual breakfas" to ("breakfast" to CompanionAssistantPolicy.IntentKind.SAVED_DRAFT),
        )
        cases.forEach { (text, expected) ->
            var providerCalled = false
            val result = MiniLmSpellCorrectionPolicy.resolve(
                MetriaAgentGroundingPolicy.requiredTool(text, today = today), text, null, today,
                EnglishSpellChecker { _, _ ->
                    providerCalled = true
                    SpellCheckResult(SpellCheckReason.RUNTIME_FAILURE)
                },
            ) { true }

            assertFalse(text, providerCalled)
            assertFalse(text, result.providerAttempted)
            assertEquals(text, SpellCheckReason.ACCEPTED, result.reason)
            assertEquals(text, expected.second, result.resolvedRead?.companion?.kind)
            assertEquals(text, expected.first, result.resolvedRead?.requiredTool?.query)
        }
        val correctedRecall = MiniLmSpellCorrectionPolicy.resolve(
            null, "Could you fnd my saved dish san jacobo?", null, today,
            EnglishSpellChecker { _, _ -> error("Saved-dish correction never calls the OS spell checker") },
        ) { true }
        assertEquals("find my saved dish name", correctedRecall.resolvedRead?.classifierText)
        assertEquals("san jacobo", correctedRecall.resolvedRead?.requiredTool?.query)
    }

    @Test
    fun savedDishFrameNeverCorrectsOpaqueQueryOrChangesReadVersusDraftPermission() {
        val typoInOpaqueQuery = "Could you fnd my saved dish san jacob?"
        var providerCalls = 0
        val opaque = MiniLmSpellCorrectionPolicy.resolve(
            null, typoInOpaqueQuery, null, today,
            EnglishSpellChecker { _, _ -> providerCalls++; SpellCheckResult(SpellCheckReason.ACCEPTED) },
        ) { true }
        assertEquals(0, providerCalls)
        assertEquals("san jacob", opaque.resolvedRead?.requiredTool?.query)
        assertEquals("find my saved dish name", opaque.resolvedRead?.classifierText)
        assertEquals("san jacob", opaque.resolvedRead?.requiredTool?.query)

        val routingWordsInQuery = "Could you fnd my saved dish protein total show?"
        val queryResult = MiniLmSpellCorrectionPolicy.resolve(
            null, routingWordsInQuery, null, today,
            EnglishSpellChecker { _, _ -> error("Opaque saved-dish query must not be sent to the provider") },
        ) { true }
        assertEquals("protein total show", queryResult.resolvedRead?.requiredTool?.query)
        assertEquals("find my saved dish name", queryResult.resolvedRead?.classifierText)

        val invalidAction = "I hate my ususal breakfast"
        val invalid = MiniLmSpellCorrectionPolicy.resolve(
            null, invalidAction, null, today,
            EnglishSpellChecker { _, _ -> providerCalls++; SpellCheckResult(SpellCheckReason.ACCEPTED) },
        ) { true }
        assertEquals(0, providerCalls)
        assertNull(invalid.resolvedRead)

        val hostConflict = MiniLmSpellCorrectionPolicy.resolve(
            RequiredAgentTool("read_weight"), "I just had my ususal breakfast", null, today,
            EnglishSpellChecker { _, _ -> error("Saved-dish frame never calls the OS spell checker") },
        ) { true }
        assertNull(hostConflict.resolvedRead)
    }

    @Test
    fun malformedSavedDishFrameCannotSendOpaqueNameToSystemSpellChecker() {
        val text = "showme my saved dish proteen"
        var providerCalled = false
        val result = MiniLmSpellCorrectionPolicy.resolve(
            MetriaAgentGroundingPolicy.requiredTool(text, today = today), text, null, today,
            EnglishSpellChecker { _, _ ->
                providerCalled = true
                SpellCheckResult(SpellCheckReason.ACCEPTED, listOf(
                    SpellSuggestion("protein", 0, "proteen".length, looksLikeTypo = true, inDictionary = false),
                ))
            },
        ) { true }

        assertFalse(providerCalled)
        assertFalse(result.providerAttempted)
        assertNull(result.resolvedRead)

        val malformedNamedDraft = "I just had my saved sncak"
        val scope = requireNotNull(CompanionAssistantPolicy.savedDishCorrectionScope(
            MiniLmReadTextNormalization.normalize(malformedNamedDraft),
        ))
        assertEquals("sncak", scope.opaqueQuery)
        assertEquals(CompanionAssistantPolicy.IntentKind.SAVED_DRAFT, scope.intent)
        assertTrue(scope.editableTargetsByWordIndex.isEmpty())
        val draftResult = MiniLmSpellCorrectionPolicy.resolve(
            MetriaAgentGroundingPolicy.requiredTool(malformedNamedDraft, today = today),
            malformedNamedDraft, null, today,
            EnglishSpellChecker { _, _ -> error("Saved dish names must never reach the spell checker") },
        ) { true }
        assertFalse(draftResult.providerAttempted)
        assertNull(draftResult.resolvedRead)
    }

    @Test
    fun savedDishInvalidDateMixedNegationAndAdviceFramesAbstainWithoutProvider() {
        listOf(
            "Find my saved dish san jacobo tomorrow",
            "Find my saved dish san jacobo today",
            "Find my saved dish san jacobo yesterday",
            "Find my saved dish san jacobo on Monday",
            "Could you fnd my saved dish san jacobo yesterday",
            "I just had my ususal breakfast yesterday",
            "I just had my ususal breakfast and log it",
            "I did not eat my ususal breakfast",
            "Should I eat my ususal breakfast?",
            "I just ate my breakfas",
        ).forEach { text ->
            var called = false
            val result = MiniLmSpellCorrectionPolicy.resolve(
                MetriaAgentGroundingPolicy.requiredTool(text, today = today), text, null, today,
                EnglishSpellChecker { _, _ -> called = true; SpellCheckResult(SpellCheckReason.ACCEPTED) },
            ) { true }
            assertFalse(text, called)
            assertNull(text, result.resolvedRead)
            assertFalse(text, result.providerAttempted)
        }
    }

    @Test
    fun commandWordTypoMayReachProviderOnlyWhenWholeReadCanBeCompleted() {
        val text = "gime me my protein total for today"
        val host = MetriaAgentGroundingPolicy.requiredTool(text, today = today)
        var seen: String? = null
        val result = MiniLmSpellCorrectionPolicy.resolve(host, text, null, today, EnglishSpellChecker { token, _ ->
            seen = token
            SpellCheckResult(SpellCheckReason.ACCEPTED, listOf(
                SpellSuggestion("give", 0, token.length, looksLikeTypo = true, inDictionary = false),
            ))
        }) { true }

        assertEquals("gime", seen)
        assertEquals(SpellCheckReason.ACCEPTED, result.reason)
        assertEquals("read_day_nutrition", result.resolvedRead?.requiredTool?.name)
    }

    @Test
    fun localDateWordsAndProtectedValidWordsAreNeverLocallyRewritten() {
        listOf(
            "Show my recorded meals for today",
            "Show my recorded meals for yesterday",
            "Show my recorded meat today",
            "Show my recorded meals for October",
            "Show my meals and do not save anything",
            "Show my meals for my friend today",
        ).forEach { text ->
            var called = false
            val result = MiniLmSpellCorrectionPolicy.resolve(
                MetriaAgentGroundingPolicy.requiredTool(text, today = today), text, null, today,
                EnglishSpellChecker { _, _ -> called = true; SpellCheckResult(SpellCheckReason.RUNTIME_FAILURE) },
            ) { true }
            assertFalse(text, called)
            assertNull(text, result.resolvedRead)
        }
    }

    @Test
    fun fourWordCorrectionPlanIsRejectedRatherThanSilentlyTruncated() {
        val text = "shwo my recored mealsz for tday"
        var calls = 0
        val result = MiniLmSpellCorrectionPolicy.resolve(
            MetriaAgentGroundingPolicy.requiredTool(text, today = today), text, null, today,
            EnglishSpellChecker { _, _ -> calls++; SpellCheckResult(SpellCheckReason.RUNTIME_FAILURE) },
        ) { true }

        assertEquals(0, calls)
        assertNull(result.resolvedRead)
        assertFalse(result.providerAttempted)
    }

    @Test
    fun multipleProviderCallsShareOneDecreasingDeadlineBudget() {
        val text = "Give me my recorded proteen total for tday"
        val host = MetriaAgentGroundingPolicy.requiredTool(text, today = today)
        val budgets = mutableListOf<Long>()
        var fakeNowNanos = 0L
        val checker = object : EnglishSpellChecker {
            override fun check(token: String, isActive: () -> Boolean): SpellCheckResult =
                error("The budget-aware method must be used by the Android adapter")

            override fun checkWithin(token: String, isActive: () -> Boolean, budgetMillis: Long): SpellCheckResult {
                budgets += budgetMillis
                fakeNowNanos += 35_000_000L
                val correction = if (token == "proteen") "protein" else "today"
                return SpellCheckResult(SpellCheckReason.ACCEPTED, listOf(
                    SpellSuggestion(correction, 0, token.length, true, false),
                ))
            }
        }
        val result = MiniLmSpellCorrectionPolicy.resolve(
            host, text, null, today, checker, nowNanos = { fakeNowNanos }, isActive = { true },
        )

        assertEquals(2, budgets.size)
        assertTrue(budgets.first() <= 400L)
        assertTrue(budgets.last() < budgets.first())
        assertEquals(SpellCheckReason.ACCEPTED, result.reason)
    }

    @Test
    fun expiredSharedDeadlineRejectsLateSuggestionBeforeAnotherProviderCall() {
        val text = "Give me my recorded proteen total for tday"
        val host = MetriaAgentGroundingPolicy.requiredTool(text, today = today)
        var fakeNowNanos = 0L
        var calls = 0
        val checker = object : EnglishSpellChecker {
            override fun check(token: String, isActive: () -> Boolean): SpellCheckResult =
                error("The budget-aware method must be used by the Android adapter")

            override fun checkWithin(token: String, isActive: () -> Boolean, budgetMillis: Long): SpellCheckResult {
                calls++
                fakeNowNanos += 401_000_000L
                return SpellCheckResult(SpellCheckReason.ACCEPTED, listOf(
                    SpellSuggestion("protein", 0, token.length, true, false),
                ))
            }
        }
        val result = MiniLmSpellCorrectionPolicy.resolve(
            host, text, null, today, checker, nowNanos = { fakeNowNanos }, isActive = { true },
        )

        assertEquals(1, calls)
        assertEquals(SpellCheckReason.TIMEOUT, result.reason)
        assertNull(result.resolvedRead)
    }

    @Test
    fun exactAndWhitespaceCorrectQuestionsNeverConsultProvider() {
        listOf(
            "Give me my recorded protein total for today",
            "Give me my recorded protein total for today  ",
        ).forEach { text ->
            var called = false
            val host = MetriaAgentGroundingPolicy.requiredTool(text, today = today)
            assertEquals("read_day_nutrition", MiniLmMealReadPolicy.resolveReadRequest(host, text, null, today)?.requiredTool?.name)
            val result = MiniLmSpellCorrectionPolicy.resolve(host, text, null, today, EnglishSpellChecker { _, _ ->
                called = true
                SpellCheckResult(SpellCheckReason.RUNTIME_FAILURE)
            }) { true }
            assertFalse(called)
            assertEquals(SpellCheckReason.NO_CANDIDATE, result.reason)
            assertFalse(result.providerAttempted)
        }
    }

    @Test
    fun rejectsDictionarySuggestionsOffsetsAndMixedOrSensitiveRequestsBeforeProvider() {
        val text = "Give me my recorded protein total for tday"
        val host = MetriaAgentGroundingPolicy.requiredTool(text, today = today)
        listOf(
            SpellSuggestion("today", 1, 4, true, false),
            SpellSuggestion("today", 0, 3, true, false),
            SpellSuggestion("today", 0, 4, false, false),
            SpellSuggestion("today", 0, 4, true, true),
        ).forEach { suggestion ->
            val result = MiniLmSpellCorrectionPolicy.resolve(host, text, null, today, EnglishSpellChecker { token, _ ->
                SpellCheckResult(SpellCheckReason.ACCEPTED, listOf(suggestion))
            }) { true }
            assertNull(result.resolvedRead)
        }
        listOf(
            "Show my meals for tday and save a snack",
            "Show my food log for breakfast tday",
            "I had my saved dish san jacobo tday",
            "Give me my recorded protein total for tday and record calories",
        ).forEach { unsafe ->
            var called = false
            val unsafeHost = MetriaAgentGroundingPolicy.requiredTool(unsafe, today = today)
            val result = MiniLmSpellCorrectionPolicy.resolve(unsafeHost, unsafe, null, today, EnglishSpellChecker { _, _ ->
                called = true
                SpellCheckResult(SpellCheckReason.ACCEPTED)
            }) { true }
            assertFalse(unsafe, called)
            assertFalse(unsafe, result.providerAttempted)
        }
    }

    @Test
    fun skipsSelectedRecordsTerseFollowUpsAndConflictingHostTools() {
        val text = "Show my recorded meals tday"
        val host = MetriaAgentGroundingPolicy.requiredTool(text, today = today)
        var called = false
        val selected = MiniLmSpellCorrectionPolicy.resolve(
            host, text, "selected-meal", today,
            EnglishSpellChecker { _, _ -> called = true; SpellCheckResult(SpellCheckReason.ACCEPTED) },
        ) { true }
        assertFalse(called)
        assertNull(selected.resolvedRead)

        val followUp = "And protien?"
        val followUpResult = MiniLmSpellCorrectionPolicy.resolve(
            MetriaAgentGroundingPolicy.requiredTool(followUp, today = today), followUp, null, today,
            EnglishSpellChecker { _, _ -> called = true; SpellCheckResult(SpellCheckReason.ACCEPTED) },
        ) { true }
        assertFalse(called)
        assertFalse(followUpResult.providerAttempted)
        assertNull(followUpResult.resolvedRead)
        called = false
        val conflict = MiniLmSpellCorrectionPolicy.resolve(
            RequiredAgentTool("read_weight", metric = "latest_weight"), text, null, today,
            EnglishSpellChecker { _, _ -> called = true; SpellCheckResult(SpellCheckReason.ACCEPTED) },
        ) { true }
        assertFalse(called)
        assertNull(conflict.resolvedRead)
    }

    @Test
    fun ambiguousSuggestionsAndNonActiveRequestsAbstain() {
        val text = "Show my recorded meals tday"
        val host = MetriaAgentGroundingPolicy.requiredTool(text, today = today)
        val withIrrelevantSuggestion = MiniLmSpellCorrectionPolicy.resolve(host, text, null, today, EnglishSpellChecker { token, _ ->
            SpellCheckResult(SpellCheckReason.ACCEPTED, listOf(
                SpellSuggestion("today", 0, token.length, true, false),
                SpellSuggestion("teddy", 0, token.length, true, false),
            ))
        }) { true }
        assertEquals(SpellCheckReason.ACCEPTED, withIrrelevantSuggestion.reason)
        assertEquals("read_meals", withIrrelevantSuggestion.resolvedRead?.requiredTool?.name)

        var called = false
        val inactive = MiniLmSpellCorrectionPolicy.resolve(host, text, null, today, EnglishSpellChecker { _, _ ->
            called = true
            SpellCheckResult(SpellCheckReason.ACCEPTED)
        }) { false }
        assertFalse(called)
        assertEquals(SpellCheckReason.NO_CANDIDATE, inactive.reason)
    }

    @Test
    fun unavailableDisabledTimeoutNoSuggestionAndCancellationStatusesAreBounded() {
        val text = "Show my recorded meals tday"
        val host = MetriaAgentGroundingPolicy.requiredTool(text, today = today)
        listOf(
            SpellCheckReason.UNAVAILABLE,
            SpellCheckReason.DISABLED,
            SpellCheckReason.TIMEOUT,
            SpellCheckReason.NO_SUGGESTIONS,
            SpellCheckReason.RUNTIME_FAILURE,
        ).forEach { status ->
            val result = MiniLmSpellCorrectionPolicy.resolve(host, text, null, today, EnglishSpellChecker { _, _ ->
                SpellCheckResult(status)
            }) { true }
            assertEquals(status, result.reason)
            assertTrue(result.providerAttempted)
            assertNull(result.resolvedRead)
        }

        var active = true
        val cancelled = MiniLmSpellCorrectionPolicy.resolve(host, text, null, today, EnglishSpellChecker { token, _ ->
            active = false
            SpellCheckResult(SpellCheckReason.ACCEPTED, listOf(
                SpellSuggestion("today", 0, token.length, true, false),
            ))
        }) { active }
        assertEquals(SpellCheckReason.INTERRUPTED, cancelled.reason)
        assertTrue(cancelled.providerAttempted)
        assertNull(cancelled.resolvedRead)
    }

    @Test
    fun unrelatedAndWriteMeaningsDoNotReachProvider() {
        val text = "Show my meals for tday and save a snack"
        var called = false
        val result = MiniLmSpellCorrectionPolicy.resolve(
            MetriaAgentGroundingPolicy.requiredTool(text, today = today), text, null, today,
            EnglishSpellChecker { _, _ -> called = true; SpellCheckResult(SpellCheckReason.ACCEPTED) },
        ) { true }
        assertFalse(called)
        assertFalse(result.providerAttempted)
    }
}

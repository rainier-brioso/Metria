package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatCurrentDraftFoodPolicyTest {
    private fun catalog(vararg descriptions: String) = OfflineFoodCatalog(
        sourceName = "test", sourceRelease = "test",
        foods = descriptions.mapIndexed { index, description -> CatalogFood(
            fdcId = index + 1L, description = description, spanishDisplayName = null,
            category = null, nutrients = emptyMap(), portions = emptyList(),
        ) },
    )

    private fun item(name: String, group: String? = null) = MealProposalItem(
        name = name,
        description = "plain",
        visiblePortion = "one serving",
        estimatedGramsRange = GramRange(100, 180),
        preparation = "boiled",
        confidence = "medium",
        evidence = "seen",
        groupLabel = group,
    )

    private fun proposal(
        items: List<MealProposalItem> = listOf(item("white rice")),
        omitted: Int = 0,
        partial: MealProposalOmissionCategory? = null,
    ) = MealProposal(items, listOf("original assumption"), listOf("original question"), "medium", omitted, partial)

    @Test fun `eligibility is limited to a complete ordinary single simple food draft`() {
        val simple = proposal()
        assertEquals(0 to simple.items.single(), ChatCurrentDraftFoodPolicy.eligibleItem(simple, "meal", true))
        assertEquals(0 to simple.items.single(), ChatCurrentDraftFoodPolicy.eligibleItem(simple, "agent_meal_portion_edit", true))
        assertNull(ChatCurrentDraftFoodPolicy.eligibleItem(simple, "saved_dish", true))
        assertNull(ChatCurrentDraftFoodPolicy.eligibleItem(simple, "meal", false))
        assertNull(ChatCurrentDraftFoodPolicy.eligibleItem(null, "meal", true))
        assertNull(ChatCurrentDraftFoodPolicy.eligibleItem(proposal(omitted = 1), "meal", true))
        assertNull(ChatCurrentDraftFoodPolicy.eligibleItem(
            proposal(partial = MealProposalOmissionCategory.MULTIPLE), "meal", true,
        ))
        assertNull(ChatCurrentDraftFoodPolicy.eligibleItem(proposal(listOf(item("rice", "Lunch"))), "meal", true))
        assertNull(ChatCurrentDraftFoodPolicy.eligibleItem(proposal(listOf(item("rice"), item("beans"))), "meal", true))
        assertNull(ChatCurrentDraftFoodPolicy.eligibleItem(proposal(listOf(item("rice and beans"))), "meal", true))
    }

    @Test fun `only a literal single positive correction name from this turn is accepted`() {
        assertEquals("brown rice", ChatCurrentDraftFoodPolicy.matchingReplacement(
            "Actually, it was brown rice.", "brown rice",
        ))
        assertEquals("brown rice", ChatCurrentDraftFoodPolicy.matchingReplacement(
            "Actually, it was brown rice, please.", "brown rice",
        ))
        assertEquals("Brown Rice", ChatCurrentDraftFoodPolicy.matchingReplacement(
            "Actually, it was BROWN RICE!", "Brown Rice",
        ))
        listOf(
            "Was it brown rice?",
            "Actually, it was not brown rice",
            "Actually, it wasn't brown rice",
            "Actually, it was brown rice and add an egg",
            "Actually, it was brown rice instead of white rice, 150 g",
            "Actually, it was rice with beans",
            "Actually, it was brown rice, chicken",
            "Actually, it was brown rice, or chicken",
            "Actually, it was brown rice/chicken",
            "Actually, it was brown rice; chicken",
            "Actually, it was brown rice, not brown rice",
        ).forEach { text ->
            assertNull(text, ChatCurrentDraftFoodPolicy.matchingReplacement(text, "brown rice"))
        }
        assertNull(ChatCurrentDraftFoodPolicy.matchingReplacement("Actually, it was brown rice", "basmati rice"))
    }

    @Test fun `local catalog route accepts complete description-backed name phrases only`() {
        val original = proposal()
        val context = requireNotNull(ChatCurrentDraftFoodPolicy.context(
            proposal = original, operation = "agent_meal_draft", requestId = 41,
            activeRequestId = 41, handledReplyRequestId = 41, conversationGeneration = 7,
            acceptedOverride = null, currentGrams = 150, unlocked = true, stopping = false, pending = false,
        ))
        fun route(text: String, foods: OfflineFoodCatalog? = catalog(
            "Rice, brown, long grain, raw", "Chicken, broilers or fryers, breast, meat only, cooked",
            "Egg, whole, cooked",
        ), request: Long? = 41, active: Long? = 41, handled: Long? = 41, unlocked: Boolean = true,
            stopping: Boolean = false, pending: Boolean = false, draftContext: ChatCurrentDraftFoodContext? = context,
        ) = ChatCurrentDraftFoodPolicy.localCatalogReplacement(
            context = draftContext, text = text, catalog = foods, proposal = original,
            requestId = request, activeRequestId = active, handledReplyRequestId = handled, conversationGeneration = 7,
            operation = "agent_meal_draft", unlocked = unlocked, stopping = stopping, pending = pending,
        )

        val brown = requireNotNull(route("Actually, it was brown rice"))
        assertEquals("brown rice", brown.replacementName)
        assertEquals(41L, brown.requestId)
        assertEquals(7L, brown.conversationGeneration)
        assertEquals(original, brown.originalProposal)
        assertEquals("white rice", brown.originalItemName)
        assertEquals(150, context.currentGrams)

        assertEquals("chicken breast", route("Actually, it was chicken breast")?.replacementName)
        assertEquals("eggs", route("Actually, it was eggs")?.replacementName)
        listOf(
            "Actually, it was brown", // modifier alone
            "Actually, it was basmati rice", // not in packaged vocabulary
            "Actually, it was ricey", // substring is not a token match
            "Actually, it was brown rice, chicken", // ambiguous list
            "Actually, it was brown rice and add an egg", // mixed edit
            "Actually, it was brown rice, 200 g", // quantity
        ).forEach { assertNull(it, route(it)) }
        assertNull(route("Actually, it was brown rice", foods = null))
        assertNull(route("Actually, it was brown rice", active = 42))
        assertNull(route("Actually, it was brown rice", request = 42))
        assertNull(route("Actually, it was brown rice", handled = 42))
        assertNull(route("Actually, it was brown rice", unlocked = false))
        assertNull(route("Actually, it was brown rice", stopping = true))
        assertNull(route("Actually, it was brown rice", pending = true))
        assertNull(route("Actually, it was brown rice", draftContext = null))
        assertNull(route(
            "Actually, it was brown rice", draftContext = context.copy(conversationGeneration = 6),
        ))

        val repeatedContext = requireNotNull(ChatCurrentDraftFoodPolicy.context(
            proposal = original, operation = "agent_meal_draft", requestId = 41,
            activeRequestId = 41, handledReplyRequestId = 41, conversationGeneration = 7,
            acceptedOverride = brown, currentGrams = 150, unlocked = true, stopping = false, pending = false,
        ))
        val repeated = requireNotNull(ChatCurrentDraftFoodPolicy.localCatalogReplacement(
            context = repeatedContext, text = "Actually, it was chicken breast",
            catalog = catalog("Chicken, broilers or fryers, breast, meat only, cooked"),
            proposal = original, activeRequestId = 41, handledReplyRequestId = 41,
            requestId = 41,
            conversationGeneration = 7, operation = "agent_meal_draft", unlocked = true,
            stopping = false, pending = false,
        ))
        assertEquals("chicken breast", repeated.replacementName)
        assertEquals(original, repeated.originalProposal)
        assertEquals(150, repeatedContext.currentGrams)

        assertNull(ChatCurrentDraftFoodPolicy.localCatalogReplacement(
            context = context, text = "Actually, it was brown rice",
            catalog = catalog("Restaurant Foods, brown rice, long grain, raw"),
            proposal = original, activeRequestId = 41, handledReplyRequestId = 41,
            requestId = 41,
            conversationGeneration = 7, operation = "agent_meal_draft", unlocked = true,
            stopping = false, pending = false,
        ))
        assertNull(ChatCurrentDraftFoodPolicy.localCatalogReplacement(
            context = context, text = "Actually, it was brown rice",
            catalog = catalog("Rice, white, long grain, raw").copy(
                foods = listOf(catalog("Rice, white, long grain, raw").foods.single().copy(category = "brown rice")),
            ),
            proposal = original, activeRequestId = 41, handledReplyRequestId = 41,
            requestId = 41,
            conversationGeneration = 7, operation = "agent_meal_draft", unlocked = true,
            stopping = false, pending = false,
        ))
    }

    @Test fun `native food correction retry is bounded to one eligible no-tool turn`() {
        assertTrue(ChatCurrentDraftFoodPolicy.isEligibleCorrectionTurn("Actually, it was basmati rice"))
        assertTrue(ChatCurrentDraftFoodPolicy.shouldRetryNoToolCorrection(
            currentDraftFoodAvailable = true, text = "Actually, it was basmati rice",
            noToolCalls = true, requiredRead = false, navigation = false, retryAlreadyAttempted = false,
        ))
        assertFalse(ChatCurrentDraftFoodPolicy.shouldRetryNoToolCorrection(
            currentDraftFoodAvailable = true, text = "Actually, it was basmati rice",
            noToolCalls = false, requiredRead = false, navigation = false, retryAlreadyAttempted = false,
        ))
        assertFalse(ChatCurrentDraftFoodPolicy.shouldRetryNoToolCorrection(
            currentDraftFoodAvailable = true, text = "Actually, it was basmati rice",
            noToolCalls = true, requiredRead = false, navigation = false, retryAlreadyAttempted = true,
        ))
        assertFalse(ChatCurrentDraftFoodPolicy.shouldRetryNoToolCorrection(
            currentDraftFoodAvailable = false, text = "Actually, it was basmati rice",
            noToolCalls = true, requiredRead = false, navigation = false, retryAlreadyAttempted = false,
        ))
        listOf(
            "Was it basmati rice?", "It was not basmati rice", "Actually, basmati rice, chicken",
            "Actually, basmati rice/chicken", "Actually, basmati rice; chicken", "Actually, basmati rice, 150 g",
            "Actually, I had a bad night's sleep", "Actually, I had brown rice", "Actually, what do you think about brown rice?",
            "Actually, brown rice was delicious",
        ).forEach { text ->
            assertFalse(text, ChatCurrentDraftFoodPolicy.isEligibleCorrectionTurn(text))
            assertFalse(text, ChatCurrentDraftFoodPolicy.shouldRetryNoToolCorrection(
                currentDraftFoodAvailable = true, text = text,
                noToolCalls = true, requiredRead = false, navigation = false, retryAlreadyAttempted = false,
            ))
        }
        assertFalse(ChatCurrentDraftFoodPolicy.shouldRetryNoToolCorrection(
            currentDraftFoodAvailable = true, text = "Actually, it was basmati rice",
            noToolCalls = true, requiredRead = true, navigation = false, retryAlreadyAttempted = false,
        ))
        assertFalse(ChatCurrentDraftFoodPolicy.shouldRetryNoToolCorrection(
            currentDraftFoodAvailable = true, text = "Actually, it was basmati rice",
            noToolCalls = true, requiredRead = false, navigation = true, retryAlreadyAttempted = false,
        ))
        assertNull(ChatCurrentDraftFoodPolicy.matchingReplacement("Actually, it was organic brown rice", "brown rice"))
        assertEquals("organic brown rice", ChatCurrentDraftFoodPolicy.matchingReplacement(
            "Actually, it was organic brown rice", "organic brown rice",
        ))
        val fallback = ChatCurrentDraftFoodPolicy.correctionRetryNotSelectedReply()
        assertTrue(fallback.contains("draft is unchanged"))
        assertFalse(fallback.contains("changed to"))
    }

    @Test fun `food labels reject instructions quantities and unsupported syntax`() {
        listOf("brown rice and beans", "rice 150 g", "Actually", "2 eggs", "rice?", "rice/beans")
            .forEach { assertFalse(it, ChatCurrentDraftFoodPolicy.validReviewLabel(it)) }
        assertTrue(ChatCurrentDraftFoodPolicy.validReviewLabel("brown rice"))
    }

    @Test fun `context and replacement overlay stay bound to the live request and immutable proposal`() {
        val original = proposal()
        val context = requireNotNull(ChatCurrentDraftFoodPolicy.context(
            proposal = original,
            operation = "agent_meal_draft",
            requestId = 41,
            activeRequestId = 41,
            handledReplyRequestId = 41,
            conversationGeneration = 7,
            acceptedOverride = null,
            currentGrams = 150,
            unlocked = true,
            stopping = false,
            pending = false,
        ))
        assertTrue(ChatCurrentDraftFoodPolicy.contextIsCurrent(context, original, 41, 7, "agent_meal_draft", true, false))

        val override = requireNotNull(ChatCurrentDraftFoodPolicy.overrideForCurrentDraft(context, 42, "brown rice"))
        assertEquals(42L, override.requestId)
        assertEquals(original, override.originalProposal)
        assertEquals("white rice", override.originalItemName)
        assertTrue(ChatCurrentDraftFoodPolicy.isCurrent(override, original, 42, 7, true, false))
        assertTrue(ChatCurrentDraftFoodPolicy.isCurrent(
            requireNotNull(ChatCurrentDraftFoodPolicy.rebind(context, 43, "brown rice")), original, 43, 7, true, false,
        ))
        assertNull(ChatCurrentDraftFoodPolicy.rebind(context, 43, null))

        assertFalse(ChatCurrentDraftFoodPolicy.isCurrent(override, original, 41, 7, true, false))
        assertFalse(ChatCurrentDraftFoodPolicy.isCurrent(override, original, 42, 8, true, false))
        assertFalse(ChatCurrentDraftFoodPolicy.isCurrent(override, original, 42, 7, false, false))
        assertFalse(ChatCurrentDraftFoodPolicy.isCurrent(override, original, 42, 7, true, true))
        assertFalse(ChatCurrentDraftFoodPolicy.isCurrent(
            override, original.copy(assumptions = listOf("changed")), 42, 7, true, false,
        ))

        assertFalse(ChatCurrentDraftFoodPolicy.contextIsCurrent(context, original, 40, 7, "agent_meal_draft", true, false))
        assertFalse(ChatCurrentDraftFoodPolicy.contextIsCurrent(context, original, 41, 8, "agent_meal_draft", true, false))
        assertFalse(ChatCurrentDraftFoodPolicy.contextIsCurrent(
            context, original.copy(items = listOf(item("oats"))), 41, 7, "agent_meal_draft", true, false,
        ))
        assertFalse(ChatCurrentDraftFoodPolicy.contextIsCurrent(context, original, 41, 7, "read_meals", true, false))
        assertFalse(ChatCurrentDraftFoodPolicy.contextIsCurrent(context, original, 41, 7, "agent_meal_draft", false, false))
        assertFalse(ChatCurrentDraftFoodPolicy.contextIsCurrent(context, original, 41, 7, "agent_meal_draft", true, true))
    }

    @Test fun `context creation rejects stale lifecycle and malformed accepted overlay`() {
        val original = proposal()
        fun context(
            request: Long? = 41,
            active: Long? = 41,
            handled: Long? = 41,
            generation: Long = 7,
            grams: Int? = 150,
            unlocked: Boolean = true,
            stopping: Boolean = false,
            pending: Boolean = false,
            accepted: ChatFoodReplacementOverride? = null,
        ) = ChatCurrentDraftFoodPolicy.context(
            original, "agent_meal_draft", request, active, handled, generation, accepted,
            grams, unlocked, stopping, pending,
        )

        assertNull(context(active = 42))
        assertNull(context(handled = 42))
        assertNull(context(request = null))
        assertNull(context(unlocked = false))
        assertNull(context(stopping = true))
        assertNull(context(pending = true))
        assertNull(context(grams = 2001))
        val old = requireNotNull(context())
        val staleOverride = requireNotNull(ChatCurrentDraftFoodPolicy.overrideForCurrentDraft(old, 40, "brown rice"))
        assertNull(context(accepted = staleOverride))
    }

    @Test fun `final draft turn applies only the explicit replacement tool and retains retry cases`() {
        assertEquals(
            CurrentDraftTurnDisposition.APPLY_FOOD_EDIT,
            ChatPortionClarificationPolicy.currentDraftTurnDisposition(
                contextAvailable = true,
                finalToolNames = setOf("replace_current_draft_food"),
                selectedIntent = "replace_current_draft_food",
                requiredRead = false,
                navigation = false,
                newMealIntent = false,
            ),
        )
        assertEquals(
            CurrentDraftTurnDisposition.RETAIN_CONVERSATIONAL_REPLY,
            ChatPortionClarificationPolicy.currentDraftTurnDisposition(
                true, emptySet(), null, false, false, false,
            ),
        )
        assertEquals(
            CurrentDraftTurnDisposition.RETAIN_AND_CLARIFY_CONFIRMATION,
            ChatPortionClarificationPolicy.currentDraftTurnDisposition(
                true, setOf("confirm_meal_draft"), "confirm_meal_draft", false, false, false,
            ),
        )
        assertEquals(
            CurrentDraftTurnDisposition.RETIRE_FOR_EXPLICIT_OPERATION,
            ChatPortionClarificationPolicy.currentDraftTurnDisposition(
                true, setOf("read_meals"), "read_meals", false, false, false,
            ),
        )
        assertEquals(
            CurrentDraftTurnDisposition.NO_CURRENT_DRAFT,
            ChatPortionClarificationPolicy.currentDraftTurnDisposition(
                false, setOf("replace_current_draft_food"), "replace_current_draft_food", false, false, false,
            ),
        )
    }
}

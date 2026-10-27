package app.metria.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import app.metria.CatalogNutrition
import app.metria.GramRange
import app.metria.LabelNutritionDraft
import app.metria.MealProposal
import app.metria.MealProposalItem
import app.metria.MealEntryItem
import app.metria.PersonalDish
import app.metria.ui.components.AssistantContextPanel
import app.metria.ui.components.AssistantPanelChoices
import app.metria.ui.components.AssistantPanelHeader
import app.metria.ui.theme.MetriaColors
import app.metria.ui.theme.MetriaTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.Instant

/** Fixed, invented values. These previews never open the vault or run photo processing. */
private val sampleMeal = MealProposal(
    items = listOf(
        MealProposalItem(
            name = "Grilled chicken breast",
            description = "Lightly seasoned",
            visiblePortion = "one portion",
            estimatedGramsRange = GramRange(120, 160),
            preparation = "grilled",
            confidence = "high",
            groupLabel = "Chicken plate",
        ),
        MealProposalItem(
            name = "Roasted potatoes",
            description = "Some pieces may be hidden",
            visiblePortion = "small side",
            estimatedGramsRange = GramRange(90, 140),
            preparation = "roasted",
            confidence = "medium",
            groupLabel = "Chicken plate",
            evidence = "possible_inside",
        ),
        MealProposalItem(
            name = "Green salad",
            description = "Leafy greens",
            visiblePortion = "side",
            estimatedGramsRange = GramRange(50, 90),
            preparation = "fresh",
            confidence = "medium",
            groupLabel = "Salad",
        ),
    ),
    assumptions = emptyList(),
    questions = emptyList(),
    overallConfidence = "medium",
    omittedItemCount = 1,
)

@Preview(name = "Assistant · idle · portrait", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AssistantIdlePreview() = PreviewHost {
    MetriaAssistantConversationScreen(
        messages = emptyList(), input = "", onInputChange = {},
        assistantThinking = false, assistantStopping = false, responseStopped = false,
        mealDraftThinking = false, streamingReply = null, assistantDiagnostic = null,
        coachDiagnostic = null, savedWeight = null, mealProposal = null,
        recordingVoice = false, preparingVoice = false, voiceMessage = null,
        modelReady = true, onBack = {}, onNewConversation = { false },
        onCancelAssistant = {}, onEditSavedWeight = {}, onUndoSavedWeight = {},
        onReviewMeal = {}, onOpenSavedMeal = {}, onDiscardMealDraft = {},
        onToggleVoice = {}, onEditLatestMemberMessage = {}, onSend = {}, onPanelAction = {},
        introductoryConversation = true,
    )
}

@Preview(name = "Assistant · conversation and choices", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AssistantChoicesPreview() = MetriaTheme {
    var expanded by remember { mutableStateOf(true) }
    AssistantContextPanel(expanded = expanded, onDismiss = {}) { full, canResize ->
        AssistantPanelHeader(
            fullHeight = full,
            canResize = canResize,
            onToggleExpanded = { expanded = !expanded },
            onClose = {}, onOptions = {}, optionsEnabled = true,
        )
        AssistantPanelChoices(
            options = false, enabled = true, voiceEnabled = true, newConversationEnabled = true,
            onBack = {}, onSpeak = {}, onChat = {}, onAction = {}, onNewConversation = {},
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(name = "Assistant · preparing", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AssistantPreparingPreview() = PreviewHost {
    MetriaAssistantConversationScreen(
        messages = listOf(MetriaChatMessage("Analyse this meal", fromMember = true)),
        input = "", onInputChange = {}, assistantThinking = false, assistantStopping = false,
        responseStopped = false, mealDraftThinking = true, streamingReply = null,
        assistantDiagnostic = null, coachDiagnostic = null, savedWeight = null, mealProposal = null,
        recordingVoice = false, preparingVoice = false, voiceMessage = null, modelReady = true,
        onBack = {}, onNewConversation = { false }, onCancelAssistant = {},
        onEditSavedWeight = {}, onUndoSavedWeight = {}, onReviewMeal = {}, onOpenSavedMeal = {},
        onDiscardMealDraft = {}, onToggleVoice = {}, onEditLatestMemberMessage = {}, onSend = {},
        onPanelAction = {},
    )
}

@Preview(name = "Assistant · partial draft", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AssistantPartialDraftPreview() = PreviewHost {
    MetriaAssistantConversationScreen(
        messages = listOf(
            MetriaChatMessage("Analyse this meal", fromMember = true),
            MetriaChatMessage("I made a draft from the visible foods. One item may be missing.", fromMember = false),
        ),
        input = "", onInputChange = {}, assistantThinking = false, assistantStopping = false,
        responseStopped = false, mealDraftThinking = false, streamingReply = null,
        assistantDiagnostic = null, coachDiagnostic = null, savedWeight = null,
        mealProposal = sampleMeal, recordingVoice = false, preparingVoice = false,
        voiceMessage = null, modelReady = true, onBack = {}, onNewConversation = { false },
        onCancelAssistant = {}, onEditSavedWeight = {}, onUndoSavedWeight = {}, onReviewMeal = {},
        onOpenSavedMeal = {}, onDiscardMealDraft = {}, onToggleVoice = {},
        onEditLatestMemberMessage = {}, onSend = {}, onPanelAction = {},
    )
}

@Preview(name = "Assistant · recoverable error", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AssistantErrorPreview() = PreviewHost {
    MetriaAssistantConversationScreen(
        messages = listOf(MetriaChatMessage("Analyse this meal", fromMember = true)),
        input = "", onInputChange = {}, assistantThinking = false, assistantStopping = false,
        responseStopped = false, mealDraftThinking = false, streamingReply = null,
        assistantDiagnostic = "The photo could not be analysed. Try another image or enter the foods manually.",
        coachDiagnostic = null, savedWeight = null, mealProposal = null,
        recordingVoice = false, preparingVoice = false, voiceMessage = null, modelReady = true,
        onBack = {}, onNewConversation = { false }, onCancelAssistant = {},
        onEditSavedWeight = {}, onUndoSavedWeight = {}, onReviewMeal = {}, onOpenSavedMeal = {},
        onDiscardMealDraft = {}, onToggleVoice = {}, onEditLatestMemberMessage = {}, onSend = {},
        onPanelAction = {},
    )
}

@Preview(name = "Assistant · meal saved", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AssistantMealSavedPreview() = PreviewHost {
    MetriaAssistantConversationScreen(
        messages = listOf(MetriaChatMessage(
            "Meal saved on this phone: Chicken plate.\nFoods: Grilled chicken breast, Green salad. Reviewed and confirmed.",
            fromMember = false, savedMealId = "preview-saved-meal", savedMealAvailable = true,
        )),
        input = "", onInputChange = {}, assistantThinking = false, assistantStopping = false,
        responseStopped = false, mealDraftThinking = false, streamingReply = null,
        assistantDiagnostic = null, coachDiagnostic = null, savedWeight = null, mealProposal = null,
        recordingVoice = false, preparingVoice = false, voiceMessage = null, modelReady = true,
        onBack = {}, onNewConversation = { false }, onCancelAssistant = {},
        onEditSavedWeight = {}, onUndoSavedWeight = {}, onReviewMeal = {}, onOpenSavedMeal = {},
        onDiscardMealDraft = {}, onToggleVoice = {}, onEditLatestMemberMessage = {}, onSend = {},
        onPanelAction = {},
    )
}

@Preview(name = "Assistant · meal updated", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AssistantMealUpdatedPreview() = PreviewHost {
    MetriaAssistantConversationScreen(
        messages = listOf(MetriaChatMessage(
            "Meal updated on this phone: Chicken plate.\nFoods: Grilled chicken breast, Green salad. Reviewed and confirmed.",
            fromMember = false, savedMealId = "preview-updated-meal", savedMealAvailable = true,
        )),
        input = "", onInputChange = {}, assistantThinking = false, assistantStopping = false,
        responseStopped = false, mealDraftThinking = false, streamingReply = null,
        assistantDiagnostic = null, coachDiagnostic = null, savedWeight = null, mealProposal = null,
        recordingVoice = false, preparingVoice = false, voiceMessage = null, modelReady = true,
        onBack = {}, onNewConversation = { false }, onCancelAssistant = {},
        onEditSavedWeight = {}, onUndoSavedWeight = {}, onReviewMeal = {}, onOpenSavedMeal = {},
        onDiscardMealDraft = {}, onToggleVoice = {}, onEditLatestMemberMessage = {}, onSend = {},
        onPanelAction = {},
    )
}

@Preview(name = "Saved dishes · narrow · font 150%", showBackground = true, widthDp = 360, heightDp = 740, fontScale = 1.5f)
@Composable
private fun AssistantSavedDishesPreview() = SyntheticSavedDishes(empty = false)

@Preview(name = "Saved dishes · empty", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AssistantSavedDishesEmptyPreview() = SyntheticSavedDishes(empty = true)

@Composable
private fun SyntheticSavedDishes(empty: Boolean) = MetriaTheme {
    AssistantContextPanel(expanded = true, onDismiss = {}) { full, canResize ->
        AssistantPanelHeader(
            fullHeight = full, canResize = canResize, onToggleExpanded = {},
            onClose = {}, onOptions = {}, optionsEnabled = true,
        )
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            androidx.compose.material3.TextButton(onClick = {}) {
                androidx.compose.material3.Text("Back to chat")
            }
            val confirmedAt = Instant.parse("2026-10-26T12:00:00Z")
            AssistantSavedDishesContent(
                dishes = if (empty) emptyList() else listOf(
                    PersonalDish(
                        id = "00000000-0000-0000-0000-000000000001",
                        name = "Chicken plate with roasted potatoes and green salad",
                        aliases = listOf("Usual chicken lunch"),
                        items = listOf(MealEntryItem("Grilled chicken breast", 140, "grilled"), MealEntryItem("Roasted potatoes", 110, "roasted")),
                        createdAt = confirmedAt, lastConfirmedAt = confirmedAt,
                    ),
                    PersonalDish(
                        id = "00000000-0000-0000-0000-000000000002",
                        name = "Rice and vegetables",
                        items = listOf(MealEntryItem("Cooked rice", 150, "boiled"), MealEntryItem("Vegetables", 100, "steamed")),
                        createdAt = confirmedAt, lastConfirmedAt = confirmedAt,
                    ),
                ),
                onSelectDish = {},
            )
        }
    }
}

@Preview(name = "Meal review · portrait", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MealReviewPreview() = MetriaTheme { SyntheticMealReview(partial = false) }

@Preview(name = "Saved dish review · portrait", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun SavedDishReviewPreview() = MetriaTheme { SyntheticMealReview(partial = false, savedDish = true) }

@Preview(name = "Meal review · partial · landscape · font 150%", showBackground = true, widthDp = 852, heightDp = 393, fontScale = 1.5f)
@Composable
private fun MealReviewLargeFontLandscapePreview() = MetriaTheme { SyntheticMealReview(partial = true) }

@Preview(name = "Meal review · narrow · font 150%", showBackground = true, widthDp = 360, heightDp = 740, fontScale = 1.5f)
@Composable
private fun MealReviewNarrowLargeFontPreview() = MetriaTheme { SyntheticMealReview(partial = true) }

@Preview(name = "Label review · font 150%", showBackground = true, widthDp = 393, heightDp = 852, fontScale = 1.5f)
@Composable
private fun LabelReviewLargeFontPreview() = MetriaTheme {
    Column(
        Modifier.fillMaxSize().background(MetriaColors.Canvas).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AssistantLabelReviewContent(
            draft = LabelNutritionDraft(caloriesKcal = 210.0, proteinGrams = 8.0, carbohydrateGrams = 31.0, totalFatGrams = null),
            thumbnail = null,
            initialDate = LocalDate.of(2026, 10, 26),
            initialTime = LocalTime.of(12, 30),
            onSave = { "Preview only. Nothing is saved." },
        )
    }
}

@Composable
private fun SyntheticMealReview(partial: Boolean, savedDish: Boolean = false) {
    val items = remember {
        listOf(
            MealReviewItemDraft("preview-chicken", "dish-main", "Chicken plate", "Grilled chicken breast", "Lightly seasoned", "140", "grilled", "main_food", "seen"),
            MealReviewItemDraft("preview-potato", "dish-main", "Chicken plate", "Roasted potatoes", "May be partly hidden", "110", "roasted", "side", if (partial) "possible_inside" else "seen"),
            MealReviewItemDraft("preview-salad", "dish-side", "Green salad", "Green salad", "Leafy greens", "70", "fresh", "side", "seen"),
        )
    }
    var title by remember { mutableStateOf("Lunch") }
    var date by remember { mutableStateOf("2026-10-26") }
    var detailsExpanded by remember { mutableStateOf(true) }
    var mealSlot by remember { mutableStateOf<String?>("lunch") }
    var saveInFlight by remember { mutableStateOf(false) }
    MealReview(
        proposal = sampleMeal.copy(omittedItemCount = if (partial) 1 else 0),
        manualEntry = savedDish,
        reviewingSavedDish = savedDish,
        usingPreviousDrinkPortion = false,
        unresolvedDrinkQuantity = false,
        items = items,
        title = title,
        onTitleChange = { title = it },
        rememberedDishNames = emptyList(),
        personalDishSuggestions = emptyMap(),
        acceptedDishIds = emptyMap(),
        referenceThumbnailBase64 = null,
        keepReference = false,
        onKeepReferenceChange = {},
        similarMeal = null,
        totals = CatalogNutrition(486.0, 37.0, 46.0, 16.0, caloriesDerivedFromMacros = false),
        onScalePortions = { false },
        isPartialEstimate = partial,
        nutrientDetails = emptyList(),
        includedIds = if (partial) setOf("preview-chicken", "preview-salad")
            else setOf("preview-chicken", "preview-potato", "preview-salad"),
        needsReviewIds = if (partial) setOf("preview-potato") else emptySet(),
        catalog = null,
        date = date,
        onDateChange = { date = it },
        mealSlot = mealSlot,
        suggestedMealSlot = "lunch",
        onMealSlotChange = { mealSlot = it },
        detailsExpanded = detailsExpanded,
        onDetailsExpandedChange = { detailsExpanded = it },
        error = null,
        onEditDish = {},
        onAcceptPersonalDish = { _, _ -> },
        onRejectPersonalDish = {},
        onAddDish = {},
        onUseSimilarName = {},
        onDismiss = {},
        saveInFlight = saveInFlight,
        onSaveInFlightChange = { saveInFlight = it },
        onSave = {},
        composeDialogNavigationInsets = true,
    )
}

/** Android Studio previews do not always provide the activity back-dispatcher local. */
@Composable
private fun PreviewHost(content: @Composable () -> Unit) {
    val owner = remember {
        object : OnBackPressedDispatcherOwner {
            override val lifecycle = LifecycleRegistry(this).apply {
                currentState = Lifecycle.State.RESUMED
            }
            override val onBackPressedDispatcher = OnBackPressedDispatcher()
        }
    }
    CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides owner) {
        MetriaTheme(content)
    }
}

package app.metria.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.metria.GramRange
import app.metria.MealProposal
import app.metria.MealProposalItem
import app.metria.ui.components.AssistantContextPanel
import app.metria.ui.components.AssistantPanelHeader
import app.metria.ui.theme.MetriaTheme

/** Fixed, invented states. All actions are inert; no OS launcher or meal pipeline is attached. */
@Preview(name = "Capture · preparing · Stop", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AssistantCaptureLoadingPreview() = CaptureStatePreview("Analyse meal") {
    AssistantCaptureLoadingState(imageState = "analyzing", onStop = {})
}

@Preview(name = "Capture · partial meal · 150%", showBackground = true, widthDp = 360, heightDp = 820, fontScale = 1.5f)
@Composable
private fun AssistantCapturePartialPreview() = CaptureStatePreview("Analyse meal") {
    AssistantMealDraftCaptureCard(
        proposal = MealProposal(
            items = listOf(
                MealProposalItem(
                    name = "Chickpeas",
                    description = "Visible in the pan",
                    visiblePortion = "one portion",
                    estimatedGramsRange = GramRange(180, 240),
                    preparation = "cooked",
                    confidence = "medium",
                ),
                MealProposalItem(
                    name = "Egg",
                    description = "Visible on top",
                    visiblePortion = "one egg",
                    estimatedGramsRange = GramRange(45, 65),
                    preparation = "cooked",
                    confidence = "high",
                ),
            ),
            assumptions = emptyList(),
            questions = emptyList(),
            overallConfidence = "medium",
            omittedItemCount = 1,
        ),
        onReview = {},
        onDiscard = {},
    )
}

@Preview(name = "Capture · recoverable error", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AssistantCaptureFailurePreview() = CaptureStatePreview("Analyse meal") {
    AssistantCaptureFailureState(label = false, onManualMeal = {})
    AssistantCaptureSourceAction(text = "Choose image", primary = false, onClick = {})
}

@Composable
private fun CaptureStatePreview(title: String, content: @Composable () -> Unit) = MetriaTheme {
    AssistantContextPanel(expanded = true, onDismiss = {}) {
        fullHeight, canResize ->
        AssistantPanelHeader(
            fullHeight = fullHeight,
            canResize = canResize,
            onToggleExpanded = {},
            onClose = {},
            onOptions = {},
            optionsEnabled = false,
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = {}) { Text("Back to chat") }
            Text(title, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            content()
        }
    }
}

package app.metria.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import app.metria.MealProposal
import app.metria.ModelStatus
import app.metria.PhotoMealReviewGate

/** Keeps automatic navigation tied to the active capture destination. */
@Composable
fun rememberPhotoMealReviewHandoff(status: ModelStatus, onReview: (MealProposal) -> Unit): PhotoMealReviewGate {
    val gate = remember { PhotoMealReviewGate() }
    val review = rememberUpdatedState(onReview)
    LaunchedEffect(status.imageState, status.latestImageAudit?.id, status.proposal) {
        gate.consume(status)?.let { review.value(it) }
    }
    DisposableEffect(gate) { onDispose { gate.cancel() } }
    return gate
}

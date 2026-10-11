package app.metria.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import app.metria.EditorDismissalPolicy

/** Creates a sheet state that keeps changed or saving editors visible on dismissal. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberGuardedEditorSheetState(
    hasChanges: Boolean,
    saving: Boolean,
    onAskToLeave: () -> Unit,
): SheetState {
    val currentHasChanges = rememberUpdatedState(hasChanges)
    val currentSaving = rememberUpdatedState(saving)
    val currentOnAskToLeave = rememberUpdatedState(onAskToLeave)

    return rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            if (targetValue != SheetValue.Hidden) {
                true
            } else {
                when (EditorDismissalPolicy.decision(currentHasChanges.value, currentSaving.value)) {
                    EditorDismissalPolicy.Decision.ALLOW -> true
                    EditorDismissalPolicy.Decision.ASK -> {
                        currentOnAskToLeave.value()
                        false
                    }
                    EditorDismissalPolicy.Decision.BLOCK -> false
                }
            }
        },
    )
}

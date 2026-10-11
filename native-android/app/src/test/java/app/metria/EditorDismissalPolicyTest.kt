package app.metria

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorDismissalPolicyTest {
    @Test
    fun cleanEditorMayDismiss() {
        assertEquals(EditorDismissalPolicy.Decision.ALLOW, EditorDismissalPolicy.decision(hasChanges = false, saving = false))
    }

    @Test
    fun changedEditorAsksBeforeDismissal() {
        assertEquals(EditorDismissalPolicy.Decision.ASK, EditorDismissalPolicy.decision(hasChanges = true, saving = false))
    }

    @Test
    fun savingEditorBlocksDismissalWithOrWithoutChanges() {
        assertEquals(EditorDismissalPolicy.Decision.BLOCK, EditorDismissalPolicy.decision(hasChanges = false, saving = true))
        assertEquals(EditorDismissalPolicy.Decision.BLOCK, EditorDismissalPolicy.decision(hasChanges = true, saving = true))
    }

}

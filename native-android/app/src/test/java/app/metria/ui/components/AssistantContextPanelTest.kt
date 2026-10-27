package app.metria.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantContextPanelTest {
    @Test
    fun `portrait permits a compact panel and explicit expansion`() {
        assertFalse(assistantPanelFullHeight(false, 760f, 1f, false))
        assertTrue(assistantPanelFullHeight(true, 760f, 1f, false))
    }

    @Test
    fun `keyboard and short windows use all available height even when compact was chosen`() {
        assertTrue(assistantPanelFullHeight(false, 760f, 1f, true))
        assertTrue(assistantPanelFullHeight(false, 360f, 1f, false))
    }

    @Test
    fun `enlarged text gets the full available height`() {
        assertTrue(assistantPanelFullHeight(false, 760f, 1.3f, false))
        assertTrue(assistantPanelFullHeight(false, 760f, 2f, false))
    }

    @Test
    fun `closing keyboard restores chosen size without changing the preference`() {
        val chosenExpanded = false
        assertFalse(assistantPanelFullHeight(chosenExpanded, 760f, 1f, false))
        assertTrue(assistantPanelFullHeight(chosenExpanded, 400f, 1f, true))
        assertFalse(assistantPanelFullHeight(chosenExpanded, 760f, 1f, false))
    }
}

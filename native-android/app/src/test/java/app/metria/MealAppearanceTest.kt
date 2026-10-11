package app.metria

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MealAppearanceTest {
    @Test fun `reviewed drink roles and standalone coffee use a drink fallback`() {
        assertTrue(MealAppearance.isDrink(meal(MealEntryItem("Water", null, null, role = "drink"))))
        assertTrue(MealAppearance.isDrink(meal(MealEntryItem(" Café   largo ", null, null))))
    }

    @Test fun `coffee desserts mixed meals and misleading titles stay meals`() {
        assertFalse(MealAppearance.isDrink(meal(MealEntryItem("Coffee cake", null, null))))
        assertFalse(MealAppearance.isDrink(meal(MealEntryItem("Coffee", null, null, role = "drink"), MealEntryItem("Toast", null, null))))
        assertFalse(MealAppearance.isDrink(meal(MealEntryItem("Cake", null, null)).copy(title = "Café largo")))
        assertFalse(MealAppearance.isDrink(meal()))
    }

    private fun meal(vararg items: MealEntryItem) = MealEntry(items = items.toList(), assumptions = emptyList(), confidence = "low", source = "manual_entry")
}

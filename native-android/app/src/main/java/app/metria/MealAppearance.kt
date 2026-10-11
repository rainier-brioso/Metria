package app.metria

import java.text.Normalizer
import java.util.Locale

/** Presentation only: never changes a saved role, nutrition value or meal section. */
object MealAppearance {
    fun isDrink(meal: MealEntry): Boolean {
        if (meal.items.isEmpty()) return false
        if (meal.items.all { it.role == "drink" }) return true
        // Older/manual entries may not have a drink role. Recognize only a
        // standalone coffee name, never a title or a dessert containing coffee.
        val item = meal.items.singleOrNull() ?: return false
        val name = Normalizer.normalize(item.name.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "").trim().replace(Regex("\\s+"), " ")
        return name in setOf("cafe", "cafe largo", "cafe solo", "cafe con leche", "cafe cortado",
            "coffee", "black coffee", "long coffee", "espresso", "americano", "cappuccino", "latte")
    }
}

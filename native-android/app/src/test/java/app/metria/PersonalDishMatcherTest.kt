package app.metria

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalDishMatcherTest {
    @Test
    fun `exact member name is the strongest candidate`() {
        val sanJacobo = dish("San Jacobo", item("chicken", 10), item("ham", 20))
        val rice = dish("Rice bowl", item("rice", 30))

        val matches = PersonalDishMatcher.search(
            PersonalDishQuery("san jacobo", listOf(item("breaded chicken", 10))),
            listOf(rice, sanJacobo),
        )

        assertEquals(sanJacobo.id, matches.first().dish.id)
        assertTrue(PersonalDishMatchReason.NAME in matches.first().reasons)
    }

    @Test
    fun `confirmed catalog identities match across different wording`() {
        val peasAndHam = dish("Guisantes con jamón", item("guisantes", 101), item("jamón", 202))

        val matches = PersonalDishMatcher.search(
            PersonalDishQuery("green side", listOf(item("peas", 101), item("ham bits", 202))),
            listOf(peasAndHam),
        )

        assertEquals(peasAndHam.id, matches.single().dish.id)
        assertTrue(PersonalDishMatchReason.CONFIRMED_FOODS in matches.single().reasons)
    }

    @Test
    fun `aliases are accent insensitive`() {
        val dish = dish("Tortilla de patata", item("egg", 1), aliases = listOf("Tortilla española"))

        val match = PersonalDishMatcher.search(PersonalDishQuery(name = "tortilla espanola"), listOf(dish)).single()

        assertTrue(PersonalDishMatchReason.ALIAS in match.reasons)
    }

    @Test
    fun `disjoint confirmed foods reject a generic name overlap`() {
        val chickenRice = dish("Lunch bowl", item("chicken", 11), item("rice", 12))

        val matches = PersonalDishMatcher.search(
            PersonalDishQuery("Lunch plate", listOf(item("fish", 91), item("potato", 92))),
            listOf(chickenRice),
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `results are bounded to three`() {
        val dishes = (1..6).map { index -> dish("Chicken plate $index", item("chicken", 11), confirmations = index) }

        val matches = PersonalDishMatcher.search(
            PersonalDishQuery("Chicken plate", listOf(item("chicken", 11))),
            dishes,
            limit = 10,
        )

        assertEquals(3, matches.size)
        assertEquals(6, matches.first().dish.confirmationCount)
    }

    @Test
    fun `same encrypted photo is context but still returns bounded candidates`() {
        val dishes = (1..5).map { index ->
            dish("Saved dish $index", item("food $index", index.toLong())).copy(referenceThumbnailBase64 = "same-photo")
        }

        val matches = PersonalDishMatcher.search(
            PersonalDishQuery(referenceThumbnailBase64 = "same-photo"),
            dishes,
        )

        assertEquals(3, matches.size)
        assertTrue(matches.all { PersonalDishMatchReason.SAME_PHOTO_CONTEXT in it.reasons })
    }

    @Test
    fun `different photo does not create a visual-context candidate`() {
        val saved = dish("San Jacobo", item("chicken", 10)).copy(referenceThumbnailBase64 = "old-photo")

        assertTrue(
            PersonalDishMatcher.search(PersonalDishQuery(referenceThumbnailBase64 = "new-photo"), listOf(saved)).isEmpty(),
        )
    }

    @Test
    fun `compound dish presentation separates its identity from components`() {
        val saved = dish(
            "San Jacobo",
            item("Cutlet", 10),
            item("San Jacobo", 11),
            item("Cheese", 12),
            item("Ham", 13),
        )

        assertEquals(
            listOf("Cutlet", "Cheese", "Ham"),
            PersonalDishStructure.componentItems(saved).map(MealEntryItem::name),
        )
        assertEquals(
            listOf("rice"),
            PersonalDishStructure.componentItems(dish("rice", item("rice", 30))).map(MealEntryItem::name),
        )
    }

    private fun dish(
        name: String,
        vararg items: MealEntryItem,
        aliases: List<String> = emptyList(),
        confirmations: Int = 1,
    ) = PersonalDish(
        name = name,
        aliases = aliases,
        items = items.toList(),
        confirmationCount = confirmations,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        lastConfirmedAt = Instant.parse("2026-01-02T00:00:00Z"),
    )

    private fun item(name: String, foodId: Long) = MealEntryItem(
        name = name,
        grams = 100,
        preparation = null,
        catalogFoodId = foodId,
        catalogRelease = "test",
    )
}

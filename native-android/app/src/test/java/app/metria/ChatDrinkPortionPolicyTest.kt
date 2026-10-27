package app.metria

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class ChatDrinkPortionPolicyTest {
    private fun quantity(text: String) =
        (ChatDrinkPortionPolicy.parseReply(text) as ChatDrinkPortionReply.Quantity).value

    private fun coffeeCatalog(): OfflineFoodCatalog {
        val file = sequenceOf(File("src/main/assets/usda-beverages-sr-2018-v1.json"),
            File("app/src/main/assets/usda-beverages-sr-2018-v1.json")).first { it.isFile }
        val source = FoodCatalog.fromJson(file.readText())
        return source.copy(foods = source.foods.filter { it.fdcId == 171890L })
    }

    @Test fun `mL spellings decimal comma and natural corrections share one quantity`() {
        listOf("200 mL", "200mL", "200 milliliters", "200 millilitres",
            "Actually, make that 200 mL", "It was 200 mL.").forEach { text ->
            assertEquals(text, ChatDrinkQuantity(200.0, "ml"), quantity(text))
        }
        assertEquals(ChatDrinkQuantity(150.5, "ml"), quantity("150,5 ml"))
    }

    @Test fun `fractional serving expressions do not assume a cup volume`() {
        listOf("half a cup", "a half cup", "0.5 cups", "1/2 cup",
            "Make that half a cup").forEach { text ->
            assertEquals(text, ChatDrinkQuantity(0.5, "serving", "cup"), quantity(text))
        }
        assertEquals(ChatDrinkQuantity(0.25, "serving", "cup"), quantity("a quarter cup"))
        assertEquals(ChatDrinkQuantity(2.0, "serving", "cup"), quantity("two cups"))
        assertEquals(ChatDrinkQuantity(1.0, "serving", "glass"), quantity("one glass"))
    }

    @Test fun `unrelated meals reads and whole grams stay on their existing paths`() {
        listOf("150 g", "Actually, make that 150 g", "I had rice and 150 g chicken",
            "I had 200 mL of coffee", "Show my meals today", "How many cups did I drink?").forEach { text ->
            assertEquals(text, ChatDrinkPortionReply.Unrelated, ChatDrinkPortionPolicy.parseReply(text))
        }
    }

    @Test fun `negative zero ranges multiple amounts and negation never become a quantity`() {
        listOf("0 ml", "-200 ml", "10001 ml", "1/0 cup", "101 cups", "200 to 300 ml",
            "200 ml and 1 cup", "Not 200 mL", "Do not make that 200 mL", "NaN ml").forEach { text ->
            assertFalse(text, ChatDrinkPortionPolicy.parseReply(text) is ChatDrinkPortionReply.Quantity)
        }
    }

    @Test fun `initial reports require one positive known drink and preserve meal suffix`() {
        assertEquals(ChatDrinkQuantity(200.0, "ml"),
            ChatDrinkPortionPolicy.reportQuantity("I had 200mL of coffee for breakfast"))
        assertEquals(ChatDrinkQuantity(0.5, "serving", "cup"),
            ChatDrinkPortionPolicy.reportQuantity("I had half a cup of coffee"))
        listOf("Did I have 200 mL of coffee?", "I did not have 200 mL of coffee",
            "I had 200 mL of coffee and toast", "I had 200 mL of coffee. Please ask first.",
            "I had 200 mL of rice").forEach { text ->
            assertNull(text, ChatDrinkPortionPolicy.reportQuantity(text))
        }
    }

    @Test fun `packaged coffee resolves exact source mass for mL and half cup`() {
        val catalog = coffeeCatalog()
        val ml = ChatDrinkPortionPolicy.resolve(quantity("200 mL"), "coffee", "", catalog)!!
        assertEquals(171890L, ml.catalogFoodId)
        assertEquals(237.0 / 236.58824, ml.gramsPerUnit, 1e-9)
        assertEquals(200, DrinkPortionPolicy.canonicalGrams(ml))
        val cup = ChatDrinkPortionPolicy.resolve(quantity("half a cup"), "coffee", "", catalog)!!
        assertEquals("1 cup (8 fl oz)", cup.label)
        assertEquals(0.5, cup.amount, 0.0)
        assertEquals(119, DrinkPortionPolicy.canonicalGrams(cup))
        assertEquals(cup, DrinkPortionCodec.decode(DrinkPortionCodec.encode(cup)))
    }

    @Test fun `source cups without volume support servings but cannot supply mL`() {
        val milk = CatalogFood(42L, "Milk, whole", null, "Dairy", emptyMap(),
            listOf(CatalogPortion(249.0, "1 cup")))
        val catalog = OfflineFoodCatalog("fixture", "fixture", listOf(milk))
        assertNull(ChatDrinkPortionPolicy.resolve(quantity("200 mL"), "milk", "", catalog))
        val cup = ChatDrinkPortionPolicy.resolve(quantity("half a cup"), "milk", "", catalog)!!
        assertEquals(125, DrinkPortionPolicy.canonicalGrams(cup))
        assertNull(ChatDrinkPortionPolicy.resolve(quantity("one mug"), "milk", "", catalog))
    }

    @Test fun `different source cup sizes abstain rather than picking the first`() {
        val coffee = coffeeCatalog().foods.single().copy(portions = listOf(
            CatalogPortion(237.0, "1 cup (8 fl oz)", 236.58824),
            CatalogPortion(178.0, "1 cup (6 fl oz)", 177.44118),
        ))
        val catalog = OfflineFoodCatalog("fixture", "fixture", listOf(coffee))
        assertNull(ChatDrinkPortionPolicy.resolve(quantity("one cup"), "coffee", "", catalog))
    }

    @Test fun `ounce qualifiers inside cup labels cannot become standalone ounce servings`() {
        val catalog = coffeeCatalog()
        val portion = ChatDrinkPortionPolicy.resolve(quantity("two fluid ounces"), "coffee", "", catalog)!!
        assertEquals("1 fl oz", portion.label)
        assertEquals(59, DrinkPortionPolicy.canonicalGrams(portion))
    }

    @Test fun `a fractional source serving cannot silently become one whole cup`() {
        val coffee = coffeeCatalog().foods.single().copy(portions = listOf(CatalogPortion(118.5, "0.5 cup")))
        val catalog = OfflineFoodCatalog("fixture", "fixture", listOf(coffee))
        assertNull(ChatDrinkPortionPolicy.resolve(quantity("one cup"), "coffee", "", catalog))
    }
}

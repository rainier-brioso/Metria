package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LabelNutritionParserTest {
    @Test
    fun extractsEnglishNutritionLabelValues() {
        val draft = requireNotNull(
            LabelNutritionParser.extract(
                "Calories 240 kcal\nTotal Fat 8 g\nTotal Carbohydrate 31 g\nProtein 12 g"
            )
        )

        assertEquals(240.0, draft.caloriesKcal!!, 0.0)
        assertEquals(8.0, draft.totalFatGrams!!, 0.0)
        assertEquals(31.0, draft.carbohydrateGrams!!, 0.0)
        assertEquals(12.0, draft.proteinGrams!!, 0.0)
    }

    @Test
    fun extractsSpanishDecimalValues() {
        val draft = requireNotNull(
            LabelNutritionParser.extract(
                "Energia 112 kcal\nGrasas totales 4,5 g\nCarbohidratos 10 g\nProteinas 7,2 g"
            )
        )

        assertEquals(112.0, draft.caloriesKcal!!, 0.0)
        assertEquals(4.5, draft.totalFatGrams!!, 0.0)
        assertEquals(10.0, draft.carbohydrateGrams!!, 0.0)
        assertEquals(7.2, draft.proteinGrams!!, 0.0)
    }

    @Test
    fun returnsNothingForUnrelatedText() {
        assertNull(LabelNutritionParser.extract("Application error at line 42"))
    }
}

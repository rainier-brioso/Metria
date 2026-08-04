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

    @Test
    fun extractsMultiColumnNutritionFactsUsingSpatialRows() {
        val draft = requireNotNull(LabelNutritionParser.extract(
            "Calories Total Fat Total Carb. Protein 110 0.5g 23g 3g",
            listOf(
                OcrTextLine("Calories", 10, 10, 120, 30), OcrTextLine("110", 200, 10, 240, 30),
                OcrTextLine("Total Fat", 10, 45, 130, 65), OcrTextLine("0.5g", 200, 45, 255, 65),
                OcrTextLine("Total Carb.", 10, 80, 145, 100), OcrTextLine("23g", 200, 80, 245, 100),
                OcrTextLine("Protein", 10, 115, 110, 135), OcrTextLine("3g", 200, 115, 230, 135),
            ),
        ))

        assertEquals(110.0, draft.caloriesKcal!!, 0.0)
        assertEquals(0.5, draft.totalFatGrams!!, 0.0)
        assertEquals(23.0, draft.carbohydrateGrams!!, 0.0)
        assertEquals(3.0, draft.proteinGrams!!, 0.0)
    }
}

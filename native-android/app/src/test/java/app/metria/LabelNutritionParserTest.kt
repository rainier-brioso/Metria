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

    @Test
    fun geometryWinsOverMisleadingColumnOrderedTextAndChoosesLeftValueColumn() {
        // Text is intentionally in a different order from the image geometry.
        val draft = requireNotNull(LabelNutritionParser.extract(
            "Serving 2\nCalories 390\nTotal Fat 7 g\nCarbohydrates 68 g\nProtein 9 g",
            listOf(
                OcrTextLine("Protein", 10, 130, 100, 150),
                OcrTextLine("9g", 320, 130, 350, 150), OcrTextLine("3g", 180, 130, 205, 150),
                OcrTextLine("Calories", 10, 10, 105, 30),
                OcrTextLine("390", 320, 10, 355, 30), OcrTextLine("140", 180, 10, 210, 30),
                OcrTextLine("Total Fat", 10, 50, 115, 70),
                OcrTextLine("7g", 320, 50, 345, 70), OcrTextLine("1g", 180, 50, 205, 70),
                OcrTextLine("Total Carbohydrates", 10, 90, 160, 110),
                OcrTextLine("68g", 320, 90, 350, 110), OcrTextLine("24g", 180, 90, 210, 110),
            ),
        ))

        assertEquals(140.0, draft.caloriesKcal!!, 0.0)
        assertEquals(1.0, draft.totalFatGrams!!, 0.0)
        assertEquals(24.0, draft.carbohydrateGrams!!, 0.0)
        assertEquals(3.0, draft.proteinGrams!!, 0.0)
    }

    @Test
    fun missingLeftColumnRowDoesNotFallBackToRightColumnOrFlattenedText() {
        val draft = requireNotNull(LabelNutritionParser.extract(
            "Calories 450\nProtein 10 g",
            listOf(
                OcrTextLine("Calories", 10, 10, 100, 30),
                OcrTextLine("450", 300, 10, 335, 30),
                OcrTextLine("Protein", 10, 60, 100, 80),
                OcrTextLine("10 g", 300, 60, 345, 80),
                OcrTextLine("Total Fat", 10, 110, 100, 130),
                OcrTextLine("1 g", 180, 110, 205, 130), OcrTextLine("4 g", 300, 110, 325, 130),
            ),
        ))

        assertNull(draft.caloriesKcal)
        assertNull(draft.proteinGrams)
        assertEquals(1.0, draft.totalFatGrams!!, 0.0)
    }

    @Test
    fun textOnlyAmbiguousDualColumnRowsAbstain() {
        assertNull(LabelNutritionParser.extract("Calories 120 360\nProtein 4g 12g"))
        assertNull(LabelNutritionParser.extract("Calories 838 kJ"))
    }

    @Test
    fun spatialRowsIgnorePercentagesMisalignedNumbersAndSaturatedFat() {
        val draft = requireNotNull(LabelNutritionParser.extract(
            "Calories 150\nTotal Fat 2 g\nSaturated Fat 8 g\nTotal Carbohydrate 26 g\nProtein 5 g",
            listOf(
                OcrTextLine("Calories", 10, 10, 100, 30), OcrTextLine("150", 180, 10, 210, 30),
                OcrTextLine("6%", 300, 10, 320, 30),
                OcrTextLine("Total Fat", 10, 50, 110, 70), OcrTextLine("2g", 180, 50, 205, 70),
                OcrTextLine("Saturated Fat", 10, 72, 135, 92), OcrTextLine("8g", 180, 72, 205, 92),
                OcrTextLine("Total Carbohydrates", 10, 110, 155, 130), OcrTextLine("26g", 180, 110, 210, 130),
                OcrTextLine("Protein", 10, 150, 100, 170), OcrTextLine("5g", 180, 150, 205, 170),
            ),
        ))

        assertEquals(150.0, draft.caloriesKcal!!, 0.0)
        assertEquals(2.0, draft.totalFatGrams!!, 0.0)
        assertEquals(26.0, draft.carbohydrateGrams!!, 0.0)
        assertEquals(5.0, draft.proteinGrams!!, 0.0)
    }

    @Test
    fun inlineRightColumnElementDoesNotOverrideSharedLeftColumn() {
        val draft = requireNotNull(LabelNutritionParser.extract(
            "Calories 140 390\nTotal Fat 1g 7g\nCarbohydrates 24g 68g\nProtein 12g",
            listOf(
                OcrTextLine("Calories 140 390", 10, 10, 355, 30, listOf(
                    OcrTextElement("Calories", 10, 10, 100, 30), OcrTextElement("140", 180, 10, 210, 30),
                    OcrTextElement("390", 320, 10, 355, 30),
                )),
                OcrTextLine("Total Fat 1g 7g", 10, 50, 345, 70, listOf(
                    OcrTextElement("Total", 10, 50, 60, 70), OcrTextElement("Fat", 65, 50, 100, 70),
                    OcrTextElement("1g", 180, 50, 205, 70), OcrTextElement("7g", 320, 50, 345, 70),
                )),
                OcrTextLine("Carbohydrates 24g 68g", 10, 90, 350, 110, listOf(
                    OcrTextElement("Carbohydrates", 10, 90, 160, 110), OcrTextElement("24g", 180, 90, 210, 110),
                    OcrTextElement("68g", 320, 90, 350, 110),
                )),
                OcrTextLine("Protein 12g", 10, 130, 345, 150, listOf(
                    OcrTextElement("Protein", 10, 130, 100, 150), OcrTextElement("12g", 320, 130, 345, 150),
                )),
            ),
        ))

        assertEquals(140.0, draft.caloriesKcal!!, 0.0)
        assertEquals(1.0, draft.totalFatGrams!!, 0.0)
        assertEquals(24.0, draft.carbohydrateGrams!!, 0.0)
        assertNull(draft.proteinGrams)
    }

    @Test
    fun percentageAloneIsNotCalories() {
        val draft = LabelNutritionParser.extract(
            "Calories 6%\nProtein 4g",
            listOf(
                OcrTextLine("Calories", 10, 10, 100, 30), OcrTextLine("6%", 180, 10, 200, 30),
                OcrTextLine("Protein", 10, 60, 100, 80), OcrTextLine("4g", 180, 60, 200, 80),
            ),
        )

        assertNull(draft?.caloriesKcal)
    }

    @Test
    fun closeNumericLineBelongsOnlyToNearestNutrientRow() {
        val draft = requireNotNull(LabelNutritionParser.extract(
            "Carbohydrates\nProtein",
            listOf(
                OcrTextLine("Carbohydrates", 10, 0, 130, 30),
                OcrTextLine("Protein", 10, 30, 100, 60),
                OcrTextLine("5 g", 180, 37, 205, 47),
            ),
        ))

        assertNull(draft.carbohydrateGrams)
        assertEquals(5.0, draft.proteinGrams!!, 0.0)
    }

    @Test
    fun extractsAccentedSpanishNutritionLabels() {
        val draft = requireNotNull(LabelNutritionParser.extract("Energ\u00eda 130 kcal\nProte\u00ednas 7 g"))

        assertEquals(130.0, draft.caloriesKcal!!, 0.0)
        assertEquals(7.0, draft.proteinGrams!!, 0.0)
    }

    @Test
    fun mergedColumnsWithoutElementPositionsAbstain() {
        assertNull(LabelNutritionParser.extract("", listOf(
            OcrTextLine("Calories 140 390", 0, 0, 300, 30),
            OcrTextLine("Total Fat 1g 3g", 0, 50, 300, 80),
        )))
    }

    @Test
    fun splitEnergyUnitsAndPercentagesDoNotBecomeCalories() {
        val tokens = listOf("Calories", "1064", "kJ", "254", "kcal", "6", "%")
        val line = OcrTextLine(tokens.joinToString(" "), 0, 0, 700, 30,
            tokens.mapIndexed { i, token -> OcrTextElement(token, i * 100, 0, i * 100 + 60, 30) })
        assertEquals(254.0, LabelNutritionParser.extract("", listOf(line))!!.caloriesKcal!!, 0.0)
    }

    @Test
    fun transFatIsNotTotalFat() {
        assertNull(LabelNutritionParser.extract("Trans Fat 2g"))
        assertNull(LabelNutritionParser.extract("", listOf(OcrTextLine("Trans Fat 2g", 0, 0, 150, 30))))
    }

    @Test
    fun caloriesFromFatAreNotTotalEnergy() {
        assertNull(LabelNutritionParser.extract("Calories from Fat 45"))
        assertNull(LabelNutritionParser.extract("", listOf(
            OcrTextLine("Calories from Fat", 0, 0, 150, 30), OcrTextLine("45", 200, 0, 230, 30),
        )))
    }

    @Test
    fun centeredCaloriesAreAssignedToNearestRepeatedMacroColumn() {
        val draft = requireNotNull(LabelNutritionParser.extract("", listOf(
            OcrTextLine("Calories", 10, 0, 100, 24),
            OcrTextLine("140", 230, 0, 260, 24), OcrTextLine("390", 320, 0, 355, 24),
            OcrTextLine("Total Fat", 10, 40, 110, 60),
            OcrTextLine("1g", 180, 40, 198, 60), OcrTextLine("7g", 320, 40, 340, 60),
            OcrTextLine("Total Carbohydrates", 10, 80, 155, 100),
            OcrTextLine("24g", 180, 80, 205, 100), OcrTextLine("68g", 320, 80, 345, 100),
            OcrTextLine("Protein", 10, 120, 100, 140),
            OcrTextLine("3g", 180, 120, 200, 140), OcrTextLine("9g", 320, 120, 340, 140),
        )))

        assertEquals(140.0, draft.caloriesKcal!!, 0.0)
    }

    @Test
    fun exactlyAmbiguousCenteredCaloriesRemainUnknown() {
        val draft = LabelNutritionParser.extract("", listOf(
            OcrTextLine("Calories", 10, 0, 100, 24),
            OcrTextLine("140", 250, 0, 271, 24), OcrTextLine("390", 320, 0, 355, 24),
            OcrTextLine("Total Fat", 10, 40, 110, 60),
            OcrTextLine("1g", 180, 40, 198, 60), OcrTextLine("7g", 320, 40, 340, 60),
            OcrTextLine("Total Carbohydrates", 10, 80, 155, 100),
            OcrTextLine("24g", 180, 80, 205, 100), OcrTextLine("68g", 320, 80, 345, 100),
            OcrTextLine("Protein", 10, 120, 100, 140),
            OcrTextLine("3g", 180, 120, 200, 140), OcrTextLine("9g", 320, 120, 340, 140),
        ))

        assertNull(draft?.caloriesKcal)
    }

    @Test
    fun leftCaloriesRemainAvailableWhenMacrosOnlyHaveRightColumnValues() {
        val draft = requireNotNull(LabelNutritionParser.extract("", listOf(
            OcrTextLine("Calories 140 390", 0, 0, 360, 24, listOf(
                OcrTextElement("Calories", 0, 0, 80, 24),
                OcrTextElement("140", 180, 0, 208, 24), OcrTextElement("390", 320, 0, 355, 24),
            )),
            OcrTextLine("Total Fat 7g", 0, 40, 340, 60, listOf(
                OcrTextElement("Total", 0, 40, 50, 60), OcrTextElement("Fat", 52, 40, 78, 60),
                OcrTextElement("7g", 320, 40, 340, 60),
            )),
            OcrTextLine("Protein 12g", 0, 80, 344, 100, listOf(
                OcrTextElement("Protein", 0, 80, 75, 100), OcrTextElement("12g", 320, 80, 344, 100),
            )),
        )))

        assertEquals(140.0, draft.caloriesKcal!!, 0.0)
        assertNull(draft.totalFatGrams)
        assertNull(draft.proteinGrams)
    }

    @Test
    fun separatedLineCaloriesRemainAvailableWhenMacrosOnlyHaveRightColumnValues() {
        val draft = requireNotNull(LabelNutritionParser.extract("", listOf(
            OcrTextLine("Calories", 0, 0, 80, 24),
            OcrTextLine("140", 180, 0, 208, 24), OcrTextLine("390", 320, 0, 355, 24),
            OcrTextLine("Total Fat", 0, 40, 78, 60), OcrTextLine("7g", 320, 40, 340, 60),
            OcrTextLine("Protein", 0, 80, 75, 100), OcrTextLine("12g", 320, 80, 344, 100),
        )))

        assertEquals(140.0, draft.caloriesKcal!!, 0.0)
        assertNull(draft.totalFatGrams)
        assertNull(draft.proteinGrams)
    }

    @Test
    fun separateNumericLinePreservesBothElementBoxesForCalories() {
        val draft = requireNotNull(LabelNutritionParser.extract("", listOf(
            OcrTextLine("Calories", 0, 0, 80, 24),
            OcrTextLine("140 390", 180, 0, 355, 24, listOf(
                OcrTextElement("140", 180, 0, 208, 24), OcrTextElement("390", 320, 0, 355, 24),
            )),
            OcrTextLine("Total Fat", 0, 40, 78, 60), OcrTextLine("7g", 320, 40, 340, 60),
            OcrTextLine("Protein", 0, 80, 75, 100), OcrTextLine("12g", 320, 80, 344, 100),
        )))

        assertEquals(140.0, draft.caloriesKcal!!, 0.0)
        assertNull(draft.totalFatGrams)
        assertNull(draft.proteinGrams)
    }

    @Test
    fun unpairedRowsWithDistantValuesDoNotFormMixedColumns() {
        val draft = requireNotNull(LabelNutritionParser.extract("", listOf(
            OcrTextLine("Calories", 0, 0, 80, 24), OcrTextLine("140", 180, 0, 208, 24),
            OcrTextLine("Total Fat", 0, 40, 78, 60), OcrTextLine("7g", 320, 40, 340, 60),
            OcrTextLine("Protein", 0, 80, 75, 100), OcrTextLine("12g", 320, 80, 344, 100),
        )))

        assertEquals(140.0, draft.caloriesKcal!!, 0.0)
        assertNull(draft.totalFatGrams)
        assertNull(draft.proteinGrams)
    }

    @Test
    fun closeUnpairedColumnsAbstainAtSmallTextScale() {
        val draft = requireNotNull(LabelNutritionParser.extract("", listOf(
            OcrTextLine("Calories", 0, 0, 60, 16), OcrTextLine("140", 100, 0, 116, 16),
            OcrTextLine("Total Fat", 0, 30, 48, 46), OcrTextLine("7g", 112, 30, 128, 46),
        )))

        assertEquals(140.0, draft.caloriesKcal!!, 0.0)
        assertNull(draft.totalFatGrams)
    }

    @Test
    fun narrowScaledColumnsAreSeparatedUsingSameRowEvidence() {
        val draft = requireNotNull(LabelNutritionParser.extract("", listOf(
            OcrTextLine("Calories 120 360", 0, 0, 180, 16, listOf(
                OcrTextElement("Calories", 0, 0, 44, 16), OcrTextElement("120", 72, 0, 91, 16),
                OcrTextElement("360", 130, 0, 158, 16),
            )),
            OcrTextLine("Total Fat 1g 3g", 0, 30, 165, 46, listOf(
                OcrTextElement("Total", 0, 30, 26, 46), OcrTextElement("Fat", 28, 30, 42, 46),
                OcrTextElement("1g", 74, 30, 88, 46), OcrTextElement("3g", 132, 30, 162, 46),
            )),
        )))

        assertEquals(120.0, draft.caloriesKcal!!, 0.0)
        assertEquals(1.0, draft.totalFatGrams!!, 0.0)
    }
}

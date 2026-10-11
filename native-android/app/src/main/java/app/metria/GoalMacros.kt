package app.metria

import kotlin.math.floor

/** Explicit training context; MANUAL and DEMANDING intentionally have no automatic proposal. */
enum class GoalTraining {
    GENERAL,
    STRENGTH,
    EXERCISE,
    DEMANDING,
    MANUAL,
}

enum class GoalMacroField { PROTEIN, CARBOHYDRATE, FAT }

data class GoalMacroInputs(val age: Int, val weightKg: Double, val training: GoalTraining)

/** Parse explicitly reviewed macro inputs; never reads the latest recorded weight. */
object GoalMacroInput {
    fun parse(age: String, weight: String, weightUnit: String, training: GoalTraining): Result<GoalMacroInputs> = runCatching {
        require(weightUnit == "kg" || weightUnit == "lb") { "Choose kg or lb." }
        val ageValue = age.trim().toIntOrNull() ?: error("Enter your age in whole years.")
        val rawWeight = weight.trim().replace(',', '.').toDoubleOrNull()?.takeIf(Double::isFinite)
            ?: error("Enter a reviewed weight for this calculation.")
        val kilograms = rawWeight * if (weightUnit == "lb") 0.45359237 else 1.0
        require(ageValue in 19..64) { "Macro suggestions support ages 19-64. Enter references manually." }
        require(kilograms.isFinite() && kilograms in 35.0..250.0) { "Enter a reviewed weight between 35 and 250 kg, or use manual references." }
        GoalMacroInputs(ageValue, kilograms, training)
    }

    /**
     * Coordinate a proposal only after the caller freshly confirms suitability and the member
     * explicitly selects an eligible training choice. This path never supplies a calorie default.
     */
    fun propose(
        age: String,
        weight: String,
        unit: String,
        training: GoalTraining?,
        calories: String,
        suitable: Boolean,
    ): Result<MacroSuggestion> = runCatching {
        require(suitable) { "Confirm that this estimate is suitable before requesting a macro suggestion." }
        val selectedTraining = requireNotNull(training) { "Choose a training context or use manual references." }
        val reviewed = parse(age, weight, unit, selectedTraining).getOrThrow()
        val calorieBudget = NutritionTargetInput.parse(listOf(calories, "", "", "")).getOrThrow()
            ?.caloriesKcal ?: error("Enter an explicit daily calorie budget before requesting a macro suggestion.")
        MacroCalculator.calculate(reviewed.age, reviewed.weightKg, calorieBudget, reviewed.training)
    }
}

/** Reproducible source for displayed phase macros. Targets live in GoalPhase.targets. */
data class MacroSuggestion(
    val age: Int,
    val weightKg: Double,
    val caloriesKcal: Double,
    val training: GoalTraining,
    val method: String,
    val roundingVersion: String,
    val rawProteinGrams: Double,
    val rawCarbohydrateGrams: Double,
    val rawFatGrams: Double,
    val proteinGrams: Double,
    val carbohydrateGrams: Double,
    val fatGrams: Double,
    val proteinDerived: Boolean = true,
    val carbohydrateDerived: Boolean = true,
    val fatDerived: Boolean = true,
)

/** Deterministic local macro proposal. These are editable starting references, not requirements. */
object MacroCalculator {
    const val balancedMethod = "macro-balanced-v1"
    const val exerciseMethod = "macro-exercise-v1"
    const val roundingVersion = "half-up-gram-v1"

    fun calculate(age: Int, weightKg: Double, caloriesKcal: Double, training: GoalTraining): MacroSuggestion {
        require(age in 19..64) { "Macro suggestions support ages 19-64. Enter references manually." }
        require(weightKg.isFinite() && weightKg in 35.0..250.0) { "Enter a reviewed weight between 35 and 250 kg, or use manual references." }
        require(caloriesKcal.isFinite() && caloriesKcal in 1.0..10_000.0) { "Enter a positive daily calorie budget, or use manual references." }

        val method: String
        val protein: Double
        val carbohydrate: Double
        val fat = 0.30 * caloriesKcal / 9.0
        when (training) {
            GoalTraining.GENERAL -> {
                method = balancedMethod
                protein = 0.20 * caloriesKcal / 4.0
                carbohydrate = 0.50 * caloriesKcal / 4.0
                require(protein + SHARE_TOLERANCE >= 0.83 * weightKg) {
                    "This calorie budget does not meet the selected balanced split's protein reference. Choose manual references or review a different calorie budget."
                }
            }
            GoalTraining.STRENGTH, GoalTraining.EXERCISE -> {
                method = exerciseMethod
                protein = (if (training == GoalTraining.STRENGTH) 1.6 else 1.4) * weightKg
                carbohydrate = (caloriesKcal - 4.0 * protein - 9.0 * fat) / 4.0
                require(carbohydrate.isFinite() && carbohydrate > 0.0) {
                    "This combination does not leave a positive carbohydrate reference. Choose manual references or review the calorie budget."
                }
                val proteinShare = 4.0 * protein / caloriesKcal * 100.0
                val carbohydrateShare = 4.0 * carbohydrate / caloriesKcal * 100.0
                require(inShareRange(proteinShare, 10.0, 35.0) && inShareRange(carbohydrateShare, 45.0, 60.0)) {
                    "This combination is outside the supported macro split. Choose manual references or review the inputs."
                }
            }
            GoalTraining.DEMANDING, GoalTraining.MANUAL -> throw IllegalArgumentException("Automatic suggestions are not available for this training choice. Enter manual references.")
        }

        val roundedProtein = roundPositiveGrams(protein)
        val roundedCarbohydrate = roundPositiveGrams(carbohydrate)
        val roundedFat = roundPositiveGrams(fat)
        require(roundedProtein in 1.0..1_000.0 && roundedCarbohydrate in 1.0..2_000.0 && roundedFat in 1.0..1_000.0) {
            "This suggestion is outside the supported reference limits. Enter references manually."
        }
        return MacroSuggestion(
            age = age,
            weightKg = weightKg,
            caloriesKcal = caloriesKcal,
            training = training,
            method = method,
            roundingVersion = roundingVersion,
            rawProteinGrams = protein,
            rawCarbohydrateGrams = carbohydrate,
            rawFatGrams = fat,
            proteinGrams = roundedProtein,
            carbohydrateGrams = roundedCarbohydrate,
            fatGrams = roundedFat,
        )
    }

    /** Validate persisted calculation provenance against the canonical calculator and rounding. */
    fun validate(value: MacroSuggestion) {
        require(value.proteinDerived || value.carbohydrateDerived || value.fatDerived) { "Macro source has no derived fields." }
        require(value.roundingVersion == roundingVersion) { "Unsupported macro rounding version." }
        val expected = calculate(value.age, value.weightKg, value.caloriesKcal, value.training)
        require(value.method == expected.method && value.rawProteinGrams == expected.rawProteinGrams &&
            value.rawCarbohydrateGrams == expected.rawCarbohydrateGrams && value.rawFatGrams == expected.rawFatGrams &&
            value.proteinGrams == expected.proteinGrams && value.carbohydrateGrams == expected.carbohydrateGrams &&
            value.fatGrams == expected.fatGrams) { "Invalid macro suggestion source." }
    }

    private fun roundPositiveGrams(value: Double): Double {
        require(value.isFinite() && value > 0.0) { "This combination has no positive macro suggestion. Enter manual references." }
        return floor(value + 0.5)
    }

    private fun inShareRange(value: Double, low: Double, high: Double): Boolean =
        value.isFinite() && value >= low - SHARE_TOLERANCE && value <= high + SHARE_TOLERANCE

    private const val SHARE_TOLERANCE = 1e-9
}

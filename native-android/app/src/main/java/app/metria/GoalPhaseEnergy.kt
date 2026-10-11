package app.metria

import kotlin.math.floor

/** Reproducible source for a phase-adjusted calorie reference. */
data class PhaseEnergySuggestion(
    val estimate: MaintenanceEstimate,
    val maintenanceKcal: Double,
    val intent: GoalIntent,
    val training: GoalTraining?,
    val method: String,
    val roundingVersion: String,
    val rawCaloriesKcal: Double,
    val caloriesKcal: Double,
)

/** Fixed, local phase-energy proposals. The maintenance estimate remains the sole energy basis. */
object PhaseEnergyCalculator {
    const val method = "phase-energy-modest-v1"
    const val roundingVersion = "half-up-kcal-v1"

    fun calculate(
        estimate: MaintenanceEstimate,
        maintenanceKcal: Double,
        intent: GoalIntent,
        training: GoalTraining?,
    ): PhaseEnergySuggestion {
        require(estimate.method == MaintenanceCalculator.method && estimate == MaintenanceCalculator.calculate(estimate.profile)) {
            "Review a supported maintenance estimate first."
        }
        require(maintenanceKcal == estimate.minimumKcal || maintenanceKcal == estimate.maximumKcal) {
            "Choose one of the displayed maintenance estimate endpoints."
        }
        val profile = estimate.profile
        require(profile.activity != DailyActivity.VIGOROUS) {
            "This phase estimate is not available with vigorous whole-day activity. Enter a reference manually."
        }
        require(training != null && training in supportedTraining) {
            "Choose a supported training context or enter a calorie reference manually."
        }
        // Compare cross-products to avoid turning an exact boundary into 24.999999... by division.
        val bmiNumerator = profile.weightKg * 10_000.0
        val heightSquared = profile.heightCm * profile.heightCm
        fun bmiAtLeast(boundary: Double) = bmiNumerator >= boundary * heightSquared
        fun bmiBelow(boundary: Double) = bmiNumerator < boundary * heightSquared
        val raw = when (intent) {
            GoalIntent.LOSE -> {
                require(bmiAtLeast(25.0) && bmiBelow(40.0)) {
                    "This optional phase estimate is outside its reviewed input range. Enter a reference manually."
                }
                val deficit = maintenanceKcal * 0.10
                require(deficit <= 500.0) { "This proposal exceeds the supported calorie adjustment. Enter a reference manually." }
                maintenanceKcal * 0.90
            }
            GoalIntent.GAIN -> {
                require(bmiAtLeast(18.5) && bmiBelow(30.0)) {
                    "This optional phase estimate is outside its reviewed input range. Enter a reference manually."
                }
                require(training == GoalTraining.STRENGTH) {
                    "A Gain phase proposal requires an explicit regular strength-training selection. Enter a reference manually."
                }
                maintenanceKcal * 1.05
            }
            GoalIntent.MAINTAIN, GoalIntent.TRACK -> throw IllegalArgumentException(
                "A phase calorie adjustment is available only for Lose or Gain."
            )
        }
        require(raw.isFinite() && raw >= 1500.0) {
            "This proposal is below the supported calorie boundary. Enter a reference manually."
        }
        val rounded = floor(raw + 0.5)
        require(rounded in 1500.0..10_000.0) {
            "This proposal is outside the supported calorie range. Enter a reference manually."
        }
        return PhaseEnergySuggestion(
            estimate = estimate,
            maintenanceKcal = maintenanceKcal,
            intent = intent,
            training = training,
            method = method,
            roundingVersion = roundingVersion,
            rawCaloriesKcal = raw,
            caloriesKcal = rounded,
        )
    }

    /** Validate persisted provenance by canonical recomputation. */
    fun validate(value: PhaseEnergySuggestion) {
        require(value.method == method && value.roundingVersion == roundingVersion) {
            "Unsupported phase calorie source."
        }
        val expected = calculate(value.estimate, value.maintenanceKcal, value.intent, value.training)
        require(value.rawCaloriesKcal == expected.rawCaloriesKcal && value.caloriesKcal == expected.caloriesKcal) {
            "Invalid phase calorie source."
        }
    }

    private val supportedTraining = setOf(GoalTraining.GENERAL, GoalTraining.EXERCISE, GoalTraining.STRENGTH)
}

/** Fresh-confirmation coordinator for the focused proposal flow. */
object PhaseEnergyInput {
    fun propose(
        estimate: MaintenanceEstimate?,
        maintenanceKcal: Double?,
        intent: GoalIntent,
        training: GoalTraining?,
        suitable: Boolean,
    ): Result<PhaseEnergySuggestion> = runCatching {
        require(suitable) { "Confirm that this estimate is suitable before requesting a phase calorie proposal." }
        PhaseEnergyCalculator.calculate(
            requireNotNull(estimate) { "Review the maintenance estimate first." },
            requireNotNull(maintenanceKcal) { "Choose a maintenance estimate endpoint first." },
            intent,
            training,
        )
    }
}

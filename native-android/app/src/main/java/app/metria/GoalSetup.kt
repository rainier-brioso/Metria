package app.metria

import java.time.Instant
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.floor

enum class GoalIntent(val title: String) { MAINTAIN("Maintain weight"), GAIN("Gain weight"), LOSE("Lose weight"), TRACK("Just track") }
enum class EnergyEquation(val title: String, val coefficient: Double) {
    MALE("Original male equation", 5.0), FEMALE("Original female equation", -161.0),
}
enum class DailyActivity(val title: String, val detail: String, val minimum: Double, val maximum: Double) {
    LIGHT("Light", "Mostly seated work and light leisure activity", 1.40, 1.69),
    ACTIVE("Active", "Regular movement across work and leisure", 1.70, 1.99),
    VIGOROUS("Vigorous", "Sustained demanding activity across the day", 2.00, 2.40),
}

/** Age at calculation, never a date of birth. No eligibility answers are stored. */
data class GoalProfile(val age: Int, val heightCm: Double, val weightKg: Double, val equation: EnergyEquation, val activity: DailyActivity)
data class MaintenanceEstimate(val profile: GoalProfile, val minimumKcal: Double, val maximumKcal: Double, val method: String = MaintenanceCalculator.method)

object MaintenanceCalculator {
    const val method = "maintenance-msj-pal-v1"
    fun calculate(profile: GoalProfile): MaintenanceEstimate {
        require(profile.age in 19..64 && profile.heightCm.isFinite() && profile.heightCm in 120.0..230.0 &&
            profile.weightKg.isFinite() && profile.weightKg in 35.0..250.0) { "Estimates support ages 19–64, heights 120–230 cm and weights 35–250 kg. You can enter daily targets manually." }
        val resting = 10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.age + profile.equation.coefficient
        val low = floor(resting * profile.activity.minimum / 50) * 50
        val high = ceil(resting * profile.activity.maximum / 50) * 50
        require(resting.isFinite() && resting > 0 && low in 1.0..10_000.0 && high in low..10_000.0) { "This estimate is outside the supported range. Enter targets manually." }
        return MaintenanceEstimate(profile, low, high)
    }
    fun profile(age: String, height: String, heightUnit: String, weight: String, weightUnit: String, equation: EnergyEquation?, activity: DailyActivity?): GoalProfile {
        fun number(value: String) = value.trim().replace(',', '.').toDoubleOrNull()?.takeIf(Double::isFinite)
        require(heightUnit in listOf("cm", "in") && weightUnit in listOf("kg", "lb")) { "Choose supported units." }
        return GoalProfile(
            age.trim().toIntOrNull() ?: error("Enter your age in whole years."),
            (number(height) ?: error("Enter your height.")) * if (heightUnit == "in") 2.54 else 1.0,
            (number(weight) ?: error("Enter and review your weight.")) * if (weightUnit == "lb") 0.45359237 else 1.0,
            requireNotNull(equation) { "Choose an equation, or use manual targets." },
            requireNotNull(activity) { "Choose your whole-day activity." },
        ).also { calculate(it) }
    }
}

data class GoalPhase(
    val id: String = UUID.randomUUID().toString(),
    val intent: GoalIntent,
    val confirmedAt: Instant = Instant.now(),
    val updatedAt: Instant = confirmedAt,
    val targets: NutritionTargets?,
    val referenceWeightKg: Double? = null,
    val targetWeightKg: Double? = null,
    val rangeMinimumKg: Double? = null,
    val rangeMaximumKg: Double? = null,
    val acceptedEstimate: MaintenanceEstimate? = null,
    val macroSuggestion: MacroSuggestion? = null,
    val phaseEnergySuggestion: PhaseEnergySuggestion? = null,
    val endedAt: Instant? = null,
)
data class GoalSetup(val current: GoalPhase, val history: List<GoalPhase> = emptyList())
data class GoalSnapshot(val targets: NutritionTargets?, val setup: GoalSetup?)

object GoalSetupInput {
    /** A replacement wizard must not silently relabel a copied estimate as manual. */
    fun initialReferences(targets: NutritionTargets?, previous: GoalPhase?, newPhase: Boolean = false): List<String> =
        listOf(
            targets?.caloriesKcal.takeIf { previous?.acceptedEstimate == null && (!newPhase || previous?.phaseEnergySuggestion == null) },
            targets?.proteinGrams.takeIf { !newPhase || previous?.macroSuggestion?.proteinDerived != true },
            targets?.carbohydrateGrams.takeIf { !newPhase || previous?.macroSuggestion?.carbohydrateDerived != true },
            targets?.fatGrams.takeIf { !newPhase || previous?.macroSuggestion?.fatDerived != true },
        )
            .map { it?.toString().orEmpty() }
}

object GoalPolicy {
    const val historyLimit = 12
    private val minimumPhaseInstant = Instant.parse("0001-01-01T00:00:00Z")
    private val maximumPhaseInstantExclusive = Instant.parse("+10000-01-01T00:00:00Z")
    fun hasProfile(setup: GoalSetup?): Boolean =
        setup?.let { (it.history + it.current).any { phase -> phase.acceptedEstimate != null || phase.macroSuggestion != null || phase.phaseEnergySuggestion != null } } ?: false

    fun validate(phase: GoalPhase) {
        require(runCatching { UUID.fromString(phase.id).toString() == phase.id }.getOrDefault(false)) { "Invalid goal ID." }
        require(listOfNotNull(phase.confirmedAt, phase.updatedAt, phase.endedAt).all { it >= minimumPhaseInstant && it < maximumPhaseInstantExclusive }) { "Goal dates must be within years 1 through 9999." }
        require(phase.updatedAt >= phase.confirmedAt) { "Invalid goal dates." }
        require(phase.endedAt == null || (phase.endedAt >= phase.updatedAt && phase.endedAt >= phase.confirmedAt)) { "Invalid goal dates." }
        require(phase.targets == null || (NutritionTargetValidation.valid(phase.targets) && !NutritionTargetValidation.empty(phase.targets))) { "Invalid goal references." }
        listOf(phase.referenceWeightKg, phase.targetWeightKg, phase.rangeMinimumKg, phase.rangeMaximumKg).forEach {
            require(it == null || (it.isFinite() && it in 15.0..500.0)) { "Enter weights between 15 and 500 kg." }
        }
        require((phase.rangeMinimumKg == null) == (phase.rangeMaximumKg == null)) { "Enter both ends of the weight range, or leave both empty." }
        phase.rangeMinimumKg?.let { low ->
            require(low <= requireNotNull(phase.rangeMaximumKg)) { "Check the weight range order." }
            require(phase.targetWeightKg == null || phase.targetWeightKg in low..phase.rangeMaximumKg) { "The target weight must be within your range." }
        }
        phase.acceptedEstimate?.let {
            require(phase.phaseEnergySuggestion == null) { "Maintenance and phase calorie sources cannot both set current calories." }
            require(it.method == MaintenanceCalculator.method && it == MaintenanceCalculator.calculate(it.profile)) { "Invalid estimate source." }
            require(phase.referenceWeightKg == it.profile.weightKg && requireNotNull(phase.targets?.caloriesKcal) in it.minimumKcal..it.maximumKcal) { "Invalid accepted estimate." }
        }
        phase.phaseEnergySuggestion?.let { source ->
            require(phase.acceptedEstimate == null) { "Maintenance and phase calorie sources cannot both set current calories." }
            PhaseEnergyCalculator.validate(source)
            require(source.intent == phase.intent) { "Phase calorie source does not match the current direction." }
            require(phase.referenceWeightKg == source.estimate.profile.weightKg) { "Phase calorie source must retain its reviewed weight." }
            require(phase.targets?.caloriesKcal == source.caloriesKcal) { "Current calories do not match the phase calorie source." }
        }
        phase.macroSuggestion?.let { source ->
            MacroCalculator.validate(source)
            val targets = requireNotNull(phase.targets) { "Macro provenance requires current references." }
            require(targets.caloriesKcal == source.caloriesKcal) { "Macro references must match their reviewed calorie budget." }
            require(!source.proteinDerived || targets.proteinGrams == source.proteinGrams) { "Derived protein reference does not match its source." }
            require(!source.carbohydrateDerived || targets.carbohydrateGrams == source.carbohydrateGrams) { "Derived carbohydrate reference does not match its source." }
            require(!source.fatDerived || targets.fatGrams == source.fatGrams) { "Derived fat reference does not match its source." }
        }
    }
    fun validate(snapshot: GoalSnapshot) {
        require(snapshot.targets == null || (NutritionTargetValidation.valid(snapshot.targets) && !NutritionTargetValidation.empty(snapshot.targets)))
        snapshot.setup?.let {
            require(it.history.size <= historyLimit && it.current.targets == snapshot.targets) { "Goal references do not match." }
            require(it.current.endedAt == null) { "The current phase cannot have an end date." }
            (it.history + it.current).forEach(::validate)
            require((it.history + it.current).map(GoalPhase::id).distinct().size == it.history.size + 1) { "Duplicate goal phases." }
            val known = it.history.filter { phase -> phase.endedAt != null }
            for (leftIndex in known.indices) for (rightIndex in leftIndex + 1 until known.size) {
                val left = known[leftIndex]
                val right = known[rightIndex]
                require(left.confirmedAt >= requireNotNull(right.endedAt) || right.confirmedAt >= requireNotNull(left.endedAt)) { "Known goal periods cannot overlap." }
            }
            require(known.all { phase -> requireNotNull(phase.endedAt) <= it.current.confirmedAt }) { "A previous phase cannot overlap the current phase." }
        }
    }
    fun confirm(previous: GoalSnapshot, phase: GoalPhase, newPhase: Boolean): GoalSnapshot {
        validate(phase)
        val old = previous.setup
        require(old == null || newPhase || (phase.id == old.current.id && phase.confirmedAt == old.current.confirmedAt)) { "Edit must preserve the current goal." }
        require(old == null || !newPhase || phase.id != old.current.id) { "A new phase needs a new ID." }
        if (old != null && newPhase) require(phase.confirmedAt >= old.current.updatedAt) { "A new phase cannot start before the last saved change." }
        val archived = if (old != null && newPhase) old.current.copy(endedAt = phase.confirmedAt) else null
        val history = if (archived != null) (old!!.history + archived).takeLast(historyLimit) else old?.history.orEmpty()
        return GoalSnapshot(phase.targets, GoalSetup(phase, history)).also(::validate)
    }
    fun manualTargets(
        previous: GoalSnapshot,
        targets: NutritionTargets?,
        calorieWasEdited: Boolean = false,
        manuallyEditedMacros: Set<GoalMacroField> = emptySet(),
    ): GoalSnapshot {
        val current = previous.setup?.current
        val calorieChanged = calorieWasEdited || current?.targets?.caloriesKcal != targets?.caloriesKcal
        val macroSource = current?.macroSuggestion
        val retainedTargets = if (calorieChanged && macroSource != null) targets?.let { proposed ->
            proposed.copy(
                proteinGrams = proposed.proteinGrams.takeUnless { macroSource.proteinDerived && GoalMacroField.PROTEIN !in manuallyEditedMacros },
                carbohydrateGrams = proposed.carbohydrateGrams.takeUnless { macroSource.carbohydrateDerived && GoalMacroField.CARBOHYDRATE !in manuallyEditedMacros },
                fatGrams = proposed.fatGrams.takeUnless { macroSource.fatDerived && GoalMacroField.FAT !in manuallyEditedMacros },
            ).takeUnless(NutritionTargetValidation::empty)
        } else targets
        val updatedSource = if (calorieChanged) null else macroSource?.let { source ->
            val oldTargets = requireNotNull(current).targets
            source.copy(
                proteinDerived = source.proteinDerived && GoalMacroField.PROTEIN !in manuallyEditedMacros && oldTargets?.proteinGrams == retainedTargets?.proteinGrams,
                carbohydrateDerived = source.carbohydrateDerived && GoalMacroField.CARBOHYDRATE !in manuallyEditedMacros && oldTargets?.carbohydrateGrams == retainedTargets?.carbohydrateGrams,
                fatDerived = source.fatDerived && GoalMacroField.FAT !in manuallyEditedMacros && oldTargets?.fatGrams == retainedTargets?.fatGrams,
            ).takeIf { it.proteinDerived || it.carbohydrateDerived || it.fatDerived }
        }
        val setup = if (current == null) null else previous.setup.copy(current = current.copy(
            targets = retainedTargets, updatedAt = maxOf(current.updatedAt, Instant.now()),
            acceptedEstimate = current.acceptedEstimate.takeIf { !calorieWasEdited && current.targets?.caloriesKcal == retainedTargets?.caloriesKcal },
            macroSuggestion = updatedSource,
            phaseEnergySuggestion = current.phaseEnergySuggestion.takeUnless { calorieChanged },
        ))
        return GoalSnapshot(retainedTargets, setup).also(::validate)
    }
    /** All target/profile changes are staged together; even an I/O failure restores the whole snapshot. */
    fun commit(previous: GoalSnapshot, next: GoalSnapshot, apply: (GoalSnapshot) -> Unit, persist: () -> Unit) {
        validate(next)
        apply(next)
        try { persist() } catch (error: Exception) { apply(previous); throw error }
    }
}

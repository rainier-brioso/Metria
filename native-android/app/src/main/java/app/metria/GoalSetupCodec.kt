package app.metria

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Strict versioned encrypted-only representation. Never use this object as a prompt or cloud payload. */
object GoalSetupCodec {
    private fun JSONObject.keysExactly(vararg allowed: String) {
        require(keys().asSequence().toSet() == allowed.toSet()) { "Unsupported goal fields." }
    }
    private fun JSONObject.text(key: String): String = get(key).let { require(it is String); it }
    private fun JSONObject.number(key: String): Double = get(key).let { require(it is Number && it.toDouble().isFinite()); it.toDouble() }
    private fun JSONObject.boolean(key: String): Boolean = get(key).let { require(it is Boolean); it }
    private fun JSONObject.optionalNumber(key: String): Double? = if (isNull(key)) null else number(key)
    private fun nullable(value: Any?) = value ?: JSONObject.NULL
    fun encodeTargets(value: NutritionTargets?) = value?.let {
        JSONObject().put("caloriesKcal", nullable(it.caloriesKcal)).put("proteinGrams", nullable(it.proteinGrams))
            .put("carbohydrateGrams", nullable(it.carbohydrateGrams)).put("fatGrams", nullable(it.fatGrams))
    }
    fun decodeTargets(value: JSONObject): NutritionTargets {
        value.keysExactly("caloriesKcal", "proteinGrams", "carbohydrateGrams", "fatGrams")
        return NutritionTargets(value.optionalNumber("caloriesKcal"), value.optionalNumber("proteinGrams"), value.optionalNumber("carbohydrateGrams"), value.optionalNumber("fatGrams"))
            .also { require(NutritionTargetValidation.valid(it) && !NutritionTargetValidation.empty(it)) }
    }
    private fun encodeMacro(value: MacroSuggestion): JSONObject = JSONObject()
        .put("age", value.age).put("weightKg", value.weightKg).put("caloriesKcal", value.caloriesKcal)
        .put("training", value.training.name).put("method", value.method).put("roundingVersion", value.roundingVersion)
        .put("rawProteinGrams", value.rawProteinGrams).put("rawCarbohydrateGrams", value.rawCarbohydrateGrams).put("rawFatGrams", value.rawFatGrams)
        .put("proteinGrams", value.proteinGrams).put("carbohydrateGrams", value.carbohydrateGrams).put("fatGrams", value.fatGrams)
        .put("proteinDerived", value.proteinDerived).put("carbohydrateDerived", value.carbohydrateDerived).put("fatDerived", value.fatDerived)

    private fun decodeMacro(value: JSONObject): MacroSuggestion {
        value.keysExactly(
            "age", "weightKg", "caloriesKcal", "training", "method", "roundingVersion",
            "rawProteinGrams", "rawCarbohydrateGrams", "rawFatGrams", "proteinGrams", "carbohydrateGrams", "fatGrams",
            "proteinDerived", "carbohydrateDerived", "fatDerived",
        )
        val age = value.number("age").also { require(it == it.toInt().toDouble()) }.toInt()
        return MacroSuggestion(
            age = age,
            weightKg = value.number("weightKg"),
            caloriesKcal = value.number("caloriesKcal"),
            training = GoalTraining.valueOf(value.text("training")),
            method = value.text("method"),
            roundingVersion = value.text("roundingVersion"),
            rawProteinGrams = value.number("rawProteinGrams"),
            rawCarbohydrateGrams = value.number("rawCarbohydrateGrams"),
            rawFatGrams = value.number("rawFatGrams"),
            proteinGrams = value.number("proteinGrams"),
            carbohydrateGrams = value.number("carbohydrateGrams"),
            fatGrams = value.number("fatGrams"),
            proteinDerived = value.boolean("proteinDerived"),
            carbohydrateDerived = value.boolean("carbohydrateDerived"),
            fatDerived = value.boolean("fatDerived"),
        )
    }

    private fun encodeEstimate(value: MaintenanceEstimate): JSONObject = JSONObject()
        .put("method", value.method).put("minimumKcal", value.minimumKcal).put("maximumKcal", value.maximumKcal)
        .put("profile", JSONObject().put("age", value.profile.age).put("heightCm", value.profile.heightCm).put("weightKg", value.profile.weightKg)
            .put("equation", value.profile.equation.name).put("activity", value.profile.activity.name))

    private fun decodeEstimate(encoded: JSONObject): MaintenanceEstimate {
        encoded.keysExactly("method", "minimumKcal", "maximumKcal", "profile")
        val profile = encoded.getJSONObject("profile").let { p ->
            p.keysExactly("age", "heightCm", "weightKg", "equation", "activity")
            val age = p.number("age").also { require(it == it.toInt().toDouble()) }
            GoalProfile(age.toInt(), p.number("heightCm"), p.number("weightKg"), EnergyEquation.valueOf(p.text("equation")), DailyActivity.valueOf(p.text("activity")))
        }
        return MaintenanceEstimate(profile, encoded.number("minimumKcal"), encoded.number("maximumKcal"), encoded.text("method"))
    }

    private fun encodePhaseEnergy(value: PhaseEnergySuggestion): JSONObject = JSONObject()
        .put("estimate", encodeEstimate(value.estimate)).put("maintenanceKcal", value.maintenanceKcal)
        .put("intent", value.intent.name).put("training", nullable(value.training?.name))
        .put("method", value.method).put("roundingVersion", value.roundingVersion)
        .put("rawCaloriesKcal", value.rawCaloriesKcal).put("caloriesKcal", value.caloriesKcal)

    private fun decodePhaseEnergy(value: JSONObject): PhaseEnergySuggestion {
        value.keysExactly("estimate", "maintenanceKcal", "intent", "training", "method", "roundingVersion", "rawCaloriesKcal", "caloriesKcal")
        return PhaseEnergySuggestion(
            estimate = decodeEstimate(value.getJSONObject("estimate")),
            maintenanceKcal = value.number("maintenanceKcal"),
            intent = GoalIntent.valueOf(value.text("intent")),
            training = if (value.isNull("training")) null else GoalTraining.valueOf(value.text("training")),
            method = value.text("method"),
            roundingVersion = value.text("roundingVersion"),
            rawCaloriesKcal = value.number("rawCaloriesKcal"),
            caloriesKcal = value.number("caloriesKcal"),
        )
    }

    private fun encodePhase(value: GoalPhase, representationVersion: Int): JSONObject {
        val result = JSONObject()
        .put("id", value.id).put("intent", value.intent.name).put("confirmedAt", value.confirmedAt.toString()).put("updatedAt", value.updatedAt.toString())
        .put("targets", nullable(encodeTargets(value.targets))).put("referenceWeightKg", nullable(value.referenceWeightKg))
        .put("targetWeightKg", nullable(value.targetWeightKg)).put("rangeMinimumKg", nullable(value.rangeMinimumKg)).put("rangeMaximumKg", nullable(value.rangeMaximumKg))
        .put("acceptedEstimate", nullable(value.acceptedEstimate?.let(::encodeEstimate)))
        if (representationVersion >= 2) result.put("macroSuggestion", nullable(value.macroSuggestion?.let(::encodeMacro)))
        if (representationVersion >= 3) result.put("phaseEnergySuggestion", nullable(value.phaseEnergySuggestion?.let(::encodePhaseEnergy)))
        if (representationVersion >= 4) result.put("endedAt", nullable(value.endedAt?.toString()))
        return result
    }
    private fun decodePhase(value: JSONObject, representationVersion: Int): GoalPhase {
        if (representationVersion == 1) value.keysExactly("id", "intent", "confirmedAt", "updatedAt", "targets", "referenceWeightKg", "targetWeightKg", "rangeMinimumKg", "rangeMaximumKg", "acceptedEstimate")
        else if (representationVersion == 2) value.keysExactly("id", "intent", "confirmedAt", "updatedAt", "targets", "referenceWeightKg", "targetWeightKg", "rangeMinimumKg", "rangeMaximumKg", "acceptedEstimate", "macroSuggestion")
        else if (representationVersion == 3) value.keysExactly("id", "intent", "confirmedAt", "updatedAt", "targets", "referenceWeightKg", "targetWeightKg", "rangeMinimumKg", "rangeMaximumKg", "acceptedEstimate", "macroSuggestion", "phaseEnergySuggestion")
        else value.keysExactly("id", "intent", "confirmedAt", "updatedAt", "targets", "referenceWeightKg", "targetWeightKg", "rangeMinimumKg", "rangeMaximumKg", "acceptedEstimate", "macroSuggestion", "phaseEnergySuggestion", "endedAt")
        val estimate = if (value.isNull("acceptedEstimate")) null else decodeEstimate(value.getJSONObject("acceptedEstimate"))
        return GoalPhase(
            id = value.text("id"), intent = GoalIntent.valueOf(value.text("intent")),
            confirmedAt = Instant.parse(value.text("confirmedAt")), updatedAt = Instant.parse(value.text("updatedAt")),
            targets = if (value.isNull("targets")) null else decodeTargets(value.getJSONObject("targets")),
            referenceWeightKg = value.optionalNumber("referenceWeightKg"), targetWeightKg = value.optionalNumber("targetWeightKg"),
            rangeMinimumKg = value.optionalNumber("rangeMinimumKg"), rangeMaximumKg = value.optionalNumber("rangeMaximumKg"),
            acceptedEstimate = estimate,
            macroSuggestion = if (representationVersion == 1 || value.isNull("macroSuggestion")) null else decodeMacro(value.getJSONObject("macroSuggestion")),
            phaseEnergySuggestion = if (representationVersion < 3 || value.isNull("phaseEnergySuggestion")) null else decodePhaseEnergy(value.getJSONObject("phaseEnergySuggestion")),
            endedAt = if (representationVersion < 4 || value.isNull("endedAt")) null else Instant.parse(value.text("endedAt")),
        )
    }
    fun encode(value: GoalSetup): JSONObject {
        GoalPolicy.validate(GoalSnapshot(value.current.targets, value))
        val phases = value.history + value.current
        val version = when {
            phases.any { it.endedAt != null } -> 4
            phases.any { it.phaseEnergySuggestion != null } -> 3
            phases.any { it.macroSuggestion != null } -> 2
            else -> 1
        }
        return JSONObject().put("version", version).put("current", encodePhase(value.current, version))
            .put("history", JSONArray(value.history.map { encodePhase(it, version) }))
    }
    fun decode(value: JSONObject, targets: NutritionTargets?): GoalSetup {
        value.keysExactly("version", "current", "history")
        val version = value.number("version").also { require(it == it.toInt().toDouble()) }.toInt()
        require(version in 1..4) { "Unsupported goal version." }
        val history = value.getJSONArray("history")
        require(history.length() <= GoalPolicy.historyLimit)
        return GoalSetup(decodePhase(value.getJSONObject("current"), version), List(history.length()) { decodePhase(history.getJSONObject(it), version) })
            .also { GoalPolicy.validate(GoalSnapshot(targets, it)) }
    }
    /** Absence in a legacy archive is absence, never a fabricated profile or phase. */
    fun fromPayload(root: JSONObject, targets: NutritionTargets?): GoalSetup? {
        val version = root.number("version")
        require(version == 1.0 || version == 2.0) { "Unsupported archive data version." }
        if (!root.has("goalSetup") || root.isNull("goalSetup")) return null
        require(version == 2.0) { "Goal data requires archive data version 2." }
        return decode(root.getJSONObject("goalSetup"), targets)
    }
}

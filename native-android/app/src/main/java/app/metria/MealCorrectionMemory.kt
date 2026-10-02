package app.metria

import java.time.Instant

/** Saved, privacy-bounded feedback from one member-reviewed AI meal. */
data class MealCorrectionMemory(
    val mealId: String,
    val recordedAt: Instant,
    val changes: List<MealCorrectionChange>,
)

/** Validation and bounded retention policy for encrypted meal correction memories. */
object MealCorrectionMemoryPolicy {
    const val MAX_RECORDS = 200
    const val MAX_CHANGES_PER_RECORD = 36
    private val safeId = Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,79}")
    private val safeRelease = Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,39}")

    fun valid(memory: MealCorrectionMemory): Boolean =
        validId(memory.mealId) &&
            memory.changes.size in 1..MAX_CHANGES_PER_RECORD &&
            memory.changes.all(::validChange) &&
            memory.changes.map { it.itemId to it.kind }.distinct().size == memory.changes.size

    /** Drops unrelated snapshot fields before retention; only the changed signal is stored. */
    fun normalized(memory: MealCorrectionMemory): MealCorrectionMemory? {
        if (!validSourceChangeList(memory)) return null
        return memory.copy(changes = memory.changes.map { change ->
            when (change.kind) {
                MealCorrectionKind.ADDED -> change.copy(
                    oldName = null, oldGroupId = null, oldGroupName = null, oldPortionGrams = null,
                    oldCatalogFoodId = null, oldCatalogRelease = null,
                )
                MealCorrectionKind.REMOVED -> change.copy(
                    newName = null, newGroupId = null, newGroupName = null, newPortionGrams = null,
                    newCatalogFoodId = null, newCatalogRelease = null,
                )
                MealCorrectionKind.RENAMED -> change.copy(
                    oldGroupId = null, newGroupId = null, oldGroupName = null, newGroupName = null,
                    oldPortionGrams = null, newPortionGrams = null,
                    oldCatalogFoodId = null, newCatalogFoodId = null, oldCatalogRelease = null, newCatalogRelease = null,
                )
                MealCorrectionKind.REGROUPED -> change.copy(
                    oldName = null, newName = null, oldPortionGrams = null, newPortionGrams = null,
                    oldCatalogFoodId = null, newCatalogFoodId = null, oldCatalogRelease = null, newCatalogRelease = null,
                )
                MealCorrectionKind.PORTION_CHANGED -> change.copy(
                    oldName = null, newName = null, oldGroupId = null, newGroupId = null,
                    oldGroupName = null, newGroupName = null,
                    oldCatalogFoodId = null, newCatalogFoodId = null, oldCatalogRelease = null, newCatalogRelease = null,
                )
                MealCorrectionKind.CATALOG_MATCH_CHANGED -> change.copy(
                    oldName = null, newName = null, oldGroupId = null, newGroupId = null,
                    oldGroupName = null, newGroupName = null, oldPortionGrams = null, newPortionGrams = null,
                )
            }
        }).takeIf(::valid)
    }

    /** Replaces a prior record for the same meal and evicts oldest records at capacity. */
    fun withRecord(
        existing: List<MealCorrectionMemory>,
        incoming: MealCorrectionMemory,
    ): List<MealCorrectionMemory> {
        require(valid(incoming))
        require(existing.size <= MAX_RECORDS && existing.all(::valid))
        val retained = existing.filterNot { it.mealId == incoming.mealId }.toMutableList()
        retained.add(incoming)
        return retained.sortedBy(MealCorrectionMemory::recordedAt).takeLast(MAX_RECORDS)
    }

    private fun validChange(change: MealCorrectionChange): Boolean {
        if (!validId(change.itemId)) return false
        if (!validOptionalText(change.oldName, 120) || !validOptionalText(change.newName, 120)) return false
        if (!validOptionalId(change.oldGroupId) || !validOptionalId(change.newGroupId)) return false
        if (!validOptionalText(change.oldGroupName, 120) || !validOptionalText(change.newGroupName, 120)) return false
        if (!validOptionalPortion(change.oldPortionGrams) || !validOptionalPortion(change.newPortionGrams)) return false
        if (!validOptionalFoodId(change.oldCatalogFoodId) || !validOptionalFoodId(change.newCatalogFoodId)) return false
        if (!validOptionalRelease(change.oldCatalogRelease) || !validOptionalRelease(change.newCatalogRelease)) return false
        if ((change.oldCatalogFoodId == null) != (change.oldCatalogRelease == null)) return false
        if ((change.newCatalogFoodId == null) != (change.newCatalogRelease == null)) return false

        val normalized = normalizeChange(change) ?: return false
        return normalized == change
    }

    private fun validSourceChangeList(memory: MealCorrectionMemory): Boolean =
        validId(memory.mealId) && memory.changes.size in 1..MAX_CHANGES_PER_RECORD &&
            memory.changes.all(::validSourceChange) &&
            memory.changes.map { it.itemId to it.kind }.distinct().size == memory.changes.size

    private fun validSourceChange(change: MealCorrectionChange): Boolean {
        if (!validBase(change)) return false
        return when (change.kind) {
            MealCorrectionKind.ADDED -> change.oldName == null && change.newName != null &&
                change.oldGroupId == null && change.newGroupId != null && change.oldPortionGrams == null && change.oldCatalogFoodId == null
            MealCorrectionKind.REMOVED -> change.oldName != null && change.newName == null &&
                change.oldGroupId != null && change.newGroupId == null && change.newPortionGrams == null && change.newCatalogFoodId == null
            MealCorrectionKind.RENAMED -> change.oldName != null && change.newName != null && change.oldName != change.newName
            MealCorrectionKind.REGROUPED -> change.oldGroupId != null && change.newGroupId != null && change.oldGroupId != change.newGroupId
            MealCorrectionKind.PORTION_CHANGED -> change.oldPortionGrams != change.newPortionGrams
            MealCorrectionKind.CATALOG_MATCH_CHANGED ->
                (change.oldCatalogFoodId to change.oldCatalogRelease) != (change.newCatalogFoodId to change.newCatalogRelease)
        }
    }

    private fun normalizeChange(change: MealCorrectionChange): MealCorrectionChange? {
        if (!validBase(change)) return null
        return when (change.kind) {
            MealCorrectionKind.ADDED -> if (change.oldName == null && change.newName != null && change.oldGroupId == null && change.newGroupId != null) change.copy(
                oldGroupName = null, oldPortionGrams = null, oldCatalogFoodId = null, oldCatalogRelease = null,
            ) else null
            MealCorrectionKind.REMOVED -> if (change.oldName != null && change.newName == null && change.oldGroupId != null && change.newGroupId == null) change.copy(
                newGroupName = null, newPortionGrams = null, newCatalogFoodId = null, newCatalogRelease = null,
            ) else null
            MealCorrectionKind.RENAMED -> if (change.oldName != null && change.newName != null && change.oldName != change.newName) change.copy(
                oldGroupId = null, newGroupId = null, oldGroupName = null, newGroupName = null,
                oldPortionGrams = null, newPortionGrams = null,
                oldCatalogFoodId = null, newCatalogFoodId = null, oldCatalogRelease = null, newCatalogRelease = null,
            ) else null
            MealCorrectionKind.REGROUPED -> if (change.oldGroupId != null && change.newGroupId != null && change.oldGroupId != change.newGroupId) change.copy(
                oldName = null, newName = null, oldPortionGrams = null, newPortionGrams = null,
                oldCatalogFoodId = null, newCatalogFoodId = null, oldCatalogRelease = null, newCatalogRelease = null,
            ) else null
            MealCorrectionKind.PORTION_CHANGED -> if (change.oldPortionGrams != change.newPortionGrams) change.copy(
                oldName = null, newName = null, oldGroupId = null, newGroupId = null,
                oldGroupName = null, newGroupName = null,
                oldCatalogFoodId = null, newCatalogFoodId = null, oldCatalogRelease = null, newCatalogRelease = null,
            ) else null
            MealCorrectionKind.CATALOG_MATCH_CHANGED -> if ((change.oldCatalogFoodId to change.oldCatalogRelease) != (change.newCatalogFoodId to change.newCatalogRelease)) change.copy(
                oldName = null, newName = null, oldGroupId = null, newGroupId = null,
                oldGroupName = null, newGroupName = null, oldPortionGrams = null, newPortionGrams = null,
            ) else null
        }
    }

    private fun validBase(change: MealCorrectionChange): Boolean =
        validId(change.itemId) && validOptionalText(change.oldName, 120) && validOptionalText(change.newName, 120) &&
            validOptionalId(change.oldGroupId) && validOptionalId(change.newGroupId) &&
            validOptionalText(change.oldGroupName, 120) && validOptionalText(change.newGroupName, 120) &&
            validOptionalPortion(change.oldPortionGrams) && validOptionalPortion(change.newPortionGrams) &&
            validOptionalFoodId(change.oldCatalogFoodId) && validOptionalFoodId(change.newCatalogFoodId) &&
            validOptionalRelease(change.oldCatalogRelease) && validOptionalRelease(change.newCatalogRelease) &&
            (change.oldCatalogFoodId == null) == (change.oldCatalogRelease == null) &&
            (change.newCatalogFoodId == null) == (change.newCatalogRelease == null)

    private fun validId(value: String): Boolean = safeId.matches(value)
    private fun validOptionalId(value: String?): Boolean = value == null || validId(value)
    private fun validOptionalText(value: String?, maxLength: Int): Boolean =
        value == null || (value == value.trim() && value.length in 1..maxLength && value.none(Character::isISOControl))
    private fun validOptionalPortion(value: Int?): Boolean = value == null || value in 1..2_000
    private fun validOptionalFoodId(value: Long?): Boolean = value == null || value > 0L
    private fun validOptionalRelease(value: String?): Boolean = value == null || safeRelease.matches(value)
}

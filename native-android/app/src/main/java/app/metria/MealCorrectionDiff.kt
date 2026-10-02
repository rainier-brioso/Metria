package app.metria

/** A bounded, in-memory view of one item at the start or end of meal review. */
data class MealReviewItemSnapshot(
    val itemId: String,
    val name: String,
    val groupId: String,
    val groupName: String,
    val portionGrams: Int?,
    val catalogFoodId: Long?,
    val catalogRelease: String?,
)

data class MealReviewSnapshot(val items: List<MealReviewItemSnapshot>)

enum class MealCorrectionKind {
    ADDED,
    REMOVED,
    RENAMED,
    REGROUPED,
    PORTION_CHANGED,
    CATALOG_MATCH_CHANGED,
}

/** Only normalized, bounded values needed to describe a review change are returned. */
data class MealCorrectionChange(
    val kind: MealCorrectionKind,
    val itemId: String,
    val oldName: String? = null,
    val newName: String? = null,
    val oldGroupId: String? = null,
    val newGroupId: String? = null,
    val oldGroupName: String? = null,
    val newGroupName: String? = null,
    val oldPortionGrams: Int? = null,
    val newPortionGrams: Int? = null,
    val oldCatalogFoodId: Long? = null,
    val newCatalogFoodId: Long? = null,
    val oldCatalogRelease: String? = null,
    val newCatalogRelease: String? = null,
)

sealed interface MealCorrectionDiffResult {
    data class Valid(val changes: List<MealCorrectionChange>) : MealCorrectionDiffResult
    data object InvalidSnapshot : MealCorrectionDiffResult
}

/**
 * Diffs validated initial and final review snapshots strictly by stable item ID.
 * This is intentionally computation only: it has no storage or ranking behavior.
 */
object MealCorrectionDiff {
    const val MAX_ITEMS = 12
    private const val MAX_ID_LENGTH = 80
    private const val MAX_NAME_LENGTH = 120
    private const val MAX_GROUP_NAME_LENGTH = 120
    private const val MAX_RELEASE_LENGTH = 40
    private val safeId = Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,79}")
    private val safeRelease = Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,39}")

    fun diff(
        initial: MealReviewSnapshot,
        reviewed: MealReviewSnapshot,
    ): MealCorrectionDiffResult {
        val before = normalize(initial) ?: return MealCorrectionDiffResult.InvalidSnapshot
        val after = normalize(reviewed) ?: return MealCorrectionDiffResult.InvalidSnapshot
        val changes = buildList {
            before.keys.filterNot(after::containsKey).sorted().forEach { id ->
                val item = before.getValue(id)
                add(MealCorrectionChange(
                    kind = MealCorrectionKind.REMOVED,
                    itemId = id,
                    oldName = item.name,
                    oldGroupId = item.groupId,
                    oldGroupName = item.groupName,
                    oldPortionGrams = item.portionGrams,
                    oldCatalogFoodId = item.catalogFoodId,
                    oldCatalogRelease = item.catalogRelease,
                ))
            }
            after.keys.filterNot(before::containsKey).sorted().forEach { id ->
                val item = after.getValue(id)
                add(MealCorrectionChange(
                    kind = MealCorrectionKind.ADDED,
                    itemId = id,
                    newName = item.name,
                    newGroupId = item.groupId,
                    newGroupName = item.groupName,
                    newPortionGrams = item.portionGrams,
                    newCatalogFoodId = item.catalogFoodId,
                    newCatalogRelease = item.catalogRelease,
                ))
            }
            before.keys.intersect(after.keys).sorted().forEach { id ->
                val old = before.getValue(id)
                val new = after.getValue(id)
                if (old.name != new.name) add(change(id, MealCorrectionKind.RENAMED, old, new))
                // The label can be edited without moving the item to another dish.
                if (old.groupId != new.groupId) {
                    add(change(id, MealCorrectionKind.REGROUPED, old, new))
                }
                if (old.portionGrams != new.portionGrams) {
                    add(change(id, MealCorrectionKind.PORTION_CHANGED, old, new))
                }
                if (old.catalogFoodId != new.catalogFoodId || old.catalogRelease != new.catalogRelease) {
                    add(change(id, MealCorrectionKind.CATALOG_MATCH_CHANGED, old, new))
                }
            }
        }
        return MealCorrectionDiffResult.Valid(changes)
    }

    private fun change(
        id: String,
        kind: MealCorrectionKind,
        old: MealReviewItemSnapshot,
        new: MealReviewItemSnapshot,
    ) = MealCorrectionChange(
        kind = kind,
        itemId = id,
        oldName = old.name,
        newName = new.name,
        oldGroupId = old.groupId,
        newGroupId = new.groupId,
        oldGroupName = old.groupName,
        newGroupName = new.groupName,
        oldPortionGrams = old.portionGrams,
        newPortionGrams = new.portionGrams,
        oldCatalogFoodId = old.catalogFoodId,
        newCatalogFoodId = new.catalogFoodId,
        oldCatalogRelease = old.catalogRelease,
        newCatalogRelease = new.catalogRelease,
    )

    private fun normalize(snapshot: MealReviewSnapshot): Map<String, MealReviewItemSnapshot>? {
        if (snapshot.items.size > MAX_ITEMS) return null
        val normalized = LinkedHashMap<String, MealReviewItemSnapshot>()
        for (item in snapshot.items) {
            val itemId = item.itemId.takeIf(::validId) ?: return null
            val groupId = item.groupId.takeIf(::validId) ?: return null
            val name = item.name.trim().takeIf {
                it.isNotEmpty() && it.length <= MAX_NAME_LENGTH && it.none { character -> Character.isISOControl(character) }
            } ?: return null
            val groupName = item.groupName.trim().takeIf {
                it.isNotEmpty() && it.length <= MAX_GROUP_NAME_LENGTH &&
                    it.none { character -> Character.isISOControl(character) }
            } ?: return null
            if (item.portionGrams != null && item.portionGrams !in 1..2_000) return null
            if (item.catalogFoodId != null && item.catalogFoodId <= 0L) return null
            val release = item.catalogRelease?.trim()?.takeIf(String::isNotEmpty)
            if (release != null && (release.length > MAX_RELEASE_LENGTH || !safeRelease.matches(release))) return null
            if ((item.catalogFoodId == null) != (release == null)) return null
            if (normalized.put(itemId, item.copy(
                    itemId = itemId,
                    name = name,
                    groupId = groupId,
                    groupName = groupName,
                    catalogRelease = release,
                )) != null
            ) return null
        }
        return normalized
    }

    private fun validId(value: String): Boolean =
        value.length <= MAX_ID_LENGTH && safeId.matches(value)
}

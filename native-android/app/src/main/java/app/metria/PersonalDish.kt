package app.metria

import java.time.Instant
import java.text.Normalizer
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.ln

/**
 * Metria's member-only food memory. It is encrypted with the vault, never used
 * to train a model, and is returned to the model only through a bounded lookup.
 */
data class PersonalDish(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val aliases: List<String> = emptyList(),
    val items: List<MealEntryItem>,
    val createdAt: Instant = Instant.now(),
    val lastConfirmedAt: Instant = Instant.now(),
    val confirmationCount: Int = 1,
    /** Optional encrypted, reduced reference; the original source image is never retained here. */
    val referenceThumbnailBase64: String? = null,
)

object PersonalDishValidation {
    fun valid(dish: PersonalDish): Boolean =
        runCatching { UUID.fromString(dish.id) }.isSuccess &&
            dish.name.trim().length in 1..120 &&
            dish.aliases.size <= 8 && dish.aliases.all { it.trim().length in 1..80 } &&
            dish.items.isNotEmpty() && dish.items.size <= 12 && dish.items.all(DrinkPortionPolicy::valid) &&
            dish.confirmationCount in 1..10_000 &&
            dish.referenceThumbnailBase64?.length?.let { it in 1..180_000 } != false

    fun fromConfirmedMeal(
        meal: MealEntry,
        name: String,
        existing: PersonalDish? = null,
        confirmedAt: Instant = Instant.now(),
    ): PersonalDish? {
        val dish = PersonalDish(
            id = existing?.id ?: UUID.randomUUID().toString(),
            name = name.trim(),
            aliases = existing?.aliases.orEmpty(),
            items = meal.items,
            createdAt = existing?.createdAt ?: confirmedAt,
            lastConfirmedAt = confirmedAt,
            confirmationCount = existing?.confirmationCount ?: 1,
            referenceThumbnailBase64 = meal.referenceThumbnailBase64,
        )
        return dish.takeIf(::valid)
    }
}

/**
 * Presentation boundary for compound remembered dishes. A repeated item that is exactly the
 * member's dish name is identity metadata, not an ingredient. The encrypted record is preserved
 * so uncertain components are never silently deleted; member review remains authoritative.
 */
object PersonalDishStructure {
    fun componentItems(dish: PersonalDish): List<MealEntryItem> {
        if (dish.items.size <= 1) return dish.items
        val dishKey = PersonalDishMatcher.identityKey(dish.name)
        return dish.items
            .filterNot { item -> PersonalDishMatcher.identityKey(item.name) == dishKey }
            .ifEmpty { dish.items }
    }
}

data class PersonalDishQuery(
    val name: String? = null,
    val items: List<MealEntryItem> = emptyList(),
    /** Whole-meal context only; equality never proves an individual dish identity. */
    val referenceThumbnailBase64: String? = null,
)

enum class PersonalDishMatchReason {
    NAME,
    ALIAS,
    CONFIRMED_FOODS,
    INGREDIENTS,
    PREPARATION,
    REPEATED_CONFIRMATIONS,
    SAME_PHOTO_CONTEXT,
}

data class PersonalDishMatch(
    val dish: PersonalDish,
    val score: Double,
    val reasons: Set<PersonalDishMatchReason>,
)

/**
 * Bounded, deterministic retrieval over member-confirmed structure. This is a
 * candidate generator, not an identity claim: the UI must still ask the member
 * to accept, edit, or ignore every result.
 */
object PersonalDishMatcher {
    private const val MINIMUM_SCORE = 0.50
    private val ignoredWords = setOf(
        "a", "an", "and", "con", "de", "del", "el", "la", "las", "los", "of", "the", "with", "y",
    )
    private val canonicalWords = mapOf(
        "arroz" to "rice", "aguacate" to "avocado", "guisante" to "pea", "guisantes" to "pea",
        "huevo" to "egg", "huevos" to "egg", "jamon" to "ham", "patata" to "potato",
        "patatas" to "potato", "pescado" to "fish", "pollo" to "chicken",
    )

    fun search(query: PersonalDishQuery, dishes: List<PersonalDish>, limit: Int = 3): List<PersonalDishMatch> {
        if (query.name.isNullOrBlank() && query.items.isEmpty() && query.referenceThumbnailBase64.isNullOrBlank()) return emptyList()
        val safeLimit = limit.coerceIn(1, 3)
        val queryName = words(query.name.orEmpty())
        val queryIngredients = ingredientWords(query.items)
        val queryFoodIds = query.items.mapNotNullTo(linkedSetOf(), MealEntryItem::catalogFoodId)
        val queryPreparation = query.items.flatMapTo(linkedSetOf()) { words(it.preparation.orEmpty()) }
        val queryReference = query.referenceThumbnailBase64?.takeIf(String::isNotBlank)?.let(::fingerprint)

        return dishes.asSequence()
            .filter(PersonalDishValidation::valid)
            .mapNotNull { dish ->
                val dishName = words(dish.name)
                val aliasNames = dish.aliases.map(::words)
                val exactName = queryName.isNotEmpty() && queryName == dishName
                val exactAlias = queryName.isNotEmpty() && aliasNames.any { it == queryName }
                val nameSimilarity = maxOf(
                    jaccard(queryName, dishName),
                    aliasNames.maxOfOrNull { jaccard(queryName, it) } ?: 0.0,
                )
                val dishIngredients = ingredientWords(dish.items)
                val ingredientSimilarity = jaccard(queryIngredients, dishIngredients)
                val dishFoodIds = dish.items.mapNotNullTo(linkedSetOf(), MealEntryItem::catalogFoodId)
                val foodSimilarity = jaccard(queryFoodIds, dishFoodIds)
                val dishPreparation = dish.items.flatMapTo(linkedSetOf()) { words(it.preparation.orEmpty()) }
                val preparationSimilarity = jaccard(queryPreparation, dishPreparation)
                val samePhotoContext = queryReference != null && dish.referenceThumbnailBase64
                    ?.takeIf(String::isNotBlank)?.let(::fingerprint) == queryReference
                val reasons = linkedSetOf<PersonalDishMatchReason>()
                var score = 0.0

                when {
                    exactName -> { score += 0.70; reasons += PersonalDishMatchReason.NAME }
                    exactAlias -> { score += 0.70; reasons += PersonalDishMatchReason.ALIAS }
                    nameSimilarity > 0.0 -> { score += 0.35 * nameSimilarity; reasons += PersonalDishMatchReason.NAME }
                }
                if (foodSimilarity > 0.0) {
                    score += 0.75 * foodSimilarity
                    reasons += PersonalDishMatchReason.CONFIRMED_FOODS
                }
                if (ingredientSimilarity > 0.0) {
                    score += 0.45 * ingredientSimilarity
                    reasons += PersonalDishMatchReason.INGREDIENTS
                }
                if (preparationSimilarity > 0.0) {
                    score += 0.10 * preparationSimilarity
                    reasons += PersonalDishMatchReason.PREPARATION
                }
                if (dish.confirmationCount > 1) {
                    score += (ln(dish.confirmationCount.toDouble()) / ln(10.0) * 0.05).coerceAtMost(0.10)
                    reasons += PersonalDishMatchReason.REPEATED_CONFIRMATIONS
                }
                if (samePhotoContext) {
                    score += 0.60
                    reasons += PersonalDishMatchReason.SAME_PHOTO_CONTEXT
                }

                // Confirmed but disjoint catalog identities are strong evidence
                // against a match, even when generic words happen to overlap.
                if (queryFoodIds.isNotEmpty() && dishFoodIds.isNotEmpty() && queryFoodIds.intersect(dishFoodIds).isEmpty()) {
                    score -= 0.55
                }
                if (!exactName && !exactAlias && queryIngredients.isNotEmpty() &&
                    dishIngredients.isNotEmpty() && ingredientSimilarity == 0.0) {
                    score -= 0.25
                }

                score.takeIf { it >= MINIMUM_SCORE }?.let { PersonalDishMatch(dish, it, reasons) }
            }
            .sortedWith(
                compareByDescending<PersonalDishMatch> { it.score }
                    .thenByDescending { it.dish.confirmationCount }
                    .thenByDescending { it.dish.lastConfirmedAt },
            )
            .take(safeLimit)
            .toList()
    }

    internal fun identityKey(value: String): String = words(value).sorted().joinToString(" ")

    private fun fingerprint(encoded: String): String = MessageDigest.getInstance("SHA-256")
        .digest(encoded.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun ingredientWords(items: List<MealEntryItem>): Set<String> =
        items.flatMapTo(linkedSetOf()) { item -> words("${item.name} ${item.description.orEmpty()}") }

    private fun words(value: String): Set<String> = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .asSequence()
        .filter { it.length > 1 && it !in ignoredWords }
        .map { canonicalWords[it] ?: it }
        .toCollection(linkedSetOf())

    private fun <T> jaccard(left: Set<T>, right: Set<T>): Double {
        if (left.isEmpty() || right.isEmpty()) return 0.0
        return left.intersect(right).size.toDouble() / left.union(right).size.toDouble()
    }
}

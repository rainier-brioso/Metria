package app.metria

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** A restored panel may show only the result of its own explicitly started request. */
object AssistantCaptureResultPolicy {
    fun meal(requestId: String?, status: ModelStatus): MealProposal? = status.proposal.takeIf {
        requestId != null && status.imageState == "none" && status.imageAnalysisRequestId == requestId &&
            status.latestImageAudit?.id == requestId && status.latestImageAudit?.outcome in setOf("draft_ready", "draft_partial")
    }
}

/** Same reviewed-label record contract as the existing label editor; no inferred values. */
object AssistantReviewedLabelPolicy {
    fun meal(
        name: String,
        date: String,
        time: LocalTime,
        rawValues: List<String>,
        thumbnail: String?,
        mealSlot: String? = null,
    ): MealEntry? {
        if (mealSlot != null && mealSlot !in MealSlots.allowed) return null
        if (rawValues.size != 4) return null
        val values = rawValues.map { it.replace(',', '.').toDoubleOrNull() }
        if (rawValues.zip(values).any { (raw, value) -> raw.isNotBlank() && value == null }) return null
        val instant = runCatching { LocalDate.parse(date).atTime(time).atZone(ZoneId.systemDefault()).toInstant() }.getOrNull() ?: return null
        return MealEntry(
            recordedAt = instant,
            mealSlot = mealSlot,
            items = listOf(MealEntryItem(name.trim(), null, null)),
            assumptions = listOf("Nutrition values were reviewed from a package label."),
            confidence = "high",
            source = "manual_entry",
            nutrition = MealNutrition(values[0], values[1], values[2], values[3], "verified_label"),
            referenceThumbnailBase64 = thumbnail,
        ).takeIf(MealEntryValidation::valid)
    }
}

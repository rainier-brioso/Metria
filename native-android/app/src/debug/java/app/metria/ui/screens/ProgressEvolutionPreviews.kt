package app.metria.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import app.metria.WeightEntry
import app.metria.ui.theme.MetriaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val previewEnd = LocalDate.parse("2026-10-27")

private fun previewWeights(): List<ProgressDay> {
    val dates = listOf("2026-08-02", "2026-08-09", "2026-08-16", "2026-08-23", "2026-08-30", "2026-09-06", "2026-09-13", "2026-09-20", "2026-09-27", "2026-10-04", "2026-10-11", "2026-10-18", "2026-10-25")
    val weights = dates.mapIndexed { index, dateText ->
        val date = LocalDate.parse(dateText)
        val first = WeightEntry(kilograms = 78.4, recordedAt = Instant.parse("2026-08-02T07:00:00Z"))
        val entry = WeightEntry(kilograms = 78.4 - index * 0.15, recordedAt = date.atTime(8, 0).toInstant(ZoneOffset.UTC))
        val extra = if (index == 10) listOf(WeightEntry(kilograms = 76.7, recordedAt = date.atTime(19, 0).toInstant(ZoneOffset.UTC))) else emptyList()
        val all = if (index == 0) listOf(first) else listOf(entry) + extra
        date to all
    }.toMap()
    return ProgressRange.Quarter.let { range ->
        (0L..java.time.temporal.ChronoUnit.DAYS.between(range.start(previewEnd), previewEnd)).map { offset ->
            val date = range.start(previewEnd).plusDays(offset)
            val records = weights[date].orEmpty()
            ProgressDay(date, records, emptyList(), records.maxByOrNull(WeightEntry::recordedAt)?.kilograms, null, null, false)
        }
    }
}

@Composable
private fun ProgressOverviewFixture(empty: Boolean = false) {
    val days = if (empty) emptyList() else previewWeights()
    PreviewHost {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
            MetriaProgressScreen(
                entries = days.flatMap(ProgressDay::weightEntries),
                meals = emptyList(),
                unit = "kg",
                nutritionTargets = null,
                onEditWeight = {},
                onEditMeal = {},
                onEditNutritionTargets = {},
                goalPhase = null,
                endDate = previewEnd,
            )
        }
    }
}

@Preview(name = "Progress · full overview · fixed date", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ProgressOverviewPreview() = ProgressOverviewFixture()

@Preview(name = "Progress · full overview · empty", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ProgressOverviewEmptyPreview() = ProgressOverviewFixture(empty = true)

@Preview(name = "Progress · overview · narrow large text", showBackground = true, widthDp = 320, heightDp = 880)
@Composable
private fun ProgressOverviewLargeTextPreview() {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    androidx.compose.runtime.CompositionLocalProvider(
        LocalDensity provides Density(density.density, fontScale = 1.5f),
        LocalLayoutDirection provides layoutDirection,
    ) {
        ProgressOverviewFixture()
    }
}

private fun previewNutrition(partialOnly: Boolean = false): List<ProgressDay> =
    (0L..java.time.temporal.ChronoUnit.DAYS.between(ProgressRange.Year.start(previewEnd), previewEnd)).map { offset ->
        val date = ProgressRange.Year.start(previewEnd).plusDays(offset)
        val recorded = date.dayOfMonth % 5 == 0 || date == previewEnd
        val value = if (recorded) 1600.0 + (date.dayOfMonth * 17.0) else null
        ProgressDay(
            date = date,
            weightEntries = emptyList(),
            meals = emptyList(),
            latestWeightKilograms = null,
            caloriesKcal = value,
            proteinGrams = if (value != null) 76.0 else null,
            nutritionIsPartial = value != null && (partialOnly || date.dayOfMonth % 2 == 0),
        )
    }

@Preview(name = "Progress · compact · sparse weight", showBackground = true, widthDp = 393, heightDp = 520)
@Composable
private fun ProgressEvolutionCompactPreview() = MetriaTheme {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        ProgressEvolutionContent(ProgressMetric.Weight, ProgressRange.Quarter, previewWeights(), selectedDate = LocalDate.parse("2026-10-18"))
    }
}

@Preview(name = "Progress · detail · year nutrition", showBackground = true, widthDp = 393, heightDp = 780)
@Composable
private fun ProgressEvolutionDetailPreview() = MetriaTheme {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        ProgressEvolutionContent(ProgressMetric.Calories, ProgressRange.Year, previewNutrition(), selectedDate = previewEnd, detail = true)
    }
}

@Preview(name = "Progress · empty", showBackground = true, widthDp = 393, heightDp = 520)
@Composable
private fun ProgressEvolutionEmptyPreview() = MetriaTheme {
    val emptyDays = ProgressTimeline.range(emptyList(), emptyList(), ProgressRange.Quarter, previewEnd, ZoneOffset.UTC)
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        ProgressEvolutionContent(ProgressMetric.Weight, ProgressRange.Quarter, emptyDays, selectedDate = previewEnd)
    }
}

@Preview(name = "Progress · 150% text · narrow", showBackground = true, widthDp = 320, heightDp = 620)
@Composable
private fun ProgressEvolutionLargeTextPreview() {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    androidx.compose.runtime.CompositionLocalProvider(
        LocalDensity provides Density(density.density, fontScale = 1.5f),
        LocalLayoutDirection provides layoutDirection,
    ) {
        MetriaTheme {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                ProgressEvolutionContent(ProgressMetric.Weight, ProgressRange.Quarter, previewWeights(), selectedDate = LocalDate.parse("2026-10-25"))
            }
        }
    }
}

@Preview(name = "Progress · one reading", showBackground = true, widthDp = 393, heightDp = 520)
@Composable
private fun ProgressEvolutionOnePointPreview() = MetriaTheme {
    val date = LocalDate.parse("2026-10-18")
    val entry = WeightEntry(kilograms = 75.8, recordedAt = date.atTime(8, 0).toInstant(ZoneOffset.UTC))
    val days = ProgressTimeline.range(listOf(entry), emptyList(), ProgressRange.Quarter, previewEnd, ZoneOffset.UTC)
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        ProgressEvolutionContent(ProgressMetric.Weight, ProgressRange.Quarter, days, selectedDate = date)
    }
}

@Composable
private fun PreviewHost(content: @Composable () -> Unit) {
    val owner = remember {
        object : OnBackPressedDispatcherOwner {
            override val lifecycle = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
            override val onBackPressedDispatcher = OnBackPressedDispatcher()
        }
    }
    CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides owner) { MetriaTheme(content) }
}

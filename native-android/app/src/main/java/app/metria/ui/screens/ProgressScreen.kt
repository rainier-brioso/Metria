package app.metria.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameNanos
import app.metria.MealEntry
import app.metria.GoalPhase
import app.metria.MealIdentity
import app.metria.MealSlots
import app.metria.NutritionTargets
import app.metria.WeightEntry
import app.metria.WeightUnits
import app.metria.ui.components.MetriaEstimateBadge
import app.metria.ui.components.MetriaMealThumbnail
import app.metria.ui.components.ProgressGoalCard
import app.metria.ui.theme.MetriaColors
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

/** Local, read-only progress views over member-recorded weight and meal data. */
enum class ProgressMetric(val label: String, val unit: String) {
    Weight("Weight", "kg"),
    Calories("Calories", "kcal"),
    Protein("Protein", "g"),
}

data class ProgressDay(
    val date: LocalDate,
    val weightEntries: List<WeightEntry>,
    val meals: List<MealEntry>,
    val latestWeightKilograms: Double?,
    val caloriesKcal: Double?,
    val proteinGrams: Double?,
    val nutritionIsPartial: Boolean,
)

data class WeightHistoryGroup(val date: LocalDate, val entries: List<WeightEntry>)
data class MealHistoryGroup(val date: LocalDate, val meals: List<MealEntry>)

enum class ProgressRange(val label: String) {
    Week("7 days"), Month("1 month"), Quarter("3 months"), Year("1 year");

    fun start(endDate: LocalDate): LocalDate = when (this) {
        Week -> endDate.minusDays(6)
        Month -> endDate.minusMonths(1).plusDays(1)
        Quarter -> endDate.minusMonths(3).plusDays(1)
        Year -> endDate.minusYears(1).plusDays(1)
    }

    companion object {
        fun defaultFor(metric: ProgressMetric): ProgressRange =
            if (metric == ProgressMetric.Weight) Quarter else Week
    }
}

object ProgressHistory {
    fun weightGroups(entries: List<WeightEntry>, zoneId: ZoneId = ZoneId.systemDefault()): List<WeightHistoryGroup> =
        entries.groupBy { it.recordedAt.atZone(zoneId).toLocalDate() }
            .entries.sortedByDescending(Map.Entry<LocalDate, List<WeightEntry>>::key)
            .map { (date, values) -> WeightHistoryGroup(date, values.sortedByDescending(WeightEntry::recordedAt)) }

    fun mealGroups(meals: List<MealEntry>, zoneId: ZoneId = ZoneId.systemDefault()): List<MealHistoryGroup> =
        meals.groupBy { it.recordedAt.atZone(zoneId).toLocalDate() }
            .entries.sortedByDescending(Map.Entry<LocalDate, List<MealEntry>>::key)
            .map { (date, values) -> MealHistoryGroup(date, values.sortedByDescending(MealEntry::recordedAt)) }
}

object ProgressTimeline {
    fun range(
        entries: List<WeightEntry>,
        meals: List<MealEntry>,
        range: ProgressRange,
        endDate: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<ProgressDay> {
        val weightByDate = entries.groupBy { it.recordedAt.atZone(zoneId).toLocalDate() }
            .mapValues { (_, dayWeights) -> dayWeights.sortedByDescending(WeightEntry::recordedAt) }
        val mealsByDate = meals.groupBy { it.recordedAt.atZone(zoneId).toLocalDate() }
            .mapValues { (_, dayMeals) -> dayMeals.sortedByDescending(MealEntry::recordedAt) }
        val startDate = range.start(endDate)
        return (0L..ChronoUnit.DAYS.between(startDate, endDate)).map { offset ->
            val date = startDate.plusDays(offset)
            buildDay(date, weightByDate[date].orEmpty(), mealsByDate[date].orEmpty())
        }
    }

    fun sevenDays(
        entries: List<WeightEntry>,
        meals: List<MealEntry>,
        endDate: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<ProgressDay> = range(entries, meals, ProgressRange.Week, endDate, zoneId)

    private fun buildDay(date: LocalDate, dayWeights: List<WeightEntry>, dayMeals: List<MealEntry>): ProgressDay {
        val completeNutrition = dayMeals.mapNotNull(MealEntry::nutrition).filter {
            it.estimateCoverage == "full" && it.caloriesKcal != null && it.proteinGrams != null &&
                it.carbohydrateGrams != null && it.fatGrams != null
        }
        val partialNutrition = dayMeals.mapNotNull(MealEntry::nutrition).filter {
            it.estimateCoverage == "partial" && it.caloriesKcal != null && it.proteinGrams != null &&
                it.carbohydrateGrams != null && it.fatGrams != null
        }
        val includedNutritionCount = completeNutrition.size + partialNutrition.size
        val completeCalories = completeNutrition.takeIf { it.isNotEmpty() }?.sumOf { it.caloriesKcal!! }
        val partialCalories = partialNutrition.takeIf { it.isNotEmpty() }?.sumOf { it.caloriesKcal!! }
        val completeProtein = completeNutrition.takeIf { it.isNotEmpty() }?.sumOf { it.proteinGrams!! }
        val partialProtein = partialNutrition.takeIf { it.isNotEmpty() }?.sumOf { it.proteinGrams!! }
        return ProgressDay(
            date = date,
            weightEntries = dayWeights,
            meals = dayMeals,
            latestWeightKilograms = dayWeights.firstOrNull()?.kilograms,
            caloriesKcal = sumOrNull(completeCalories, partialCalories),
            proteinGrams = sumOrNull(completeProtein, partialProtein),
            nutritionIsPartial = (dayMeals.isNotEmpty() && includedNutritionCount < dayMeals.size) ||
                partialNutrition.isNotEmpty(),
        )
    }

    private fun sumOrNull(complete: Double?, partial: Double?): Double? =
        if (complete == null && partial == null) null else (complete ?: 0.0) + (partial ?: 0.0)
}

@Composable
fun MetriaProgressScreen(
    entries: List<WeightEntry>,
    meals: List<MealEntry>,
    unit: String,
    nutritionTargets: NutritionTargets?,
    onEditWeight: (WeightEntry) -> Unit,
    onEditMeal: (MealEntry) -> Unit,
    onEditNutritionTargets: () -> Unit,
    goalPhase: GoalPhase?,
    endDate: LocalDate = LocalDate.now(),
) {
    var metricName by rememberSaveable { mutableStateOf(ProgressMetric.Weight.name) }
    val metric = ProgressMetric.valueOf(metricName)
    var weightRangeName by rememberSaveable { mutableStateOf(ProgressRange.Quarter.name) }
    var caloriesRangeName by rememberSaveable { mutableStateOf(ProgressRange.Week.name) }
    var proteinRangeName by rememberSaveable { mutableStateOf(ProgressRange.Week.name) }
    var detailOpen by rememberSaveable { mutableStateOf(false) }
    var detailPage by rememberSaveable { mutableStateOf(0) }
    val rangeName = when (metric) {
        ProgressMetric.Weight -> weightRangeName
        ProgressMetric.Calories -> caloriesRangeName
        ProgressMetric.Protein -> proteinRangeName
    }
    val range = runCatching { ProgressRange.valueOf(rangeName) }.getOrDefault(ProgressRange.defaultFor(metric))
    val days = remember(entries, meals, range, endDate) { ProgressTimeline.range(entries, meals, range, endDate) }
    var weightSelectedDateText by rememberSaveable { mutableStateOf(lastDateWithValue(ProgressTimeline.range(entries, meals, ProgressRange.Quarter, endDate), ProgressMetric.Weight).toString()) }
    var caloriesSelectedDateText by rememberSaveable { mutableStateOf(lastDateWithValue(ProgressTimeline.range(entries, meals, ProgressRange.Week, endDate), ProgressMetric.Calories).toString()) }
    var proteinSelectedDateText by rememberSaveable { mutableStateOf(lastDateWithValue(ProgressTimeline.range(entries, meals, ProgressRange.Week, endDate), ProgressMetric.Protein).toString()) }
    val selectedDateText = when (metric) {
        ProgressMetric.Weight -> weightSelectedDateText
        ProgressMetric.Calories -> caloriesSelectedDateText
        ProgressMetric.Protein -> proteinSelectedDateText
    }
    fun selectDate(date: LocalDate, forMetric: ProgressMetric) {
        when (forMetric) {
            ProgressMetric.Weight -> weightSelectedDateText = date.toString()
            ProgressMetric.Calories -> caloriesSelectedDateText = date.toString()
            ProgressMetric.Protein -> proteinSelectedDateText = date.toString()
        }
    }
    var historyOpen by rememberSaveable { mutableStateOf(false) }
    var historyTab by rememberSaveable { mutableStateOf("Weight") }
    val selectedDate = runCatching { LocalDate.parse(selectedDateText) }.getOrDefault(days.last().date)
    val selectedDay = days.firstOrNull { it.date == selectedDate } ?: days.last()

    BackHandler(enabled = historyOpen || detailOpen) {
        if (historyOpen) historyOpen = false else detailOpen = false
    }
    if (historyOpen) {
        AllHistory(
            entries = entries,
            meals = meals,
            unit = unit,
            selectedTab = historyTab,
            onSelectedTab = { historyTab = it },
            onBack = { historyOpen = false },
            onEditWeight = onEditWeight,
            onEditMeal = onEditMeal,
        )
    } else if (detailOpen) {
        EvolutionDetail(
            metric = metric, range = range, days = days, unit = unit,
            selectedDate = selectedDay.date, page = detailPage,
            onPageChange = { detailPage = it }, onBack = { detailOpen = false },
            onSelectDate = { selectDate(it, metric) },
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProgressHeader {
                historyTab = if (metric == ProgressMetric.Weight) "Weight" else "Meals"
                historyOpen = true
            }
            MetricSelector(metric) { next ->
                metricName = next.name
                val nextRangeName = when (next) {
                    ProgressMetric.Weight -> weightRangeName
                    ProgressMetric.Calories -> caloriesRangeName
                    ProgressMetric.Protein -> proteinRangeName
                }
                val nextRange = runCatching { ProgressRange.valueOf(nextRangeName) }.getOrDefault(ProgressRange.defaultFor(next))
                val nextDays = ProgressTimeline.range(entries, meals, nextRange, endDate)
                selectDate(lastDateWithValue(nextDays, next), next)
            }
            RangeSelector(range) { next ->
                when (metric) {
                    ProgressMetric.Weight -> weightRangeName = next.name
                    ProgressMetric.Calories -> caloriesRangeName = next.name
                    ProgressMetric.Protein -> proteinRangeName = next.name
                }
                selectDate(lastDateWithValue(ProgressTimeline.range(entries, meals, next, endDate), metric), metric)
            }
            ProgressHero(metric, selectedDay, days, range, unit, nutritionTargets)
            EvolutionChart(metric, range, days, selectedDay.date, unit, compact = true, onSelectDate = { selectDate(it, metric) })
            TextButton(onClick = { detailPage = 0; detailOpen = true }, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Rounded.Timeline, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text("View detail")
            }
            ProgressGoalCard(goalPhase, nutritionTargets, unit, onEditNutritionTargets)
            DayDetails(metric, selectedDay, unit, onEditWeight, onEditMeal)
        }
    }
}

@Composable
private fun ProgressHeader(onOpenHistory: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Progress", style = MaterialTheme.typography.displaySmall)
            Text(
                "Explore your recorded trends",
                color = MetriaColors.InkMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        TextButton(onClick = onOpenHistory) {
            Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.padding(end = 6.dp).size(20.dp))
            Text("All history")
        }
    }
}

@Composable
private fun AllHistory(
    entries: List<WeightEntry>,
    meals: List<MealEntry>,
    unit: String,
    selectedTab: String,
    onSelectedTab: (String) -> Unit,
    onBack: () -> Unit,
    onEditWeight: (WeightEntry) -> Unit,
    onEditMeal: (MealEntry) -> Unit,
) {
    val weightGroups = ProgressHistory.weightGroups(entries)
    val mealGroups = ProgressHistory.mealGroups(meals)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to progress")
            }
            Column(Modifier.padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("All history", style = MaterialTheme.typography.headlineMedium)
                Text("${entries.size} weights · ${meals.size} meals", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        HistorySelector(selectedTab, onSelectedTab)
        if (selectedTab == "Weight") {
            if (weightGroups.isEmpty()) {
                EmptyDayCard("No weight history", "Your saved weight entries will appear here.")
            } else {
                weightGroups.forEach { group ->
                    HistoryDateHeading(group.date)
                    group.entries.forEach { entry -> WeightEntryRow(entry, unit, onEditWeight) }
                }
            }
        } else {
            if (mealGroups.isEmpty()) {
                EmptyDayCard("No meal history", "Your saved meals will appear here.")
            } else {
                mealGroups.forEach { group ->
                    HistoryDateHeading(group.date)
                    group.meals.forEach { meal -> MealHistoryRow(meal, onEditMeal) }
                }
            }
        }
    }
}

@Composable
private fun HistorySelector(selectedTab: String, onSelected: (String) -> Unit) {
    Surface(color = MetriaColors.SurfaceSubtle, shape = CircleShape, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Weight", "Meals").forEach { tab ->
                val isSelected = tab == selectedTab
                Surface(
                    color = if (isSelected) MetriaColors.Surface else Color.Transparent,
                    shape = CircleShape,
                    tonalElevation = if (isSelected) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .semantics { selected = isSelected }
                        .clickable(role = Role.RadioButton) { onSelected(tab) },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(tab, color = if (isSelected) MetriaColors.Ink else MetriaColors.InkMuted, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeSelector(selectedRange: ProgressRange, onSelected: (ProgressRange) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val minimumLabelWidth = 80.dp * LocalDensity.current.fontScale
        val columns = when {
            maxWidth >= minimumLabelWidth * 4 -> 4
            maxWidth >= minimumLabelWidth * 2 -> 2
            else -> 1
        }
        Surface(color = MetriaColors.SurfaceSubtle, shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ProgressRange.entries.chunked(columns).forEach { ranges ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ranges.forEach { range ->
                            val isSelected = range == selectedRange
                            Surface(
                                color = if (isSelected) MetriaColors.Surface else Color.Transparent,
                                shape = MaterialTheme.shapes.large,
                                tonalElevation = if (isSelected) 1.dp else 0.dp,
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                                    .semantics { selected = isSelected }
                                    .clickable(role = Role.RadioButton) { onSelected(range) },
                            ) {
                                Row(
                                    Modifier.padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (isSelected) Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MetriaColors.Primary)
                                    Text(range.label, color = if (isSelected) MetriaColors.Ink else MetriaColors.InkMuted, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryDateHeading(date: LocalDate) {
    Text(historyDateLabel(date), color = MetriaColors.InkMuted, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun MetricSelector(selectedMetric: ProgressMetric, onSelected: (ProgressMetric) -> Unit) {
    Surface(color = MetriaColors.SurfaceSubtle, shape = CircleShape, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ProgressMetric.entries.forEach { metric ->
                val isSelected = metric == selectedMetric
                Surface(
                    color = if (isSelected) MetriaColors.Surface else Color.Transparent,
                    shape = CircleShape,
                    tonalElevation = if (isSelected) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .semantics { selected = isSelected }
                        .clickable(role = Role.RadioButton) { onSelected(metric) },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            metric.label,
                            color = if (isSelected) MetriaColors.Ink else MetriaColors.InkMuted,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressHero(
    metric: ProgressMetric,
    day: ProgressDay,
    days: List<ProgressDay>,
    range: ProgressRange,
    unit: String,
    targets: NutritionTargets?,
) {
    val rawValue = valueFor(metric, day)
    val shownValue = rawValue?.let { formatMetricValue(metric, it, unit) }
    val shownUnit = if (metric == ProgressMetric.Weight) unit else metric.unit
    val target = when (metric) {
        ProgressMetric.Calories -> targets?.caloriesKcal
        ProgressMetric.Protein -> targets?.proteinGrams
        ProgressMetric.Weight -> null
    }
    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.AccentSoft)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(dayLabel(day.date), style = MaterialTheme.typography.labelLarge)
                    Text(metric.label, color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                Text(shownValue ?: "—", style = MaterialTheme.typography.displayLarge)
                if (shownValue != null) Text(shownUnit, color = MetriaColors.InkMuted, modifier = Modifier.padding(bottom = 7.dp))
            }
            if (metric != ProgressMetric.Weight && day.nutritionIsPartial && rawValue != null) {
                MetriaEstimateBadge("Partial estimate")
            }
            Text(heroContext(metric, day, days, range, unit, target), color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EvolutionChart(
    metric: ProgressMetric,
    range: ProgressRange,
    days: List<ProgressDay>,
    selectedDate: LocalDate,
    unit: String,
    compact: Boolean,
    onSelectDate: (LocalDate) -> Unit,
) {
    val present = days.filter { valueFor(metric, it) != null }
    val selected = days.firstOrNull { it.date == selectedDate } ?: days.last()
    val selectedValue = valueFor(metric, selected)
    val first = present.firstOrNull()
    val last = present.lastOrNull()
    val selectedIndex = days.indexOfFirst { it.date == selectedDate }.coerceAtLeast(0)
    val description = buildString {
        append("${range.label} ${metric.label.lowercase()} history, ${days.first().date}, through ${days.last().date}. ")
        if (present.isEmpty()) append("No recorded values in this period.")
        else append("${present.size} dates with recorded values.")
        selectedValue?.let { append(" Selected ${selected.date}: ${formatMetricValue(metric, it, unit)} ${if (metric == ProgressMetric.Weight) unit else metric.unit}.") }
        if (metric == ProgressMetric.Weight && first != null && last != null) {
            val firstValue = valueFor(metric, first)!!
            val lastValue = valueFor(metric, last)!!
            append(" First reading ${first.date}, ${formatMetricValue(metric, firstValue, unit)}; latest ${last.date}, ${formatMetricValue(metric, lastValue, unit)}.")
        }
        if (metric != ProgressMetric.Weight && present.any(ProgressDay::nutritionIsPartial)) append(" Some recorded nutrition is partial.")
    }
    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Your evolution", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 18.dp))
            Text(periodDateLabel(days.first().date, days.last().date), color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 18.dp))
            EvolutionCanvas(metric, days, selectedIndex, description, compact) { fraction ->
                val targetIndex = (fraction.coerceIn(0f, 1f) * (days.lastIndex)).toInt()
                val nearest = days.indices.minByOrNull { index ->
                    val dateDistance = kotlin.math.abs(index - targetIndex)
                    if (valueFor(metric, days[index]) == null) dateDistance + days.size else dateDistance
                }
                nearest?.let { onSelectDate(days[it].date) }
            }
            EvolutionDateAxis(days)
            if (present.isEmpty()) {
                Text(if (metric == ProgressMetric.Weight) "No weight records in this period." else "No calculated nutrition in this period.", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 18.dp))
            } else {
                if (metric == ProgressMetric.Weight && first != null && last != null) {
                    val firstValue = valueFor(metric, first)!!
                    val lastValue = valueFor(metric, last)!!
                    val delta = lastValue - firstValue
                    val signed = if (delta > 0) "+" else if (delta < 0) "−" else ""
                    Text(
                        if (present.size < 2) "Readings are on one date; add a reading on another day to compare."
                        else "$signed${formatMetricValue(metric, abs(delta), unit)} $unit from ${first.date} to ${last.date} · latest reading ${last.date}",
                        color = MetriaColors.InkMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                }
                AxisScaleContext(metric, present, unit)
                if (metric != ProgressMetric.Weight && present.any(ProgressDay::nutritionIsPartial)) {
                    Text("Includes daily recorded estimates. Partial values use square marks and ≈. Unrecorded days are not inferred.", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 18.dp))
                }
                DateStepSelector(metric, present, selected.date, unit, onSelectDate)
            }
        }
    }
}

@Composable
private fun AxisScaleContext(metric: ProgressMetric, present: List<ProgressDay>, unit: String) {
    val values = present.mapNotNull { valueFor(metric, it) }
    if (values.isEmpty()) return
    val low = if (metric == ProgressMetric.Weight) values.minOrNull()!! else 0.0
    val high = values.maxOrNull()!!
    val padding = if (metric == ProgressMetric.Weight) maxOf((high - low) * 0.2, 1.0) else 0.0
    Text(
        "Scale ${formatMetricValue(metric, low - padding, unit)}–${formatMetricValue(metric, high + padding, unit)} ${if (metric == ProgressMetric.Weight) unit else metric.unit}",
        color = MetriaColors.InkMuted,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(horizontal = 18.dp),
    )
}

@Composable
private fun DateStepSelector(
    metric: ProgressMetric,
    present: List<ProgressDay>,
    selectedDate: LocalDate,
    unit: String,
    onSelectDate: (LocalDate) -> Unit,
) {
    val selectedIndex = present.indexOfFirst { it.date == selectedDate }.let { if (it < 0) 0 else it }
    val previous = present.getOrNull(selectedIndex - 1)
    val next = present.getOrNull(selectedIndex + 1)
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = { previous?.let { onSelectDate(it.date) } }, enabled = previous != null, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Previous recorded date")
        }
        val current = present.getOrNull(selectedIndex)
        val value = current?.let { valueFor(metric, it) }
        Text(
            current?.let {
                "${it.date} · ${formatMetricValue(metric, value!!, unit)} ${if (metric == ProgressMetric.Weight) unit else metric.unit}" +
                    if (metric != ProgressMetric.Weight && it.nutritionIsPartial) " · ≈ Partial estimate" else ""
            } ?: "No recorded dates",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { next?.let { onSelectDate(it.date) } }, enabled = next != null, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = "Next recorded date")
        }
    }
}

@Composable
private fun EvolutionCanvas(
    metric: ProgressMetric,
    days: List<ProgressDay>,
    selectedIndex: Int,
    description: String,
    compact: Boolean,
    onSelectFraction: (Float) -> Unit,
) {
    val height = if (compact) 156.dp else 232.dp
    val values = days.map { valueFor(metric, it) }
    Canvas(
        Modifier.fillMaxWidth().height(height).padding(horizontal = 20.dp)
            .semantics { contentDescription = description }
            .pointerInput(days, metric) {
                detectTapGestures { point -> onSelectFraction(point.x / size.width) }
            },
    ) {
        val rangeStart = days.first().date
        val totalDays = ChronoUnit.DAYS.between(rangeStart, days.last().date).coerceAtLeast(1).toFloat()
        val chartTop = 12.dp.toPx()
        val chartBottom = size.height - 10.dp.toPx()
        val chartHeight = chartBottom - chartTop
        val subtle = MetriaColors.SurfaceSubtle
        val primary = MetriaColors.Primary
        val accent = MetriaColors.Accent
        drawLine(subtle, Offset(0f, chartBottom), Offset(size.width, chartBottom), strokeWidth = 2.dp.toPx())
        val presentValues = values.filterNotNull()
        if (presentValues.isEmpty()) return@Canvas
        val minValue = presentValues.minOrNull()!!
        val maxValue = presentValues.maxOrNull()!!
        val pad = if (metric == ProgressMetric.Weight) maxOf((maxValue - minValue) * 0.2, 1.0) else 0.0
        val axisMin = if (metric == ProgressMetric.Weight) minValue - pad else 0.0
        val axisMax = if (metric == ProgressMetric.Weight) maxValue + pad else maxValue.coerceAtLeast(1.0)
        val span = (axisMax - axisMin).takeIf { it > 0.0001 } ?: 1.0
        val points = values.mapIndexedNotNull { index, value ->
            value?.let {
                val x = ((ChronoUnit.DAYS.between(rangeStart, days[index].date).toFloat() / totalDays) * size.width)
                val y = chartBottom - (((it - axisMin) / span).toFloat().coerceIn(0f, 1f) * chartHeight)
                index to Offset(x, y)
            }
        }
        if (metric == ProgressMetric.Weight) {
            points.zipWithNext().forEach { (a, b) ->
                drawLine(primary, a.second, b.second, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }
        } else {
            val barWidth = (size.width / days.size * 0.7f).coerceIn(1f, 16.dp.toPx())
            points.forEach { (index, point) ->
                val color = when {
                    index == selectedIndex -> primary
                    days[index].nutritionIsPartial -> MetriaColors.Estimated
                    else -> accent
                }
                drawLine(color, Offset(point.x, chartBottom), point, strokeWidth = barWidth)
            }
        }
        points.forEach { (index, point) ->
            if (index == selectedIndex) drawCircle(primary.copy(alpha = 0.2f), radius = 10.dp.toPx(), center = point)
            val partialNutrition = metric != ProgressMetric.Weight && days[index].nutritionIsPartial
            if (partialNutrition) {
                val edge = if (index == selectedIndex) 9.dp.toPx() else 6.dp.toPx()
                drawRect(
                    if (index == selectedIndex) primary else MetriaColors.Estimated,
                    Offset(point.x - edge / 2, point.y - edge / 2),
                    Size(edge, edge),
                )
            } else if (metric == ProgressMetric.Weight || index == selectedIndex) {
                drawCircle(if (index == selectedIndex) primary else accent, radius = if (index == selectedIndex) 5.dp.toPx() else 3.dp.toPx(), center = point)
            }
        }
        // Dates anchor the plotting scale, so sparse weight readings are not presented as daily values.
        listOf(0, days.lastIndex / 2, days.lastIndex).distinct().forEach { index ->
            val x = ChronoUnit.DAYS.between(rangeStart, days[index].date).toFloat() / totalDays * size.width
            drawLine(subtle, Offset(x, chartTop), Offset(x, chartBottom), strokeWidth = 1.dp.toPx())
        }
    }
}

@Composable
private fun EvolutionDateAxis(days: List<ProgressDay>) {
    val indices = listOf(0, days.lastIndex / 2, days.lastIndex).distinct()
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        indices.forEach { dayIndex ->
            Text(
                if (days.first().date.year == days.last().date.year) shortDate(days[dayIndex].date)
                else days[dayIndex].date.format(DateTimeFormatter.ofPattern("d MMM\nyyyy", Locale.getDefault())),
                color = MetriaColors.InkMuted,
                style = MaterialTheme.typography.labelSmall,
                textAlign = when (dayIndex) {
                    0 -> TextAlign.Start
                    days.lastIndex -> TextAlign.End
                    else -> TextAlign.Center
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun EvolutionDetail(
    metric: ProgressMetric,
    range: ProgressRange,
    days: List<ProgressDay>,
    unit: String,
    selectedDate: LocalDate,
    page: Int,
    onPageChange: (Int) -> Unit,
    onBack: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
) {
    val headerBringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        headerBringIntoViewRequester.bringIntoView()
    }
    val present = days.filter { valueFor(metric, it) != null }
    val pageSize = 12
    val pageCount = (present.size + pageSize - 1) / pageSize
    val safePage = page.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    val newestFirst = present.asReversed()
    val visible = newestFirst.drop(safePage * pageSize).take(pageSize)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth().bringIntoViewRequester(headerBringIntoViewRequester),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to evolution")
            }
            Column(Modifier.padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${metric.label} detail", style = MaterialTheme.typography.headlineSmall)
                Text("${range.label} · ${periodDateLabel(days.first().date, days.last().date)}", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        EvolutionCanvas(metric, days, days.indexOfFirst { it.date == selectedDate }.coerceAtLeast(0), "${range.label} ${metric.label.lowercase()} trend, ${present.size} recorded dates", compact = false) { fraction ->
            val target = (fraction.coerceIn(0f, 1f) * days.lastIndex).toInt()
            present.minByOrNull { kotlin.math.abs(days.indexOf(it) - target) }?.let {
                onSelectDate(it.date)
                val pageForDate = (present.lastIndex - present.indexOf(it)).coerceAtLeast(0) / pageSize
                onPageChange(pageForDate)
            }
        }
        EvolutionDateAxis(days)
        AxisScaleContext(metric, present, unit)
        DateStepSelector(metric, present, selectedDate, unit) { date ->
            onSelectDate(date)
            val selectedIndex = present.indexOfFirst { it.date == date }
            onPageChange((present.lastIndex - selectedIndex).coerceAtLeast(0) / pageSize)
        }
        if (metric != ProgressMetric.Weight && present.isNotEmpty()) {
            Text(
                "Daily recorded estimates. Square marks indicate partial values; unrecorded days stay empty.",
                color = MetriaColors.InkMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (present.isEmpty()) {
            EmptyDayCard(
                if (metric == ProgressMetric.Weight) "No weight records in this period" else "No calculated nutrition in this period",
                "Saved ${metric.label.lowercase()} readings will appear here.",
            )
        } else {
            Text("Recorded values", style = MaterialTheme.typography.titleMedium)
            if (metric == ProgressMetric.Weight) {
                visible.flatMap { day -> day.weightEntries.map { day to it } }.forEach { (day, entry) ->
                    val value = shownWeight(entry.kilograms, unit)
                    ReadOnlyValueRow(day.date, "${value.first} ${value.second}", day.date == selectedDate, entry.recordedAt.atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0).toString()) {
                        onSelectDate(day.date)
                    }
                }
            } else {
                visible.forEach { day ->
                    val value = valueFor(metric, day)!!
                    ReadOnlyValueRow(
                        day.date,
                        "${formatMetricValue(metric, value, unit)} ${metric.unit}",
                        day.date == selectedDate,
                        if (day.nutritionIsPartial) "Partial estimate" else "Recorded daily estimate",
                    ) { onSelectDate(day.date) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onPageChange((safePage - 1).coerceAtLeast(0)) }, enabled = safePage > 0) { Text("Newer dates") }
                Text("${safePage + 1} of ${pageCount.coerceAtLeast(1)}", style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { onPageChange((safePage + 1).coerceAtMost((pageCount - 1).coerceAtLeast(0))) }, enabled = safePage < pageCount - 1) { Text("Earlier dates") }
            }
        }
    }
}

@Composable
internal fun ProgressEvolutionContent(
    metric: ProgressMetric,
    range: ProgressRange,
    days: List<ProgressDay>,
    unit: String = "kg",
    selectedDate: LocalDate = days.last().date,
    detail: Boolean = false,
) {
    var selected by rememberSaveable { mutableStateOf(selectedDate.toString()) }
    var page by rememberSaveable { mutableStateOf(0) }
    val selectedDateValue = runCatching { LocalDate.parse(selected) }.getOrDefault(selectedDate)
    if (detail) {
        EvolutionDetail(metric, range, days, unit, selectedDateValue, page, { page = it }, {}, { selected = it.toString() })
    } else {
        EvolutionChart(metric, range, days, selectedDateValue, unit, compact = true, onSelectDate = { selected = it.toString() })
    }
}

@Composable
private fun ReadOnlyValueRow(date: LocalDate, value: String, selected: Boolean, detail: String, onSelect: () -> Unit) {
    Surface(
        color = if (selected) MetriaColors.PrimarySoft else MetriaColors.Surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .semantics { this.selected = selected; contentDescription = "$date, $value, $detail${if (selected) ", selected" else ""}" }
            .clickable(role = Role.Button, onClick = onSelect),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(date.toString(), style = MaterialTheme.typography.labelLarge)
                Text(detail, color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected date", tint = MetriaColors.Primary, modifier = Modifier.padding(end = 8.dp).size(18.dp))
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun DayDetails(
    metric: ProgressMetric,
    day: ProgressDay,
    unit: String,
    onEditWeight: (WeightEntry) -> Unit,
    onEditMeal: (MealEntry) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(dayLabel(day.date), style = MaterialTheme.typography.titleLarge)
        if (metric == ProgressMetric.Weight) {
            if (day.weightEntries.isEmpty()) EmptyDayCard("No weight recorded", "Use the Metria button whenever you want to add one.")
            else day.weightEntries.forEach { entry -> WeightEntryRow(entry, unit, onEditWeight) }
        } else {
            if (day.meals.isEmpty()) EmptyDayCard("No meals recorded", "Choose another day or add a meal with the Metria button.")
            else day.meals.forEach { meal -> MealMetricRow(meal, metric, onEditMeal) }
        }
    }
}

@Composable
private fun WeightEntryRow(entry: WeightEntry, unit: String, onEdit: (WeightEntry) -> Unit) {
    val (value, shownUnit) = shownWeight(entry.kilograms, unit)
    Card(
        colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface),
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { onEdit(entry) },
    ) {
        Row(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = 72.dp).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(color = MetriaColors.PrimarySoft, shape = CircleShape) {
                Icon(Icons.Rounded.Scale, contentDescription = null, tint = MetriaColors.Primary, modifier = Modifier.padding(10.dp).size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("$value $shownUnit", style = MaterialTheme.typography.titleMedium)
                Text(entry.note?.takeIf { it.isNotBlank() } ?: entry.recordedAt.atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0).toString(), color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = "Edit weight entry", tint = MetriaColors.InkMuted)
        }
    }
}

@Composable
private fun MealMetricRow(meal: MealEntry, metric: ProgressMetric, onEdit: (MealEntry) -> Unit) {
    val value = when (metric) {
        ProgressMetric.Calories -> meal.nutrition?.caloriesKcal
        ProgressMetric.Protein -> meal.nutrition?.proteinGrams
        ProgressMetric.Weight -> null
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface),
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { onEdit(meal) },
    ) {
        Row(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = 76.dp).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetriaMealThumbnail(meal.referenceThumbnailBase64, size = 56.dp, drink = app.metria.MealAppearance.isDrink(meal))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(MealIdentity.displayTitle(meal), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    value?.let { "${formatMetricValue(metric, it, "kg")} ${metric.unit} · ${MealSlots.label(meal.mealSlot)}" }
                        ?: "Nutrition needs review · ${MealSlots.label(meal.mealSlot)}",
                    color = MetriaColors.InkMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (meal.nutrition?.estimateCoverage == "partial") MetriaEstimateBadge("Partial estimate")
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = "Edit ${MealIdentity.displayTitle(meal)}", tint = MetriaColors.InkMuted)
        }
    }
}

@Composable
private fun MealHistoryRow(meal: MealEntry, onEdit: (MealEntry) -> Unit) {
    val nutrition = meal.nutrition
    val nutritionText = listOfNotNull(
        nutrition?.caloriesKcal?.let { "${"%.0f".format(it)} kcal" },
        nutrition?.proteinGrams?.let { "${"%.1f".format(it)} g protein" },
    ).joinToString(" · ").ifBlank { "Nutrition needs review" }
    Card(
        colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface),
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { onEdit(meal) },
    ) {
        Row(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = 76.dp).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetriaMealThumbnail(meal.referenceThumbnailBase64, size = 56.dp, drink = app.metria.MealAppearance.isDrink(meal))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(MealIdentity.displayTitle(meal), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${MealSlots.label(meal.mealSlot)} · $nutritionText", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
                if (nutrition?.estimateCoverage == "partial") MetriaEstimateBadge("Partial estimate")
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = "Edit ${MealIdentity.displayTitle(meal)}", tint = MetriaColors.InkMuted)
        }
    }
}

@Composable
private fun EmptyDayCard(title: String, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun valueFor(metric: ProgressMetric, day: ProgressDay): Double? = when (metric) {
    ProgressMetric.Weight -> day.latestWeightKilograms
    ProgressMetric.Calories -> day.caloriesKcal
    ProgressMetric.Protein -> day.proteinGrams
}

private fun lastDateWithValue(days: List<ProgressDay>, metric: ProgressMetric): LocalDate =
    days.lastOrNull { valueFor(metric, it) != null }?.date ?: days.last().date

private fun heroContext(metric: ProgressMetric, day: ProgressDay, days: List<ProgressDay>, range: ProgressRange, unit: String, target: Double?): String {
    val value = valueFor(metric, day)
    if (value == null) return if (metric == ProgressMetric.Weight) "No weight entry on this day." else "No calculated nutrition on this day."
    if (target != null) return "Recorded estimate · ${"%.0f".format(value / target * 100.0)}% of your current daily reference."
    if (metric != ProgressMetric.Weight) return "Add a goal if you want a daily reference."
    val readings = days.filter { it.latestWeightKilograms != null }
    if (readings.isEmpty()) return "No weight entries in the ${range.label.lowercase()} period."
    val first = readings.first()
    val latest = readings.last()
    if (readings.size < 2) return "Readings are on ${first.date}. Add one on another day to compare."
    val change = latest.latestWeightKilograms!! - first.latestWeightKilograms!!
    val signed = if (change > 0) "+" else if (change < 0) "−" else ""
    return "${signed}${formatMetricValue(metric, abs(change), unit)} $unit from ${first.date} to ${latest.date}. Latest reading ${latest.date}."
}

private fun formatMetricValue(metric: ProgressMetric, value: Double, unit: String): String = when (metric) {
    ProgressMetric.Weight -> shownWeight(value, unit).first
    ProgressMetric.Calories -> "%.0f".format(value)
    ProgressMetric.Protein -> "%.1f".format(value)
}

private fun shownWeight(kilograms: Double, unit: String): Pair<String, String> =
    if (unit == "lb") "%.1f".format(WeightUnits.kilogramsToPounds(kilograms)) to "lb"
    else "%.1f".format(kilograms) to "kg"

private fun dayLabel(date: LocalDate): String = when (date) {
    LocalDate.now() -> "Today"
    LocalDate.now().minusDays(1) -> "Yesterday"
    else -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))
}

private fun shortDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))

private fun periodDateLabel(start: LocalDate, end: LocalDate): String {
    val pattern = if (start.year == end.year) "d MMM" else "d MMM yyyy"
    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    return "${start.format(formatter)} – ${end.format(formatter)}"
}

private fun historyDateLabel(date: LocalDate): String = when (date) {
    LocalDate.now() -> "Today"
    LocalDate.now().minusDays(1) -> "Yesterday"
    else -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()))
}

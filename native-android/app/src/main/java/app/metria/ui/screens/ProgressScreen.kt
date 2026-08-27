package app.metria.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.metria.MealEntry
import app.metria.MealIdentity
import app.metria.MealNutritionSummaries
import app.metria.MealSlots
import app.metria.NutritionTargets
import app.metria.WeightEntry
import app.metria.WeightUnits
import app.metria.ui.components.MetriaEstimateBadge
import app.metria.ui.components.MetriaMealThumbnail
import app.metria.ui.theme.MetriaColors
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/** Approved Progress A: one seven-day view, one dominant metric, and in-place day inspection. */
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
    fun sevenDays(
        entries: List<WeightEntry>,
        meals: List<MealEntry>,
        endDate: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<ProgressDay> = (6 downTo 0).map { offset ->
        val date = endDate.minusDays(offset.toLong())
        val dayWeights = entries.filter { it.recordedAt.atZone(zoneId).toLocalDate() == date }
            .sortedByDescending(WeightEntry::recordedAt)
        val dayMeals = meals.filter { it.recordedAt.atZone(zoneId).toLocalDate() == date }
            .sortedByDescending(MealEntry::recordedAt)
        val summary = MealNutritionSummaries.between(dayMeals, date, date)
        val includedNutritionCount = summary.completeNutritionMeals + summary.partialNutritionMeals
        ProgressDay(
            date = date,
            weightEntries = dayWeights,
            meals = dayMeals,
            latestWeightKilograms = dayWeights.firstOrNull()?.kilograms,
            caloriesKcal = sumOrNull(summary.caloriesKcal, summary.partialCaloriesKcal),
            proteinGrams = sumOrNull(summary.proteinGrams, summary.partialProteinGrams),
            nutritionIsPartial = (dayMeals.isNotEmpty() && includedNutritionCount < summary.recordedMeals) ||
                summary.partialNutritionMeals > 0,
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
) {
    val days = ProgressTimeline.sevenDays(entries, meals)
    var metricName by rememberSaveable { mutableStateOf(ProgressMetric.Weight.name) }
    val metric = ProgressMetric.valueOf(metricName)
    var selectedDateText by rememberSaveable {
        mutableStateOf(lastDateWithValue(days, ProgressMetric.Weight).toString())
    }
    var historyOpen by rememberSaveable { mutableStateOf(false) }
    var historyTab by rememberSaveable { mutableStateOf("Weight") }
    val selectedDate = runCatching { LocalDate.parse(selectedDateText) }.getOrDefault(days.last().date)
    val selectedDay = days.firstOrNull { it.date == selectedDate } ?: days.last()

    BackHandler(enabled = historyOpen) { historyOpen = false }
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
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProgressHeader(days.first().date, days.last().date) {
                historyTab = if (metric == ProgressMetric.Weight) "Weight" else "Meals"
                historyOpen = true
            }
            MetricSelector(metric) { next ->
                metricName = next.name
                selectedDateText = lastDateWithValue(days, next).toString()
            }
            ProgressHero(metric, selectedDay, days, unit, nutritionTargets, onEditNutritionTargets)
            SevenDayChart(metric, days, selectedDay.date, unit) { selectedDateText = it.toString() }
            DayDetails(metric, selectedDay, unit, onEditWeight, onEditMeal)
        }
    }
}

@Composable
private fun ProgressHeader(start: LocalDate, end: LocalDate, onOpenHistory: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Progress", style = MaterialTheme.typography.displaySmall)
            Text(
                "Last 7 days · ${shortDate(start)}–${shortDate(end)}",
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
    unit: String,
    targets: NutritionTargets?,
    onEditTargets: () -> Unit,
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
                if (metric != ProgressMetric.Weight) {
                    TextButton(onClick = onEditTargets) { Text(if (target == null) "Set goal" else "Edit goal") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                Text(shownValue ?: "—", style = MaterialTheme.typography.displayLarge)
                if (shownValue != null) Text(shownUnit, color = MetriaColors.InkMuted, modifier = Modifier.padding(bottom = 7.dp))
            }
            if (metric != ProgressMetric.Weight && day.nutritionIsPartial && rawValue != null) {
                MetriaEstimateBadge("Partial estimate")
            }
            Text(heroContext(metric, day, days, unit, target), color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SevenDayChart(
    metric: ProgressMetric,
    days: List<ProgressDay>,
    selectedDate: LocalDate,
    unit: String,
    onSelectDate: (LocalDate) -> Unit,
) {
    val values = days.map { valueFor(metric, it) }
    val spokenSummary = days.joinToString(". ") { day ->
        val value = valueFor(metric, day)?.let { formatMetricValue(metric, it, unit) } ?: "no entry"
        "${dayLabel(day.date)} $value${if (value != "no entry") " ${if (metric == ProgressMetric.Weight) unit else metric.unit}" else ""}"
    }
    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Your week", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 18.dp))
            MetricCanvas(
                metric = metric,
                values = values,
                selectedIndex = days.indexOfFirst { it.date == selectedDate }.coerceAtLeast(0),
                description = "Seven day ${metric.label.lowercase()} chart. $spokenSummary",
            )
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val dayWidth = ((maxWidth - 28.dp) / 7).coerceAtLeast(48.dp)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    days.forEach { day ->
                        val isSelected = day.date == selectedDate
                        val value = valueFor(metric, day)
                        val description = buildString {
                            append(dayLabel(day.date))
                            append(if (value == null) ", no entry" else ", ${formatMetricValue(metric, value, unit)} ${if (metric == ProgressMetric.Weight) unit else metric.unit}")
                            if (metric != ProgressMetric.Weight && day.nutritionIsPartial && value != null) append(", partial estimate")
                            if (isSelected) append(", selected")
                        }
                        Surface(
                            color = if (isSelected) MetriaColors.PrimarySoft else Color.Transparent,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .width(dayWidth)
                                .defaultMinSize(minHeight = 58.dp)
                                .semantics { contentDescription = description; selected = isSelected }
                                .clickable(role = Role.RadioButton) { onSelectDate(day.date) },
                        ) {
                            Column(
                                Modifier.padding(vertical = 7.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), color = MetriaColors.InkMuted, style = MaterialTheme.typography.labelMedium)
                                Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium)
                                if (metric != ProgressMetric.Weight && day.nutritionIsPartial && value != null) {
                                    Text("≈", color = MetriaColors.Estimated, style = MaterialTheme.typography.labelMedium)
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
private fun MetricCanvas(metric: ProgressMetric, values: List<Double?>, selectedIndex: Int, description: String) {
    val accent = MetriaColors.Accent
    val primary = MetriaColors.Primary
    val subtle = MetriaColors.SurfaceSubtle
    Canvas(Modifier.fillMaxWidth().height(164.dp).padding(horizontal = 20.dp).semantics { contentDescription = description }) {
        val slotWidth = size.width / values.size
        val chartTop = 12.dp.toPx()
        val chartBottom = size.height - 10.dp.toPx()
        val chartHeight = chartBottom - chartTop
        drawLine(subtle, Offset(0f, chartBottom), Offset(size.width, chartBottom), strokeWidth = 2.dp.toPx())
        val selectedX = slotWidth * selectedIndex + slotWidth / 2f
        drawLine(primary.copy(alpha = 0.16f), Offset(selectedX, chartTop), Offset(selectedX, chartBottom), strokeWidth = 2.dp.toPx())

        val present = values.filterNotNull()
        if (present.isEmpty()) return@Canvas
        if (metric == ProgressMetric.Weight) {
            val minimum = present.minOrNull()!!
            val maximum = present.maxOrNull()!!
            val span = (maximum - minimum).takeIf { it > 0.01 } ?: 1.0
            val points = values.mapIndexedNotNull { index, value ->
                value?.let {
                    val x = slotWidth * index + slotWidth / 2f
                    val normalized = ((it - minimum) / span).toFloat()
                    val y = chartBottom - (0.18f + normalized * 0.72f) * chartHeight
                    index to Offset(x, y)
                }
            }
            points.zipWithNext().forEach { (first, second) ->
                drawLine(primary, first.second, second.second, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
            }
            points.forEach { (index, point) ->
                if (index == selectedIndex) drawCircle(primary.copy(alpha = 0.18f), radius = 10.dp.toPx(), center = point)
                drawCircle(if (index == selectedIndex) primary else accent, radius = 5.dp.toPx(), center = point)
                drawCircle(Color.White, radius = 2.dp.toPx(), center = point)
            }
        } else {
            val maximum = present.maxOrNull()!!.coerceAtLeast(1.0)
            values.forEachIndexed { index, value ->
                val x = slotWidth * index + slotWidth / 2f
                val barWidth = slotWidth * 0.36f
                drawRoundRect(subtle, Offset(x - barWidth / 2f, chartTop), Size(barWidth, chartHeight), CornerRadius(barWidth / 2f))
                if (value != null) {
                    val barHeight = (value / maximum).toFloat().coerceIn(0.04f, 1f) * chartHeight
                    drawRoundRect(
                        if (index == selectedIndex) primary else accent,
                        Offset(x - barWidth / 2f, chartBottom - barHeight),
                        Size(barWidth, barHeight),
                        CornerRadius(barWidth / 2f),
                    )
                }
            }
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
            MetriaMealThumbnail(meal.referenceThumbnailBase64, size = 56.dp)
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
            MetriaMealThumbnail(meal.referenceThumbnailBase64, size = 56.dp)
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

private fun heroContext(metric: ProgressMetric, day: ProgressDay, days: List<ProgressDay>, unit: String, target: Double?): String {
    val value = valueFor(metric, day)
    if (value == null) return if (metric == ProgressMetric.Weight) "No weight entry on this day." else "No calculated nutrition on this day."
    if (target != null) return "${"%.0f".format(value / target * 100.0)}% of your daily goal."
    if (metric != ProgressMetric.Weight) return "Add a goal if you want a daily reference."
    val weights = days.mapNotNull(ProgressDay::latestWeightKilograms)
    if (weights.size < 2) return "Add another reading to see a seven-day change."
    val change = weights.last() - weights.first()
    return when {
        abs(change) < 0.05 -> "Broadly unchanged across recorded days this week."
        change > 0 -> "${formatMetricValue(metric, change, unit)} $unit higher across recorded days this week."
        else -> "${formatMetricValue(metric, -change, unit)} $unit lower across recorded days this week."
    }
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

private fun historyDateLabel(date: LocalDate): String = when (date) {
    LocalDate.now() -> "Today"
    LocalDate.now().minusDays(1) -> "Yesterday"
    else -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()))
}

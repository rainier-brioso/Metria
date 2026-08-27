package app.metria.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Restaurant
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.metria.CycleContext
import app.metria.MealEntry
import app.metria.MealIdentity
import app.metria.MealNutritionSummaries
import app.metria.MealSlots
import app.metria.NutritionTargets
import app.metria.WeightEntry
import app.metria.WeightUnits
import app.metria.ui.components.MetriaEstimateBadge
import app.metria.ui.components.MetriaMetricCard
import app.metria.ui.theme.MetriaColors
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MetriaTodayScreen(
    entries: List<WeightEntry>,
    meals: List<MealEntry>,
    unit: String,
    nutritionTargets: NutritionTargets?,
    cycleContext: CycleContext,
    onEditMeal: (MealEntry) -> Unit,
    onEditNutritionTargets: () -> Unit,
    onOpenProgress: () -> Unit,
) {
    val today = LocalDate.now()
    val latestWeight = entries.maxByOrNull(WeightEntry::recordedAt)
    val todayMeals = meals.filter { it.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate() == today }
        .sortedByDescending(MealEntry::recordedAt)
    val todayNutrition = MealNutritionSummaries.between(meals, today, today)
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val loggedDays = days.count { day -> meals.any { it.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate() == day } }
    val calories = (todayNutrition.caloriesKcal ?: 0.0) + (todayNutrition.partialCaloriesKcal ?: 0.0)
    val hasNutrition = todayNutrition.completeNutritionMeals + todayNutrition.partialNutritionMeals > 0

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TodayHeader(today)
        WeeklyHero(loggedDays)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetriaMetricCard(
                label = "Latest weight",
                value = latestWeight?.let { formatWeightValue(it.kilograms, unit).first } ?: "—",
                unit = latestWeight?.let { formatWeightValue(it.kilograms, unit).second },
                icon = Icons.Rounded.Scale,
                supporting = latestWeight?.recordedAt?.atZone(ZoneId.systemDefault())?.toLocalDate()?.let(::friendlyDate)
                    ?: "Add your first entry",
                accessibilityLabel = latestWeight?.let { "Latest weight ${formatWeightValue(it.kilograms, unit).first} ${formatWeightValue(it.kilograms, unit).second}" }
                    ?: "No weight entry yet",
                onClick = onOpenProgress,
                modifier = Modifier.weight(1f),
            )
            MetriaMetricCard(
                label = "Nutrition",
                value = if (hasNutrition) "%.0f".format(calories) else "—",
                unit = if (hasNutrition) "kcal" else null,
                icon = Icons.Rounded.Restaurant,
                supporting = nutritionTargets?.caloriesKcal?.let { "of ${"%.0f".format(it)} kcal goal" }
                    ?: "Tap to set a goal",
                accessibilityLabel = if (hasNutrition) "Nutrition estimate ${"%.0f".format(calories)} kilocalories" else "No nutrition estimate today",
                onClick = onEditNutritionTargets,
                modifier = Modifier.weight(1f),
            )
        }
        WeekStrip(days, today)
        CycleContextCard(entries, cycleContext)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Today’s meals", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onOpenProgress) { Text("See all") }
        }
        if (todayMeals.isEmpty()) {
            EmptyMealsCard()
        } else {
            todayMeals.forEach { meal -> TodayMealRow(meal, onEditMeal) }
        }
    }
}

@Composable
private fun TodayHeader(today: LocalDate) {
    val hour = java.time.LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(greeting, color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodyMedium)
        Text("Today", style = MaterialTheme.typography.displaySmall)
        Text(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())), color = MetriaColors.InkMuted)
    }
}

@Composable
private fun WeeklyHero(loggedDays: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.AccentSoft)) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("THIS WEEK", style = MaterialTheme.typography.labelMedium)
                Text("Your nutrition\nat a glance", style = MaterialTheme.typography.headlineMedium)
            }
            Box(Modifier.size(86.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.matchParentSize().semantics { contentDescription = "$loggedDays of 7 days have meal records" }) {
                    val stroke = 10.dp.toPx()
                    drawArc(
                        color = MetriaColors.Surface.copy(alpha = 0.82f), startAngle = -90f, sweepAngle = 360f,
                        useCenter = false, topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = MetriaColors.Accent.copy(red = 0.55f, green = 0.75f, blue = 0.18f), startAngle = -90f,
                        sweepAngle = 360f * loggedDays / 7f, useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(loggedDays.toString(), style = MaterialTheme.typography.titleLarge)
                    Text("of 7 days", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun WeekStrip(days: List<LocalDate>, today: LocalDate) {
    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                days.forEach { day ->
                    val selected = day == today
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(day.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault()), color = MetriaColors.InkMuted, style = MaterialTheme.typography.labelMedium)
                        Surface(color = if (selected) MetriaColors.Accent else androidx.compose.ui.graphics.Color.Transparent, shape = CircleShape) {
                            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                                Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CycleContextCard(entries: List<WeightEntry>, context: CycleContext) {
    if (!context.enabled || entries.size < 2 || context.periodStarts.isEmpty()) return
    val ordered = entries.sortedBy(WeightEntry::recordedAt)
    val latest = ordered.last()
    val previous = ordered[ordered.lastIndex - 1]
    if (latest.kilograms <= previous.kilograms) return
    val latestDate = latest.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate()
    val periodStart = context.periodStarts.filter { !it.isAfter(latestDate) }.maxOrNull() ?: return
    val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(periodStart, latestDate)
    if (daysSinceStart !in 0..7) return
    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.PrimarySoft)) {
        Text(
            "Short-term weight changes can include normal fluid variation. Your private cycle context may be relevant; follow several readings.",
            color = MetriaColors.Primary,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun EmptyMealsCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface)) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(color = MetriaColors.SurfaceSubtle, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Rounded.Restaurant, contentDescription = null, tint = MetriaColors.Primary, modifier = Modifier.padding(14.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("No meals yet", style = MaterialTheme.typography.titleMedium)
                Text("Use the Metria button to analyse, speak, or add one manually.", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TodayMealRow(meal: MealEntry, onEdit: (MealEntry) -> Unit) {
    val nutrition = meal.nutrition
    val summary = nutrition?.caloriesKcal?.let { calories ->
        val prefix = if (nutrition.estimateCoverage == "partial") "Partial · " else ""
        "$prefix${"%.0f".format(calories)} kcal"
    } ?: "Nutrition needs review"
    Card(
        colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface),
        modifier = Modifier.fillMaxWidth().clickable { onEdit(meal) },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MealThumbnail(meal.referenceThumbnailBase64)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(MealIdentity.displayTitle(meal), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${MealSlots.label(meal.mealSlot)} · $summary", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
                if (nutrition?.estimateCoverage == "partial" || meal.source == "ai_estimate") {
                    MetriaEstimateBadge(if (nutrition?.estimateCoverage == "partial") "Partial estimate" else "AI estimate")
                }
            }
            IconButton(onClick = { onEdit(meal) }) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Review ${MealIdentity.displayTitle(meal)}")
            }
        }
    }
}

@Composable
private fun MealThumbnail(encoded: String?) {
    val bitmap = remember(encoded) {
        encoded?.let { value ->
            runCatching {
                val bytes = Base64.decode(value.substringAfter("base64,", value), Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
    }
    Surface(color = MetriaColors.AccentSoft, shape = RoundedCornerShape(16.dp), modifier = Modifier.size(64.dp)) {
        if (bitmap != null) {
            Image(
                bitmap.asImageBitmap(), contentDescription = "Private meal thumbnail",
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)),
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Restaurant, contentDescription = null, tint = MetriaColors.Primary)
            }
        }
    }
}

private fun formatWeightValue(kilograms: Double, unit: String): Pair<String, String> =
    if (unit == "lb") "%.1f".format(WeightUnits.kilogramsToPounds(kilograms)) to "lb"
    else "%.1f".format(kilograms) to "kg"

private fun friendlyDate(date: LocalDate): String = when (date) {
    LocalDate.now() -> "Today"
    LocalDate.now().minusDays(1) -> "Yesterday"
    else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
}

package app.metria.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.metria.AssistantReviewedLabelPolicy
import app.metria.LabelNutritionDraft
import app.metria.MealEntry
import app.metria.MealSlots
import app.metria.ui.theme.MetriaColors
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.launch

/** Inline focused editor. Its panel supplies scrolling and IME insets. */
@Composable
fun AssistantLabelReviewContent(
    draft: LabelNutritionDraft,
    thumbnail: String?,
    onSave: suspend (MealEntry) -> String?,
    onSavingChanged: (Boolean) -> Unit = {},
    initialDate: LocalDate = LocalDate.now(),
    initialTime: LocalTime = LocalTime.now().withSecond(0).withNano(0),
    initialMealSlot: String? = null,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(initialDate.toString()) }
    val time = rememberSaveable { initialTime.toString() }
    val suggestedSlot = remember(time) { MealSlots.suggestedFor(LocalTime.parse(time)) }
    var mealSlot by rememberSaveable { mutableStateOf<String?>(initialMealSlot?.takeIf { it in MealSlots.allowed } ?: suggestedSlot) }
    var calories by rememberSaveable { mutableStateOf(draft.caloriesKcal?.toString().orEmpty()) }
    var protein by rememberSaveable { mutableStateOf(draft.proteinGrams?.toString().orEmpty()) }
    var carbs by rememberSaveable { mutableStateOf(draft.carbohydrateGrams?.toString().orEmpty()) }
    var fat by rememberSaveable { mutableStateOf(draft.totalFatGrams?.toString().orEmpty()) }
    var keepReference by rememberSaveable { mutableStateOf(thumbnail != null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Text("Compare the values and serving size with the package. Missing values stay unknown.", color = MetriaColors.InkMuted)
    OutlinedTextField(name, { name = it }, label = { Text("Food or product name") }, modifier = Modifier.fillMaxWidth())
    LabelValueField("Calories (kcal)", calories) { calories = it }
    LabelValueField("Protein (g)", protein) { protein = it }
    LabelValueField("Carbohydrates (g)", carbs) { carbs = it }
    LabelValueField("Fat (g)", fat) { fat = it }
    OutlinedTextField(date, { date = it }, label = { Text("Date - YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Meal type", style = MaterialTheme.typography.labelLarge)
        Text("Selected: ${MealSlots.label(mealSlot)}", style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MealSlots.allowed.forEach { slot ->
                FilterChip(
                    selected = mealSlot == slot,
                    onClick = { mealSlot = if (mealSlot == slot) null else slot },
                    leadingIcon = if (mealSlot == slot) {{ Icon(Icons.Rounded.Check, contentDescription = null) }} else null,
                    label = { Text(MealSlots.label(slot) + if (mealSlot == null && suggestedSlot == slot) " · suggested" else "") },
                    modifier = Modifier.heightIn(min = 48.dp).semantics {
                        contentDescription = MealSlots.label(slot)
                        stateDescription = if (mealSlot == slot) "Selected" else "Not selected"
                    },
                )
            }
        }
    }
    if (thumbnail != null) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(keepReference, { keepReference = it }, modifier = Modifier.semantics { contentDescription = "Keep a private reference photo" })
            Text("Keep a private reference photo", modifier = Modifier.weight(1f))
        }
    }
    error?.let { Text(it, color = MetriaColors.Danger) }
    Button(onClick = {
        val meal = AssistantReviewedLabelPolicy.meal(name, date, LocalTime.parse(time), listOf(calories, protein, carbs, fat), thumbnail.takeIf { keepReference }, mealSlot)
        if (meal == null) error = "Enter a product name, valid non-negative values, and a valid date."
        else {
            scope.launch {
                saving = true
                onSavingChanged(true)
                try {
                    error = onSave(meal)
                    saved = error == null
                } catch (_: Exception) {
                    error = "The label could not be saved locally. Review it and try again."
                } finally {
                    saving = false
                    onSavingChanged(false)
                }
            }
        }
    }, enabled = !saved && !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Save verified label locally") }
    if (saving) AlertDialog(
        onDismissRequest = {},
        title = { Text("Saving label") },
        text = { Text("Saving this reviewed label securely on this phone.") },
        confirmButton = {},
    )
}

@Composable
private fun LabelValueField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(), singleLine = true)
}

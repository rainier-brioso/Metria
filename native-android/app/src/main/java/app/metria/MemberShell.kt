package app.metria

import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay

private val Ink = Color(0xFF201A2B)
private val Plum = Color(0xFF5C3E77)
private val PlumSoft = Color(0xFFECE3F3)
private val Sage = Color(0xFF3F6C5B)
private val MetriaCanvas = Color(0xFFFBF8FC)

@Composable
fun MetriaMemberShell(vault: EncryptedVault, activity: MainActivity) {
    var status by remember { mutableStateOf(vault.status()) }
    var destination by remember { mutableStateOf("Today") }
    var addSheet by remember { mutableStateOf(false) }
    var editEntry by remember { mutableStateOf<WeightEntry?>(null) }
    var deleteEntry by remember { mutableStateOf<WeightEntry?>(null) }
    var voiceDraft by remember { mutableStateOf<VoiceWeightDraft?>(null) }
    var modelStatus by remember { mutableStateOf(activity.modelStatus()) }
    var mealProposalForConfirmation by remember { mutableStateOf<MealProposal?>(null) }
    DisposableEffect(activity) {
        activity.setVaultCallback { status = it }
        activity.setVoiceDraftCallback { voiceDraft = it }
        onDispose { activity.setVaultCallback(null); activity.setVoiceDraftCallback(null) }
    }
    LaunchedEffect(status, voiceDraft) {
        if (status is EncryptedVault.Status.Unlocked && voiceDraft != null) addSheet = true
    }
    LaunchedEffect(destination) {
        while (destination == "Assistant") {
            modelStatus = activity.modelStatus()
            delay(500)
        }
    }
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(
        primary = Plum, onPrimary = Color.White, secondary = Sage,
        surface = MetriaCanvas, onSurface = Ink, surfaceVariant = PlumSoft,
    )) {
        when (val vaultStatus = status) {
            EncryptedVault.Status.Missing -> VaultGate("A private space for your health.", "Create an encrypted vault on this phone. No account is needed.") {
                activity.authenticateVault(true)
            }
            EncryptedVault.Status.Locked -> VaultGate("Welcome back.", "Your entries are protected until you unlock your vault.") {
                activity.authenticateVault(false)
            }
            is EncryptedVault.Status.RecoveryRequired -> VaultGate("Vault unavailable", vaultStatus.message) {}
            is EncryptedVault.Status.Failed -> VaultGate("Try again", vaultStatus.message) { status = vault.status() }
            is EncryptedVault.Status.Unlocked -> {
                Scaffold(
                    containerColor = MetriaCanvas,
                    bottomBar = { MemberNavigation(destination) { destination = it } },
                    floatingActionButton = { if (destination != "Privacy") FloatingActionButton(onClick = { addSheet = true }, containerColor = Plum) { Text("+") } },
                ) { padding ->
                    Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp).padding(bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        when (destination) {
                            "Today" -> TodayScreen(vaultStatus.entries, vaultStatus.meals, vaultStatus.displayUnit, onAdd = { addSheet = true })
                            "History" -> HistoryScreen(vaultStatus.entries, vaultStatus.meals, vaultStatus.displayUnit, onEdit = { editEntry = it }, onDelete = { deleteEntry = it })
                            "Assistant" -> AssistantScreen(
                                modelStatus = modelStatus,
                                prepare = { modelStatus = activity.prepareLocalAssistant() },
                                ask = { modelStatus = activity.askLocalMealAssistant(it) },
                                review = { mealProposalForConfirmation = it },
                            )
                            else -> PrivacyScreen(vaultStatus.displayUnit, { status = vault.setDisplayUnit(if (vaultStatus.displayUnit == "kg") "lb" else "kg") }) { activity.lockVault() }
                        }
                    }
                }
                if (addSheet || editEntry != null) WeightBottomSheet(editEntry, voiceDraft, onDismiss = { addSheet = false; editEntry = null; voiceDraft = null }) { kilograms, note, date ->
                    status = editEntry?.let { vault.updateWeight(it.id, kilograms, note, date) } ?: vault.addWeight(kilograms, note, date)
                    addSheet = false
                    editEntry = null
                    voiceDraft = null
                }
                deleteEntry?.let { entry -> AlertDialog(onDismissRequest = { deleteEntry = null }, title = { Text("Delete entry?") }, text = { Text("This removes ${"%.1f".format(entry.kilograms)} kg from your encrypted vault.") }, confirmButton = { Button(onClick = { status = vault.deleteWeight(entry.id); deleteEntry = null }) { Text("Delete") } }, dismissButton = { Button(onClick = { deleteEntry = null }) { Text("Keep entry") } }) }
                mealProposalForConfirmation?.let { proposal -> MealConfirmationSheet(
                    proposal = proposal,
                    onDismiss = { mealProposalForConfirmation = null },
                    onSave = { meal ->
                        status = vault.addMeal(meal)
                        mealProposalForConfirmation = null
                        modelStatus = activity.clearLocalMealDraft()
                    },
                ) }
            }
        }
    }
}

@Composable
private fun VaultGate(title: String, body: String, action: () -> Unit) {
    Surface(color = MetriaCanvas, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
            Text("METRIA", color = Plum, style = MaterialTheme.typography.labelLarge)
            Text(title, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(top = 20.dp))
            Text(body, modifier = Modifier.padding(top = 12.dp, bottom = 28.dp))
            Button(onClick = action, enabled = title != "Vault unavailable") { Text(if (title == "Welcome back.") "Unlock with biometrics" else "Create private vault") }
        }
    }
}

@Composable
private fun MemberNavigation(current: String, choose: (String) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        listOf("Today", "History", "Assistant", "Privacy").forEach { item ->
            val icon = when (item) {
                "Today" -> Icons.Default.Home
                "History" -> Icons.Default.ShowChart
                "Assistant" -> Icons.Default.SmartToy
                else -> Icons.Default.Lock
            }
            NavigationBarItem(selected = current == item, onClick = { choose(item) }, icon = { Icon(icon, contentDescription = item) }, label = { Text(item) })
        }
    }
}

@Composable
private fun AssistantScreen(modelStatus: ModelStatus, prepare: () -> Unit, ask: (String) -> Unit, review: (MealProposal) -> Unit) {
    var mealText by remember { mutableStateOf("") }
    Text("Ask Metria", style = MaterialTheme.typography.displaySmall)
    Text("Describe a meal in your own words. It stays on this phone and becomes an editable estimate, never an automatic entry.", color = Color(0xFF655D6E))
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Private local assistant", style = MaterialTheme.typography.titleMedium)
            when (modelStatus.state) {
                "loaded" -> Text("Ready on this phone. Estimates can be wrong—review every assumption.", color = Sage)
                "thinking" -> Text("Thinking locally. This can take a little while on your phone.", color = Plum)
                "ready" -> {
                    Text("Your optional local model is ready to prepare.")
                    Button(onClick = prepare, modifier = Modifier.fillMaxWidth()) { Text("Prepare assistant") }
                }
                else -> Text("The optional local model is ${modelStatus.state}. This developer build needs a downloaded model before chat can run.")
            }
            OutlinedTextField(mealText, { mealText = it }, enabled = modelStatus.state == "loaded", label = { Text("For example: chicken breast with half a plate of rice") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Button(onClick = { ask(mealText) }, enabled = modelStatus.state == "loaded" && mealText.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Create editable estimate") }
        }
    }
    modelStatus.assistantDiagnostic?.let { Text(it, color = Color(0xFFB3261E)) }
    modelStatus.assistantProposal?.let { proposal ->
        Card(colors = CardDefaults.cardColors(containerColor = PlumSoft)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Review this estimate", style = MaterialTheme.typography.titleLarge)
                Text("Confidence: ${proposal.overallConfidence}. Nothing has been saved.", color = Color(0xFF655D6E))
                proposal.items.forEach { item ->
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text("${item.visiblePortion} · ${item.estimatedGramsRange.min}–${item.estimatedGramsRange.max} g · ${item.confidence} confidence")
                    Text(item.preparation, color = Color(0xFF655D6E))
                }
                proposal.assumptions.forEach { Text("Assumption: $it") }
                proposal.questions.forEach { Text("Question: $it") }
                Text("Nothing has been saved. Review every portion before confirming.", color = Plum)
                Button(onClick = { review(proposal) }, modifier = Modifier.fillMaxWidth()) { Text("Review and confirm meal") }
            }
        }
    }
}

@Composable
private fun TodayScreen(entries: List<WeightEntry>, meals: List<MealEntry>, unit: String, onAdd: () -> Unit) {
    val latest = entries.maxByOrNull(WeightEntry::recordedAt)
    val month = WeightTrend.filter(entries, LocalDate.now().minusDays(29), LocalDate.now())
    Text("Today", style = MaterialTheme.typography.displaySmall)
    Text("A calm view of your own progress.", color = Color(0xFF655D6E))
    Card(colors = CardDefaults.cardColors(containerColor = Plum), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("LATEST WEIGHT", color = Color(0xFFEADFF1), style = MaterialTheme.typography.labelMedium)
            Text(latest?.let { formatWeight(it.kilograms, unit) } ?: "No entry yet", color = Color.White, style = MaterialTheme.typography.displayMedium)
            Text(latest?.recordedAt?.atZone(ZoneId.systemDefault())?.toLocalDate()?.toString() ?: "Your first entry stays on this phone.", color = Color(0xFFEADFF1))
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("30-day reflection", style = MaterialTheme.typography.titleMedium)
            Text(WeightTrend.description(month), color = Color(0xFF655D6E))
            if (month.size >= 2) MiniTrend(month)
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Meals", style = MaterialTheme.typography.titleMedium)
            val latestMeal = meals.maxByOrNull(MealEntry::recordedAt)
            Text(if (latestMeal == null) "No meals recorded yet." else latestMeal.items.joinToString { it.name }, color = Color(0xFF655D6E))
            Text(if (meals.isEmpty()) "Use Assistant to create an editable private meal draft." else "${meals.size} private meal ${if (meals.size == 1) "record" else "records"} on this phone.", color = Color(0xFF655D6E))
        }
    }
    Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Record today’s weight") }
}

@Composable
private fun HistoryScreen(entries: List<WeightEntry>, meals: List<MealEntry>, unit: String, onEdit: (WeightEntry) -> Unit, onDelete: (WeightEntry) -> Unit) {
    var days by remember { mutableStateOf(30) }
    var customStart by remember { mutableStateOf("") }
    var customEnd by remember { mutableStateOf("") }
    val start = if (days == 0) runCatching { LocalDate.parse(customStart) }.getOrNull() else LocalDate.now().minusDays((days - 1).toLong())
    val end = if (days == 0) runCatching { LocalDate.parse(customEnd) }.getOrNull() else LocalDate.now()
    val points = WeightTrend.filter(entries, start, end)
    Text("History", style = MaterialTheme.typography.displaySmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(7, 30, 90).forEach { option -> FilterChip(selected = days == option, onClick = { days = option }, label = { Text("${option}D") }) }
        FilterChip(selected = days == 0, onClick = { days = 0 }, label = { Text("Custom") })
    }
    if (days == 0) { OutlinedTextField(customStart, { customStart = it }, label = { Text("Start · YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(customEnd, { customEnd = it }, label = { Text("End · YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth()) }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(20.dp)) {
        Text("Weight trend", style = MaterialTheme.typography.titleMedium)
        Text(WeightTrend.description(points), color = Color(0xFF655D6E))
        if (points.size >= 2) MiniTrend(points)
    } }
    Text("Entries", style = MaterialTheme.typography.titleLarge)
    entries.sortedByDescending(WeightEntry::recordedAt).forEach { entry ->
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(formatWeight(entry.kilograms, unit), style = MaterialTheme.typography.titleMedium); entry.note?.let { Text(it, color = Color(0xFF655D6E)) } }
            Column(horizontalAlignment = Alignment.End) {
                Text(entry.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate().toString(), color = Color(0xFF655D6E))
                Row {
                    IconButton(onClick = { onEdit(entry) }) { Icon(Icons.Default.Edit, contentDescription = "Edit weight entry") }
                    IconButton(onClick = { onDelete(entry) }) { Icon(Icons.Default.Delete, contentDescription = "Delete weight entry") }
                }
            }
        } }
    }
    if (meals.isNotEmpty()) {
        Text("Meals", style = MaterialTheme.typography.titleLarge)
        meals.sortedByDescending(MealEntry::recordedAt).forEach { meal ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(meal.items.joinToString { it.name }, style = MaterialTheme.typography.titleMedium)
                    Text("${meal.source.replace('_', ' ')} - ${meal.confidence} confidence - ${meal.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate()}", color = Color(0xFF655D6E))
                    meal.nutrition?.let { nutrition -> Text(nutritionSummary(nutrition), color = Color(0xFF655D6E)) }
                }
            }
        }
    }
}

@Composable
private fun PrivacyScreen(unit: String, changeUnit: () -> Unit, lock: () -> Unit) {
    Text("Private by default", style = MaterialTheme.typography.displaySmall)
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your data stays with you", style = MaterialTheme.typography.titleLarge)
        Text("Weight entries are encrypted on this phone. Metria has no account, advertising, analytics, or default sync.")
        Text("Optional photo assistance is processed locally and temporary copies are deleted after use.")
    } }
    Button(onClick = changeUnit, modifier = Modifier.fillMaxWidth()) { Text(if (unit == "kg") "Use pounds" else "Use kilograms") }
    Button(onClick = lock, modifier = Modifier.fillMaxWidth()) { Text("Lock vault") }
}

private fun formatWeight(kilograms: Double, unit: String): String = if (unit == "lb") "${"%.1f".format(WeightUnits.kilogramsToPounds(kilograms))} lb" else "${"%.1f".format(kilograms)} kg"

private data class EditableMealItem(val name: String, val grams: String, val preparation: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealConfirmationSheet(proposal: MealProposal, onDismiss: () -> Unit, onSave: (MealEntry) -> Unit) {
    var items by remember(proposal) { mutableStateOf(proposal.items.map { item ->
        EditableMealItem(item.name, ((item.estimatedGramsRange.min + item.estimatedGramsRange.max) / 2).toString(), item.preparation)
    }) }
    var date by remember(proposal) { mutableStateOf(LocalDate.now().toString()) }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(24.dp).padding(bottom = 72.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Confirm meal", style = MaterialTheme.typography.headlineMedium)
            Text("AI estimate - correct each portion and preparation before saving. This is a private meal record, not medical advice.", color = Plum)
            items.forEachIndexed { index, item ->
                OutlinedTextField(item.name, { value -> items = items.toMutableList().also { it[index] = item.copy(name = value) } }, label = { Text("Food") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(item.grams, { value -> items = items.toMutableList().also { it[index] = item.copy(grams = value) } }, label = { Text("Portion in grams") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(item.preparation, { value -> items = items.toMutableList().also { it[index] = item.copy(preparation = value) } }, label = { Text("Preparation (optional)") }, modifier = Modifier.fillMaxWidth())
            }
            Text("Optional nutrition you enter yourself", style = MaterialTheme.typography.titleMedium)
            Text("Metria has no food database yet, so it will not calculate these values for you.", color = Color(0xFF655D6E))
            OutlinedTextField(calories, { calories = it }, label = { Text("Calories (kcal)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(protein, { protein = it }, label = { Text("Protein (g)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(carbs, { carbs = it }, label = { Text("Carbohydrates (g)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(fat, { fat = it }, label = { Text("Fat (g)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(date, { date = it }, label = { Text("Date - YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = Color(0xFFB3261E)) }
            Button(onClick = {
                val recordedAt = runCatching { LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant() }.getOrNull()
                val savedItems = items.map { item -> MealEntryItem(item.name.trim(), item.grams.replace(',', '.').toIntOrNull(), item.preparation.trim().takeIf(String::isNotEmpty)) }
                val rawNutrition = listOf(calories, protein, carbs, fat)
                val nutritionValues = rawNutrition.map { it.replace(',', '.').toDoubleOrNull() }
                val containsInvalidNutrition = rawNutrition.zip(nutritionValues).any { (raw, value) -> raw.isNotBlank() && value == null }
                val nutrition = if (rawNutrition.all(String::isBlank)) null else MealNutrition(nutritionValues[0], nutritionValues[1], nutritionValues[2], nutritionValues[3])
                val meal = if (recordedAt == null) null else MealEntry(recordedAt = recordedAt, items = savedItems, assumptions = proposal.assumptions, confidence = proposal.overallConfidence, source = "ai_estimate", nutrition = nutrition)
                if (containsInvalidNutrition || meal == null || !MealEntryValidation.valid(meal)) error = "Review the food names, portions (1-2,000 g), nutrition values, and date." else onSave(meal)
            }, modifier = Modifier.fillMaxWidth()) { Text("Save confirmed meal locally") }
        }
    }
}

private fun nutritionSummary(nutrition: MealNutrition): String = listOfNotNull(
    nutrition.caloriesKcal?.let { "${"%.0f".format(it)} kcal" },
    nutrition.proteinGrams?.let { "${"%.1f".format(it)} g protein" },
    nutrition.carbohydrateGrams?.let { "${"%.1f".format(it)} g carbs" },
    nutrition.fatGrams?.let { "${"%.1f".format(it)} g fat" },
).joinToString(" - ").ifBlank { "No nutrition values entered." }

@Composable
private fun MiniTrend(points: List<WeightTrendPoint>) {
    Canvas(Modifier.fillMaxWidth().height(130.dp)) {
        val low = points.minOf(WeightTrendPoint::kilograms)
        val span = (points.maxOf(WeightTrendPoint::kilograms) - low).takeIf { it > 0 } ?: 1.0
        val step = size.width / (points.size - 1)
        for (index in 1 until points.size) drawLine(Plum, Offset((index - 1) * step, size.height - (((points[index - 1].kilograms - low) / span) * size.height).toFloat()), Offset(index * step, size.height - (((points[index].kilograms - low) / span) * size.height).toFloat()), 7f)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightBottomSheet(entry: WeightEntry?, voiceDraft: VoiceWeightDraft?, onDismiss: () -> Unit, save: (Double, String?, Instant) -> Unit) {
    var value by remember(entry, voiceDraft) { mutableStateOf(entry?.kilograms?.toString() ?: voiceDraft?.kilograms?.toString().orEmpty()) }; var note by remember(entry) { mutableStateOf(entry?.note.orEmpty()) }; var date by remember(entry, voiceDraft) { mutableStateOf(entry?.recordedAt?.atZone(ZoneId.systemDefault())?.toLocalDate()?.toString() ?: voiceDraft?.recordedAt?.atZone(ZoneId.systemDefault())?.toLocalDate()?.toString().orEmpty()) }; var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) { Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (entry == null) "Record weight" else "Edit weight", style = MaterialTheme.typography.headlineMedium)
        if (voiceDraft != null) Text("Voice draft — review before saving.", color = Plum)
        Text("Saved only after you confirm.", color = Color(0xFF655D6E))
        OutlinedTextField(value, { value = it }, label = { Text("Weight in kg") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(date, { date = it }, label = { Text("Date · YYYY-MM-DD (optional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(note, { note = it }, label = { Text("A private note (optional)") }, modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = Color(0xFFB3261E)) }
        Button(onClick = { val kg = value.replace(',', '.').toDoubleOrNull(); val instant = if (date.isBlank()) Instant.now() else runCatching { LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant() }.getOrNull(); if (kg == null || instant == null || !WeightUnits.isValidKilograms(kg)) error = "Enter 15–500 kg and a valid optional date." else save(kg, note, instant) }, modifier = Modifier.fillMaxWidth()) { Text("Save locally") }
    } }
}

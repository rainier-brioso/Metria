package app.metria.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.metria.MealPhotoContext
import app.metria.ui.components.MetriaMealThumbnail
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.CheckCircle

@Composable
fun MealPhotoAnalysisProgress(title: String, detail: String, onStop: () -> Unit) {
    BackHandler(onBack = onStop)
    Card {
        // Assistant's parent owns scrolling. A nested vertical scroller here is
        // measured with infinite height and crashes when analysis begins.
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text("Preparing your meal…", style = MaterialTheme.typography.headlineSmall)
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Stop analysis") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPhotoSourceSheet(purpose: String, onDismiss: () -> Unit, onTakePhoto: () -> Unit, onChoosePhoto: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (purpose == "label") "Nutrition label" else "Meal photo", style = MaterialTheme.typography.headlineMedium)
            Text("Choose a photo to preview. Nothing is analyzed until you ask.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Take photo")
            }
            OutlinedButton(onClick = onChoosePhoto, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Choose from library")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPhotoPreviewSheet(
    purpose: String,
    thumbnail: String?,
    imageState: String,
    description: String,
    onDescriptionChange: (String) -> Unit,
    onPurposeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onReplace: () -> Unit,
    onAnalyze: () -> Unit,
) {
    var showTips by rememberSaveable { mutableStateOf(false) }
    val ready = imageState == "ready"
    val label = purpose == "label"
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(if (label) "Preview label" else "Preview meal", style = MaterialTheme.typography.headlineMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !label, onClick = { onPurposeChange("meal") }, label = { Text("Meal photo") }, modifier = Modifier.heightIn(min = 48.dp))
                FilterChip(selected = label, onClick = { onPurposeChange("label") }, label = { Text("Nutrition label") }, modifier = Modifier.heightIn(min = 48.dp))
            }
            if (ready) {
                MetriaMealThumbnail(encoded = thumbnail, size = 192.dp, contentScale = ContentScale.Fit)
                Text("Preview of your private photo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (imageState == "preparing") {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Preparing your photo…")
            } else {
                Text("Couldn’t prepare this photo. Try another image.", color = MaterialTheme.colorScheme.error)
            }
            TextButton(onClick = { showTips = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Rounded.Info, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Photo tips")
            }
            if (!label) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= MealPhotoContext.MAX_LENGTH && it.none { c -> c.isISOControl() && c !in "\n\r\t" }) onDescriptionChange(it) },
                    label = { Text("Meal details (optional)") },
                    placeholder = { Text("For example: chicken with rice, sauce on the side") },
                    supportingText = { Text("${description.length}/${MealPhotoContext.MAX_LENGTH} · Used for this draft only") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
            Text("Processed on this phone. Review the draft before saving.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onAnalyze, enabled = ready, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (label) "Read label" else "Create meal draft")
            }
            OutlinedButton(onClick = onReplace, enabled = imageState != "preparing", modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Use another photo") }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cancel") }
        }
    }
    if (showTips) {
        AlertDialog(
            onDismissRequest = { showTips = false },
            icon = { Icon(Icons.Rounded.CameraAlt, contentDescription = null) },
            title = { Text(if (label) "Make the label readable" else "Help Metria see your meal") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val tips = if (label) listOf(
                        "Include the serving size and full nutrition panel.",
                        "Use good light and avoid glare.",
                        "Keep the text sharp and readable.",
                    ) else listOf(
                        "Keep the whole plate in frame.",
                        "Use good light and hold the camera steady.",
                        "Show separate dishes clearly.",
                    )
                    tips.forEach { tip ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text(tip, modifier = Modifier.weight(1f))
                        }
                    }
                    if (!label) Text("Oils, sauces or fillings may be hidden. Add meal details, then check the draft.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = { showTips = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Done") }
            },
        )
    }
}

package app.metria.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.metria.ui.theme.MetriaColors
import app.metria.ui.theme.MetriaTheme

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun FirstRunOnboarding(
    selectedMode: String,
    onModeChange: (String) -> Unit,
    onComplete: () -> Unit,
    initialPage: Int = 0,
) {
    var page by rememberSaveable { mutableStateOf(initialPage.coerceIn(0, 2)) }
    val pages = listOf(
        Triple("Your health data stays with you", "Metria has no account and no default cloud sync. Your entries are encrypted on this phone.", "Private by default"),
        Triple("You stay in control", "Your fingerprint unlocks the vault. Create a recovery archive later if you want a way back after device loss or a biometric change.", "Protected on this phone"),
        Triple("Choose your assistant", "Use Gemma on this phone or Nemotron through the cloud. Cloud chat sends your new message and up to 9 earlier messages you sent to Metria's broker and Nebius. Meal photos require a separate confirmation before sending.", "Your choice"),
    )
    val current = pages[page]
    Surface(color = MetriaColors.Canvas, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("METRIA", color = MetriaColors.Primary, style = MaterialTheme.typography.labelLarge)
                Text("${page + 1} of ${pages.size}", color = MetriaColors.InkMuted, style = MaterialTheme.typography.labelMedium)
                Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.PrimarySoft)) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(current.third, color = MetriaColors.Primary, style = MaterialTheme.typography.labelLarge)
                        Text(current.first, style = MaterialTheme.typography.headlineMedium, color = MetriaColors.Ink)
                        Text(current.second, color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (page == pages.lastIndex) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selectedMode == "local", { onModeChange("local") }, label = { Text("Gemma · this phone") }, modifier = Modifier.heightIn(min = 48.dp))
                        FilterChip(selectedMode == "cloud", { onModeChange("cloud") }, label = { Text("Nemotron · cloud") }, modifier = Modifier.heightIn(min = 48.dp))
                    }
                    Text(
                        if (selectedMode == "local") "Local setup needs about 2.6 GB and works offline."
                        else "Cloud mode needs an internet connection and a private evaluator pass. Confirm each meal photo before it is sent.",
                        color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { index ->
                        Surface(
                            color = if (index == page) MetriaColors.Primary else MetriaColors.Divider,
                            shape = CircleShape,
                            modifier = Modifier.weight(1f).height(6.dp),
                        ) {}
                    }
                }
                Button(onClick = { if (page == pages.lastIndex) onComplete() else page += 1 }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (page == pages.lastIndex) "Continue with ${if (selectedMode == "cloud") "Nemotron" else "Gemma"}" else "Continue")
                }
                if (page > 0) Button(onClick = { page -= 1 }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        }
    }
}

@Preview(name = "Assistant choice onboarding", showBackground = true)
@Composable
private fun AssistantChoiceOnboardingPreview() {
    MetriaTheme {
        FirstRunOnboarding(selectedMode = "cloud", onModeChange = {}, onComplete = {}, initialPage = 2)
    }
}

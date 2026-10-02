package app.metria.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.metria.ui.theme.MetriaColors

/** A rejected vision draft never supplies foods to the member's manual review. */
@Composable
fun MealAnalysisFallbackCard(onEnterManually: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface)) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Couldn't make a reliable meal draft", style = MaterialTheme.typography.titleMedium)
            Text(
                "Nothing was saved. Add the foods you recognise and review their portions yourself.",
                color = MetriaColors.InkMuted,
            )
            Button(onClick = onEnterManually, modifier = Modifier.fillMaxWidth()) {
                Text("Enter meal manually")
            }
        }
    }
}

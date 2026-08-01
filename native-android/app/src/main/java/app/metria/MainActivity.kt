package app.metria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var modelLifecycle: ModelLifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        modelLifecycle = ModelLifecycle(filesDir)
        setContent { MetriaApp(modelLifecycle) }
    }

    override fun onDestroy() {
        modelLifecycle.close()
        super.onDestroy()
    }
}

@Composable
private fun MetriaApp(modelLifecycle: ModelLifecycle) {
    var status by remember { mutableStateOf(modelLifecycle.status()) }
    LaunchedEffect(modelLifecycle) {
        while (true) {
            status = modelLifecycle.status()
            delay(500)
        }
    }
    DisposableEffect(modelLifecycle) {
        onDispose { /* Activity owns lifecycle shutdown. */ }
    }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Metria", style = MaterialTheme.typography.displaySmall)
                Text(
                    "Native Android vision spike",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("Model: ${status.state}", modifier = Modifier.padding(top = 24.dp))
                Text(status.reason, modifier = Modifier.padding(top = 8.dp))
                Text("Storage: ${status.downloadedBytes} / ${status.expectedBytes} bytes")
                Button(onClick = { status = modelLifecycle.download() }) {
                    Text("Download model")
                }
                Button(onClick = { status = modelLifecycle.cancelDownload() }) {
                    Text("Cancel download")
                }
                Button(onClick = { status = modelLifecycle.verifyInstalledModel() }) {
                    Text("Verify stored model")
                }
                Button(onClick = { status = modelLifecycle.initializeRuntime() }) {
                    Text("Initialize LiteRT")
                }
                Button(onClick = { status = modelLifecycle.unloadRuntime() }) {
                    Text("Unload LiteRT")
                }
                Button(onClick = { status = modelLifecycle.remove() }) {
                    Text("Remove model")
                }
            }
        }
    }
}

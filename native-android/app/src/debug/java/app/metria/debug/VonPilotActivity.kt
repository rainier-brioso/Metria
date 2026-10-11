package app.metria.debug

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.exp

/** Debug-only, synthetic-data ONNX smoke pilot. It is never part of a member flow. */
class VonPilotActivity : Activity() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val cancelRequested = AtomicBoolean(false)
    @Volatile private var activityStarted = false
    @Volatile private var activityDestroyed = false
    @Volatile private var lifecycleGeneration = 0L
    @Volatile private var workerThread: Thread? = null
    private lateinit var statusView: TextView
    private lateinit var reportView: TextView
    private lateinit var runButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Local model runtime pilot"

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(dp(20) + bars.left, dp(20) + bars.top, dp(20) + bars.right, dp(20) + bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(content)
        val scroll = ScrollView(this)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        column.addView(TextView(this).apply {
            text = "Synthetic runtime test. Uses fixed fictional examples only; it does not read saved data, open chat, or create a chat response. Per-case reference temperatures are frozen and do not port dynamic calibration."
            textSize = 18f
            contentDescription = text
        }, matchWidth())
        statusView = TextView(this).apply {
            text = "Ready. Install the ONNX model under app-private files/decision-model/. Eight synthetic cases are bundled in this debug build."
            textSize = 16f
            setPadding(0, dp(16), 0, dp(12))
            contentDescription = text
        }
        column.addView(statusView, matchWidth())
        runButton = Button(this).apply {
            text = "Run synthetic test"
            minHeight = dp(48)
            setOnClickListener { startPilot() }
        }
        column.addView(runButton, matchWidth())
        reportView = TextView(this).apply {
            textSize = 14f
            setTextIsSelectable(true)
            setPadding(0, dp(16), 0, 0)
        }
        column.addView(reportView, matchWidth())
        scroll.addView(column)
        content.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(content)
    }

    override fun onStart() {
        super.onStart()
        activityStarted = true
        runButton.isEnabled = !PROCESS_WORKER_ACTIVE.get()
        if (PROCESS_WORKER_ACTIVE.get()) {
            statusView.text = "A previous run is stopping. This screen does not start a second run."
            scheduleBusyRefresh()
        } else if (reportView.text.isEmpty()) {
            statusView.text = "Ready. This synthetic runtime test does not read saved data or chat."
        }
    }

    override fun onStop() {
        activityStarted = false
        lifecycleGeneration += 1
        mainHandler.removeCallbacksAndMessages(null)
        cancelRequested.set(true)
        workerThread?.interrupt()
        // ORT may not stop an in-progress native call immediately. The worker owns and
        // closes its session/tensors after that call returns; the screen drops callbacks.
        super.onStop()
    }

    override fun onDestroy() {
        activityDestroyed = true
        activityStarted = false
        mainHandler.removeCallbacksAndMessages(null)
        cancelRequested.set(true)
        workerThread?.interrupt()
        super.onDestroy()
    }

    private fun startPilot() {
        if (!activityStarted || activityDestroyed) return
        if (!PROCESS_WORKER_ACTIVE.compareAndSet(false, true)) {
            statusView.text = "A previous run is still stopping. Please wait."
            scheduleBusyRefresh()
            return
        }
        cancelRequested.set(false)
        runButton.isEnabled = false
        reportView.text = ""
        statusView.text = "Checking the local model file. The checksum pass may take a while."
        val directory = File(filesDir, "decision-model")
        val runGeneration = lifecycleGeneration
        val startedAt = SystemClock.elapsedRealtimeNanos()
        PILOT_EXECUTOR.submit {
            workerThread = Thread.currentThread()
            try {
                checkInterrupted()
                val report = runInference(directory, startedAt)
                checkInterrupted()
                File(directory, "pilot-result.txt").writeText(report, Charsets.UTF_8)
                publishResult(report, null, startedAt, runGeneration)
            } catch (error: InterruptedException) {
                Thread.currentThread().interrupt()
                publishResult(null, "Run cancelled.", startedAt, runGeneration)
            } catch (error: Exception) {
                // This process handles only bundled fictional inputs and a public model.
                val failure = "Runtime smoke failed (${error.javaClass.simpleName}): ${error.message.orEmpty().take(400)}"
                runCatching { File(directory, "pilot-result.txt").writeText(failure, Charsets.UTF_8) }
                publishResult(null, failure, startedAt, runGeneration)
            } finally {
                workerThread = null
                PROCESS_WORKER_ACTIVE.set(false)
                postUi {
                    runButton.isEnabled = !PROCESS_WORKER_ACTIVE.get()
                    if (reportView.text.isEmpty() && statusView.text.contains("stopping", ignoreCase = true)) {
                        statusView.text = "Previous run finished. Ready for another synthetic test."
                    }
                }
            }
        }
    }

    private fun runInference(directory: File, totalStart: Long): String {
        checkInterrupted()
        val model = File(directory, MODEL_NAME)
        require(model.isFile) { "model missing" }
        require(model.length() == MODEL_BYTES) { "model size mismatch" }
        val actualHash = sha256(model)
        require(actualHash == MODEL_SHA256) { "model checksum mismatch" }
        val cases = parseCases()
        val environment = OrtEnvironment.getEnvironment()
        val sessionOptions = OrtSession.SessionOptions()
        sessionOptions.setIntraOpNumThreads(4)
        sessionOptions.setInterOpNumThreads(1)
        sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
        val loadStart = SystemClock.elapsedRealtimeNanos()
        val session = try {
            environment.createSession(model.absolutePath, sessionOptions)
        } finally {
            sessionOptions.close()
        }
        val loadMs = elapsedMs(loadStart)
        try {
            require(session.inputNames == EXPECTED_INPUTS) { "model input names mismatch" }
            require(session.outputNames == setOf("logits")) { "model output name mismatch" }
            val lines = mutableListOf<String>()
            var allParity = true
            for (case in cases) {
                checkInterrupted()
                val runStart = SystemClock.elapsedRealtimeNanos()
                val inputs = createTensors(environment, case)
                val logits: FloatArray
                try {
                    session.run(inputs).use { result ->
                        val output = result["logits"].orElseThrow { IllegalStateException("logits missing") }
                        val tensor = output as? OnnxTensor ?: error("logits are not a tensor")
                        val tensorInfo = tensor.info as? TensorInfo ?: error("logits tensor info missing")
                        require(tensorInfo.shape.contentEquals(longArrayOf(1L, 2L))) { "unexpected logits shape" }
                        val values = tensor.value as? Array<*> ?: error("logits tensor value shape invalid")
                        val row = values.single() as? FloatArray ?: error("logits row invalid")
                        require(row.size == 2 && row.all(Float::isFinite)) { "logits are invalid" }
                        logits = row.copyOf()
                    }
                } finally {
                    inputs.values.forEach { it.close() }
                }
                val runtimeMs = elapsedMs(runStart)
                val parityError = maxAbsDifference(logits, case.referenceLogits)
                val referenceWinner = argmax(case.referenceLogits)
                val modelWinner = argmax(logits)
                val parityPass = parityError <= MAX_LOGIT_ERROR && modelWinner == referenceWinner
                allParity = allParity && parityPass
                val probabilities = stableSoftmax(logits, case.temperature)
                val confidence = probabilities.maxOrNull() ?: 0f
                val route = if (confidence >= CONFIDENCE_CUTOFF && modelWinner == 0) {
                    TODAY_MEAL_LIST
                } else {
                    FALLBACK
                }
                val correct = route == case.expected
                lines += buildString {
                    append("Case ").append(case.id).append(" [").append(case.text).append("]: ")
                    append(if (correct) "correct" else "incorrect").append("; ")
                    append("route=").append(route).append("; expected=").append(case.expected).append("; ")
                    append("confidence=").append(format(confidence)).append("; ")
                    append("logitParity=").append(if (parityPass) "pass" else "fail")
                    append(" (max abs ").append(format(parityError)).append("); ")
                    append("runtimeMs=").append(format(runtimeMs))
                }
                checkInterrupted()
            }
            checkInterrupted()
            val thermal = thermalStatus()
            val pssKb = Debug.getPss()
            return buildString {
                appendLine("Model checksum: verified")
                appendLine("ONNX session loadMs: ${format(loadMs)}")
                appendLine("Total click-to-result elapsedMs (includes SHA-256 verification): ${format(elapsedMs(totalStart))}")
                appendLine("Process PSS: $pssKb KB")
                appendLine("Thermal status: $thermal")
                appendLine("Logit parity: ${if (allParity) "pass" else "fail"} (max abs tolerance $MAX_LOGIT_ERROR; winner must match reference)")
                appendLine("Route rule: confidence >= $CONFIDENCE_CUTOFF and winner today_meal_list; otherwise fallback")
                lines.forEach(::appendLine)
                append("Synthetic test only. No saved data or chat was used.")
            }
        } finally {
            session.close()
        }
    }

    private fun createTensors(environment: OrtEnvironment, case: PilotCase): LinkedHashMap<String, OnnxTensor> {
        val result = linkedMapOf<String, OnnxTensor>()
        try {
            for (name in EXPECTED_INPUTS) {
                checkInterrupted()
                val input = case.tensors.getValue(name)
                result[name] = if (input.dtype == "int64") {
                    OnnxTensor.createTensor(environment, LongBuffer.wrap(input.longs!!), input.shape)
                } else {
                    OnnxTensor.createTensor(environment, FloatBuffer.wrap(input.floats!!), input.shape)
                }
            }
            return result
        } catch (error: Throwable) {
            result.values.forEach { it.close() }
            throw error
        }
    }

    private fun parseCases(): List<PilotCase> {
        checkInterrupted()
        val bytes = assets.open(CASES_ASSET).use { stream ->
            val content = stream.readBytes()
            require(content.size in 1..MAX_CASE_FILE_BYTES) { "pilot asset size invalid" }
            content
        }
        val root = JSONObject(String(bytes, Charsets.UTF_8))
        require(root.optString("model_sha256") == MODEL_SHA256) { "pilot model checksum does not match" }
        require(root.optString("scope").startsWith("Fixed fictional runtime/parity smoke test")) { "pilot scope invalid" }
        val rawCases = root.optJSONArray("cases") ?: error("cases array missing")
        require(rawCases.length() == PILOT_CASE_COUNT) { "case count invalid" }
        return (0 until rawCases.length()).map { index ->
            checkInterrupted()
            val raw = rawCases.getJSONObject(index)
            val id = raw.getString("id")
            require(id.matches(Regex("[A-Za-z0-9_-]{1,48}"))) { "case id invalid" }
            val text = raw.getString("text")
            require(text.length in 1..MAX_TEXT_LENGTH) { "synthetic case text invalid" }
            require(text.none { it.code < 0x20 || it.code == 0x7f }) { "synthetic case text invalid" }
            val expected = raw.getString("expected")
            require(expected == TODAY_MEAL_LIST || expected == FALLBACK) { "expected route invalid" }
            val temperature = raw.getDouble("temperature").toFloat()
            require(temperature.isFinite() && temperature > 0f) { "temperature invalid" }
            val refs = raw.getJSONArray("reference_logits")
            require(refs.length() == 2) { "reference logits must contain two values" }
            val referenceLogits = FloatArray(2) { refs.getDouble(it).toFloat() }
            require(referenceLogits.all(Float::isFinite)) { "reference logits invalid" }
            val rawTensors = raw.getJSONObject("tensors")
            require(rawTensors.length() == EXPECTED_INPUTS.size) { "tensor count invalid" }
            val tensors = EXPECTED_INPUTS.associateWith { name -> parseTensor(rawTensors.getJSONObject(name)) }
            require(tensors.getValue("input_ids").dtype == "int64") { "input_ids dtype mismatch" }
            require(tensors.getValue("position_ids").dtype == "int64") { "position_ids dtype mismatch" }
            require(tensors.getValue("marker_positions").dtype == "int64") { "marker_positions dtype mismatch" }
            require(tensors.getValue("full_mask").dtype == "float32") { "full_mask dtype mismatch" }
            require(tensors.getValue("sliding_mask").dtype == "float32") { "sliding_mask dtype mismatch" }
            val idsShape = tensors.getValue("input_ids").shape
            require(idsShape.size == 2 && idsShape[0] == 1L && idsShape[1] in 1..MAX_SEQUENCE.toLong()) { "input_ids shape invalid" }
            val sequenceLength = idsShape[1]
            require(tensors.getValue("position_ids").shape.contentEquals(idsShape)) { "position_ids shape mismatch" }
            require(tensors.getValue("marker_positions").shape.contentEquals(longArrayOf(1L, 2L))) { "marker_positions shape invalid" }
            for (marker in tensors.getValue("marker_positions").longs!!) {
                require(marker >= 0L && marker < sequenceLength) { "marker position outside input sequence" }
            }
            val expectedMaskShape = longArrayOf(1L, 1L, sequenceLength, sequenceLength)
            require(tensors.getValue("full_mask").shape.contentEquals(expectedMaskShape)) { "full_mask shape mismatch" }
            require(tensors.getValue("sliding_mask").shape.contentEquals(expectedMaskShape)) { "sliding_mask shape mismatch" }
            PilotCase(id, text, expected, temperature, referenceLogits, tensors)
        }
    }

    private fun parseTensor(value: JSONObject): PilotTensor {
        val dtype = value.getString("dtype")
        require(dtype == "int64" || dtype == "float32") { "tensor dtype unsupported" }
        val shapeArray = value.getJSONArray("shape")
        require(shapeArray.length() in 1..8) { "tensor rank invalid" }
        val shape = LongArray(shapeArray.length()) { shapeArray.getLong(it) }
        require(shape.all { it > 0 }) { "tensor shape invalid" }
        val count = shape.fold(1L) { product, dimension ->
            require(product <= MAX_TENSOR_ELEMENTS / dimension) { "tensor too large" }
            product * dimension
        }.toInt()
        val rawData = value.getJSONArray("data")
        require(rawData.length() == count) { "tensor data length mismatch" }
        return if (dtype == "int64") {
            PilotTensor(dtype, shape, LongArray(count) { rawData.getLong(it) }, null)
        } else {
            val floats = FloatArray(count) { rawData.getDouble(it).toFloat() }
            require(floats.all(Float::isFinite)) { "tensor contains non-finite values" }
            PilotTensor(dtype, shape, null, floats)
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        BufferedInputStream(FileInputStream(file), HASH_BUFFER_BYTES).use { input ->
            val buffer = ByteArray(HASH_BUFFER_BYTES)
            while (true) {
                checkInterrupted()
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun publishResult(report: String?, status: String?, startedAt: Long, generation: Long) {
        postUi(generation = generation) {
            reportView.text = report.orEmpty()
            statusView.text = status ?: "Synthetic runtime test complete in ${format(elapsedMs(startedAt))} ms."
        }
    }

    private fun postUi(generation: Long? = null, action: () -> Unit) {
        if (activityDestroyed || !activityStarted) return
        mainHandler.post {
            if (!activityDestroyed && activityStarted && (generation == null || generation == lifecycleGeneration)) action()
        }
    }

    private fun scheduleBusyRefresh() {
        if (!activityStarted || activityDestroyed || !PROCESS_WORKER_ACTIVE.get()) return
        mainHandler.postDelayed({
            if (activityDestroyed || !activityStarted) return@postDelayed
            val active = PROCESS_WORKER_ACTIVE.get()
            runButton.isEnabled = !active
            if (active) {
                statusView.text = "A previous run is stopping. This screen does not start a second run."
                scheduleBusyRefresh()
            } else if (reportView.text.isEmpty()) {
                statusView.text = "Previous run finished. Ready for another synthetic test."
            }
        }, BUSY_POLL_MS)
    }

    private fun thermalStatus(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "unavailable (Android below 10)"
        val power = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return "unavailable"
        return power.currentThermalStatus.toString()
    }

    private fun stableSoftmax(logits: FloatArray, temperature: Float): FloatArray {
        val scaled = FloatArray(logits.size) { logits[it] / temperature }
        val maximum = scaled.maxOrNull() ?: return FloatArray(0)
        val values = FloatArray(scaled.size) { exp((scaled[it] - maximum).toDouble()).toFloat() }
        val sum = values.sum()
        require(sum.isFinite() && sum > 0f) { "softmax invalid" }
        return FloatArray(values.size) { values[it] / sum }
    }

    private fun maxAbsDifference(left: FloatArray, right: FloatArray): Float {
        require(left.size == right.size) { "logit count mismatch" }
        return left.indices.maxOf { kotlin.math.abs(left[it] - right[it]) }
    }

    private fun argmax(values: FloatArray): Int = values.indices.maxBy { values[it] }

    private fun checkInterrupted() {
        if (cancelRequested.get() || Thread.currentThread().isInterrupted) throw InterruptedException()
    }

    private fun elapsedMs(start: Long): Double = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0

    private fun format(value: Double): String = String.format(Locale.US, "%.3f", value)
    private fun format(value: Float): String = String.format(Locale.US, "%.6f", value)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun matchWidth() = LinearLayout.LayoutParams(-1, -2)

    private data class PilotCase(
        val id: String,
        val text: String,
        val expected: String,
        val temperature: Float,
        val referenceLogits: FloatArray,
        val tensors: Map<String, PilotTensor>
    )

    private data class PilotTensor(
        val dtype: String,
        val shape: LongArray,
        val longs: LongArray?,
        val floats: FloatArray?
    )

    private companion object {
        val PILOT_EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor { task ->
            Thread(task, "von-android-pilot").apply { isDaemon = true }
        }
        val PROCESS_WORKER_ACTIVE = AtomicBoolean(false)
        const val BUSY_POLL_MS = 300L
        const val MODEL_NAME = "von-option-marker.onnx"
        const val CASES_ASSET = "decision-model/pilot.json"
        const val MODEL_BYTES = 1_582_047_027L
        const val MODEL_SHA256 = "949c7be11a8a2225999089651c34a7dcc88567d8d0c906a67d99eb45cf5a7482"
        const val MAX_LOGIT_ERROR = 0.002f
        const val CONFIDENCE_CUTOFF = 0.8f
        const val TODAY_MEAL_LIST = "today_meal_list"
        const val FALLBACK = "fallback"
        const val PILOT_CASE_COUNT = 8
        const val MAX_TEXT_LENGTH = 500
        const val MAX_SEQUENCE = 512
        const val MAX_TENSOR_ELEMENTS = 2_000_000L
        const val MAX_CASE_FILE_BYTES = 64 * 1024 * 1024
        const val HASH_BUFFER_BYTES = 1024 * 1024
        val EXPECTED_INPUTS = setOf("input_ids", "full_mask", "sliding_mask", "position_ids", "marker_positions")
    }
}

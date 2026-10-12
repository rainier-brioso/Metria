package app.metria.debug

import android.content.Context
import app.metria.MiniLmMealReadPolicy
import app.metria.MiniLmRouteDecision
import app.metria.MiniLmRouteOutcome
import app.metria.MiniLmRouteScoring
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import java.io.File
import java.io.InputStream
import java.nio.LongBuffer
import java.security.MessageDigest
import kotlin.math.sqrt

/**
 * Debug-only, local MiniLM prototype router for bounded meal-list and nutrition reads.
 * Create/use this away from the UI thread. Input text is never logged or persisted.
 */
internal class MiniLmMealRouter(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val session: OrtSession
    private val tokenizer: MiniLmWordPieceTokenizer
    private val mealListPrototypes: List<FloatArray>
    private val latestMealPrototypes: List<FloatArray>
    private val todayNutritionPrototypes: List<FloatArray>
    private val fallbackPrototypes: List<FloatArray>
    private var closed = false

    init {
        val vocabBytes = appContext.assets.open(VOCAB_ASSET).use { it.readBytesBounded() }
        require(sha256(vocabBytes) == VOCAB_SHA256) { "MiniLM vocabulary checksum mismatch" }
        tokenizer = MiniLmWordPieceTokenizer.fromVocabulary(vocabBytes.inputStream())
        val modelFile = privateModelFile(appContext)
        val environment = OrtEnvironment.getEnvironment()
        val options = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(2)
            setInterOpNumThreads(1)
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
        }
        val created = try {
            environment.createSession(modelFile.absolutePath, options)
        } finally {
            options.close()
        }
        try {
            validateSession(created)
            session = created
            val examples = MEAL_LIST_EXAMPLES + LATEST_MEAL_EXAMPLES + TODAY_NUTRITION_EXAMPLES + FALLBACK_EXAMPLES
            val vectors = embed(examples)
            mealListPrototypes = vectors.take(MEAL_LIST_EXAMPLES.size)
            latestMealPrototypes = vectors.subList(
                MEAL_LIST_EXAMPLES.size,
                MEAL_LIST_EXAMPLES.size + LATEST_MEAL_EXAMPLES.size,
            )
            todayNutritionPrototypes = vectors.subList(
                MEAL_LIST_EXAMPLES.size + LATEST_MEAL_EXAMPLES.size,
                MEAL_LIST_EXAMPLES.size + LATEST_MEAL_EXAMPLES.size + TODAY_NUTRITION_EXAMPLES.size,
            )
            fallbackPrototypes = vectors.drop(
                MEAL_LIST_EXAMPLES.size + LATEST_MEAL_EXAMPLES.size + TODAY_NUTRITION_EXAMPLES.size,
            )
        } catch (error: Throwable) {
            created.close()
            throw error
        }
    }

    /** Returns scores only for bounded, content-free diagnostics; input is never retained. */
    @Synchronized
    fun routeMealRead(text: String, toolName: String): MiniLmRouteDecision {
        if (closed) return MiniLmRouteDecision(MiniLmRouteOutcome.RUNTIME_UNAVAILABLE)
        if (Thread.currentThread().isInterrupted) return MiniLmRouteDecision(MiniLmRouteOutcome.INTERRUPTED)
        if (!MiniLmMealReadPolicy.isInScope(text, toolName)) {
            if (toolName != "read_day_nutrition" || !MiniLmMealReadPolicy.isTodayNutritionInScope(text)) {
                return MiniLmRouteDecision(MiniLmRouteOutcome.SCOPE_REJECTED)
            }
        }
        return try {
            val query = embed(listOf(text)).single()
            if (Thread.currentThread().isInterrupted) return MiniLmRouteDecision(MiniLmRouteOutcome.INTERRUPTED)
            val (positiveExamples, competingExamples) = when (toolName) {
                "read_meals" -> mealListPrototypes to (latestMealPrototypes + todayNutritionPrototypes)
                "read_latest_meal" -> latestMealPrototypes to (mealListPrototypes + todayNutritionPrototypes)
                "read_day_nutrition" -> todayNutritionPrototypes to (mealListPrototypes + latestMealPrototypes)
                else -> return MiniLmRouteDecision(MiniLmRouteOutcome.SCOPE_REJECTED)
            }
            val positive = positiveExamples.maxOfOrNull { cosine(query, it) }
                ?: return MiniLmRouteDecision(MiniLmRouteOutcome.INFERENCE_FAILED)
            val competing = competingExamples.maxOfOrNull { cosine(query, it) }
                ?: return MiniLmRouteDecision(MiniLmRouteOutcome.INFERENCE_FAILED)
            val fallback = fallbackPrototypes.maxOfOrNull { cosine(query, it) }
                ?: return MiniLmRouteDecision(MiniLmRouteOutcome.INFERENCE_FAILED)
            MiniLmRouteScoring.decide(positive, competing, fallback)
        } catch (_: Exception) {
            MiniLmRouteDecision(MiniLmRouteOutcome.INFERENCE_FAILED)
        }
    }

    @Synchronized
    override fun close() {
        if (!closed) {
            closed = true
            session.close()
        }
    }

    private fun embed(texts: List<String>): List<FloatArray> {
        require(texts.isNotEmpty())
        val rows = texts.map { text -> tokenizer.encode(text) ?: error("Text cannot be tokenized safely") }
        val width = rows.maxOf { it.inputIds.size }
        val inputIds = LongArray(rows.size * width) { tokenizer.padTokenId }
        val attentionMask = LongArray(rows.size * width)
        val tokenTypeIds = LongArray(rows.size * width)
        rows.forEachIndexed { row, encoded ->
            val offset = row * width
            encoded.inputIds.copyInto(inputIds, offset)
            encoded.attentionMask.copyInto(attentionMask, offset)
            encoded.tokenTypeIds.copyInto(tokenTypeIds, offset)
        }
        val shape = longArrayOf(rows.size.toLong(), width.toLong())
        val values = linkedMapOf<String, OnnxTensor>()
        try {
            for (name in session.inputNames) {
                val data = when (name) {
                    "input_ids" -> inputIds
                    "attention_mask" -> attentionMask
                    "token_type_ids" -> tokenTypeIds
                    else -> error("Unexpected MiniLM input")
                }
                values[name] = OnnxTensor.createTensor(environment, LongBuffer.wrap(data), shape)
            }
            require("input_ids" in values && "attention_mask" in values) { "MiniLM input contract mismatch" }
            session.run(values).use { result ->
                val outputName = session.outputNames.single()
                val output = result[outputName].orElseThrow { IllegalStateException("MiniLM output missing") }
                val tensor = output as? OnnxTensor ?: error("MiniLM output is not a tensor")
                val info = tensor.info as? TensorInfo ?: error("MiniLM tensor metadata missing")
                require(info.shape.size == 3 && info.shape[0] == rows.size.toLong() &&
                    info.shape[1] == width.toLong() && info.shape[2] == EMBEDDING_DIMENSION.toLong()) {
                    "MiniLM output shape mismatch"
                }
                val batch = tensor.value as? Array<*> ?: error("MiniLM batch output invalid")
                require(batch.size == rows.size) { "MiniLM batch output size mismatch" }
                return batch.mapIndexed { rowIndex, rawSequence ->
                    val sequence = rawSequence as? Array<*> ?: error("MiniLM sequence output invalid")
                    require(sequence.size == width) { "MiniLM sequence output size mismatch" }
                    val pooled = DoubleArray(EMBEDDING_DIMENSION)
                    var tokenCount = 0
                    for (tokenIndex in rows[rowIndex].attentionMask.indices) {
                        if (rows[rowIndex].attentionMask[tokenIndex] == 0L) continue
                        val token = sequence[tokenIndex] as? FloatArray ?: error("MiniLM hidden state invalid")
                        require(token.size == EMBEDDING_DIMENSION && token.all(Float::isFinite)) {
                            "MiniLM hidden state invalid"
                        }
                        for (dimension in pooled.indices) pooled[dimension] += token[dimension].toDouble()
                        tokenCount++
                    }
                    require(tokenCount > 0) { "MiniLM received an empty sentence" }
                    val mean = FloatArray(EMBEDDING_DIMENSION) { (pooled[it] / tokenCount).toFloat() }
                    normalize(mean)
                }
            }
        } finally {
            values.values.forEach(OnnxTensor::close)
        }
    }

    private fun validateSession(candidate: OrtSession) {
        require(candidate.inputNames.containsAll(setOf("input_ids", "attention_mask"))) {
            "MiniLM input names mismatch"
        }
        require(candidate.inputNames.all { it in setOf("input_ids", "attention_mask", "token_type_ids") }) {
            "Unsupported MiniLM input"
        }
        require(candidate.outputNames.size == 1) { "MiniLM output names mismatch" }
    }

    private fun privateModelFile(context: Context): File {
        val directory = File(context.filesDir, MODEL_DIRECTORY)
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create private MiniLM directory" }
        val target = File(directory, MODEL_FILENAME)
        if (target.isFile && target.length() == MODEL_BYTES && sha256(target) == MODEL_SHA256) return target

        val temporary = File(directory, "$MODEL_FILENAME.pending")
        try {
            appContext.assets.open(MODEL_ASSET).use { input ->
                temporary.outputStream().buffered().use { output -> input.copyTo(output) }
            }
            require(temporary.length() == MODEL_BYTES && sha256(temporary) == MODEL_SHA256) {
                "MiniLM model checksum mismatch"
            }
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                require(target.length() == MODEL_BYTES && sha256(target) == MODEL_SHA256) {
                    "MiniLM model copy verification failed"
                }
                temporary.delete()
            }
            return target
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }
    }

    private fun normalize(vector: FloatArray): FloatArray {
        val norm = sqrt(vector.sumOf { it.toDouble() * it.toDouble() })
        require(norm.isFinite() && norm > 0.0) { "MiniLM embedding is invalid" }
        return FloatArray(vector.size) {
            (vector[it] / norm).toFloat().also { value -> require(value.isFinite()) }
        }
    }

    private fun cosine(left: FloatArray, right: FloatArray): Float {
        require(left.size == right.size)
        return left.indices.sumOf { left[it].toDouble() * right[it].toDouble() }.toFloat()
    }

    private val environment: OrtEnvironment get() = OrtEnvironment.getEnvironment()

    private fun sha256(file: File): String =
        file.inputStream().buffered().use { input -> sha256(input.readBytesBounded()) }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun InputStream.readBytesBounded(): ByteArray {
        val bytes = readBytes()
        require(bytes.size <= MAX_ASSET_BYTES) { "MiniLM asset is too large" }
        return bytes
    }

    private companion object {
        const val MODEL_DIRECTORY = "decision-model/minilm"
        const val MODEL_FILENAME = "model_qint8_arm64.onnx"
        const val MODEL_ASSET = "decision-model/minilm/model_qint8_arm64.onnx"
        const val VOCAB_ASSET = "decision-model/minilm/vocab.txt"
        const val MODEL_BYTES = 23_026_053L
        const val MODEL_SHA256 = "4278337fd0ff3c68bfb6291042cad8ab363e1d9fbc43dcb499fe91c871902474"
        const val VOCAB_SHA256 = "07eced375cec144d27c900241f3e339478dec958f92fddbc551f295c992038a3"
        const val MAX_ASSET_BYTES = 24 * 1024 * 1024
        const val EMBEDDING_DIMENSION = 384
        val MEAL_LIST_EXAMPLES = listOf(
            "Show the meals I recorded today",
            "List my saved meals for today",
            "What meals have I logged today",
            "What did I eat today",
            "Which meals are in my record for today",
            "Remind me what I ate today",
            "Could you remind me what I ate today",
            "Show me all the food I saved today",
            "Show the meals I recorded yesterday",
            "List my saved meals for yesterday",
            "What meals have I logged yesterday",
            "What did I eat yesterday",
            "Which meals are in my record for yesterday",
            "Remind me what I ate yesterday",
            "Could you remind me what I ate yesterday",
            "Show me all the food I saved yesterday"
        )
        val LATEST_MEAL_EXAMPLES = listOf(
            "How many calories were in my last meal",
            "Could you tell me how many calories were in my last meal",
            "How much protein was in my latest meal",
            "Could you tell me how much protein was in my latest meal",
            "What were the calories in my most recent meal",
            "What was the protein in my last meal",
            "Tell me the calories in my latest meal",
            "How many calories did my latest meal have",
            "Calories in my last meal",
            "Protein in my latest meal",
        )
        val TODAY_NUTRITION_EXAMPLES = listOf(
            "How many calories did I eat today",
            "How much protein have I logged today",
            "Could you tell me how many calories I recorded today",
            "Could you tell me how much protein I recorded today",
            "Show my recorded calories for today",
            "Show my recorded protein for today",
            "How many calories have I recorded today",
            "How much protein did I log today",
            "What's my protein total today",
            "What's my calorie total today",
            "How much protein did I have today",
            "How many calories have I had today",
        )
        val FALLBACK_EXAMPLES = listOf(
            "I just ate eggs and toast",
            "Log a chicken meal for today",
            "What should I eat tomorrow",
            "Show today's meals and suggest dinner",
            "Show yesterday's meals and suggest dinner",
            "What did I eat this week",
            "Help me make a meal plan",
            "Add the lunch I just had",
            "Show today's meals and my weight"
        )
    }
}

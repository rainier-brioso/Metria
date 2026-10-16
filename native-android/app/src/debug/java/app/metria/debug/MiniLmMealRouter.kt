package app.metria.debug

import android.content.Context
import app.metria.MiniLmMealReadPolicy
import app.metria.MiniLmReadTextNormalization
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
 * Debug-only, local MiniLM prototype router for bounded meal, nutrition, and weight reads.
 * Create/use this away from the UI thread. Input text is never logged or persisted.
 */
internal class MiniLmMealRouter(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val session: OrtSession
    private val tokenizer: MiniLmWordPieceTokenizer
    private val mealListPrototypes: List<FloatArray>
    private val latestMealPrototypes: List<FloatArray>
    private val todayNutritionPrototypes: List<FloatArray>
    private val weightPrototypes: List<FloatArray>
    private val goalRemainingPrototypes: List<FloatArray>
    private val savedRecallPrototypes: List<FloatArray>
    private val savedDraftPrototypes: List<FloatArray>
    private val navigationPrototypes: List<FloatArray>
    private val todayOverviewPrototypes: List<FloatArray>
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
            val examples = MEAL_LIST_EXAMPLES + LATEST_MEAL_EXAMPLES + TODAY_NUTRITION_EXAMPLES + WEIGHT_EXAMPLES +
                GOAL_REMAINING_EXAMPLES + SAVED_RECALL_EXAMPLES + SAVED_DRAFT_EXAMPLES + NAVIGATION_EXAMPLES +
                TODAY_OVERVIEW_EXAMPLES + FALLBACK_EXAMPLES
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
            weightPrototypes = vectors.subList(
                MEAL_LIST_EXAMPLES.size + LATEST_MEAL_EXAMPLES.size + TODAY_NUTRITION_EXAMPLES.size,
                MEAL_LIST_EXAMPLES.size + LATEST_MEAL_EXAMPLES.size + TODAY_NUTRITION_EXAMPLES.size + WEIGHT_EXAMPLES.size,
            )
            val goalStart = MEAL_LIST_EXAMPLES.size + LATEST_MEAL_EXAMPLES.size + TODAY_NUTRITION_EXAMPLES.size + WEIGHT_EXAMPLES.size
            goalRemainingPrototypes = vectors.subList(goalStart, goalStart + GOAL_REMAINING_EXAMPLES.size)
            val recallStart = goalStart + GOAL_REMAINING_EXAMPLES.size
            savedRecallPrototypes = vectors.subList(recallStart, recallStart + SAVED_RECALL_EXAMPLES.size)
            val draftStart = recallStart + SAVED_RECALL_EXAMPLES.size
            savedDraftPrototypes = vectors.subList(draftStart, draftStart + SAVED_DRAFT_EXAMPLES.size)
            val navigationStart = draftStart + SAVED_DRAFT_EXAMPLES.size
            navigationPrototypes = vectors.subList(navigationStart, navigationStart + NAVIGATION_EXAMPLES.size)
            val todayOverviewStart = navigationStart + NAVIGATION_EXAMPLES.size
            todayOverviewPrototypes = vectors.subList(todayOverviewStart, todayOverviewStart + TODAY_OVERVIEW_EXAMPLES.size)
            fallbackPrototypes = vectors.drop(
                todayOverviewStart + TODAY_OVERVIEW_EXAMPLES.size,
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
        if (!MiniLmMealReadPolicy.isMiniLmRouterInputInScope(text, toolName)) {
            if (toolName != "read_day_nutrition" ||
                (!MiniLmMealReadPolicy.isTodayNutritionInScope(text) && !MiniLmMealReadPolicy.isHistoricalNutritionInScope(text))) {
                return MiniLmRouteDecision(MiniLmRouteOutcome.SCOPE_REJECTED)
            }
        }
        return try {
            val query = embed(listOf(MiniLmReadTextNormalization.normalize(text))).single()
            if (Thread.currentThread().isInterrupted) return MiniLmRouteDecision(MiniLmRouteOutcome.INTERRUPTED)
            val groups = listOf(
                "read_meals" to mealListPrototypes,
                "read_latest_meal" to latestMealPrototypes,
                "read_day_nutrition" to todayNutritionPrototypes,
                "read_weight" to weightPrototypes,
                "goal_remaining" to goalRemainingPrototypes,
                "saved_recall" to savedRecallPrototypes,
                "saved_draft" to savedDraftPrototypes,
                "navigation" to navigationPrototypes,
                "today_overview" to todayOverviewPrototypes,
            )
            val positiveExamples = groups.singleOrNull { it.first == toolName }?.second
                ?: return MiniLmRouteDecision(MiniLmRouteOutcome.SCOPE_REJECTED)
            val competingExamples = groups.filterNot { it.first == toolName }.flatMap { it.second }
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
            "Show me all the food I saved yesterday",
            "What have I eaten today",
            "What have I eaten yesterday",
            "Remind me of today's meals",
            "Remind me of yesterday's meals"
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
            "How many calories in my last meal",
            "What's the protein in my latest meal",
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
            "What's today's protein total",
            "What's today's calorie total",
            "Tell me my calorie total for today",
            "Tell me my protein total for today",
        )
        val WEIGHT_EXAMPLES = listOf(
            "What's my latest weight",
            "What is my latest weight",
            "Show me the latest weight",
            "When did I last weigh myself",
            "When did I last weight myself",
            "When was my last weigh-in",
            "Show my recent weigh-ins",
            "Show my weigh-ins from the last 30 days",
            "List my recent weigh-ins",
            "When was my most recent weigh in",
            "Let me see my recent weigh-ins",
            "Could you show me my latest weight",
        )
        val GOAL_REMAINING_EXAMPLES = listOf(
            "How much protein do I have left today",
            "How many calories do I have left today",
            "How much protein remains against my goal today",
            "How many calories remain against my goal today",
        )
        val SAVED_RECALL_EXAMPLES = listOf(
            "Find my usual breakfast",
            "Show my usual breakfast",
            "Can you show me my usual breakfast",
            "Find my saved dish name",
            "Show my saved dish name",
            "Look up my saved dish name",
        )
        val SAVED_DRAFT_EXAMPLES = listOf(
            "I had name",
            "I ate name",
            "I've just had name",
            "I had a named meal",
            "I had my saved dish name",
            "I ate a named meal",
            "I've eaten a named meal",
            "I had my usual breakfast",
            "I ate my usual lunch",
            "I've just had my usual breakfast",
            "I just ate my usual breakfast",
            "I ate my saved dish name",
            "I had my saved dish name",
        )
        val NAVIGATION_EXAMPLES = listOf(
            "Open my weight history",
            "Show my weight history",
            "Open today",
            "Show my meals for today",
            "Open privacy settings",
        )
        val TODAY_OVERVIEW_EXAMPLES = listOf(
            "How am I doing today",
            "How am I doing for today",
            "Give me a summary of today",
            "Show me an overview of today",
            "Give me an overview of my day today",
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

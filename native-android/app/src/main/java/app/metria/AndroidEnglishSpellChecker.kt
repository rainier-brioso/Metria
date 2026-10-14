package app.metria

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** One-shot Android framework spellcheck request. Only the candidate token enters the service. */
internal class AndroidEnglishSpellChecker(context: Context) : EnglishSpellChecker {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun check(token: String, isActive: () -> Boolean): SpellCheckResult =
        checkWithin(token, isActive, TIMEOUT_MILLIS)

    override fun checkWithin(token: String, isActive: () -> Boolean, budgetMillis: Long): SpellCheckResult {
        if (Looper.myLooper() == Looper.getMainLooper()) return SpellCheckResult(SpellCheckReason.RUNTIME_FAILURE)
        if (budgetMillis <= 0L) return SpellCheckResult(SpellCheckReason.TIMEOUT)
        val manager = appContext.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as? TextServicesManager
            ?: return SpellCheckResult(SpellCheckReason.UNAVAILABLE)
        if (!spellCheckerEnabled()) return SpellCheckResult(SpellCheckReason.DISABLED)

        val done = AtomicBoolean(false)
        val session = AtomicReference<SpellCheckerSession?>()
        val result = AtomicReference<SpellCheckResult?>()
        val latch = CountDownLatch(1)
        lateinit var createSession: Runnable
        fun closeSession() {
            val current = session.getAndSet(null) ?: return
            mainHandler.post {
                runCatching { current.cancel() }
                runCatching { current.close() }
            }
        }
        fun finish(value: SpellCheckResult) {
            if (done.compareAndSet(false, true)) {
                result.set(value)
                latch.countDown()
                closeSession()
            }
        }
        val listener = object : SpellCheckerSession.SpellCheckerSessionListener {
            override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
                // Suggestions without segment metadata are not used by this adapter.
            }

            override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
                try {
                    if (done.get() || !isActive()) return
                    if (results.isNullOrEmpty()) {
                        finish(SpellCheckResult(SpellCheckReason.EMPTY_RESPONSE))
                        return
                    }
                    val sentence = results.takeIf { it.size == 1 }?.get(0)
                    val info = sentence?.takeIf { it.suggestionsCount == 1 }?.getSuggestionsInfoAt(0)
                    if (info == null || sentence.getOffsetAt(0) != 0 || sentence.getLengthAt(0) != token.length) {
                        finish(SpellCheckResult(SpellCheckReason.SEGMENT_MISMATCH))
                        return
                    }
                    if (info.cookie != REQUEST_COOKIE || info.sequence != REQUEST_SEQUENCE) {
                        finish(SpellCheckResult(SpellCheckReason.REQUEST_MISMATCH))
                        return
                    }
                    val attrs = info.suggestionsAttributes
                    if (attrs and SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY != 0) {
                        finish(SpellCheckResult(SpellCheckReason.DICTIONARY_WORD))
                        return
                    }
                    val count = info.suggestionsCount.coerceIn(0, MAX_SUGGESTIONS)
                    if (count == 0) {
                        finish(SpellCheckResult(SpellCheckReason.NO_SUGGESTIONS))
                        return
                    }
                    if (attrs and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO == 0) {
                        finish(SpellCheckResult(SpellCheckReason.UNMARKED_TYPO))
                        return
                    }
                    val suggestions = (0 until count).mapNotNull { index ->
                        info.getSuggestionAt(index)?.let { word ->
                            if (word.length !in 1..MAX_WORD_LENGTH) return@mapNotNull null
                            SpellSuggestion(
                                word = word,
                                offset = sentence.getOffsetAt(0),
                                length = sentence.getLengthAt(0),
                                looksLikeTypo = attrs and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO != 0,
                                inDictionary = attrs and SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY != 0,
                            )
                        }
                    }
                    finish(SpellCheckResult(if (suggestions.isEmpty()) SpellCheckReason.NO_SUGGESTIONS else SpellCheckReason.ACCEPTED, suggestions))
                } catch (_: RuntimeException) {
                    finish(SpellCheckResult(SpellCheckReason.RUNTIME_FAILURE))
                }
            }

        }
        createSession = Runnable {
            if (done.get() || !isActive()) {
                finish(SpellCheckResult(SpellCheckReason.INTERRUPTED))
                return@Runnable
            }
            try {
                val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val params = SpellCheckerSession.SpellCheckerSessionParams.Builder()
                        .setLocale(Locale.ENGLISH)
                        .setShouldReferToSpellCheckerLanguageSettings(false)
                        .setSupportedAttributes(
                            SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY or
                                SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO or
                                SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS,
                        )
                        .build()
                    manager.newSpellCheckerSession(params, Executor { command -> mainHandler.post(command) }, listener)
                } else {
                    @Suppress("DEPRECATION")
                    manager.newSpellCheckerSession(null, Locale.ENGLISH, listener, true)
                }
                if (created == null) {
                    finish(SpellCheckResult(SpellCheckReason.UNAVAILABLE))
                } else if (done.get() || !isActive()) {
                    runCatching { created.cancel() }
                    runCatching { created.close() }
                    finish(SpellCheckResult(SpellCheckReason.INTERRUPTED))
                } else {
                    session.set(created)
                    if (done.get()) closeSession()
                    else created.getSentenceSuggestions(arrayOf(TextInfo(token, REQUEST_COOKIE, REQUEST_SEQUENCE)), MAX_SUGGESTIONS)
                }
            } catch (_: RuntimeException) {
                finish(SpellCheckResult(SpellCheckReason.RUNTIME_FAILURE))
            }
        }

        mainHandler.post(createSession)
        try {
            if (!latch.await(budgetMillis.coerceAtMost(TIMEOUT_MILLIS), TimeUnit.MILLISECONDS)) {
                finish(SpellCheckResult(SpellCheckReason.TIMEOUT))
            }
        } catch (_: InterruptedException) {
            finish(SpellCheckResult(SpellCheckReason.INTERRUPTED))
            Thread.currentThread().interrupt()
        } finally {
            done.set(true)
            mainHandler.removeCallbacks(createSession)
            closeSession()
        }
        return result.get() ?: SpellCheckResult(SpellCheckReason.RUNTIME_FAILURE)
    }

    private fun spellCheckerEnabled(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = appContext.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as? TextServicesManager
        runCatching { manager?.isSpellCheckerEnabled() == true }.getOrDefault(false)
    } else {
        runCatching { Settings.Secure.getInt(appContext.contentResolver, "spell_checker_enabled", 1) != 0 }
            .getOrDefault(false)
    }

    private companion object {
        const val MAX_SUGGESTIONS = 3
        const val MAX_WORD_LENGTH = 24
        const val TIMEOUT_MILLIS = 400L
        const val REQUEST_COOKIE = 0x4d455452
        const val REQUEST_SEQUENCE = 1
    }
}

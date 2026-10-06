package app.metria.cloud

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** In-memory preview only. Neither the question nor summaries enter logs or saved state. */
class OnlinePreview internal constructor(
    val payload: String, internal val createdAt: Instant,
    internal val snapshot: OnlineAssistanceRequest,
)

class ApprovedOnlineRequest internal constructor(internal val preview: OnlinePreview)

/** No networking or persisted opt-in: a future screen must explicitly enable and approve. */
class OnlineConsentSession(private val now: () -> Instant = Instant::now) {
    private var enabled = false
    private var current: OnlinePreview? = null
    private var approved: ApprovedOnlineRequest? = null
    private var submitted = false

    @Synchronized fun enable() { enabled = true }

    /** Also used for lock/exit: old permits and late output become invalid immediately. */
    @Synchronized fun disable() {
        enabled = false
        clear()
    }

    @Synchronized fun clear() {
        current = null
        approved = null
        submitted = false
    }

    @Synchronized fun preview(request: OnlineAssistanceRequest, today: LocalDate): OnlinePreview? {
        clear() // Even an invalid edit invalidates the previous sharing decision.
        if (!enabled) return null
        val payload = request.canonicalPayload(today) ?: return null
        val snapshot = OnlineBrokerContract.parseRequest(payload, today, setOf(request.model)) ?: return null
        return OnlinePreview(payload, now(), snapshot).also { current = it }
    }

    @Synchronized fun approve(preview: OnlinePreview): ApprovedOnlineRequest? {
        if (!enabled || current !== preview || !fresh(preview) || approved != null) return null
        return ApprovedOnlineRequest(preview).also { approved = it }
    }

    /** One explicit approval releases its exact preview once, with no automatic retry. */
    @Synchronized fun takeForSend(permit: ApprovedOnlineRequest): String? {
        if (!enabled || approved !== permit || current !== permit.preview || submitted || !fresh(permit.preview)) return null
        submitted = true
        return permit.preview.payload
    }

    /** Identity/lifecycle gate only; separate factual and safety validation remains required. */
    @Synchronized fun isCurrentResponse(permit: ApprovedOnlineRequest): Boolean =
        enabled && submitted && approved === permit && current === permit.preview && fresh(permit.preview)

    @Synchronized fun finishResponse(permit: ApprovedOnlineRequest): Boolean {
        if (!isCurrentResponse(permit)) return false
        clear()
        return true
    }

    /** Validates and retires one terminal result atomically, using only the approved preview. */
    @Synchronized fun completeResponse(permit: ApprovedOnlineRequest, body: String): OnlineAnswer? {
        if (!isCurrentResponse(permit)) return null
        val selection = OnlineResponseContract.validate(body, permit.preview.snapshot)
        val answer = selection?.let { OnlineAnswerRenderer.render(permit.preview.snapshot, it) }
        // Invalid output also ends this attempt: retry requires a new preview and approval.
        clear()
        return answer
    }

    private fun fresh(preview: OnlinePreview): Boolean {
        val elapsed = Duration.between(preview.createdAt, now())
        return !elapsed.isNegative && elapsed < Duration.ofMinutes(5)
    }
}

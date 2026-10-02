package app.metria

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAsyncCancellationTest {
    @Test
    fun `timeout sources racing issue only one native cancellation`() {
        val cancelCalls = AtomicInteger()
        val cancelEntered = CountDownLatch(1)
        val finishCancel = CountDownLatch(1)
        val operation = NativeAsyncCancellation(cancelNative = {
            cancelCalls.incrementAndGet()
            cancelEntered.countDown()
            check(finishCancel.await(5, TimeUnit.SECONDS))
        })
        assertTrue(operation.start { })

        val firstTimeout = Thread { operation.cancel() }
        val secondTimeout = Thread { operation.cancel() }
        firstTimeout.start()
        assertTrue(cancelEntered.await(5, TimeUnit.SECONDS))
        secondTimeout.start()
        secondTimeout.join(5_000)
        assertFalse(secondTimeout.isAlive)
        assertEquals(1, cancelCalls.get())

        finishCancel.countDown()
        firstTimeout.join(5_000)
        assertFalse(firstTimeout.isAlive)
        assertEquals(1, cancelCalls.get())
    }

    @Test
    fun `cancellation before async start prevents native work from starting`() {
        val cancelCalls = AtomicInteger()
        val starts = AtomicInteger()
        val operation = NativeAsyncCancellation(cancelNative = { cancelCalls.incrementAndGet() })

        assertTrue(operation.cancel())

        assertFalse(operation.start { starts.incrementAndGet() })
        assertEquals(0, starts.get())
        assertEquals(0, cancelCalls.get())
    }

    @Test
    fun `completion chunks preserve visible text tool calls and channels`() {
        val accumulator = NativeAsyncMessageAccumulator<String>()
        accumulator.append("Here is", emptyList(), emptyMap())
        accumulator.append(" your", listOf("read_nutrition_summary"), mapOf("format" to "text"))
        accumulator.append(" week.", listOf("read_weight"), mapOf("source" to "local"))

        val result = accumulator.snapshot()
        assertEquals("Here is your week.", result.text)
        assertEquals(listOf("read_nutrition_summary", "read_weight"), result.toolCalls)
        assertEquals(mapOf("format" to "text", "source" to "local"), result.channels)
    }

    @Test
    fun `delta chunks append without dropping repeated prefixes`() {
        val deltas = NativeAsyncMessageAccumulator<String>()
        deltas.append("Hel", emptyList(), emptyMap())
        deltas.append("lo", emptyList(), emptyMap())
        assertEquals("Hello", deltas.snapshot().text)

        val repeatedPrefix = NativeAsyncMessageAccumulator<String>()
        repeatedPrefix.append("ha", emptyList(), emptyMap())
        repeatedPrefix.append("haha", emptyList(), emptyMap())
        assertEquals("hahaha", repeatedPrefix.snapshot().text)

        val whitespaceDelta = NativeAsyncMessageAccumulator<String>()
        whitespaceDelta.append("hello", emptyList(), emptyMap())
        whitespaceDelta.append(" ", emptyList(), emptyMap())
        whitespaceDelta.append("world", emptyList(), emptyMap())
        assertEquals("hello world", whitespaceDelta.snapshot().text)

        val repeatedToolCalls = NativeAsyncMessageAccumulator<String>()
        repeatedToolCalls.append("", listOf("read_weight"), emptyMap())
        repeatedToolCalls.append("", listOf("read_weight"), emptyMap())
        assertEquals(listOf("read_weight", "read_weight"), repeatedToolCalls.snapshot().toolCalls)
    }

    @Test
    fun `terminal callback cannot release conversation while native cancel is blocked`() {
        val closed = AtomicInteger()
        val cancelEntered = CountDownLatch(1)
        val finishCancel = CountDownLatch(1)
        val timeoutPublished = CountDownLatch(1)
        lateinit var operation: NativeAsyncCancellation
        val conversation = Any()
        val lifetime = NativeConversationLifetime(
            conversation = conversation,
            cancel = { operation.cancel() },
            close = { closed.incrementAndGet() },
        )
        assertTrue(lifetime.acquire()) // The native send worker's lease.
        operation = NativeAsyncCancellation(
            acquireCancelLease = lifetime::acquireCancellationLease,
            cancelNative = {
                cancelEntered.countDown()
                check(finishCancel.await(5, TimeUnit.SECONDS))
            },
            releaseCancelLease = lifetime::release,
        )
        assertTrue(operation.start { })

        val timeout = Thread { operation.cancel { timeoutPublished.countDown() } }
        timeout.start()
        assertTrue(cancelEntered.await(5, TimeUnit.SECONDS))
        assertTrue("timeout must publish before cancelProcess returns", timeoutPublished.await(5, TimeUnit.SECONDS))

        lifetime.retire()
        operation.complete() // Simulates onDone/onError racing with the blocked cancel call.
        lifetime.release() // The send worker exits after its terminal callback.
        assertEquals("the cancel call still owns a lifetime lease", 0, closed.get())

        finishCancel.countDown()
        timeout.join(5_000)
        assertFalse(timeout.isAlive)
        assertEquals(1, closed.get())
    }
}

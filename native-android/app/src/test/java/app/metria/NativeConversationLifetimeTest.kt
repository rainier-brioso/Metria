package app.metria

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeConversationLifetimeTest {
    @Test
    fun `retired native conversation closes only after a blocked send exits`() {
        val cancelled = AtomicInteger()
        val closed = AtomicInteger()
        val conversation = Any()
        val lifetime = NativeConversationLifetime(
            conversation = conversation,
            cancel = { cancelled.incrementAndGet() },
            close = { closed.incrementAndGet() },
        )
        val sendEntered = CountDownLatch(1)
        val finishSend = CountDownLatch(1)

        assertTrue(lifetime.acquire())
        val sendWorker = Thread {
            sendEntered.countDown()
            check(finishSend.await(5, TimeUnit.SECONDS))
            lifetime.release()
        }
        sendWorker.start()
        assertTrue(sendEntered.await(5, TimeUnit.SECONDS))

        lifetime.retire()

        assertEquals(1, cancelled.get())
        assertEquals("must stay alive while synchronous send is inside native code", 0, closed.get())
        assertFalse("a retired conversation cannot accept another native operation", lifetime.acquire())

        finishSend.countDown()
        sendWorker.join(5_000)

        assertFalse(sendWorker.isAlive)
        assertEquals(1, closed.get())
    }

    @Test
    fun `native conversation is not closed while cancel call is still running`() {
        val closed = AtomicInteger()
        val conversation = Any()
        val cancelEntered = CountDownLatch(1)
        val finishCancel = CountDownLatch(1)
        val lifetime = NativeConversationLifetime(
            conversation = conversation,
            cancel = {
                cancelEntered.countDown()
                check(finishCancel.await(5, TimeUnit.SECONDS))
            },
            close = { closed.incrementAndGet() },
        )
        assertTrue(lifetime.acquire())

        val retirement = Thread { lifetime.retire() }
        retirement.start()
        assertTrue(cancelEntered.await(5, TimeUnit.SECONDS))

        lifetime.release()
        assertEquals("cancelProcess still owns the native handle", 0, closed.get())

        finishCancel.countDown()
        retirement.join(5_000)
        assertFalse(retirement.isAlive)
        assertEquals(1, closed.get())
    }
}

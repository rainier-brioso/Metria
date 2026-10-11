package app.metria

import org.junit.Assert.*
import org.junit.Test

class FirstGoalOfferTest {
    @Test fun `successful new vault offers once and cannot replay on subsequent updates`() {
        val offer = FirstGoalOffer()
        offer.recordCreation(true)
        assertTrue(offer.take(unlocked = true))
        assertFalse(offer.take(unlocked = true))
    }
    @Test fun `existing unlock and failed creation do not offer goals`() {
        val offer = FirstGoalOffer()
        assertFalse(offer.take(unlocked = true))
        offer.recordCreation(false)
        assertFalse(offer.take(unlocked = true))
    }
    @Test fun `locked state cannot expose offer and clearing or recreation discards it`() {
        val offer = FirstGoalOffer()
        offer.recordCreation(true)
        assertFalse(offer.take(unlocked = false))
        offer.clear()
        assertFalse(offer.take(unlocked = true))
        offer.recordCreation(true)
        assertFalse(FirstGoalOffer().take(unlocked = true))
    }
}

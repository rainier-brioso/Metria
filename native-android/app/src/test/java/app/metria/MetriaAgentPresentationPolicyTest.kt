package app.metria

import org.junit.Assert.assertEquals
import org.junit.Test

class MetriaAgentPresentationPolicyTest {
    @Test
    fun `record-backed Spanish wording attributes records to member without altering facts`() {
        assertEquals(
            "Según tus registros, el 19 de septiembre registraste Arroz y 90.5 kg.",
            MetriaAgentPresentationPolicy.memberOwnedRecords(
                "Según mis registros, el 19 de septiembre registraste Arroz y 90.5 kg.",
            ),
        )
    }

    @Test
    fun `record-backed English wording attributes records to member`() {
        assertEquals(
            "Your records show one saved meal.",
            MetriaAgentPresentationPolicy.memberOwnedRecords("My records show one saved meal."),
        )
    }

    @Test
    fun `other phrasing and numbers remain untouched`() {
        val reply = "No hay una estimación de proteína en tus registros para ese día."
        assertEquals(reply, MetriaAgentPresentationPolicy.memberOwnedRecords(reply))
    }
}

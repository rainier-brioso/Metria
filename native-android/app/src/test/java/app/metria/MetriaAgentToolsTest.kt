package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetriaAgentToolsTest {
    @Test
    fun `native weight call preserves bounded integer arguments`() {
        val adapted = MetriaNativeToolCallAdapter.adapt(
            "read_weight",
            mapOf("from_days_ago" to 6.0, "to_days_ago" to 0.0),
        )

        assertEquals(
            PrivateCoachToolCall(intent = "read_weight", fromDaysAgo = 6, toDaysAgo = 0),
            adapted,
        )
    }

    @Test
    fun `native progress overview accepts only the two grounded periods`() {
        assertEquals(
            PrivateCoachToolCall(intent = "read_progress_overview", period = "week"),
            MetriaNativeToolCallAdapter.adapt("read_progress_overview", mapOf("period" to "week")),
        )
        assertNull(MetriaNativeToolCallAdapter.adapt("read_progress_overview", mapOf("period" to "month")))
        assertNull(MetriaNativeToolCallAdapter.adapt("read_progress_overview", emptyMap<String, Any?>()))
    }

    @Test
    fun `unknown native tool is rejected`() {
        assertNull(MetriaNativeToolCallAdapter.adapt("delete_everything", emptyMap<String, Any?>()))
    }

    @Test
    fun `native tools reject missing malformed and out of range arguments`() {
        assertNull(MetriaNativeToolCallAdapter.adapt("read_nutrition_summary", mapOf("period" to "month")))
        assertNull(MetriaNativeToolCallAdapter.adapt("read_weight", mapOf("from_days_ago" to 6.5, "to_days_ago" to 0)))
        assertNull(MetriaNativeToolCallAdapter.adapt("read_weight", mapOf("from_days_ago" to 2, "to_days_ago" to 4)))
        assertNull(MetriaNativeToolCallAdapter.adapt("read_meals", mapOf("days_ago" to 91)))
        assertNull(MetriaNativeToolCallAdapter.adapt("read_meals", mapOf("days_ago" to 1, "meal_slot" to "brunch")))
    }

    @Test
    fun `record bounds return the latest values with explicit coverage`() {
        val bounded = MetriaAgentResultBounds.latest((1..30).toList())

        assertEquals((7..30).toList(), bounded.records)
        assertEquals(30, bounded.totalCount)
        assertEquals(6, bounded.omittedCount)
        assertEquals("latest_records_only", bounded.coverage)
    }

    @Test
    fun `reply policy accepts conversational prose and rejects protocol leakage`() {
        assertEquals(
            "Vas avanzando con datos de esta semana.",
            MetriaAgentReplyPolicy.acceptedText("Vas avanzando con datos de esta semana."),
        )
        assertNull(MetriaAgentReplyPolicy.acceptedText("<start_of_turn>system_instruction"))
    }

    @Test
    fun `reply policy shortens safe long coaching at a sentence boundary`() {
        val longReply = "Primera idea útil. " + "Una explicación segura y local. ".repeat(60)

        val accepted = MetriaAgentReplyPolicy.acceptedText(longReply)

        assertTrue(accepted != null)
        assertTrue(accepted!!.length <= 1_200)
        assertTrue(accepted.endsWith('.'))
    }

    @Test
    fun `reply policy still rejects runaway output`() {
        assertNull(MetriaAgentReplyPolicy.acceptedText("x".repeat(4_001)))
    }

    @Test
    fun `fact auditor accepts only numbers present in the latest tool response`() {
        val evidence = MetriaAgentFactEvidence.from(
            "read_nutrition_summary",
            mapOf(
                "source" to "metria_local_vault",
                "as_of" to "2026-08-28",
                "facts" to "logged_meals=3; calories_kcal=1250; protein_g=82; calorie_goal_kcal=1920",
            ),
        )

        assertEquals(
            "Tienes 3 comidas registradas, con estimaciones de 1250 kcal y 82 g de proteína.",
            MetriaAgentFactAuditor.audit(
                "Tienes 3 comidas registradas, con estimaciones de 1250 kcal y 82 g de proteína.",
                evidence,
            ).acceptedText,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit(
                "Tienes 3 comidas registradas y 1400 kcal.",
                evidence,
            ).retryable,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit(
                "Eso representa el 65% de tu objetivo.",
                evidence,
            ).retryable,
        )
    }

    @Test
    fun `fact auditor accepts locale number formatting and ignores numbered list markers`() {
        val evidence = MetriaAgentFactEvidence.from(
            "read_nutrition_summary",
            mapOf("source" to "metria_local_vault", "calories_kcal" to 1200.0),
        )

        assertEquals(
            "1. Has registrado una estimación de 1.200 kcal.",
            MetriaAgentFactAuditor.audit(
                "1. Has registrado una estimación de 1.200 kcal.",
                evidence,
            ).acceptedText,
        )
    }

    @Test
    fun `fact auditor blocks personal record claims when no current tool evidence exists`() {
        assertTrue(
            MetriaAgentFactAuditor.audit("Tu peso ha bajado desde ayer.", evidence = null).retryable,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit("Ayer comiste arroz.", evidence = null).retryable,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit("Pesas 92 kg.", evidence = null).retryable,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit("Hay 4 comidas registradas.", evidence = null).retryable,
        )
        assertEquals(
            "Podemos revisar juntos la pregunta que tengas sobre Metria.",
            MetriaAgentFactAuditor.audit(
                "Podemos revisar juntos la pregunta que tengas sobre Metria.",
                evidence = null,
            ).acceptedText,
        )
        assertEquals(
            "Puedes probar un paseo suave de 10 minutos.",
            MetriaAgentFactAuditor.audit(
                "Puedes probar un paseo suave de 10 minutos.",
                evidence = null,
            ).acceptedText,
        )
    }

    @Test
    fun `fact auditor rejects a weight direction that contradicts the latest readings`() {
        val evidence = MetriaAgentFactEvidence.from(
            "read_weight",
            mapOf(
                "source" to "metria_local_vault",
                "entries" to listOf(
                    mapOf("date" to "2026-08-27", "kilograms" to 92.0),
                    mapOf("date" to "2026-08-28", "kilograms" to 91.0),
                ),
            ),
        )

        assertTrue(
            MetriaAgentFactAuditor.audit("Tu peso subió de 92 a 91 kg.", evidence).retryable,
        )
        assertEquals(
            "Tu peso bajó de 92 a 91 kg.",
            MetriaAgentFactAuditor.audit("Tu peso bajó de 92 a 91 kg.", evidence).acceptedText,
        )
    }

    @Test
    fun `migration policy sends only explicit write language to legacy route`() {
        assertTrue(MetriaAgentMigrationPolicy.useLegacyWriteRoute("Registra 77 kg hoy"))
        assertTrue(MetriaAgentMigrationPolicy.useLegacyWriteRoute("Save this meal"))
        assertFalse(MetriaAgentMigrationPolicy.useLegacyWriteRoute("¿Cómo va mi semana?"))
        assertFalse(MetriaAgentMigrationPolicy.useLegacyWriteRoute("Sí, me gustaría"))
    }

    @Test
    fun `broad progress questions require one grounded overview retry`() {
        assertEquals(
            RequiredAgentTool("read_progress_overview", "week"),
            MetriaAgentGroundingPolicy.requiredTool("¿Cómo va mi semana?"),
        )
        assertEquals(
            RequiredAgentTool("read_progress_overview", "today"),
            MetriaAgentGroundingPolicy.requiredTool("How am I doing today?"),
        )
        assertNull(MetriaAgentGroundingPolicy.requiredTool("¿Qué comí hace cuatro días?"))
    }

    @Test
    fun `explicit remembered dish question requires saved dish grounding`() {
        assertEquals(
            RequiredAgentTool("search_saved_dishes", query = "San Jacobo"),
            MetriaAgentGroundingPolicy.requiredTool("¿Recuerdas mi San Jacobo?"),
        )
        assertEquals(
            RequiredAgentTool("search_saved_dishes", query = "lentil stew"),
            MetriaAgentGroundingPolicy.requiredTool("Do you remember my lentil stew?"),
        )
    }

    @Test
    fun `saved dish ingredient follow up reuses only the conversational subject`() {
        val history = listOf(
            PrivateConversationMessage("¿Recuerdas mi San Jacobo?", fromMember = true),
            PrivateConversationMessage("Saved private dish match: San Jacobo.", fromMember = false, localEvidence = true),
            PrivateConversationMessage("Sí, lo tengo guardado.", fromMember = false),
        )

        assertEquals(
            RequiredAgentTool("search_saved_dishes", query = "San Jacobo"),
            MetriaAgentGroundingPolicy.requiredTool("¿Y qué lleva?", history),
        )
    }
}

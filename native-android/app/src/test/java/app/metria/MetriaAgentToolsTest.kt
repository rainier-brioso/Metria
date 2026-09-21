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
    fun `fact auditor keeps each weight bound to its own date`() {
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

        assertEquals(
            "On 2026-08-27 you weighed 92 kg. On 2026-08-28 you weighed 91 kg.",
            MetriaAgentFactAuditor.audit(
                "On 2026-08-27 you weighed 92 kg. On 2026-08-28 you weighed 91 kg.",
                evidence,
            ).acceptedText,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit(
                "On 2026-08-27 you weighed 91 kg. On 2026-08-28 you weighed 92 kg.",
                evidence,
            ).retryable,
        )
    }

    @Test
    fun `fact auditor keeps a weight transition and its change distinct from dated readings`() {
        val evidence = MetriaAgentFactEvidence.from(
            "read_weight",
            mapOf(
                "source" to "metria_local_vault",
                "total_count" to 2,
                "entries" to listOf(
                    mapOf("date" to "2026-08-27", "kilograms" to 92.0),
                    mapOf("date" to "2026-08-28", "kilograms" to 91.0),
                ),
                "trend" to mapOf(
                    "first_date" to "2026-08-27",
                    "first_kg" to 92.0,
                    "latest_date" to "2026-08-28",
                    "latest_kg" to 91.0,
                    "change_kg" to -1.0,
                ),
            ),
        )

        assertEquals(
            "Tienes 2 registros de peso: de 92 a 91 kg, un cambio de -1 kg.",
            MetriaAgentFactAuditor.audit(
                "Tienes 2 registros de peso: de 92 a 91 kg, un cambio de -1 kg.",
                evidence,
            ).acceptedText,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit(
                "Tienes 2 registros de peso: de 91 a 92 kg, un cambio de -1 kg.",
                evidence,
            ).retryable,
        )
    }

    @Test
    fun `fact auditor keeps each meal bound to its own date`() {
        val evidence = MetriaAgentFactEvidence.from(
            "read_meals",
            mapOf(
                "source" to "metria_local_vault",
                "meals" to listOf(
                    mapOf("date" to "2026-08-27", "title" to "Arroz con pollo", "slot" to "lunch"),
                    mapOf("date" to "2026-08-28", "title" to "Lentejas", "slot" to "dinner"),
                ),
            ),
        )

        assertEquals(
            "El 2026-08-27 registraste Arroz con pollo. El 2026-08-28 registraste Lentejas.",
            MetriaAgentFactAuditor.audit(
                "El 2026-08-27 registraste Arroz con pollo. El 2026-08-28 registraste Lentejas.",
                evidence,
            ).acceptedText,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit(
                "El 2026-08-27 registraste Lentejas. El 2026-08-28 registraste Arroz con pollo.",
                evidence,
            ).retryable,
        )
    }

    @Test
    fun `fact auditor binds nutrition values and goals to metric and period`() {
        val evidence = MetriaAgentFactEvidence.from(
            "read_nutrition_summary",
            mapOf(
                "source" to "metria_local_vault",
                "period" to "last_7_days_including_today",
                "nutrition" to mapOf(
                    "calories_kcal" to 1250,
                    "protein_g" to 82,
                    "calorie_goal_kcal" to 1920,
                    "protein_goal_g" to 105,
                ),
            ),
        )

        assertEquals(
            "Esta semana llevas 1250 kcal y 82 g de proteína. Tu objetivo semanal es 1920 kcal.",
            MetriaAgentFactAuditor.audit(
                "Esta semana llevas 1250 kcal y 82 g de proteína. Tu objetivo semanal es 1920 kcal.",
                evidence,
            ).acceptedText,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit(
                "Esta semana llevas 82 kcal y 1250 g de proteína.",
                evidence,
            ).retryable,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit(
                "Esta semana llevas 82 calorías y 1250 de proteína.",
                evidence,
            ).retryable,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit(
                "Tu objetivo diario es 1920 kcal.",
                evidence,
            ).retryable,
        )
    }

    @Test
    fun `host calculates bounded nutrition goal progress`() {
        assertEquals(25, goalProgressPercent(4420.0, 17500.0))
        assertEquals(16, goalProgressPercent(196.0, 1260.0))
        assertNull(goalProgressPercent(100.0, 0.0))
        assertNull(goalProgressPercent(null, 100.0))
    }

    @Test
    fun `fact auditor binds host calculated percentages to their nutrition metric`() {
        val evidence = MetriaAgentFactEvidence.from(
            "read_nutrition_summary",
            mapOf(
                "source" to "metria_local_vault",
                "period" to "last_7_days_including_today",
                "nutrition" to mapOf(
                    "calories_kcal" to 4420,
                    "protein_g" to 196,
                    "calorie_goal_kcal" to 17500,
                    "protein_goal_g" to 1260,
                ),
                "progress" to mapOf(
                    "calorie_goal_percent" to 25,
                    "protein_goal_percent" to 16,
                ),
            ),
        )

        assertEquals(
            "Esta semana has registrado el 25% de tu objetivo calórico y el 16% del objetivo de proteína.",
            MetriaAgentFactAuditor.audit(
                "Esta semana has registrado el 25% de tu objetivo calórico y el 16% del objetivo de proteína.",
                evidence,
            ).acceptedText,
        )
        assertEquals(
            "This week, recorded estimates reached 25% of your calorie goal and 16% of your protein goal.",
            MetriaAgentFactAuditor.audit(
                "This week, recorded estimates reached 25% of your calorie goal and 16% of your protein goal.",
                evidence,
            ).acceptedText,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit(
                "Esta semana has registrado el 16% de tu objetivo calórico y el 25% del objetivo de proteína.",
                evidence,
            ).retryable,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit(
                "Esta semana has registrado el 26% de tu objetivo calórico.",
                evidence,
            ).retryable,
        )
    }

    @Test
    fun `member visible weight keeps stored precision without trailing zeroes`() {
        assertEquals("91.05", formatRecordedKilograms(91.05))
        assertEquals("91.1", formatRecordedKilograms(91.10))
        assertEquals("91", formatRecordedKilograms(91.0))
        assertEquals("+0.05", formatSignedRecordedKilograms(0.05))
    }

    @Test
    fun `fact auditor binds counts to their collection`() {
        val evidence = MetriaAgentFactEvidence.from(
            "read_progress_overview",
            mapOf(
                "source" to "metria_local_vault",
                "period" to "last_7_days_including_today",
                "coverage" to mapOf("period_days" to 7, "days_with_recorded_meals" to 3),
                "meals" to mapOf("recorded_count" to 4),
                "weight" to mapOf("entry_count" to 2),
            ),
        )

        assertEquals(
            "Has registrado 4 comidas. Hay comidas en 3 días. Tienes 2 registros de peso.",
            MetriaAgentFactAuditor.audit(
                "Has registrado 4 comidas. Hay comidas en 3 días. Tienes 2 registros de peso.",
                evidence,
            ).acceptedText,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit(
                "Has registrado 2 comidas. Tienes 4 registros de peso.",
                evidence,
            ).retryable,
        )
    }

    @Test
    fun `fact auditor accepts only returned saved dish identities`() {
        val evidence = MetriaAgentFactEvidence.from(
            "search_saved_dishes",
            mapOf(
                "source" to "metria_local_vault",
                "matches" to listOf(
                    mapOf(
                        "name" to "San Jacobo",
                        "ingredients" to listOf("pollo", "jamón", "queso"),
                        "confirmation_count" to 3,
                    ),
                ),
            ),
        )

        assertEquals(
            "Tu plato guardado San Jacobo está en tus platos privados.",
            MetriaAgentFactAuditor.audit(
                "Tu plato guardado San Jacobo está en tus platos privados.",
                evidence,
            ).acceptedText,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit(
                "Tu plato guardado Cutlet está en tus platos privados.",
                evidence,
            ).retryable,
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

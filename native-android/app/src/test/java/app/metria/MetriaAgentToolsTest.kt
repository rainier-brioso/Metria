package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetriaAgentToolsTest {
    @Test fun `no evidence correction preserves the original food question instead of becoming a new request`() {
        val instruction = MetriaAgentFactAuditor.retryInstruction(false, "¿El arroz tiene proteína?")
        assertTrue(instruction.contains("ORIGINAL_MEMBER_QUESTION: \"¿El arroz tiene proteína?\""))
        assertTrue(instruction.contains("Do not acknowledge the correction"))
        assertTrue(instruction.contains("direct qualitative answer"))
        assertFalse(MetriaAgentFactAuditor.retryInstruction(true, "irrelevant").contains("ORIGINAL_MEMBER_QUESTION"))
    }

    @Test fun `qualitative food education passes while unsupported personal statements still fail`() {
        val general = "Sí, el arroz contiene algo de proteína, aunque destaca principalmente por sus carbohidratos."
        assertEquals(general, MetriaAgentFactAuditor.audit(general, null).acceptedText)
        assertEquals("unsupported_personal_fact", MetriaAgentFactAuditor.audit("Tus comidas de hoy contienen 30 g de proteína.", null).rejection)
        assertEquals("unsupported_personal_fact", MetriaAgentFactAuditor.audit("I will access your personal records to help with your meals.", null).rejection)
    }

    @Test fun `recorded meal list wording requires exact day meal evidence`() {
        listOf("¿Qué comidas tengo registradas hoy?", "Que comidas tengo guardadas hoy?",
            "What meals have I logged today?", "Which meals do I have saved today?").forEach { question ->
            assertEquals(question, RequiredAgentTool("read_meals", daysAgo = 0),
                MetriaAgentGroundingPolicy.requiredTool(question))
        }
        listOf("¿Cuáles son mis comidas registradas ayer?", "Which meals did I record yesterday?").forEach { question ->
            assertEquals(question, RequiredAgentTool("read_meals", daysAgo = 1),
                MetriaAgentGroundingPolicy.requiredTool(question))
        }
        val required = MetriaAgentGroundingPolicy.requiredTool("¿Qué comidas tengo registradas hoy?")!!
        assertFalse(required.accepts(PrivateCoachToolCall(intent = "read_nutrition_summary", period = "today")))
        assertFalse(required.accepts(PrivateCoachToolCall(intent = "draft_meal")))
    }

    @Test fun `recorded meal wording retains calendar and unsupported day boundaries`() {
        val today = java.time.LocalDate.of(2026, 10, 2)
        assertEquals(RequiredAgentTool("read_meals", daysAgo = 1, targetDate = today.minusDays(1)),
            MetriaAgentGroundingPolicy.requiredTool("¿Qué comidas tengo registradas el 1 de octubre?", today = today))
        assertNull(MetriaAgentGroundingPolicy.requiredTool("Which meals have I logged 91 days ago?", today = today))
        assertTrue(MetriaAgentGroundingPolicy.historicalClarification("Which meals have I logged 91 days ago?", today) != null)
    }

    @Test fun `general food and hypothetical meal questions do not require meal record reads`() {
        listOf("¿El arroz tiene proteína?", "What meals should I log today?",
            "¿Qué comidas debería preparar hoy?", "Today I ate rice and beans").forEach { question ->
            assertNull(question, MetriaAgentGroundingPolicy.requiredTool(question))
        }
    }

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
        assertEquals(
            PrivateCoachToolCall("read_day_nutrition", period = "historical_day", daysAgo = 90),
            MetriaNativeToolCallAdapter.adapt("read_day_nutrition", mapOf("days_ago" to 90)),
        )
        assertNull(MetriaNativeToolCallAdapter.adapt("read_day_nutrition", mapOf("days_ago" to 91)))
        assertNull(MetriaNativeToolCallAdapter.adapt("read_day_nutrition", mapOf("days_ago" to -1)))
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
    fun `record dates without as of never establish today for relative claims`() {
        val evidence = MetriaAgentFactEvidence.from(
            "read_meals",
            mapOf(
                "source" to "metria_local_vault",
                "from" to "2026-09-19",
                "to" to "2026-09-19",
                "meals" to listOf(mapOf("date" to "2026-09-19", "title" to "Arroz", "slot" to "lunch")),
            ),
        )
        assertEquals(
            "El 2026-09-19 registraste Arroz.",
            MetriaAgentFactAuditor.audit("El 2026-09-19 registraste Arroz.", evidence).acceptedText,
        )
        assertTrue(MetriaAgentFactAuditor.audit("Hoy registraste Arroz.", evidence).retryable)
        val recordsOnly = MetriaAgentFactEvidence.from(
            "read_meals",
            mapOf(
                "source" to "metria_local_vault",
                "meals" to listOf(mapOf("date" to "2026-09-19", "title" to "Arroz", "slot" to "lunch")),
            ),
        )
        assertTrue(MetriaAgentFactAuditor.audit("Hoy registraste Arroz.", recordsOnly).retryable)
        val noRange = MetriaAgentFactEvidence.from(
            "read_meals",
            mapOf(
                "source" to "metria_local_vault",
                "as_of" to "2026-09-23",
                "meals" to listOf(mapOf("date" to "2026-09-19", "title" to "Arroz", "slot" to "lunch")),
            ),
        )
        assertTrue(MetriaAgentFactAuditor.audit("Hoy registraste Arroz.", noRange).retryable)
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
        assertEquals(
            "Esta semana: 1250 kcal frente a un objetivo de 1920 kcal y 82 g de proteína frente a un objetivo de 105 g de proteína.",
            MetriaAgentFactAuditor.audit(
                "Esta semana: 1250 kcal frente a un objetivo de 1920 kcal y 82 g de proteína frente a un objetivo de 105 g de proteína.",
                evidence,
            ).acceptedText,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit(
                "Esta semana: 1920 kcal frente a un objetivo de 1250 kcal.",
                evidence,
            ).retryable,
        )
        assertEquals(
            "This week you logged 82 g protein against a goal of 105 g protein.",
            MetriaAgentFactAuditor.audit(
                "This week you logged 82 g protein against a goal of 105 g protein.",
                evidence,
            ).acceptedText,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit(
                "This week you logged 105 g protein against a goal of 82 g protein.",
                evidence,
            ).retryable,
        )
        assertEquals(
            "Goal: 1920 kcal.",
            MetriaAgentFactAuditor.audit("Goal: 1920 kcal.", evidence).acceptedText,
        )
        assertEquals(
            "Your 1920 kcal goal is weekly.",
            MetriaAgentFactAuditor.audit("Your 1920 kcal goal is weekly.", evidence).acceptedText,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit(
                "Según tus registros, no hay comidas registradas la semana pasada.",
                evidence,
            ).retryable,
        )
        assertEquals(
            "Según tus registros, no hay comidas registradas en los últimos 7 días.",
            MetriaAgentFactAuditor.audit(
                "Según tus registros, no hay comidas registradas en los últimos 7 días.",
                evidence,
            ).acceptedText,
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
        assertEquals(
            RequiredAgentTool("read_meals", daysAgo = 4),
            MetriaAgentGroundingPolicy.requiredTool("¿Qué comí hace cuatro días?"),
        )
    }

    @Test
    fun `explicit nutrition goal comparisons require fresh matching summary`() {
        assertEquals(
            RequiredAgentTool("read_nutrition_summary", "week", metric = "protein"),
            MetriaAgentGroundingPolicy.requiredTool("¿Cumplí mi objetivo de proteína esta semana?"),
        )
        assertEquals(
            RequiredAgentTool("read_nutrition_summary", "week", metric = "protein"),
            MetriaAgentGroundingPolicy.requiredTool("cumplí mi objetivo de proteína de esta semana?"),
        )
        assertEquals(
            RequiredAgentTool("read_nutrition_summary", "week", metric = "protein"),
            MetriaAgentGroundingPolicy.requiredTool("cumplí mi objetivos de proteína de esta semana?"),
        )
        assertEquals(
            RequiredAgentTool("read_nutrition_summary", "today", metric = "calories"),
            MetriaAgentGroundingPolicy.requiredTool("Did I meet my calorie target today?"),
        )
        assertEquals(
            RequiredAgentTool("read_nutrition_summary", "week", metric = "protein"),
            MetriaAgentGroundingPolicy.requiredTool("Have I hit my protein goal this week?"),
        )
        assertEquals(
            RequiredAgentTool("read_nutrition_summary", "today", metric = "calories"),
            MetriaAgentGroundingPolicy.requiredTool("¿He cumplido mi meta de calorías hoy?"),
        )
        assertEquals(
            RequiredAgentTool("read_nutrition_summary", "week", metric = "protein"),
            MetriaAgentGroundingPolicy.requiredTool("¿Cumplí mi objetivo semanal de proteína?"),
        )
        assertEquals(
            RequiredAgentTool("read_nutrition_summary", "week", metric = "protein"),
            MetriaAgentGroundingPolicy.requiredTool("Did I meet my weekly protein goal?"),
        )
        assertNull(
            MetriaAgentGroundingPolicy.requiredTool("¿Cuál es mi objetivo de proteína esta semana?"),
        )
        assertNull(
            MetriaAgentGroundingPolicy.requiredTool("Did I set my protein goal today?"),
        )
        assertNull(
            MetriaAgentGroundingPolicy.requiredTool("Did I meet my protein goal last week?"),
        )
        assertNull(
            MetriaAgentGroundingPolicy.requiredTool("¿Cumplí mi objetivo de proteína hoy esta semana?"),
        )
        assertEquals(
            RequiredAgentTool("read_nutrition_summary", "week"),
            MetriaAgentGroundingPolicy.requiredTool("Did I meet my calorie and protein goals this week?"),
        )
        assertNull(
            MetriaAgentGroundingPolicy.requiredTool("¿Cumplí mi objetivo de proteína cada día esta semana?"),
        )
        assertNull(MetriaAgentGroundingPolicy.requiredTool("Did I meet my daily protein goal this week?"))
        assertNull(MetriaAgentGroundingPolicy.requiredTool("Did I meet my protein goal every day this week?"))
        assertNull(MetriaAgentGroundingPolicy.requiredTool("Did I meet my protein goal last week and this week?"))
    }

    @Test
    fun `explicit recorded nutrition totals require one supported current period and metric`() {
        val todayQuestion = "¿Cuántas calorías tengo registradas hoy?"
        val todayRequired = RequiredAgentTool("read_nutrition_summary", "today", metric = "calories")
        assertEquals(todayRequired, MetriaAgentGroundingPolicy.requiredTool(todayQuestion))
        assertEquals(
            todayRequired,
            MetriaAgentGroundingPolicy.requiredTool("How many calories do I have recorded today?"),
        )
        assertEquals(
            todayRequired,
            MetriaAgentGroundingPolicy.requiredTool("How many calories do I register today?"),
        )
        assertTrue(todayRequired.accepts(PrivateCoachToolCall("read_nutrition_summary", period = "today")))
        assertFalse(todayRequired.accepts(PrivateCoachToolCall("read_nutrition_summary", period = "week")))
        assertFalse(todayRequired.accepts(PrivateCoachToolCall("read_progress_overview", period = "today")))

        val weekProtein = RequiredAgentTool("read_nutrition_summary", "week", metric = "protein")
        assertEquals(weekProtein, MetriaAgentGroundingPolicy.requiredTool("¿Cuánta proteína tengo registrada esta semana?"))
        assertEquals(weekProtein, MetriaAgentGroundingPolicy.requiredTool("¿Cuánta proteína registré esta semana?"))
        assertEquals(
            weekProtein,
            MetriaAgentGroundingPolicy.requiredTool("How much protein did I log in the last 7 days?"),
        )
        assertEquals(
            weekProtein,
            MetriaAgentGroundingPolicy.requiredTool("How much protein did I log in the last seven days?"),
        )

        listOf(
            "How many calories should I register today?",
            "How many calories should I log today?",
            "How many calories should I record today?",
            "How many calories did I record this month?",
            "How many calories did I record this month and today?",
            "How many calories did I record last week?",
            "How many calories did I record today and this week?",
            "How many calories did I record today and in the last 7 days?",
            "How many calories and grams of protein did I record today?",
            "How many calories did I record per day this week?",
            "How many calories did I record daily this week?",
            "¿Cuántas calorías consumí al día esta semana?",
            "¿Cuántas calorías registré diariamente esta semana?",
            "¿Cuántas calorías tengo registradas este mes y hoy?",
            "¿Cuántas calorías tengo registradas hoy y en los últimos 7 días?",
            "¿Cuántas calorías debería registrar hoy?",
        ).forEach { question ->
            assertNull(question, MetriaAgentGroundingPolicy.requiredTool(question))
        }
        assertEquals(
            RequiredAgentTool("read_day_nutrition", period = "historical_day", metric = "calories", daysAgo = 4),
            MetriaAgentGroundingPolicy.requiredTool("How many calories did I eat four days ago?"),
        )
    }

    @Test
    fun `recorded nutrition total fallback keeps missing estimate period and metric bound`() {
        val required = RequiredAgentTool("read_nutrition_summary", "today", metric = "calories")
        val response = mapOf<String, Any?>(
            "source" to "metria_local_vault",
            "period" to "today",
            "reply_language" to "Spanish",
            "meals" to mapOf("recorded_count" to 2),
            "nutrition" to mapOf("calories_kcal" to null, "calorie_goal_kcal" to 1800),
        )

        val reply = MetriaNutritionReplyFallback.from(required, response)
        assertTrue(reply!!.contains("No hay una estimación de calorías hoy"))
        assertTrue(reply.contains("1800 kcal"))
        assertNull(MetriaNutritionReplyFallback.from(required, response + ("period" to "week")))
        assertNull(MetriaNutritionReplyFallback.from(required.copy(metric = "protein"), response))
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

    @Test
    fun `nutrition follow up inherits period from the recent member question`() {
        val weekHistory = listOf(
            PrivateConversationMessage("¿Cómo va mi semana?", fromMember = true),
            PrivateConversationMessage("Esta semana llevas 1250 kcal.", fromMember = false),
        )
        assertEquals(
            RequiredAgentTool("read_nutrition_summary", "week", metric = "protein"),
            MetriaAgentGroundingPolicy.requiredTool("¿Y de proteína?", weekHistory),
        )

        val todayHistory = listOf(
            PrivateConversationMessage("How am I doing today?", fromMember = true),
        )
        assertEquals(
            RequiredAgentTool("read_nutrition_summary", "today", metric = "protein"),
            MetriaAgentGroundingPolicy.requiredTool("And protein?", todayHistory),
        )

        val requiredWeek = RequiredAgentTool("read_nutrition_summary", "week")
        assertTrue(requiredWeek.accepts(PrivateCoachToolCall("read_nutrition_summary", period = "week")))
        assertFalse(requiredWeek.accepts(PrivateCoachToolCall("read_nutrition_summary", period = "today")))

        val stalePeriodHistory = listOf(
            PrivateConversationMessage("How am I doing this week?", fromMember = true),
            PrivateConversationMessage("Earlier reply", fromMember = false),
            PrivateConversationMessage("What did I eat yesterday?", fromMember = true),
            PrivateConversationMessage("Unrelated answer", fromMember = false),
        )
        assertNull(MetriaAgentGroundingPolicy.requiredTool("And protein?", stalePeriodHistory))

        val previousWeekHistory = listOf(
            PrivateConversationMessage("Did I meet my protein goal last week?", fromMember = true),
        )
        assertNull(MetriaAgentGroundingPolicy.requiredTool("And calories?", previousWeekHistory))

        val chainedFollowUpHistory = weekHistory + listOf(
            PrivateConversationMessage("¿Y de proteína?", fromMember = true),
            PrivateConversationMessage("Respuesta sobre proteína.", fromMember = false),
        )
        assertEquals(
            RequiredAgentTool("read_nutrition_summary", "week", metric = "calories"),
            MetriaAgentGroundingPolicy.requiredTool("¿Y calorías?", chainedFollowUpHistory),
        )
    }

    @Test
    fun `typed nutrition follow up crosses voice transcript and ignores recording placeholder`() {
        val history = listOf(
            PrivateConversationMessage("¿Cómo van mis objetivos de nutrición esta semana?", fromMember = true),
            PrivateConversationMessage("Respuesta semanal", fromMember = false),
            PrivateConversationMessage("Voice request received locally.", fromMember = true),
            PrivateConversationMessage("Voice: y de proteína?", fromMember = true),
            PrivateConversationMessage("Respuesta de voz", fromMember = false),
        )
        assertEquals(
            RequiredAgentTool("read_nutrition_summary", "week", metric = "protein"),
            MetriaAgentGroundingPolicy.requiredTool("¿Y de proteína?", history),
        )
        assertNull(MetriaAgentGroundingPolicy.requiredTool(
            "¿Y de proteína?",
            history + PrivateConversationMessage("What did I eat yesterday?", fromMember = true),
        ))
    }

    @Test
    fun `short nutrition fallback uses only the fresh matching tool result`() {
        val required = RequiredAgentTool("read_nutrition_summary", "week", metric = "protein")
        val response = mapOf<String, Any?>(
            "source" to "metria_local_vault",
            "period" to "last_7_days_including_today",
            "reply_language" to "Spanish",
            "meals" to mapOf("recorded_count" to 0),
            "nutrition" to mapOf("protein_g" to null, "protein_goal_g" to 1260),
        )
        val reply = MetriaNutritionReplyFallback.from(required, response)
        assertTrue(reply!!.contains("No hay una estimación de proteína"))
        assertTrue(reply.contains("1260 g de proteína"))
        assertEquals(reply, MetriaAgentFactAuditor.audit(
            reply,
            MetriaAgentFactEvidence.from("read_nutrition_summary", response),
        ).acceptedText)
        assertNull(MetriaNutritionReplyFallback.from(required, response + ("period" to "today")))
        assertNull(MetriaNutritionReplyFallback.from(required, response + ("source" to "unknown")))
        assertNull(MetriaNutritionReplyFallback.from(required, response - "nutrition"))
        assertNull(MetriaNutritionReplyFallback.from(required,
            response + ("nutrition" to mapOf("protein_g" to -1, "protein_goal_g" to 1260))))

        val withProtein = response + ("nutrition" to mapOf("protein_g" to 82, "protein_goal_g" to 1260))
        assertTrue(MetriaNutritionReplyFallback.from(required, withProtein)!!.contains("82 g de proteína"))
    }

    @Test
    fun `historical day questions route to exact bounded read tools`() {
        assertEquals(
            RequiredAgentTool("read_meals", daysAgo = 4),
            MetriaAgentGroundingPolicy.requiredTool("¿Qué comí hace cuatro días?"),
        )
        assertEquals(
            RequiredAgentTool("read_meals", daysAgo = 4),
            MetriaAgentGroundingPolicy.requiredTool("What did I eat 4 days ago?"),
        )
        assertEquals(
            RequiredAgentTool("read_weight", fromDaysAgo = 1, toDaysAgo = 1),
            MetriaAgentGroundingPolicy.requiredTool("¿Cuánto pesaba ayer?"),
        )
        assertEquals(
            RequiredAgentTool("read_weight", fromDaysAgo = 4, toDaysAgo = 4),
            MetriaAgentGroundingPolicy.requiredTool("What was my weight four days ago?"),
        )
        assertNull(MetriaAgentGroundingPolicy.requiredTool("What did I eat 91 days ago?"))
        assertEquals(
            RequiredAgentTool("read_day_nutrition", period = "historical_day", metric = "calories", daysAgo = 4),
            MetriaAgentGroundingPolicy.requiredTool("¿Cuántas calorías consumí hace cuatro días?"),
        )
        assertEquals(
            RequiredAgentTool("read_day_nutrition", period = "historical_day", metric = "protein", daysAgo = 4),
            MetriaAgentGroundingPolicy.requiredTool("How much protein did I eat 4 days ago?"),
        )
        assertEquals(
            RequiredAgentTool("read_day_nutrition", period = "historical_day", metric = "calories", daysAgo = 90),
            MetriaAgentGroundingPolicy.requiredTool("How many calories did I eat 90 days ago?"),
        )
        assertNull(MetriaAgentGroundingPolicy.requiredTool("What did I eat 91 days ago?"))
        assertNull(MetriaAgentGroundingPolicy.requiredTool("How much protein did I eat 91 days ago?"))
    }

    @Test
    fun `calendar date nutrition question binds the requested day and rejects another day`() {
        val today = java.time.LocalDate.of(2026, 9, 23)
        val question = "¿Cuántas calorías consumí el 27 de agosto?"
        val required = MetriaAgentGroundingPolicy.requiredTool(question, today = today)
        assertEquals(
            RequiredAgentTool("read_day_nutrition", period = "historical_day", metric = "calories",
                daysAgo = 27, targetDate = java.time.LocalDate.of(2026, 8, 27)),
            required,
        )
        assertTrue(required!!.accepts(PrivateCoachToolCall("read_day_nutrition", "historical_day", daysAgo = 27)))
        assertFalse(required.accepts(PrivateCoachToolCall("read_day_nutrition", "historical_day", daysAgo = 26)))
        assertNull(MetriaAgentGroundingPolicy.historicalClarification(question, today))
    }

    @Test
    fun `calendar date weight and meal reads stay exact and unsupported dates clarify`() {
        val today = java.time.LocalDate.of(2026, 9, 23)
        assertEquals(RequiredAgentTool("read_meals", daysAgo = 27, targetDate = java.time.LocalDate.of(2026, 8, 27)),
            MetriaAgentGroundingPolicy.requiredTool("¿Qué comí el 27 de agosto?", today = today))
        assertEquals(RequiredAgentTool("read_weight", fromDaysAgo = 27, toDaysAgo = 27,
            targetDate = java.time.LocalDate.of(2026, 8, 27)),
            MetriaAgentGroundingPolicy.requiredTool("What was my weight on August 27?", today = today))
        listOf("¿Cuántas calorías consumí el 27/08/2026?", "¿Cuántas calorías consumí el 27 de agosto de 2025?",
            "How many calories did I eat on August 27 compared with August 28?").forEach { question ->
            assertNull(question, MetriaAgentGroundingPolicy.requiredTool(question, today = today))
            assertTrue(question, MetriaAgentGroundingPolicy.historicalClarification(question, today) != null)
        }
    }

    @Test
    fun `natural calendar date in nutrition reply is accepted only for exact evidence day`() {
        val evidence = MetriaAgentFactEvidence.from("read_day_nutrition", mapOf(
            "source" to "metria_local_vault", "as_of" to "2026-09-23", "period" to "historical_day",
            "days_ago" to 27, "from" to "2026-08-27", "to" to "2026-08-27",
            "requested_metric" to "calories", "nutrition" to mapOf("calories_kcal" to 450.0),
        ))
        val answer = "El 27 de agosto, las comidas registradas suman una estimación de 450 kcal."
        assertEquals(answer, MetriaAgentFactAuditor.audit(answer, evidence).acceptedText)
        assertTrue(MetriaAgentFactAuditor.audit("El 28 de agosto, las comidas registradas suman 450 kcal.", evidence).retryable)
        assertTrue(MetriaAgentFactAuditor.audit("El 27 de agosto, las comidas registradas suman 450 g de proteína.", evidence).retryable)
    }

    @Test
    fun `exact day fallback discloses partial and missing coverage truthfully`() {
        val today = java.time.LocalDate.of(2026, 9, 23)
        val required = RequiredAgentTool("read_day_nutrition", period = "historical_day", metric = "calories", daysAgo = 27)
        fun recordedMeal(label: String, nutrition: MealNutrition?) = MealEntry(
                recordedAt = java.time.Instant.parse("2026-08-27T12:00:00Z"),
                items = listOf(MealEntryItem(label, 100, null)), assumptions = emptyList(),
                confidence = "high", source = "manual_entry", nutrition = nutrition,
            )
        fun responseFor(dayMeals: List<MealEntry>) = HistoricalDayNutritionResult.from(
            dayMeals, today, 27, "calories",
        ) + ("reply_language" to "English")
        val complete = recordedMeal("Complete", MealNutrition(2210.0750000000003, 82.345, 0.0, 0.0))
        val missing = recordedMeal("Missing", null)
        val partialMeal = recordedMeal("Partial", MealNutrition(50.0, 3.0, 2.0, 1.0, estimateCoverage = "partial"))
        val response = responseFor(listOf(complete))
        assertEquals(2210.0, (response["nutrition"] as Map<*, *>)["calories_kcal"])
        val fallback = MetriaNutritionReplyFallback.from(required, response)!!
        assertTrue(fallback.contains("2210 kcal"))
        assertFalse(fallback.contains("Some meals have partial estimates"))
        assertFalse(fallback.contains("no nutrition estimate"))
        assertEquals(fallback, MetriaAgentFactAuditor.audit(
            fallback, MetriaAgentFactEvidence.from("read_day_nutrition", response),
        ).acceptedText)
        assertNull(MetriaNutritionReplyFallback.from(required.copy(metric = "protein"), response))
        assertNull(MetriaNutritionReplyFallback.from(required, response + ("from" to "2026-08-28") + ("to" to "2026-08-28")))

        val missingOnly = responseFor(listOf(complete, missing))
        val missingReply = MetriaNutritionReplyFallback.from(required, missingOnly)!!
        assertTrue(missingReply.contains("Available estimates from meals recorded"))
        assertTrue(missingReply.contains("Some recorded meals have no nutrition estimate"))
        assertFalse(missingReply.contains("partial estimates"))
        assertEquals(missingReply, MetriaAgentFactAuditor.audit(
            missingReply, MetriaAgentFactEvidence.from("read_day_nutrition", missingOnly),
        ).acceptedText)

        val partialAndMissing = responseFor(listOf(complete, partialMeal, missing))
        val partialMissingReply = MetriaNutritionReplyFallback.from(required, partialAndMissing)!!
        assertTrue(partialMissingReply.contains("Some meals have partial estimates"))
        assertTrue(partialMissingReply.contains("Some recorded meals have no nutrition estimate"))
        assertEquals(partialMissingReply, MetriaAgentFactAuditor.audit(
            partialMissingReply, MetriaAgentFactEvidence.from("read_day_nutrition", partialAndMissing),
        ).acceptedText)
        val spanishProtein = required.copy(metric = "protein")
        val proteinResult = (response + mapOf(
            "requested_metric" to "protein", "reply_language" to "Spanish",
            "nutrition" to mapOf("calories_kcal" to 2210.0, "protein_g" to 82.3),
        ))
        assertTrue(MetriaNutritionReplyFallback.from(spanishProtein, proteinResult)!!.contains("82.3 g de proteína"))
    }

    @Test
    fun spelledRelativeOffsetsFromEightThroughNinetyBindToTheExactReadDay() {
        val cases = listOf(
            "What did I eat eight days ago?" to 8,
            "What did I eat ten days ago?" to 10,
            "What did I eat thirty days ago?" to 30,
            "What did I eat ninety days ago?" to 90,
            "¿Qué comí hace ocho días?" to 8,
            "¿Qué comí hace diez días?" to 10,
            "¿Qué comí hace treinta días?" to 30,
            "¿Qué comí hace noventa días?" to 90,
        )

        cases.forEach { (question, expectedOffset) ->
            val required = MetriaAgentGroundingPolicy.requiredTool(question)
            assertEquals(question, expectedOffset, required?.daysAgo)
            assertTrue(question, required!!.accepts(PrivateCoachToolCall(required.name, daysAgo = expectedOffset)))
            assertFalse(question, required.accepts(PrivateCoachToolCall(required.name, daysAgo = expectedOffset - 1)))
            assertFalse(question, required.accepts(PrivateCoachToolCall(required.name, daysAgo = expectedOffset + 1)))
        }
    }

    @Test
    fun unparsedSpelledRelativeDaysGetADeterministicNoReadClarification() {
        assertNull(MetriaAgentGroundingPolicy.requiredTool("How much protein did I eat a hundred days ago?"))
        assertTrue(
            MetriaAgentGroundingPolicy.historicalClarification("How much protein did I eat a hundred days ago?")!!.contains("90"),
        )
        assertNull(MetriaAgentGroundingPolicy.requiredTool("¿Qué comí hace varios días?"))
        assertTrue(MetriaAgentGroundingPolicy.historicalClarification("¿Qué comí hace varios días?")!!.contains("90"))
    }

    @Test
    fun SpanishSpelledOffsetsAboveNinetyClarifyInsteadOfSelectingAnInnerNumber() {
        val question = "¿Cuánta proteína consumí hace ciento veinte días?"
        assertNull(MetriaAgentGroundingPolicy.requiredTool(question))
        assertTrue(MetriaAgentGroundingPolicy.historicalClarification(question)!!.contains("90"))
    }

    @Test
    fun twoDistinctRelativeDayOffsetsClarifyInsteadOfRoutingTheFirstDay() {
        val question = "How much protein did I eat four days ago compared with three days ago?"
        assertNull(MetriaAgentGroundingPolicy.requiredTool(question))
        assertTrue(MetriaAgentGroundingPolicy.historicalClarification(question)!!.contains("one specific day"))
    }

    @Test
    fun `unsupported historical requests get deterministic no-read clarification`() {
        assertTrue(MetriaAgentGroundingPolicy.historicalClarification("¿Qué comí hace 91 días?")!!.contains("90"))
        assertTrue(MetriaAgentGroundingPolicy.historicalClarification("What did I eat 91 days ago?")!!.contains("90"))
        assertTrue(MetriaAgentGroundingPolicy.historicalClarification("How much protein did I eat 91 days ago?")!!.contains("90"))
        assertNull(MetriaAgentGroundingPolicy.historicalClarification("¿Cuántas calorías consumí hace cuatro días?"))
        assertNull(MetriaAgentGroundingPolicy.historicalClarification("How many calories did I eat 4 days ago?"))
        assertNull(MetriaAgentGroundingPolicy.historicalClarification("How was my nutrition 4 days ago?"))
        assertNull(MetriaAgentGroundingPolicy.historicalClarification("¿Cómo va mi nutrición hace cuatro días?"))
        assertNull(MetriaAgentGroundingPolicy.historicalClarification("¿Cuántas calorías consumí hoy?"))
        assertNull(MetriaAgentGroundingPolicy.historicalClarification("How many calories today?"))
        assertNull(MetriaAgentGroundingPolicy.historicalClarification("¿Cómo va mi nutrición hoy?"))
        assertNull(MetriaAgentGroundingPolicy.historicalClarification("¿Qué comí hace cuatro días?"))
    }

    @Test
    fun `historical required tools accept only exact read offset`() {
        val meals = RequiredAgentTool("read_meals", daysAgo = 4)
        assertTrue(meals.accepts(PrivateCoachToolCall("read_meals", daysAgo = 4)))
        assertFalse(meals.accepts(PrivateCoachToolCall("read_meals", daysAgo = 3)))
        assertFalse(meals.accepts(PrivateCoachToolCall("read_weight", daysAgo = 4)))
        assertTrue(meals.retryInstruction.contains("days_ago=4"))

        val weight = RequiredAgentTool("read_weight", fromDaysAgo = 4, toDaysAgo = 4)
        assertTrue(weight.accepts(PrivateCoachToolCall("read_weight", fromDaysAgo = 4, toDaysAgo = 4)))
        assertFalse(weight.accepts(PrivateCoachToolCall("read_weight", fromDaysAgo = 5, toDaysAgo = 4)))
        assertFalse(weight.accepts(PrivateCoachToolCall("read_weight", fromDaysAgo = 4, toDaysAgo = 3)))

        val nutrition = RequiredAgentTool("read_day_nutrition", "historical_day", daysAgo = 4)
        assertTrue(nutrition.accepts(PrivateCoachToolCall("read_day_nutrition", "historical_day", daysAgo = 4)))
        assertFalse(nutrition.accepts(PrivateCoachToolCall("read_day_nutrition", "historical_day", daysAgo = 3)))
        assertFalse(nutrition.accepts(PrivateCoachToolCall("read_day_nutrition", "today", daysAgo = 4)))
        assertTrue(nutrition.retryInstruction.contains("days_ago=4"))
        assertTrue(weight.retryInstruction.contains("from_days_ago=4") && weight.retryInstruction.contains("to_days_ago=4"))
    }

    @Test
    fun `historical nutrition audit binds exact day period metric and partial role`() {
        val response = mapOf<String, Any?>(
            "source" to "metria_local_vault",
            "as_of" to "2026-08-10",
            "period" to "historical_day",
            "days_ago" to 4,
            "from" to "2026-08-06",
            "to" to "2026-08-06",
            "requested_metric" to "protein",
            "nutrition" to mapOf(
                "calories_kcal" to 1_100.0,
                "complete_calories_kcal" to 900.0,
                "protein_g" to 100.0,
                "complete_protein_g" to 80.0,
                "partial_protein_g" to 20.0,
            ),
        )
        val evidence = MetriaAgentFactEvidence.from("read_day_nutrition", response)

        assertEquals(
            "Hace 4 días se registraron comidas con una estimación de 100 g de proteína.",
            MetriaAgentFactAuditor.audit("Hace 4 días se registraron comidas con una estimación de 100 g de proteína.", evidence).acceptedText,
        )
        assertEquals(
            "Hace 4 días había 80 g de proteína en comidas con nutrición completa.",
            MetriaAgentFactAuditor.audit("Hace 4 días había 80 g de proteína en comidas con nutrición completa.", evidence).acceptedText,
        )
        assertEquals(
            "Hace 4 días, la estimación parcial fue de 20 g de proteína.",
            MetriaAgentFactAuditor.audit("Hace 4 días, la estimación parcial fue de 20 g de proteína.", evidence).acceptedText,
        )
        assertTrue(MetriaAgentFactAuditor.audit("Hace 3 días había 100 g de proteína.", evidence).retryable)
        assertTrue(MetriaAgentFactAuditor.audit("El 2026-08-05 había 100 g de proteína.", evidence).retryable)
        assertTrue(MetriaAgentFactAuditor.audit("En los últimos 7 días había 100 g de proteína.", evidence).retryable)
        assertTrue(MetriaAgentFactAuditor.audit("Hace 4 días había 1.100 kcal.", evidence).retryable)
        assertTrue(MetriaAgentFactAuditor.audit("Hace 4 días el objetivo era 100 g de proteína.", evidence).retryable)
        assertTrue(MetriaAgentFactAuditor.audit("Había 100 g de proteína.", evidence).retryable)
    }

    @Test
    fun `historical nutrition audit rejects internally mismatched exact day evidence`() {
        val response = mapOf<String, Any?>(
            "source" to "metria_local_vault",
            "as_of" to "2026-08-10",
            "period" to "historical_day",
            "days_ago" to 4,
            "from" to "2026-08-05",
            "to" to "2026-08-05",
            "nutrition" to mapOf("protein_g" to 80.0),
        )
        val evidence = MetriaAgentFactEvidence.from("read_day_nutrition", response)
        assertTrue(MetriaAgentFactAuditor.audit("Hace 5 días había 80 g de proteína.", evidence).retryable)
    }

    @Test
    fun `historical audit rejects dates outside exact host response range`() {
        val evidence = MetriaAgentFactEvidence.from(
            "read_meals",
            mapOf(
                "source" to "metria_local_vault",
                "as_of" to "2026-09-23",
                "from" to "2026-09-19",
                "to" to "2026-09-19",
                "meals" to listOf(mapOf("date" to "2026-09-19", "title" to "Arroz", "slot" to "lunch")),
            ),
        )
        assertEquals(
            "El 2026-09-19 registraste Arroz.",
            MetriaAgentFactAuditor.audit("El 2026-09-19 registraste Arroz.", evidence).acceptedText,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit("El 2026-09-20 registraste Arroz.", evidence).retryable,
        )
        assertTrue(
            MetriaAgentFactAuditor.audit("Ayer registraste Arroz.", evidence).retryable,
        )
        assertEquals(
            "Hace cuatro días registraste Arroz.",
            MetriaAgentFactAuditor.audit("Hace cuatro días registraste Arroz.", evidence).acceptedText,
        )
    }

    @Test
    fun `historical weight audit rejects relative day outside exact range`() {
        val evidence = MetriaAgentFactEvidence.from(
            "read_weight",
            mapOf(
                "source" to "metria_local_vault",
                "as_of" to "2026-09-23",
                "from" to "2026-09-19",
                "to" to "2026-09-19",
                "entries" to listOf(mapOf("date" to "2026-09-19", "kilograms" to 90.5)),
            ),
        )
        assertTrue(MetriaAgentFactAuditor.audit("Ayer pesabas 90.5 kg.", evidence).retryable)
        assertEquals(
            "Four days ago you weighed 90.5 kg.",
            MetriaAgentFactAuditor.audit("Four days ago you weighed 90.5 kg.", evidence).acceptedText,
        )
    }
}

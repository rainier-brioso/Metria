package app.metria.cloud

import java.time.LocalDate
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class OnlineBrokerContractTest {
    private val today = LocalDate.of(2026, 10, 3)
    private val model = "nvidia/fictional-test-model"
    private val request = OnlineAssistanceRequest(UUID.fromString("00000000-0000-4000-8000-000000000002"),
        "Review these fictional records", OnlineLanguage.Spanish, today.minusDays(6), today, model,
        OnlineWeightSummary(2, today.minusDays(3), 70.0, today, 69.5, -0.5),
        OnlineNutritionSummary(3, 1, 1, 1, 451.0, 21.2, 120.0, 4.5))
    private fun parse(body: String) = OnlineBrokerContract.parseRequest(body, today, setOf(model))
    private fun body() = JSONObject(request.canonicalPayload(today)!!)
    private fun response() = JSONObject().put("schema_version", 1).put("request_id", request.requestId.toString())
        .put("language", "Spanish").put("sections", JSONArray().put("weight").put("nutrition"))
        .put("guidance", "recorded_context_only")

    @Test fun `canonical preview round trips with selected empty or omitted categories`() {
        listOf(request, request.copy(weight = null, nutrition = null), request.copy(
            weight = OnlineWeightSummary(0, null, null, null, null, null),
            nutrition = OnlineNutritionSummary(0, 0, 0, 0, null, null, null, null))).forEach {
            val payload = it.canonicalPayload(today)!!
            assertEquals(payload, parse(payload)!!.canonicalPayload(today))
        }
    }

    @Test fun `unknown nested fields and explicit null categories fail closed`() {
        listOf(body().put("photos", JSONArray()), body().put("weight", JSONObject.NULL),
            body().also { it.getJSONObject("period").put("notes", "private") },
            body().also { it.getJSONObject("weight").put("goal", 60) },
            body().also { it.getJSONObject("nutrition").put("meal_names", JSONArray()) })
            .forEach { assertNull(parse(it.toString())) }
    }

    @Test fun `coercible strings fractional counts wrong metadata and unconfigured models fail`() {
        listOf(body().put("schema_version", "1"), body().put("schema_version", 1.1), body().put("question", 42),
            body().put("request_id", "1-1-1-1-1"), body().put("provider", "other"), body().put("language", "French"),
            body().put("model", "nvidia/unconfigured"),
            body().also { it.getJSONObject("weight").put("count", 2.5) },
            body().also { it.getJSONObject("weight").put("first_kg", "70") },
            body().also { it.getJSONObject("weight").put("latest_kg", 69.555) })
            .forEach { assertNull(parse(it.toString())) }
    }

    @Test fun `broker independently checks date coverage counts and metric associations`() {
        listOf(body().also { it.getJSONObject("period").put("end", today.plusDays(1).toString()) },
            body().also { it.getJSONObject("weight").put("change_kg", 0.5) },
            body().also { it.getJSONObject("nutrition").put("recorded_meals", 4) },
            body().also { it.getJSONObject("nutrition").put("complete_calories_kcal", -1) },
            body().also { it.getJSONObject("nutrition").put("interpretation", "total_intake") })
            .forEach { assertNull(parse(it.toString())) }
    }

    @Test fun `strict syntax rejects duplicates permissive JSON trailing content and oversized bodies`() {
        val valid = request.canonicalPayload(today)!!
        listOf(valid.replace("\"schema_version\":1", "\"schema_version\":1,\"schema_version\":1"),
            valid.replace("\"schema_version\":1", "schema_version:1"),
            valid.replace("\"schema_version\":1", "\"schema_version\":01"),
            valid.replace("\"schema_version\":1", "\"schema_version\":NaN"),
            valid.dropLast(1) + ",}", valid + "{}", "/*comment*/$valid", " ".repeat(16_385))
            .forEach { assertNull(parse(it)) }
        assertNull(StrictOnlineJson.objectOrNull("{\"a\":1,\"\\u0061\":2}"))
        assertNull(StrictOnlineJson.objectOrNull("{\"a\":'single'}"))
        assertNull(StrictOnlineJson.objectOrNull("{\"a\":[1,]}"))
        assertNull(StrictOnlineJson.objectOrNull("{\"a\":\"bad\\x\"}"))
        assertNull(StrictOnlineJson.objectOrNull("{\"a\":" + "[".repeat(10) + "0" + "]".repeat(10) + "}"))
        assertNotNull(parse(" \n$valid\t"))
    }

    @Test fun `response selection binds language request and entire selected summaries`() {
        val result = OnlineResponseContract.validate(response().toString(), request)!!
        assertEquals(listOf(OnlineSummarySection.weight, OnlineSummarySection.nutrition), result.sections)
        assertEquals(OnlineGuidance.recorded_context_only, result.guidance)
        OnlineGuidance.entries.forEach {
            assertNotNull(OnlineResponseContract.validate(response().put("guidance", it.name).toString(), request))
        }
        assertNotNull(OnlineResponseContract.validate(response().put("sections", JSONArray()).toString(), request.copy(weight = null, nutrition = null)))
    }

    @Test fun `model cannot invent values sections actions prose or identity`() {
        listOf(response().put("answer", "You ate 2000 calories"), response().put("save", true),
            response().put("request_id", UUID.randomUUID().toString()), response().put("language", "English"),
            response().put("guidance", "restrict_food"), response().put("schema_version", "1"),
            response().put("sections", JSONArray().put("weight").put("weight")),
            response().put("sections", JSONArray().put("goals")), response().put("sections", JSONArray().put(1)))
            .forEach { assertNull(OnlineResponseContract.validate(it.toString(), request)) }
        assertNull(OnlineResponseContract.validate(response().toString(), request.copy(weight = null)))
        assertNull(OnlineResponseContract.validate("a".repeat(2_049), request))
        assertNull(OnlineResponseContract.validate(response().toString() + " extra", request))
    }
}

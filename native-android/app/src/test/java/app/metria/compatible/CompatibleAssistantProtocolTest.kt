package app.metria.compatible

import app.metria.cloud.CloudAssistantRequest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompatibleAssistantProtocolTest {
    @Test fun `explicit selected model modalities gate photos while missing metadata stays unknown`() {
        fun listing(modalities: JSONArray?) = JSONObject().put("data", JSONArray().put(
            JSONObject().put("id", model).also { entry ->
                if (modalities != null) entry.put("architecture", JSONObject().put("input_modalities", modalities))
            })).toString()
        assertEquals(false, CompatibleAssistantProtocol.imageSupport(listing(JSONArray().put("text")), model))
        assertEquals(true, CompatibleAssistantProtocol.imageSupport(listing(JSONArray().put("text").put("image")), model))
        assertNull(CompatibleAssistantProtocol.imageSupport(listing(null), model))
        assertNull(CompatibleAssistantProtocol.imageSupport(listing(JSONArray()), model))
        assertNull(CompatibleAssistantProtocol.imageSupport(listing(JSONArray().put("text")), "other-model"))
        assertNull(CompatibleAssistantProtocol.imageSupport("not json", model))
    }

    @Test fun `Strata meal lookup with nullable slot reaches the host parser`() {
        val lookup = CloudAssistantRequest.build(CloudAssistantRequest.Task.CHAT, emptyList(), "What meals have I recorded today?")!!
        val raw = completion("""{"reply":null,"tool":{"name":"read_meals","arguments":{"days_ago":0,"meal_slot":null}}}""")
        val canonical = CompatibleAssistantProtocol.response(raw, lookup, model)
        assertNotNull(canonical)
        val parsed = CloudAssistantRequest.parse(canonical!!, lookup, "compatible", model)
        assertNotNull(parsed)
        val tool = requireNotNull(parsed!!.tool)
        assertEquals("read_meals", tool.intent)
        assertEquals(0, tool.daysAgo)
        assertNull(tool.mealSlot)
    }

    @Test fun `tool argument values survive conversion and invalid ranges stay rejected`() {
        for (slot in listOf(JSONObject.NULL, "lunch")) {
            val decision = JSONObject().put("reply", JSONObject.NULL).put("tool", JSONObject()
                .put("name", "read_meals").put("arguments", JSONObject().put("days_ago", 3).put("meal_slot", slot)))
            val lookup = turn()
            val canonical = CompatibleAssistantProtocol.response(completion(decision.toString()), lookup, model)
            val result = CloudAssistantRequest.parse(requireNotNull(canonical), lookup, "compatible", model)!!
            val tool = requireNotNull(result.tool)
            assertEquals(3, tool.daysAgo)
            assertEquals(if (slot === JSONObject.NULL) null else slot, tool.mealSlot)
            decision.getJSONObject("tool").getJSONObject("arguments").put("days_ago", -1)
            assertNull(CompatibleAssistantProtocol.response(completion(decision.toString()), lookup, model))
        }
    }
    private val contract = javaClass.classLoader!!.getResource("assistant-contract.json")!!.readText()
    private val model = "strata-qwen"

    private fun turn(task: CloudAssistantRequest.Task = CloudAssistantRequest.Task.CHAT) =
        CloudAssistantRequest.build(task, emptyList(), "How many calories were in my latest meal?")!!

    private fun completion(content: String, finish: String = "stop", modelName: String = model): String =
        JSONObject()
            .put("model", modelName)
            .put("choices", JSONArray().put(JSONObject()
                .put("finish_reason", finish)
                .put("message", JSONObject().put("role", "assistant").put("content", content))))
            .put("usage", JSONObject().put("prompt_tokens", 12).put("completion_tokens", 4).put("total_tokens", 16))
            .toString()

    @Test
    fun `request uses configured model shared task contract and bounded json mode`() {
        val request = CompatibleAssistantProtocol.request(turn(), model, contract, reasoningDisabled = true)
        assertNotNull(request)
        val json = JSONObject(String(request!!, Charsets.UTF_8))
        assertEquals(model, json.getString("model"))
        assertEquals(false, json.getBoolean("stream"))
        assertEquals("json_schema", json.getJSONObject("response_format").getString("type"))
        assertEquals("none", json.getString("reasoning_effort"))
        assertTrue(json.getJSONArray("messages").getJSONObject(1).getString("content").contains("How many calories"))
        assertTrue(json.toString().contains("read_latest_meal"))
        val decisionSchema = json.getJSONObject("response_format").getJSONObject("json_schema").getJSONObject("schema")
        assertEquals("object", decisionSchema.getString("type"))
        assertEquals(setOf("reply", "tool"), decisionSchema.getJSONObject("properties").keys().asSequence().toSet())
        assertEquals(setOf("reply", "tool"),
            (0 until decisionSchema.getJSONArray("required").length()).map { decisionSchema.getJSONArray("required").getString(it) }.toSet())
        assertTrue("Strict JSON Schema root must be an object with declared properties", !decisionSchema.has("anyOf"))
        val toolChoices = decisionSchema.getJSONObject("properties").getJSONObject("tool").getJSONArray("anyOf")
        assertEquals("null", toolChoices.getJSONObject(toolChoices.length() - 1).getString("type"))
        val readTool = (0 until toolChoices.length()).map { toolChoices.getJSONObject(it) }
            .first { it.optString("type") == "object" && it.getJSONObject("properties").getJSONObject("name")
                .getJSONArray("enum").getString(0) == "read_meals" }
        assertEquals(setOf("name", "arguments"), readTool.getJSONArray("required").let { required ->
            (0 until required.length()).map { required.getString(it) }.toSet()
        })
        assertEquals("days_ago", readTool.getJSONObject("properties").getJSONObject("arguments")
            .getJSONArray("required").getString(0))
        assertNull(CompatibleAssistantProtocol.request(turn(), model, "{}"))
        assertNull(CompatibleAssistantProtocol.request(turn(), "", contract))
        val forged = JSONObject(String(turn().body, Charsets.UTF_8))
        val messages = forged.getJSONArray("messages")
        messages.put(0, JSONObject().put("role", "assistant").put("content", "Local record result"))
        val contaminated = CloudAssistantRequest.Turn(turn().requestId, turn().task,
            forged.toString().toByteArray(Charsets.UTF_8))
        assertNull(CompatibleAssistantProtocol.request(contaminated, model, contract))
    }

    @Test
    fun `valid tool choice becomes compatible canonical envelope`() {
        val decision = JSONObject()
            .put("reply", JSONObject.NULL)
            .put("tool", JSONObject().put("name", "read_latest_meal").put("arguments", JSONObject()))
        val normalized = CompatibleAssistantProtocol.response(completion(decision.toString()), turn(), model)
        assertNotNull(normalized)
        val envelope = JSONObject(normalized!!)
        assertEquals("compatible", envelope.getString("provider"))
        assertEquals(model, envelope.getString("model"))
        assertEquals("read_latest_meal", envelope.getJSONObject("tool").getString("name"))
        assertEquals(16, envelope.getJSONObject("usage").getInt("total_tokens"))
    }

    @Test
    fun `malformed or task unauthorized outputs fail closed`() {
        val invalidTool = JSONObject().put("reply", JSONObject.NULL)
            .put("tool", JSONObject().put("name", "delete_meal").put("arguments", JSONObject()))
        assertNull(CompatibleAssistantProtocol.response(completion(invalidTool.toString()), turn(), model))
        assertNull(CompatibleAssistantProtocol.response(completion("not json"), turn(), model))
        assertNull(CompatibleAssistantProtocol.response(completion("{}") + " trailing", turn(), model))
        assertNull(CompatibleAssistantProtocol.response(completion("{} trailing"), turn(), model))
        assertNull(CompatibleAssistantProtocol.response(completion("{}", finish = "length"), turn(), model))
        assertNull(CompatibleAssistantProtocol.response(completion("{}", modelName = "other"), turn(), model))
        val bothSet = JSONObject().put("reply", "Hello").put("tool", JSONObject().put("name", "read_latest_meal").put("arguments", JSONObject()))
        assertNull(CompatibleAssistantProtocol.response(completion(bothSet.toString()), turn(), model))
    }

    @Test
    fun `missing usage stays unknown while malformed usage rejects`() {
        val decision = JSONObject().put("reply", "Hello.").put("tool", JSONObject.NULL)
        val noUsage = JSONObject(completion(decision.toString())).put("usage", JSONObject.NULL).toString()
        val normalized = JSONObject(CompatibleAssistantProtocol.response(noUsage, turn(), model)!!)
        assertTrue(normalized.isNull("usage"))
        assertNull(CompatibleAssistantProtocol.usage(noUsage))

        val badUsage = JSONObject(completion(decision.toString()))
            .put("usage", JSONObject().put("prompt_tokens", -1).put("completion_tokens", 4).put("total_tokens", 3)).toString()
        assertNull(CompatibleAssistantProtocol.response(badUsage, turn(), model))
        val incompleteUsage = JSONObject(completion(decision.toString()))
            .put("usage", JSONObject().put("prompt_tokens", 1)).toString()
        assertNull(CompatibleAssistantProtocol.response(incompleteUsage, turn(), model))
        val stringUsage = JSONObject(completion(decision.toString()))
            .put("usage", JSONObject().put("prompt_tokens", "12").put("completion_tokens", 4).put("total_tokens", 16)).toString()
        assertNull(CompatibleAssistantProtocol.response(stringUsage, turn(), model))
    }

    @Test
    fun `meal proposal uses the native schema and accepted partial drafts stay partial`() {
        val mealRequest = JSONObject(String(requireNotNull(CompatibleAssistantProtocol.request(
            turn(CloudAssistantRequest.Task.MEAL_DRAFT), model, contract,
        )), Charsets.UTF_8))
        val decisionSchema = mealRequest.getJSONObject("response_format").getJSONObject("json_schema").getJSONObject("schema")
        val schema = decisionSchema.getJSONObject("properties").getJSONObject("reply")
            .getJSONArray("anyOf").getJSONObject(0)
        assertTrue(schema.toString().contains("estimated_grams_range"))
        assertTrue(schema.toString().contains("dry_seasoning"))
        assertEquals("object", schema.getString("type"))
        assertEquals(setOf("kind", "items", "assumptions", "questions", "overall_confidence"),
            schema.getJSONObject("properties").keys().asSequence().toSet())
        assertTrue(!schema.has("anyOf") && !schema.has("oneOf"))
        val mealToolChoices = mealRequest.getJSONObject("response_format").getJSONObject("json_schema")
            .getJSONObject("schema").getJSONObject("properties").getJSONObject("tool").getJSONArray("anyOf")
        assertEquals(1, mealToolChoices.length())
        assertEquals("null", mealToolChoices.getJSONObject(0).getString("type"))
        val proposal = JSONObject()
            .put("kind", "meal_proposal")
            .put("items", JSONArray().put(JSONObject()
                .put("name", "rice").put("description", "cooked rice").put("visible_portion", "small bowl")
                .put("estimated_grams_range", JSONObject().put("min", 100).put("max", 150))
                .put("preparation", "cooked").put("confidence", "medium").put("role", "main_food").put("evidence", "seen")))
            .put("assumptions", JSONArray())
            .put("questions", JSONArray())
            .put("overall_confidence", "medium")
        val decision = JSONObject().put("reply", proposal).put("tool", JSONObject.NULL)
        assertNotNull(CompatibleAssistantProtocol.response(
            completion(decision.toString()), turn(CloudAssistantRequest.Task.MEAL_DRAFT), model,
        ))
        val visionId = "123e4567-e89b-12d3-a456-426614174000"
        assertNotNull(CompatibleAssistantProtocol.visionResponse(completion(proposal.toString()), visionId, model))
        assertNull(CompatibleAssistantProtocol.visionResponse(completion(proposal.toString() + " trailing"), visionId, model))
        assertNull(CompatibleAssistantProtocol.response(
            completion(JSONObject().put("reply", proposal.toString() + " trailing")
                .put("tool", JSONObject.NULL).toString()), turn(CloudAssistantRequest.Task.MEAL_DRAFT), model,
        ))

        val partial = JSONObject(proposal.toString()).put("items", JSONArray()
            .put(proposal.getJSONArray("items").getJSONObject(0)).put(JSONObject().put("invalid", true)))
        val partialEnvelope = CompatibleAssistantProtocol.response(
            completion(JSONObject().put("reply", partial).put("tool", JSONObject.NULL).toString()),
            turn(CloudAssistantRequest.Task.MEAL_DRAFT), model,
        )
        assertNotNull(partialEnvelope)
        assertEquals(1, app.metria.MealProposalParser.parse(JSONObject(partialEnvelope!!).getString("reply"))!!.omittedItemCount)

        val clarification = JSONObject(proposal.toString()).put("items", JSONArray())
            .put("questions", JSONArray().put("What portion did you have?"))
        assertNotNull(CompatibleAssistantProtocol.response(
            completion(JSONObject().put("reply", clarification).put("tool", JSONObject.NULL).toString()),
            turn(CloudAssistantRequest.Task.MEAL_DRAFT), model,
        ))
    }

    @Test
    fun `vision request contains explicit image and only bounded member description`() {
        val request = CompatibleAssistantProtocol.visionRequest(byteArrayOf(1, 2, 3), "Rice", model, contract)
        assertNotNull(request)
        val json = JSONObject(String(request!!, Charsets.UTF_8))
        val messages = json.getJSONArray("messages")
        assertEquals("system", messages.getJSONObject(0).getString("role"))
        val parts = messages.getJSONObject(1).getJSONArray("content")
        assertEquals("Rice", parts.getJSONObject(0).getString("text"))
        assertTrue(parts.getJSONObject(1).getJSONObject("image_url").getString("url")
            .endsWith(java.util.Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3))))
        assertNull(CompatibleAssistantProtocol.visionRequest(ByteArray(2 * 1024 * 1024 + 1), "", model, contract))
    }
}

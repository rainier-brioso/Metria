package app.metria.cloud

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CloudAssistantRequestTest {
    @Test
    fun `meal confirmation is generated only for explicit yes and reads take priority`() {
        assertEquals(
            CloudAssistantRequest.Task.CHAT,
            CloudAssistantRequest.selectTask(false, false, false, true, false, false),
        )
        assertEquals(
            CloudAssistantRequest.Task.MEAL_DRAFT,
            CloudAssistantRequest.selectTask(false, false, false, true, true, false),
        )
        assertEquals(
            CloudAssistantRequest.Task.CHAT,
            CloudAssistantRequest.selectTask(true, false, false, false, false, true),
        )
    }

    @Test
    fun `cloud context excludes assistant replies that may contain local record facts`() {
        val turn = CloudAssistantRequest.build(
            CloudAssistantRequest.Task.CHAT,
            listOf(false to "You logged 2,100 kcal yesterday.", true to "How does today compare?"),
            "How does today compare?",
        )
        assertNotNull(turn)
        val messages = JSONObject(String(turn!!.body, Charsets.UTF_8)).getJSONArray("messages")
        assertEquals(1, messages.length())
        assertEquals("user", messages.getJSONObject(0).getString("role"))
        assertEquals("How does today compare?", messages.getJSONObject(0).getString("content"))
    }

    @Test
    fun `cloud response must match request identity and fixed provider`() {
        val turn = CloudAssistantRequest.build(CloudAssistantRequest.Task.CHAT, emptyList(), "Hello")!!
        val valid = JSONObject()
            .put("request_id", turn.requestId)
            .put("model", "nvidia/Nemotron-3.5-Super-VL")
            .put("provider", "nebius")
            .put("reply", "Hello.")
            .put("tool", JSONObject.NULL)
            .put("usage", JSONObject().put("prompt_tokens", 4).put("completion_tokens", 2).put("total_tokens", 6))
        assertNotNull(CloudAssistantRequest.parse(valid.toString(), turn))
        assertNull(CloudAssistantRequest.parse(valid.put("request_id", "00000000-0000-0000-0000-000000000000").toString(), turn))
    }
}

package app.metria

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AssistantContractPolicyTest {
    private fun resource(name: String): String = requireNotNull(javaClass.classLoader?.getResourceAsStream(name))
        .bufferedReader(Charsets.UTF_8).use { it.readText() }

    @Test fun sharedCasesMatchNativeMappingAndCloudArgumentRules() {
        val cases = JSONArray(resource("assistant-contract-cases.json"))
        val acceptedNames = mutableSetOf<String>()
        for (index in 0 until cases.length()) {
            val case = cases.getJSONObject(index)
            val name = case.getString("name")
            val json = case.getJSONObject("arguments")
            val arguments = json.keys().asSequence().associateWith { json.get(it) }
            val expected = case.getBoolean("accepted")
            if (expected) acceptedNames += name
            assertEquals(case.toString(), expected, AssistantContractPolicy.accepts(name, arguments))
            assertEquals(case.toString(), expected, MetriaNativeToolCallAdapter.adapt(name, arguments) != null)
        }
        assertEquals(GeneratedAssistantContract.TOOL_NAMES, acceptedNames)
    }

    @Test fun generatedToolDeclarationsMatchCanonicalDescriptionsAndParameterAnnotations() {
        val tools = JSONObject(resource("assistant-contract.json")).getJSONArray("tools")
        val classes = listOf(MetriaReadToolSet::class.java, MetriaMealDraftToolSet::class.java,
            MetriaCurrentDraftPortionToolSet::class.java, MetriaCurrentDraftFoodToolSet::class.java)
        val methods = classes.flatMap { it.declaredMethods.toList() }
            .filter { it.getAnnotation(Tool::class.java) != null }.associateBy { it.name }
        assertEquals(GeneratedAssistantContract.TOOL_NAMES, methods.keys)
        for (index in 0 until tools.length()) {
            val tool = tools.getJSONObject(index)
            val method = methods.getValue(tool.getString("name"))
            assertEquals(tool.getString("description"), requireNotNull(method.getAnnotation(Tool::class.java)).description)
            val parameters = tool.getJSONArray("parameters")
            assertEquals(parameters.length(), method.parameterCount)
            for (parameter in 0 until parameters.length()) {
                val annotation = method.parameterAnnotations[parameter].filterIsInstance<ToolParam>().single()
                assertEquals(parameters.getJSONObject(parameter).getString("description"), annotation.description)
            }
        }
    }

    @Test fun generatedContractAndPromptsUseExactSourceFingerprint() {
        val raw = resource("assistant-contract.json")
        val sha = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        assertEquals(sha, GeneratedAssistantContract.SOURCE_SHA256)
        assertEquals(raw, GeneratedAssistantContract.SOURCE_JSON)
        val json = JSONObject(raw)
        assertEquals(json.getString("base_prompt"), GeneratedAssistantContract.BASE_PROMPT)
        assertEquals(json.getString("agent_policy"), GeneratedAssistantContract.AGENT_POLICY)
    }
}

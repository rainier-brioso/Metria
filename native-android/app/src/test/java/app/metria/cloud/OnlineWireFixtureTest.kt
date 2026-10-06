package app.metria.cloud

import java.time.LocalDate
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class OnlineWireFixtureTest {
    @Test fun `shared server fixtures match Android validation`() {
        val resource = javaClass.classLoader!!.getResourceAsStream("online-contract-fixtures.json")!!
        val cases = resource.bufferedReader().use { JSONArray(it.readText()) }
        for (index in 0 until cases.length()) {
            val case = cases.getJSONObject(index)
            val body = if (case.has("raw")) case.getString("raw") else case.getJSONObject("payload").toString()
            val parsed = OnlineBrokerContract.parseRequest(body, LocalDate.of(2026, 10, 3), setOf("nvidia/fictional-test-model"))
            assertEquals(case.getString("name"), case.getBoolean("valid"), parsed != null)
        }
    }
}

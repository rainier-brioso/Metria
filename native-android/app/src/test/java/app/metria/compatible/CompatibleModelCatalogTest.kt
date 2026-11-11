package app.metria.compatible

import org.junit.Assert.*
import org.junit.Test

class CompatibleModelCatalogTest {
    @Test fun `installed Ollama names are preserved and duplicates or unsafe names are excluded`() {
        val models = CompatibleModelCatalog.ollamaModels("""{"models":[{"name":"gemma4:12b"},{"name":"gemma3:12b"},{"name":"gemma4:12b"},{"name":"bad model"}]}""")
        assertEquals(listOf("gemma3:12b", "gemma4:12b"), models.map { it.id })
        assertTrue(models.all { it.vision == null })
    }

    @Test fun `vision labels require explicit capabilities rather than a model name guess`() {
        assertEquals(true, CompatibleModelCatalog.ollamaVision("""{"capabilities":["completion","vision","tools"]}"""))
        assertEquals(false, CompatibleModelCatalog.ollamaVision("""{"capabilities":["completion"]}"""))
        assertNull(CompatibleModelCatalog.ollamaVision("""{"name":"vision-model"}"""))
        assertNull(CompatibleModelCatalog.ollamaVision("""{"capabilities":[]}"""))
    }
}

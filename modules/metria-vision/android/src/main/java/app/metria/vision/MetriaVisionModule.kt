package app.metria.vision

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

/** Gate A bridge only. Model/runtime adoption belongs to the measured Gate B work. */
class MetriaVisionModule : Module() {
  private var state = "unavailable"

  override fun definition() = ModuleDefinition {
    Name("MetriaVision")

    Function("runtimeStatus") { status() }

    AsyncFunction("loadModel") {
      // Do not pretend a model is available before its artifact, license, and
      // runtime have been recorded and verified.
      status()
    }

    AsyncFunction("unloadModel") {
      state = "unavailable"
      status()
    }
  }

  private fun status(): Map<String, Any?> = mapOf(
    "state" to state,
    "runtimeVersion" to null,
    "modelVersion" to null,
    "reason" to "No on-device vision runtime has been selected; inference is disabled."
  )
}

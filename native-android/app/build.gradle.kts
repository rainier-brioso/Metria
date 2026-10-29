import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val assistantContractFile = rootProject.file("../shared/assistant-contract.json")
val assistantContractSources = layout.buildDirectory.dir("generated/assistantContract/kotlin")
val generateAssistantContract by tasks.registering {
    inputs.file(assistantContractFile)
    outputs.dir(assistantContractSources)
    doLast {
        val raw = assistantContractFile.readText(Charsets.UTF_8)
        val spec = groovy.json.JsonSlurper().parseText(raw) as Map<*, *>
        @Suppress("UNCHECKED_CAST")
        val tools = spec["tools"] as List<Map<String, Any>>
        fun quoted(value: String) = groovy.json.JsonOutput.toJson(value).replace("\$", "\\\$")
        val names = tools.map { it["name"] as String }
        require(names.size == names.toSet().size && names.all { it.matches(Regex("[a-z][a-z_]+")) })
        val fingerprint = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        val code = buildString {
            appendLine("// Generated from shared/assistant-contract.json. Do not edit.")
            appendLine("package app.metria")
            appendLine("import com.google.ai.edge.litertlm.Tool")
            appendLine("import com.google.ai.edge.litertlm.ToolParam")
            appendLine("import com.google.ai.edge.litertlm.ToolSet")
            appendLine("internal object GeneratedAssistantContract {")
            appendLine("  const val SOURCE_SHA256 = ${quoted(fingerprint)}")
            appendLine("  const val SOURCE_JSON = ${quoted(raw)}")
            appendLine("  const val BASE_PROMPT = ${quoted(spec["base_prompt"] as String)}")
            appendLine("  const val AGENT_POLICY = ${quoted(spec["agent_policy"] as String)}")
            @Suppress("UNCHECKED_CAST")
            val examples = spec["examples"] as List<Map<String, Any>>
            val nativeExamples = examples.joinToString("\n") { example ->
                "Example: " + example["member_text"] + " -> select " + groovy.json.JsonOutput.toJson(example["tool"])
            }
            appendLine("  const val NATIVE_EXAMPLES = ${quoted(nativeExamples)}")
            appendLine("  val TOOL_NAMES = setOf(${names.joinToString(",") { quoted(it) }})")
            val reads = tools.filter { it["native_class"] == "MetriaReadToolSet" }.map { it["name"] as String }
            appendLine("  val READ_TOOL_NAMES = setOf(${reads.joinToString(",") { quoted(it) }})")
            @Suppress("UNCHECKED_CAST")
            val taskTools = spec["task_tools"] as Map<String, List<String>>
            appendLine("  val TASK_TOOLS = mapOf(${taskTools.entries.joinToString(",") { (task, values) -> quoted(task) + " to setOf(" + values.joinToString(",") { quoted(it) } + ")" }})")
            appendLine("}")
            for ((className, declarations) in tools.groupBy { it["native_class"] as String }) {
                require(className.matches(Regex("Metria[A-Za-z]+ToolSet")))
                appendLine("class $className : ToolSet {")
                for (tool in declarations) {
                    @Suppress("UNCHECKED_CAST")
                    val parameters = tool["parameters"] as List<Map<String, Any>>
                    val name = tool["name"] as String
                    appendLine("  @Tool(description = ${quoted(tool["description"] as String)})")
                    appendLine("  fun $name(")
                    for (parameter in parameters) {
                        val paramName = parameter["name"] as String
                        require(paramName.matches(Regex("[a-z][a-z_]+")))
                        val type = when (parameter["type"]) { "integer" -> "Int"; "string" -> "String"; else -> error("Unsupported argument type") }
                        val nullable = if (parameter["nullable"] == true) "? = null" else ""
                        appendLine("    @ToolParam(description = ${quoted(parameter["description"] as String)}) $paramName: $type$nullable,")
                    }
                    appendLine("  ): Map<String, Any> = mapOf(\"status\" to \"host_execution_required\", \"tool\" to ${quoted(name)}")
                    if (className == "MetriaReadToolSet") {
                        val request = if (parameters.isEmpty()) "\"\"" else
                            "listOf<Any?>(${parameters.joinToString(",") { it["name"] as String }}).joinToString(\":\") { it?.toString().orEmpty() }"
                        appendLine("    , \"request\" to $request")
                    } else for (parameter in parameters) {
                        appendLine("    , ${quoted(parameter["name"] as String)} to ${parameter["name"]}")
                    }
                    appendLine("  )")
                }
                appendLine("}")
            }
        }
        val target = assistantContractSources.get().file("app/metria/GeneratedAssistantContract.kt").asFile
        target.parentFile.mkdirs()
        target.writeText(code, Charsets.UTF_8)
    }
}

android {
    namespace = "app.metria"
    compileSdk = 35
    sourceSets.getByName("main").java.srcDir(assistantContractSources)
    sourceSets.getByName("test").resources.srcDir(rootProject.file("../shared"))

    defaultConfig {
        applicationId = "app.metria"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        manifestPlaceholders["appLabel"] = "Metria"
        manifestPlaceholders["weightScheme"] = "metria"
    }

    buildTypes {
        create("demo") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
            manifestPlaceholders["appLabel"] = "Metria Demo"
            // Recording build must not claim the normal app's external weight links.
            manifestPlaceholders["weightScheme"] = "metria-demo"
            matchingFallbacks += listOf("debug")
        }
    }
    sourceSets.getByName("demo") {
        setRoot("src/debug")
        res.srcDir("src/demo/res")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

configurations.named("demoImplementation") {
    extendsFrom(configurations.getByName("debugImplementation"))
}

tasks.named("preBuild").configure { dependsOn(generateAssistantContract) }

// LiteRT-LM's ToolSet/annotations are Java 21 bytecode. The app still targets
// Java 17/Android; JVM parity tests must load the actual SDK declarations.
tasks.withType<Test>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(maxOf(21, JavaVersion.current().majorVersion.toInt())))
    })
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("com.microsoft.onnxruntime:onnxruntime-android:1.22.0")

    // LiteRT-LM 0.13.0 publishes Kotlin 2.3 metadata. This native project owns
    // the matching compiler rather than bypassing compiler compatibility checks.
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.13.0")
}

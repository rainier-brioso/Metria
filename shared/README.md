# Metria assistant contract

`assistant-contract.json` is the provider-independent source for base policy,
agent rules/examples, tool names/descriptions, typed parameters/limits and task
allowlists. Edit it here, never copy definitions into a provider prompt.

- Android `:app:generateAssistantContract` runs before compilation. It generates
  `GeneratedAssistantContract.kt` and the actual LiteRT tool-set classes under
  `app/build/generated/assistantContract/kotlin`. These outputs are not committed.
- The Python broker loads this exact JSON through `assistant_contract.py`.
  Its container copies only this contract from `shared/`, not test vectors.
- Native and cloud tool adapters validate arguments using the same metadata.
  The existing host still checks current intent, dates, unlocked state, draft
  identity, action permissions and record grounding before execution.
- Runtime adapters retain their own wire formatting and task-specific generation
  instructions. Gemma uses LiteRT native tools; cloud selects a JSON action.
  This does not give the broker vault access or send record-read results to it.
- `assistant-contract-cases.json` is the common valid/invalid test corpus used by
  Kotlin and Python, including coverage of every tool and unexpected arguments.
  Native tests also inspect the actual generated SDK annotation descriptions.

The SDK ToolSet bytecode requires Java 21+ for JVM reflection tests. Gradle uses
its current JVM when it is 21+ or selects a Java 21 test launcher otherwise.
Production Kotlin/Java target remains 17; no app dependency was added.

After edits, run both full gates, rebuild the broker container if used, restart
the broker and install the newly built APK only with member authorization.
Matching contracts do not prove identical model choices: live Gemma/cloud
acceptance still needs verification. Models never execute tools or save data.

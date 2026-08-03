# M3 assisted meal input checkpoints

## 2026-08-03 - local draft confirmation and encrypted persistence

Implemented a first safe meal-record path:

- The local assistant emits an ephemeral `MealProposal` only after the strict
  proposal parser accepts the model response.
- The member can open a scrollable confirmation sheet, correct food,
  portion, preparation, date, and optional self-entered nutrition values.
- A separate `Save confirmed meal locally` action writes the record to the
  encrypted biometric-protected vault. There is no automatic write from model
  output.
- Existing vaults without a `meals` array unlock normally; confirmed meals are
  appended only after the member saves one.

Physical Android evidence:

1. A model meal draft was reviewed and explicitly saved.
2. History displayed `Chicken Breast, Rice` with its AI-estimate provenance.
3. After force-stop, relaunch, and biometric unlock, the meal remained visible
   in both Today and History.

Current boundary: Metria intentionally has no food database. Nutrition fields
in this checkpoint are optional member-entered values and do not represent a
calculated intake. A vetted adapter and deterministic calculation are still
required before nutrition summaries can be claimed complete.

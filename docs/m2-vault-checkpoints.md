# Metria M2 vault checkpoints

## First encrypted write — 2026-08-02

- Physical device: Samsung SM-S928B, Android 16.
- The member created the vault through the Android biometric prompt, entered a
  test weight, and saved it locally.
- Result: the vault displayed as unlocked and showed the new local entry.
- Next evidence: background/restart lock, re-unlock, persistence, and failure
  behavior. This checkpoint does not prove recovery/export or biometric-change
  behavior yet.

## Restart lock and re-unlock — 2026-08-02

- The application was force-stopped and relaunched without clearing app data.
- Result: the vault presented as locked. After fingerprint authentication, the
  previously saved local test weight returned.
- This proves the foreground session key was discarded while the encrypted
  vault persisted across process restart.

## Weight-management checkpoint — 2026-08-02

- The member unlocked the existing vault, edited a weight, added an optional
  note and date, and saved the changes.
- The deletion dialog accurately described the local entry to be removed; both
  keeping and deleting an entry behaved as expected.
- Result: create, date, note, edit, and confirmed delete are proven on the
  physical device. Trend/history presentation remains the next M2 slice.

## Member UX direction — 2026-08-03

- The member reviewed the redesigned locked and member-facing screens on the
  physical phone and approved the calm private-wellness visual direction.
- The product UI now separates Today, History, and Privacy from diagnostic
  model controls. The encrypted vault behavior remains unchanged beneath it.
- Remaining M2 UX work: restore entry edit/delete from History, support custom
  history ranges, and complete offline/biometric-cancellation accessibility
  checks before declaring M2 complete.

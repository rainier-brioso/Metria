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

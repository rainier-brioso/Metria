# Accountless cloud access research

Researched 2026-10-28 at the member's request. No implementation or new dependency
approved by this research. A hosted broker does not inherently require member
accounts or a login screen. Authorization, durable quotas and provider-secret
isolation are separate from named user authentication.

## Option A: private evaluator capability

Keep the existing per-installation private-pass activation. Embed the stable
HTTPS broker URL in the public APK, distribute passes privately, redeem once,
and retain the installation credential in existing Keystore-protected storage.
No email/password or external identity service. Per-pass expiry/request cap,
revocation and durable global limits already exist in the broker.

The current installation UUID is an app-generated identifier, not attestation
or proof of one physical phone. A leaked unredeemed pass can be claimed first
by another installation; revoke it. A copied bearer grants access within its
remaining quota. Private QR/link activation could reduce entry friction but is
not implemented and needs verified link handling/no secret logging. Never embed
a shared pass or provider key in the downloadable APK.

Assessment: best fit for the hackathon and controlled judges. Hosting still
requires TLS, trusted ingress, persistent SQLite and verified logging settings.

## Option B: automatic anonymous installation access

Possible design: selecting cloud starts app/device attestation, the broker
verifies it, then issues its own per-installation credential and a small quota.
No human registration and no Firebase Authentication account are inherently
required for Firebase App Check. The broker can verify App Check tokens using
the Python Admin SDK. App Check tokens attest app/project authenticity, not a
stable member or installation identity: separate enrollment/credentials remain
necessary for quota attribution. A Keystore-generated signing key with server
challenges could bind that identity to proof of key possession; this is a future
design, not current functionality or reinstall-proof identity.

Google documents App Check/Play Integrity support for distribution outside
Google Play. Its recommended outside-only settings do not require PLAY_RECOGNIZED
or LICENSED, and require device integrity. That relaxes Play-recognized app
identity guarantees; do not promise the same protection as Play distribution.
Developer Firebase/Play Console setup, signing certificate registration, SDKs,
Google processing/privacy review and acceptance on evaluator devices are needed.
The judge would not create a Metria account, but depends on compatible Google
services/device behavior. Do not ship the debug provider as production security.

App Check mitigates some abuse, not all. Genuine clients can also consume funded
inference; attestation does not decide who deserves a quota. Reinstallation/new
installation identities can weaken per-installation allotments. Keep durable
global admission limits, rate/concurrency controls and a service kill switch.
Current request/token reservation caps are not an exact EUR spending ceiling.
Replay protection is available for custom Python backends in beta; this does not
prevent all repeated enrollment or replace business quotas.

Assessment: feasible for an automatic public trial, but more work and dependencies
than private evaluator passes. Not implemented or verified for Metria.

## Anonymous Firebase Authentication

This hides signup from the member, but creates technical anonymous accounts.
It is optional and does not alone solve abusive enrollment or budget control.
Do not describe it as literally having no accounts, or as required for App Check.

## Primary sources

- [App Check overview, limits and non-guarantee](https://firebase.google.com/docs/app-check)
- [Android Play Integrity setup and outside-Play settings](https://firebase.google.com/docs/app-check/android/play-integrity-provider)
- [Custom backend verification, Python and replay protection](https://firebase.google.com/docs/app-check/custom-resource-backend)
- [Anonymous Authentication creates temporary accounts](https://firebase.google.com/docs/auth/android/anonymous-auth)
- [Play Integrity verdict semantics](https://developer.android.com/google/play/integrity/verdicts)
- [OWASP: secrets embedded in APKs are recoverable](https://mas.owasp.org/MASWE/MASVS-STORAGE/MASWE-0004/)

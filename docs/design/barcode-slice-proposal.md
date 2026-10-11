# Local barcode capture slice — 2026-10-07

Status: deferred by the member on 2026-10-07. Scanning a barcode without product
lookup would add an extra step before label capture. The previous dependency
approval request is superseded; do not add ZXing or a barcode route for this UI
cycle. The photo screen offers Meal photo and Nutrition label. Existing local
nutrition-label OCR remains available. The scope below is future design only.

## First bounded scope

Read a retail EAN/UPC barcode from a member-selected camera photo or library image
on the phone. A dedicated view offers photo acquisition, manual code correction,
reading status and explicit cancellation. It does not interpret arbitrary QR
content or open scanned URLs. The result must pass supported format, numeric
length and checksum validation before display as a product code.

The bundled USDA Foundation Foods catalog has no GTIN/UPC/EAN index. A successfully
decoded code therefore cannot produce product identity, portions or nutrition.
Say this plainly and offer Scan nutrition label or Enter manually using the
existing reviewed meal routes. Do not manufacture a product match, persist the
code as a confirmed food, or show an always-empty lookup as successful nutrition
analysis. This initial scope does not promise live camera scanning.

## Dependency and data decisions

A local decoder such as ZXing core requires explicit production-dependency
approval under AGENTS.md. No runtime product API, web lookup, SDK network request
or new data collection is included. Actual product lookup requires a separately
selected, licensed, versioned offline GTIN dataset with documented coverage,
source units and update policy. Approval to scan does not authorize transmitting
a code or broadening the existing local-only privacy contract.

## State and lifecycle requirements

- Idle: explain capture and offline product-lookup limits before opening a camera.
- Reading: visible local progress, cancel invalidates the operation.
- Success: show only a validated retail code and honest nutrition fallback.
- Empty/error: no supported code found, damaged image or invalid checksum; retry
  and manual entry remain available. Errors must not include source image/URI.
- Cancellation: delete temporary app-owned capture/reduced images, clear code.
- Replacement: invalidate old results before accepting a new image.
- Background/lock/process recreation: clear transient code/results; external
  camera/picker handoff must follow the existing explicit lifecycle exception.
- Never save photos, decoded text or results to logs, chat, archives or the vault.

Image bounds, EXIF handling, temporary deletion, external camera/picker behavior
and late-result suppression need implementation review before shipping. The view
must use Metria components, 48 dp controls, keyboard/system insets, scrolling and
non-color status labels. A dependency-free mock view is not a completed scanner.

## Acceptance

Automated fixtures should include EAN-13/8 and UPC-A checksum errors, unsupported
QR/URL content, unreadable images and replacement/cancellation. Physical evidence
must cover camera/library, a real retail barcode, blurry/rotated input, no-match
fallback to label/manual review, lock/background/rotation, enlarged text and
TalkBack. Install and device acceptance remain separate from the build gate.

# Production Readiness Checklist

This checklist records work that must be completed before MerchSpring AI is promoted from a prototype to a production business app.

## Verified in repository review

- [x] Native Kotlin/Jetpack Compose app structure is present.
- [x] Room is used for local persistence.
- [x] Firebase AI Logic SDK is declared and the AI service uses the Firebase proxy rather than a raw Gemini Developer API key.
- [x] App Check provider setup is included for debug and release variants.
- [x] First-run startup creates a clean workspace rather than automatically loading demo transactions.
- [x] Demo invoice data is marked as a fixture rather than real OCR.
- [x] Automated Android build and unit-test workflow added.
- [x] Basic unit tests added for money formatting and ambiguous AI action proposals.

## Required before a public release

### Identity and Firebase

- [ ] Decide the permanent Android application ID and Kotlin namespace. The current values `com.aistudio.merchspring.kvpnwx` and `com.example` are generated/prototype identifiers.
- [ ] Register the final Android application in Firebase and configure `google-services.json` locally and in any build environment that needs it.
- [ ] Enable Firebase AI Logic and verify which Gemini API provider/model is supported by the selected Firebase project.
- [ ] Register the App Check debug token only in development. Verify Play Integrity works for the signed release application.
- [ ] Enforce App Check for Firebase AI Logic in the Firebase Console, and set quotas, monitoring, and billing alerts.
- [ ] Decide whether business users need sign-in, cloud sync, business workspaces, and server-enforced user/role permissions. These are not currently delivered by Room alone.

### Data protection and resilience

- [ ] Add explicit Room schema export and versioned migrations before changing the database schema in a released app. Do not re-enable destructive migration as a shortcut.
- [x] Exclude the Room database and shared preferences from Android cloud Auto Backup; the current extraction rules allow device-to-device transfer.
- [ ] Validate device-transfer behavior on supported Android/OEM versions and communicate it clearly to users.
- [ ] Build and test a user-controlled, encrypted backup/restore strategy for local business records. Until then, users must export available business data before replacing or resetting a device.
- [ ] Review Android Auto Backup / device-transfer policy for customer and transaction data. (Cloud backup is currently excluded; a supported, user-controlled recovery/export workflow is still needed.)
- [ ] Test recovery from failed transactions, interrupted writes, duplicate submissions, and app upgrades.

### Business correctness

- [ ] Implement an append-only payment/receipt ledger with amount, method, timestamp, branch, reference, and reversal/correction records. The current models largely store an aggregate paid amount and a single payment-method string per order/purchase; therefore later partial payments cannot be reliably reconciled to their actual date/method for cash closing.
- [ ] Implement branch-specific inventory balances or a stock ledger per product and branch. Branch and transfer screens currently model branch metadata/transfer events, but the product stock record is global rather than isolated per branch.
- [ ] Implement the complete exchange workflow: the current UI records the original item return only and does not issue a replacement item or decrement replacement stock.
- [ ] Define store-credit and exchange accounting explicitly; the current return workflow updates item quantities and stock, but it is not a complete credit-note, tax adjustment, or store-credit liability system.
- [ ] Verify sale confirmation, stock reservation, dispatch, purchase receiving, return/restock, payment, refund, transfer, and cash-closing transitions with automated tests.
- [ ] Reconcile dashboard/report totals against stored records, especially revenue vs cash collected and gross profit vs expenses.
- [ ] Confirm product-specific HSN/GST configuration and invoice wording with a qualified tax professional; do not advertise GST compliance without validation.
- [ ] Replace the current invoice-extraction demo fixture with genuine OCR, source image retention rules, confidence/error handling, and explicit user review.
- [ ] Test barcode/serial/batch/expiry and multi-branch workflows on real devices if these are included in the release scope.

### AI reliability and privacy

- [ ] Test Gemini model availability and Firebase AI Logic/App Check configuration in a real debug build and a signed release build.
- [ ] Display AI errors clearly; do not substitute arbitrary answers for service failures.
- [ ] Verify Google Search grounding displays its Search entry-point content and source links as required by Firebase AI Logic terms.
- [ ] Test that missing or ambiguous product, order, amount, or quantity references never produce actionable proposals.
- [ ] Publish a privacy notice describing which prompts and business data are sent to Gemini, why, and under which service terms.

### Release engineering

- [ ] Add the official Gradle Wrapper scripts and wrapper JAR, and validate the wrapper distribution.
- [ ] Use a protected release-signing key managed outside the repository and CI logs.
- [ ] Choose and add an open-source/commercial license, or keep the repository's licensing terms explicit.
- [ ] Enable GitHub secret scanning / push protection, Dependabot alerts, and code scanning where available.
- [ ] Review dependency updates and fix all high-severity issues.
- [ ] Run accessibility, responsiveness, performance, and usability checks on representative low- and mid-range Android devices.
- [ ] Verify CI passes and manually test a release candidate before distribution.

## Current architecture boundary

Firebase AI Logic is used for AI requests. Business records remain in a local Room database; adding Firebase AI Logic does **not** automatically add Firebase Authentication, Firestore storage, secure multi-tenant business records, or cross-device sync.

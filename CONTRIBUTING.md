# Contributing to MerchSpring AI

Thank you for helping improve MerchSpring AI. This is an Android Kotlin/Jetpack Compose prototype. Prioritise correctness, clear UX, data protection, and tested business workflows over placeholder features.

## Before making changes

1. Review existing screens, models, repository methods, and Room entities before introducing a duplicate.
2. Open an issue or describe the proposal in a pull request if it affects business rules, the database schema, security, billing, stock movement, or AI-triggered actions.
3. Keep UI, state handling, persistence, and business rules separated.
4. Never commit credentials, signing keys, App Check debug tokens, real customer data, or real invoices.
5. Do not change the Android application ID or Kotlin namespace without documenting Firebase, migration, and Play distribution implications.

## Local environment

- JDK 21
- Android SDK Platform 36.1
- Android Build Tools 36.0.0
- Android Studio compatible with Android Gradle Plugin 9.1.1

The Gradle Wrapper pins Gradle 9.3.1. Build and run JVM/Robolectric tests from the repository root:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest --no-daemon
```

If the root-level `debug.keystore` does not exist, create a development-only key. Never use a production signing key for local/debug builds. Configure Firebase separately if you need to test Firebase AI Logic and App Check. Put `google-services.json` in `app/google-services.json`; the file is git-ignored.

## Code and data principles

- Store money as integer paise and use existing currency-formatting helpers.
- Reject missing/ambiguous quantities, amounts, product references, and order references instead of guessing.
- Do not silently adjust stock quantities, receive more than was ordered, or change a financial state without validation.
- Use explicit state transitions and preserve an audit trail for consequential inventory operations.
- Do not silently delete business records during schema migrations.
- Keep AI outputs grounded in workspace facts and distinguish proposals from completed operations.
- Consequential AI-proposed operations must show details and require in-app confirmation.
- When using Google Search grounding, show Google's required search entry point and visible source attribution.
- Never describe a demo invoice fixture as data extracted from a real document.
- Add automated tests for business-rule changes and preserve useful loading, empty, and error states.

## Pull requests

- Use a focused branch and descriptive commit messages.
- Describe the problem, behavior change, and user-facing, data, or schema implications.
- Include screenshots for meaningful UI changes when practical.
- State exactly which build and tests were run. If a test could not be run, say so; do not report it as passing.
- Check the production-readiness checklist when touching financial, tax, multi-branch, or release behavior.
- Ensure no credentials or real business/customer data are included in the diff.

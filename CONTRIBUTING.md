# Contributing to MerchSpring AI

Thank you for helping improve MerchSpring AI. The project is an Android Kotlin/Jetpack Compose prototype; prioritize correctness, clarity, data protection, and tested business workflows over adding placeholder screens.

## Before making changes

1. Check existing screens, models, repository methods, and Room entities before adding a duplicate implementation.
2. Open an issue or describe the proposed change in a pull request when it affects business rules, database schema, security, billing, stock movement, or AI-triggered actions.
3. Keep UI, data access, and business logic separated.
4. Never commit credentials, signing keys, App Check debug tokens, customer data, or real invoices.
5. Do not change the application ID or package namespace without documenting the migration and Firebase/Play Store implications.

## Local environment

Use JDK 17, Gradle 9.3.1, Android SDK Platform 36.1, and Build Tools 36.0.0. The repository currently does not include the Gradle Wrapper, so install the matching Gradle distribution locally.

Build and test:

```bash
gradle :app:assembleDebug --no-daemon
gradle :app:testDebugUnitTest --no-daemon
```

Firebase configuration is optional for source compilation but required to test Firebase AI Logic and App Check. Configure the Android app in Firebase and add `app/google-services.json` locally; that file is ignored by Git.

## Code and data principles

- Store money in integer paise and use the existing currency-formatting helpers.
- Treat unknown, ambiguous, or missing quantities as validation errors; never invent values.
- Do not choose the first product/order as a fallback when the user request is ambiguous.
- Do not update physical stock when merely creating a purchase order.
- Use explicit transaction states and preserve audit history for stock, payments, returns, and corrections.
- Do not silently delete user records during database migrations.
- Keep AI responses grounded in actual records and distinguish suggestions from saved actions.
- Any consequential AI-proposed action must show its details and require explicit confirmation.
- When using Google Search grounding, show Google's required search entry point and visible source attribution.
- Do not claim that sample invoice data has been extracted from a real uploaded document.
- Add tests for business-rule changes and keep empty/error/loading states useful.

## Pull requests

- Use a focused branch and a descriptive commit message.
- Explain the problem, the behavior changed, and any user-facing or database implications.
- Include test/build results. If a test could not be run, say so explicitly.
- Keep screenshots or short screen recordings for meaningful UI changes when practical.
- Do not mix formatting-only changes with business-rule changes unless necessary.

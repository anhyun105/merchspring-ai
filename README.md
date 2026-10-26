# MerchSpring AI

**Your Business, in Sync.**

MerchSpring AI is an Android business-management app prototype for retailers, small shops, wholesalers, and growing businesses. It brings common day-to-day workflows—product catalogue, stock movements, point-of-sale billing, sales orders, purchases, customers and suppliers, expenses, invoices, reports, notifications, and a 3D warehouse view—into one Android interface.

> **Project status: prototype / active development.** Source code for multiple modules exists, but that does not mean every workflow has been fully tested or is ready for production. See [Production readiness](docs/PRODUCTION_READINESS.md) for known gaps and launch requirements.

## What it is built with

- **Kotlin** and **Jetpack Compose** for the native Android UI.
- **Room** for on-device persistence of business records.
- **Kotlin coroutines and Flow** for asynchronous work and reactive state.
- **Firebase AI Logic** for Gemini-powered assistance through Firebase's proxy.
- **Firebase App Check** with a debug provider for local development and Play Integrity for release builds.
- **Gradle version catalog** for central dependency management.

The current app is a **native Android application**, not a React web app. Business records are stored locally in Room. Firebase Authentication, cloud Firestore business-data sync, multi-user login, and cross-device synchronisation are not currently implemented.

## Current feature areas

- Product catalogue and stock movement tracking.
- Quick POS billing and sales/order workflows.
- Purchase orders and receiving workflows.
- Customer and supplier records.
- Expense tracking and daily cash closing.
- Invoice and tax-related fields.
- Sales, stock, and business-report screens.
- AI business assistant with optional thinking and Google Search grounding.
- A 3D warehouse view, notifications, branch/transfer concepts, and workspace settings.
- Explicit demo-data controls in Settings.

**Demo data:** A fresh installation should start with a clean workspace. Sample Indian-store records can be loaded from the explicit **Reload Demo Data** option in Settings. Demo records are fictitious examples; do not use them for real invoices, tax filings, payment reconciliation, or business decisions.

## Requirements

The current Gradle configuration targets:

- JDK 17.
- Gradle 9.3.1.
- Android SDK Platform 36.1.
- Android Build Tools 36.0.0.
- An Android Studio version that supports the project's Android Gradle Plugin 9.1.1.

The repository currently does not include the Gradle Wrapper scripts and wrapper JAR. The CI workflow therefore installs Gradle 9.3.1 directly. Before external release, adding and validating the official Gradle Wrapper is recommended.

## Build and test

Install JDK 17, Android SDK Platform 36.1 and Build Tools 36.0.0. Install Gradle 9.3.1, then run from the repository root:

```bash
gradle --version
gradle :app:assembleDebug --no-daemon
gradle :app:testDebugUnitTest --no-daemon
```

For a debug build, the project uses a root-level `debug.keystore`. If you need a local key, create a development-only key (never reuse a production signing key):

```bash
keytool -genkeypair -v \
  -keystore debug.keystore \
  -storepass android \
  -keypass android \
  -alias androiddebugkey \
  -dname "CN=Android Debug,O=Android,C=US" \
  -keyalg RSA -keysize 2048 -validity 10000
```

The Gradle task may compile without Firebase configuration, but the AI service will not work until Firebase is configured.

## Configure Gemini safely

Do **not** embed a Gemini Developer API key in the Android APK. A key compiled into an application can be extracted from the package. MerchSpring AI uses Firebase AI Logic so model requests go through Firebase's proxy instead of the app containing a raw Gemini API key.

1. Create or select a Firebase project.
2. Register the Android application using the current application ID: `com.aistudio.merchspring.kvpnwx`.
3. Enable Firebase AI Logic and select the Gemini Developer API provider.
4. Add the downloaded `google-services.json` to `app/google-services.json` locally. This path is git-ignored.
5. Configure Firebase App Check. Debug builds use the App Check debug provider; register the generated debug token in Firebase Console. Release builds use Play Integrity.
6. In Firebase Console, verify App Check enforcement and configure monitoring, quotas, and billing alerts before distributing builds.
7. Run the app and verify the AI assistant, including source attribution when Google Search grounding is enabled.

Never commit a Firebase service-account private key, an App Check debug token, or a production signing key. Do not place a Gemini Developer API key in `.env`, Gradle properties, or source code.

## Data, privacy, and limitations

- Core business records are currently stored **on the device** using Room. They are not automatically synced to a cloud workspace.
- Android cloud Auto Backup excludes the Room database and shared preferences so customer/financial records are not silently copied into general cloud backups. Device-to-device transfer is allowed by the Android extraction rules; behavior must be tested on supported devices. There is no encrypted cloud backup/restore service yet, so export available business data before replacing or resetting a device.
- Using the AI assistant sends the user's prompt, recent chat history, and a business-summary snapshot to Firebase AI Logic / the selected Gemini service. Customer names and balances are included only when a prompt asks about customer dues, debts, or payment reminders; aggregate receivables remain part of the summary. The app displays this data-sharing notice in the AI screen. Review service terms, retention settings, and your business privacy notice before using sensitive data.
- The Settings screen includes a role-switch/testing experience. It is not a substitute for authentication or server-enforced permissions.
- Payment tracking currently stores aggregate paid amounts and a single payment-method value per order/purchase, not an append-only payment ledger. Payments made on a later day or using multiple methods cannot be reliably reconciled by transaction date/method.
- Branch/transfer screens are prototype workflows, not isolated per-branch inventory accounting: products still have a global stock record. Do not use them as the sole source of truth for multiple physical locations.
- The invoice-extraction sample is currently a deterministic demo fixture, not a production document OCR pipeline.
- Tax fields and invoice screens do not by themselves establish GST compliance, GST portal connectivity, or e-invoice filing.
- Automatic Room database wiping on schema migration has been removed. Explicit database migrations must be added before changing the Room schema in a release.

See [SECURITY.md](SECURITY.md) for vulnerability reporting and [CONTRIBUTING.md](CONTRIBUTING.md) for development guidance.

## License

A project license has not yet been selected. Until the repository owner adds a license, do not assume that the source is available for unrestricted copying, redistribution, or commercial use.

---

**MerchSpring AI — Your Business, in Sync.**

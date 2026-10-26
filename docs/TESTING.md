# MerchSpring AI — Tester Guide

This guide is for exploratory testing of the current prototype. Use a dedicated test installation and fictitious records only.

## Get the build

Download `merchspring-ai-debug-apk` from a successful run in [GitHub Actions → Android CI](https://github.com/anhyun105/merchspring-ai/actions/workflows/android-ci.yml). The artifact is a debug build, not a signed production release, and is retained for 14 days. If Android refuses to update an older CI build due to a signing mismatch, uninstall the old test app before installing the new APK.

## First launch

- [ ] Confirm the app opens and displays the Overview screen.
- [ ] Confirm a fresh installation does not automatically populate fictitious customer, order, or payment records.
- [ ] Open Settings and inspect workspace settings.
- [ ] Only select **Reload Demo Data** if you want samples; review the confirmation because it replaces the current workspace records.
- [ ] Check empty/loading/error states and back navigation.

## Manual smoke-test checklist

Use fictitious product/customer/supplier details. Record the build/run URL and expected versus observed behavior for each test.

- [ ] **Products and stock:** create or edit a product; adjust stock; verify available stock and movement history.
- [ ] **Sales order:** create a draft, confirm it, verify reservation, dispatch it, and verify physical stock changes.
- [ ] **Cancellation:** cancel a confirmed order and verify reservation release; verify a dispatched order cannot be cancelled through the confirmed-order path.
- [ ] **Quick billing:** create a cash sale and named-customer credit sale; confirm the cart clears only when checkout succeeds.
- [ ] **Purchase order:** create a purchase order; confirm stock does not increase at order creation; receive a valid quantity and check stock/payable values.
- [ ] **Returns:** open a real dispatched order, choose one of its actual lines, and verify returned-quantity limits. Use a separate test workspace for excessive-quantity cases.
- [ ] **Expenses and cash closing:** enter fictitious records and verify totals for the chosen date/branch. Financial reconciliation remains a prototype limitation.
- [ ] **Invoice preview:** confirm the screen identifies itself as a draft and does not invent missing business/tax identifiers.
- [ ] **Reports:** compare the displayed date range and sales basis to the records created.
- [ ] **AI (Firebase setup required):** ask a factual question; inspect Google Search source links; test an ambiguous action request and verify it does not guess product/order/amount/quantity or claim success before the operation succeeds.
- [ ] **Privacy:** never submit real customer details, payment credentials, confidential financial data, or actual invoice documents to the test AI assistant.

This is a test plan, not a claim that every item has passed. Exchanges, multi-branch inventory, payment-ledger reconciliation, invoice/tax behavior, and data recovery still require deeper validation; see [Production Readiness](PRODUCTION_READINESS.md).

## Automated checks

With JDK 21, Android SDK Platform 36.1, and Build Tools 36.0.0 installed:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest --no-daemon
```

The workflow publishes an HTML unit-test report when one is generated, including on failed runs. Report reproducible issues through the repository issue templates and redact secrets, device identifiers, and all personal/business records.

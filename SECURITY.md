# Security Policy

MerchSpring AI is a prototype and should not be treated as production-ready for sensitive business or customer data until the items in [Production readiness](docs/PRODUCTION_READINESS.md) are addressed.

## Reporting a vulnerability

Please report suspected vulnerabilities privately through GitHub's security advisory / private vulnerability reporting feature for this repository, if it is enabled. Do not open a public issue containing credentials, customer information, exploitable details, or a proof of concept that puts users at risk.

Include the affected file or feature, the impact, steps to reproduce in a safe test environment, and any recommended mitigation. Do not include real customer records or secrets in the report.

## Security expectations

- Never embed a Gemini Developer API key, service-account key, signing key, password, or App Check debug token in source code or an APK.
- Firebase AI requests must use Firebase AI Logic and App Check or a properly secured server-side proxy.
- App Check debug tokens must be private and never used in a release build.
- Do not treat local role-switch UI as authentication or access control.
- Do not use demo records as real financial or tax data.
- Changes to stock, payments, invoice status, returns, and access permissions must validate inputs and be auditable.
- Never silently erase stored business data during schema upgrades.

# FIXME

This is the Android client backlog. Items describe work that belongs in this app or verification needed before personal use. Backend improvements from the original Uptime Monitor remain outside this repository unless explicitly added later.

## Before first real use

- [ ] Confirm the deployed API paths use `/api/v1` and that the server accepts a bearer token on every mutating endpoint.
- [ ] Confirm the deployed server matches the repository's six-field cron contract (seconds first) and descriptors such as `@hourly`.
- [ ] Confirm `checked_at` is RFC3339 and that checks are sorted by timestamp before rendering and worker evaluation.
- [ ] Confirm `cert_expires_at` is RFC3339 UTC and is absent for plain HTTP targets.
- [ ] Test the deployed reverse proxy with the Android client's HTTPS origin and clear error responses.
- [ ] Set the app's battery mode to Unrestricted on the personal device and document the device-specific steps.

## High priority app work

- [ ] Replace the current simple status bars with accessible content descriptions and a compact response-time/status legend.
- [ ] Add a biometric app lock using `BiometricPrompt`; keep the Keystore token encryption independent from the lock state.
- [ ] Review notification permission denial and notification-channel settings in the Settings screen.
- [ ] Revisit alert noise: the current worker follows the requested “any new down check” behavior, but a retry threshold or flap suppression is likely better for daily use.

## Medium priority app work

- [ ] Add a target detail screen once the product needs more than the dashboard history window.
- [ ] Add explicit certificate renewal handling so a renewed certificate resets the local reminder state immediately.
- [ ] Add a small worker diagnostics screen showing `lastRun`, last known state per target, and the last worker failure.
- [ ] Add an optional Glance widget only after the dashboard and worker behavior are stable.
- [ ] Add a testable clock and injectable notification sender so worker tests do not depend on wall-clock time or Android notification APIs.
- [ ] Decide whether a minimal local cache is worthwhile; offline database storage is currently out of scope.

## Deliberately deferred

- [ ] On-device monitoring, FCM, ntfy, Firebase, Play Store release, and multi-account support.

## Backend context to keep visible

- [ ] The server's `GET /targets` response can contain up to 1,500 checks per target; add pagination or a bounded history endpoint before large installations make dashboard refreshes expensive.
- [ ] Add auth rate limiting and consider replacing a long-lived shared token with a more constrained credential model.
- [ ] Add external heartbeat monitoring because the monitor cannot report its own outage.
- [ ] Keep the backend README and generated Swagger contract synchronized; the Android client should follow generated Swagger, not stale prose.
- [ ] Add a repository LICENSE if the project is intended for redistribution.

## UI and project follow-ups

- [ ] Keep app documentation current as setup, behavior, and release steps change.
- [ ] Expand automated test coverage for dashboard behavior, notification navigation, and view-model logic.
- [ ] Resolve remaining Android Studio/IDE warnings where practical.
- [ ] Add Lefthook-managed local checks. Lefthook does not require `package.json`; use its native binary installer and configure Gradle tasks directly.
- [ ] Add GitHub workflows for Renovate, test/build validation, and APK builds on version tags. Deferred for later.

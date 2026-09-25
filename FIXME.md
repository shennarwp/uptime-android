# FIXME

This is the Android client backlog. Items describe work that belongs in this app or verification needed before personal use. Backend improvements from the original Uptime Monitor remain outside this repository unless explicitly added later.

## Before first real use

- [ ] Set the app's battery mode to Unrestricted on the personal device and document the device-specific steps.

## High priority app work

- [ ] Replace the current simple status bars with accessible content descriptions and a compact response-time/status legend.
- [ ] Add a biometric app lock using `BiometricPrompt`; keep the Keystore token encryption independent of the lock state.
- [ ] Review notification permission denial and notification-channel settings in the Settings screen.
- [ ] Revisit alert noise: the current worker follows the requested “any new down check” behavior, but a retry threshold or flap suppression is likely better for daily use.
- [ ] Enable release optimization and shrinking, then build and install a release APK so the R8 configuration and keep rules are exercised before distribution.

## Medium priority app work

- [ ] Add schedule presets such as hourly or daily once the backend exposes a schedule-preset API.
- [ ] Add a target detail screen once the product needs more than the dashboard history window.
- [ ] Add explicit certificate renewal handling so a renewed certificate resets the local reminder state immediately.
- [ ] Add a small worker diagnostics screen showing `lastRun`, last known state per target, and the last worker failure.
- [ ] Add an optional Glance widget only after the dashboard and worker behavior are stable.
- [ ] Add a testable clock and injectable notification sender so worker tests do not depend on wall-clock time or Android notification APIs.
- [ ] Decide whether a minimal local cache is worthwhile; offline database storage is currently out of scope.
- [ ] Add unit coverage for AlertWorker certificate-alert buckets, notification incident matching, ViewModel login/logout and expired-auth transitions, and repository HTTP error mapping.
- [ ] Split the growing MainActivity composables into focused dashboard, target-card, incident, and sheet files as the next feature work lands.

## Deliberately deferred

- [ ] On-device monitoring, FCM, Firebase, Play Store release, and multi-account support.
- [ ] Add configurable custom ntfy notifications once the backend exposes the required API.
- [ ] Add a manual check trigger for an individual target once the backend exposes the required API.

## Backend context to keep visible

- [ ] The server's `GET /targets` response can contain up to 1,500 checks per target; add pagination or a bounded history endpoint before large installations make dashboard refreshes expensive.
- [ ] Add auth rate limiting and consider replacing a long-lived shared token with a more constrained credential model.
- [ ] Add external heartbeat monitoring because the monitor cannot report its own outage.
- [ ] Keep the backend README and generated Swagger contract synchronized; the Android client should follow generated Swagger, not stale prose.
- [ ] Add a repository LICENSE if the project is intended for redistribution.

## UI and project follow-ups

- [ ] Keep app documentation current as setup, behavior, and release steps change.
- [ ] Expand automated UI coverage for dashboard behavior and notification navigation.
- [ ] Resolve remaining Android Studio/IDE warnings where practical.
